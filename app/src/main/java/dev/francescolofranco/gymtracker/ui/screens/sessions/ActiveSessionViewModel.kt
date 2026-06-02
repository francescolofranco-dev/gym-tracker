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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SetHints(
    val byExerciseId: Map<Long, List<HintRow>> = emptyMap(),
)

data class HintRow(val setNumber: Int, val reps: Int?, val kg: Double?)

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: SessionRepository,
    private val idleScheduler: IdleSessionScheduler,
    private val timer: TimerController,
    userPrefs: UserPrefs,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedState.get<Long>(SessionRoutes.ACTIVE_ARG))

    val session: StateFlow<SessionEntity?> = repo.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val details: StateFlow<List<SessionExerciseDetail>> = repo.observeExerciseDetails(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unit: StateFlow<WeightUnit> = userPrefs.unit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    val keepScreenOn: StateFlow<Boolean> = userPrefs.keepScreenOnDuringSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Anchor hints to the LAST COMPLETED session preceding this one (not the global latest,
     * which was the source of the "% always resets to 0" bug — once we logged a set in the
     * active session, the previous-session lookup picked us as our own anchor).
     *
     * Combining in [observeAllSummaries] (as a coarse trigger) makes the flow re-emit when
     * any session is added, completed, or deleted. That's how the spec's "if I delete the most
     * recent session, hints should fall back to the next one" requirement is met without
     * plumbing a dedicated invalidation channel.
     */
    val hints: StateFlow<SetHints> = combine(
        session,
        details,
        repo.observeAllSummaries(),
    ) { s, items, _ ->
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

    private val _exitRequested = MutableStateFlow(false)
    val exitRequested: StateFlow<Boolean> = _exitRequested.asStateFlow()

    fun addExercise(exerciseId: Long) = viewModelScope.launch {
        repo.addExerciseToSession(sessionId, exerciseId)
    }

    fun removeSessionExercise(sessionExerciseId: Long) = viewModelScope.launch {
        repo.removeSessionExercise(sessionExerciseId)
    }

    /**
     * Swap the target session-exercise with its neighbour [delta] slots away. The screen has
     * the current ordered list so it can compute valid bounds before calling — passing a
     * delta that would go off the edge is treated as a no-op rather than an error.
     */
    fun moveSessionExercise(targetId: Long, delta: Int) = viewModelScope.launch {
        if (delta == 0) return@launch
        val ordered = details.value.map { it.sessionExercise.id }
        val idx = ordered.indexOf(targetId)
        val neighborIdx = idx + delta
        if (idx < 0 || neighborIdx !in ordered.indices) return@launch
        repo.swapSessionExerciseOrder(targetId, ordered[neighborIdx])
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

    fun logSet(setLogId: Long, reps: Int, kg: Double) = viewModelScope.launch {
        repo.logSet(setLogId, reps = reps, kg = kg)
        // First ✓ implies the user has actually started; auto-promote a draft so it stops
        // being hidden from the home screen.
        repo.acceptSession(sessionId)
        idleScheduler.schedule(sessionId)
        timer.reset()
    }

    /** Explicit "Start workout" button — promotes a draft session to a real in-progress one. */
    fun acceptSession() = viewModelScope.launch {
        repo.acceptSession(sessionId)
    }

    fun unlogSet(setLogId: Long) = viewModelScope.launch {
        repo.unlogSet(setLogId)
    }

    /**
     * Persist a value the user typed into the numpad / bumped on the stepper but didn't
     * ✓-commit. Two behaviours roll up into the same call:
     *  1. The row's reps+kg are always written so the in-progress value survives navigation
     *     and LazyColumn recycling.
     *  2. If [kgFromExplicitEntry] is true (user actually typed kg in the numpad) AND this is
     *     the FIRST kg in this exercise, propagate it to all the other pending sets — the
     *     auto-fill the user asked for. A stepper rep bump passes false here so the hint kg
     *     doesn't propagate just because the user touched the row.
     */
    fun saveSetDraft(
        sessionExerciseId: Long,
        setLogId: Long,
        reps: Int?,
        kg: Double?,
        kgFromExplicitEntry: Boolean,
    ) = viewModelScope.launch {
        // "First explicit kg" = the user typed kg in the numpad AND no OTHER set has kg yet.
        // We exclude the source row from the check because the row may already have a draft
        // kg from a prior stepper-only interaction (which wrote the hint kg as a default) —
        // that shouldn't burn the auto-fill trigger before the user has actually entered a
        // real weight.
        val isFirstKg = kgFromExplicitEntry &&
            kg != null && kg > 0.0 &&
            !repo.anyOtherSetHasKg(sessionExerciseId, excludeSetLogId = setLogId)
        // Save this row's reps+kg draft first so reps don't get blown away by the bulk apply.
        repo.saveSetDraft(setLogId, reps, kg)
        if (isFirstKg) {
            repo.applyKgToPendingSets(sessionExerciseId, kg = kg!!)
        }
    }

    fun toggleSetSkipped(setLogId: Long, currentlySkipped: Boolean) = viewModelScope.launch {
        repo.setSetSkipped(setLogId, !currentlySkipped)
    }

    fun endSession() = viewModelScope.launch {
        val ended = withContext(Dispatchers.Default) { repo.lastActivityAt(sessionId) ?: java.time.Instant.now() }
        repo.endSession(sessionId, ended)
        idleScheduler.cancel(sessionId)
        // Tear the timer notification down — no session means nothing to display.
        timer.stop()
        _exitRequested.value = true
    }
}
