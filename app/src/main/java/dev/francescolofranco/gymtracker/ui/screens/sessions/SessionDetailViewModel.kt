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
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import dev.francescolofranco.gymtracker.work.IdleSessionScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: SessionRepository,
    private val timer: TimerController,
    private val idleScheduler: IdleSessionScheduler,
    userPrefs: UserPrefs,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedState.get<Long>(SessionRoutes.DETAIL_ARG))

    val session: StateFlow<SessionEntity?> = repo.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val details: StateFlow<List<SessionExerciseDetail>> = repo.observeExerciseDetails(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unit: StateFlow<WeightUnit> = userPrefs.unit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    /**
     * Past-session deltas compare against the session immediately BEFORE the one being viewed,
     * not the globally-latest. Anchor the hint query to this session's [startedAt] so the
     * percentage on a 3-month-old workout reflects the user's progress at that time.
     */
    val hints: StateFlow<SetHints> = combine(session, details) { s, items ->
        val anchor = s?.startedAt ?: return@combine SetHints()
        val byId = HashMap<Long, List<HintRow>>()
        items.forEach { d ->
            if (byId[d.exercise.id] == null) {
                val sets = repo.lastSessionSetsBefore(d.exercise.id, anchor, d.exercise.targetSets)
                byId[d.exercise.id] = sets.map { HintRow(it.setNumber, it.reps, it.kg) }
            }
        }
        SetHints(byId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetHints())

    fun logSet(setLogId: Long, reps: Int, kg: Double) = viewModelScope.launch {
        repo.logSet(setLogId, reps = reps, kg = kg)
    }

    fun unlogSet(setLogId: Long) = viewModelScope.launch {
        repo.unlogSet(setLogId)
    }

    /**
     * Persist a typed-but-uncommitted reps/kg so the value survives navigation. On a past
     * session the auto-fill bulk apply isn't useful (you're not building the session, just
     * editing history), so we always go through the single-row draft path.
     */
    fun saveSetDraft(sessionExerciseId: Long, setLogId: Long, reps: Int?, kg: Double?) = viewModelScope.launch {
        // sessionExerciseId is accepted for parity with the active VM's signature; unused here
        // because past-session detail shouldn't trigger the "first kg" auto-fill.
        @Suppress("UNUSED_VARIABLE") val _se = sessionExerciseId
        repo.saveSetDraft(setLogId, reps, kg)
    }

    fun toggleSetSkipped(setLogId: Long, currentlySkipped: Boolean) = viewModelScope.launch {
        repo.setSetSkipped(setLogId, !currentlySkipped)
    }

    fun setExerciseSkipped(sessionExerciseId: Long, skipped: Boolean) = viewModelScope.launch {
        repo.setSessionExerciseSkipped(sessionExerciseId, skipped)
    }

    fun setExerciseNotes(sessionExerciseId: Long, notes: String?) = viewModelScope.launch {
        repo.updateSessionExerciseNotes(sessionExerciseId, notes?.ifBlank { null })
    }

    fun setSessionNotes(notes: String?) = viewModelScope.launch {
        repo.updateSessionNotes(sessionId, notes?.ifBlank { null })
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
