package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.service.TimerController
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.RetryableViewModel
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import dev.francescolofranco.gymtracker.work.IdleSessionScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant

data class SessionDetailContent(
    val session: SessionEntity?,
    val details: List<SessionExerciseDetail>,
    val unit: WeightUnit,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: SessionRepository,
    private val timer: TimerController,
    private val idleScheduler: IdleSessionScheduler,
    userPrefs: UserPrefs,
) : RetryableViewModel() {

    private val sessionId: Long = checkNotNull(savedState.get<Long>(SessionRoutes.DETAIL_ARG))

    val content: StateFlow<Loadable<SessionDetailContent>> = combine(
        repo.observeSession(sessionId),
        repo.observeExerciseDetails(sessionId),
        userPrefs.unit,
    ) { session, details, unit ->
        SessionDetailContent(session, details, unit)
    }.asLoadableState(viewModelScope)

    val details: StateFlow<List<SessionExerciseDetail>> = content
        .map { (it as? Loadable.Ready)?.value?.details.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mergeCandidates = repo.observeAllSummaries()
        .map { summaries -> summaries.filter { it.session.id != sessionId && it.session.endedAt != null } }
        .asLoadableState(viewModelScope)

    private val messages = Channel<String>(Channel.BUFFERED)
    val editMessages = messages.receiveAsFlow()

    /**
     * Past-session deltas compare against the session immediately BEFORE the one being viewed,
     * not the globally-latest. Anchor the hint query to this session's [startedAt] so the
     * percentage on a 3-month-old workout reflects the user's progress at that time.
     *
     * The lightweight session-table observer refreshes the anchor after another session is
     * deleted, without re-running this lookup on every set edit.
     */
    private val hintRequest = content
        .map { loadable ->
            val ready = (loadable as? Loadable.Ready)?.value
            ready?.session?.startedAt to ready?.details.orEmpty()
                .groupBy { it.exercise.id }
                .toSortedMap()
                .map { (exerciseId, occurrences) ->
                    exerciseId to occurrences
                        .flatMap { detail -> detail.setLogs.map { it.setNumber to it.side } }
                        .distinct()
                        .sortedWith(compareBy({ it.first }, { it.second.ordinal }))
                }
        }
        .distinctUntilChanged()

    val hints: StateFlow<SetHints> = combine(
        hintRequest,
        repo.observeAllSessions(),
    ) { (anchor, exercises), _ ->
        anchor ?: return@combine SetHints()
        val byId = HashMap<Long, List<HintRow>>()
        exercises.forEach { (exerciseId, sets) ->
            byId[exerciseId] = sets.mapNotNull { (setNumber, side) ->
                repo.lastLoggedSetBefore(exerciseId, setNumber, side, anchor)?.let {
                    HintRow(setNumber, side, it.reps, it.kg)
                }
            }
        }
        SetHints(byId)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetHints())

    fun logSet(setLogId: Long, reps: Int, kg: Double) = viewModelScope.launch {
        repo.logSet(setLogId, reps = reps, kg = kg)
    }

    fun unlogSet(setLogId: Long) = viewModelScope.launch {
        repo.unlogSet(setLogId)
    }

    /**
     * Persist a typed-but-uncommitted reps/kg so the value survives navigation. On a past
     * session the auto-fill bulk apply isn't useful (you're not building the session, just
     * editing history), so we always go through the single-row draft path and ignore the
     * kgFromExplicitEntry flag.
     */
    @Suppress("UNUSED_PARAMETER")
    fun saveSetDraft(
        sessionExerciseId: Long,
        setLogId: Long,
        reps: Int?,
        kg: Double?,
        kgFromExplicitEntry: Boolean,
    ) = viewModelScope.launch {
        repo.saveSetDraft(setLogId, reps, kg)
    }

    fun toggleSetSkipped(setLogId: Long, currentlySkipped: Boolean) = viewModelScope.launch {
        repo.setSetSkipped(setLogId, !currentlySkipped)
    }

    fun setExerciseSkipped(sessionExerciseId: Long, skipped: Boolean) = viewModelScope.launch {
        repo.setSessionExerciseSkipped(sessionExerciseId, skipped)
    }

    fun reorderExercises(ids: List<Long>) = viewModelScope.launch {
        repo.reorderSessionExercises(ids)
    }

    fun setExerciseNote(
        sessionExerciseId: Long,
        notes: String?,
        pinnedNotes: String?,
    ) = viewModelScope.launch {
        repo.updateSessionExerciseNote(sessionExerciseId, notes, pinnedNotes)
    }

    fun setSessionNotes(notes: String?) = viewModelScope.launch {
        repo.updateSessionNotes(sessionId, notes?.ifBlank { null })
    }

    fun setSessionTiming(start: Instant, end: Instant) = viewModelScope.launch {
        repo.updateSessionTiming(sessionId, start, end)
    }

    fun addExercises(exerciseIds: List<Long>) = editSession("Couldn’t add the selected exercises.") {
        repo.addExercisesToCompletedSession(sessionId, exerciseIds)
    }

    fun removeExercise(sessionExerciseId: Long) = editSession("Couldn’t remove this exercise.") {
        repo.removeExerciseFromCompletedSession(sessionId, sessionExerciseId)
    }

    fun mergeSession(sourceId: Long) = editSession("Couldn’t merge these sessions.") {
        repo.mergeCompletedSessions(sessionId, sourceId)
        messages.send("Sessions merged.")
    }

    private fun editSession(failureMessage: String, change: suspend () -> Unit) = viewModelScope.launch {
        try {
            change()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            messages.send(failureMessage)
        }
    }

    suspend fun deleteSession() {
        val wasActive = repo.deleteSession(sessionId)
        if (wasActive) {
            // Deleting the still-active session tears down both the idle worker and the
            // timer notification — they have nothing left to tick against.
            idleScheduler.cancel(sessionId)
            timer.stop()
        }
    }
}
