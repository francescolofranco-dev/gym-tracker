package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import dev.francescolofranco.gymtracker.data.repository.TemplateRepository
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.service.TimerController
import dev.francescolofranco.gymtracker.work.IdleSessionScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val templates: TemplateRepository,
    private val idleScheduler: IdleSessionScheduler,
    private val timer: TimerController,
    userPrefs: UserPrefs,
) : ViewModel() {

    val active: StateFlow<SessionEntity?> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val past: StateFlow<List<SessionSummary>> = repo.observeAllSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unit: StateFlow<WeightUnit> = userPrefs.unit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    val suggestion: StateFlow<TemplateEntity?> = templates.observeNextSuggestion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun startBlankSession() = startWithTemplate(templateId = null)

    fun startWithTemplate(templateId: Long?) = viewModelScope.launch {
        val existing = repo.activeSession()
        val id = existing?.id ?: repo.startSession(templateId = templateId)
        idleScheduler.schedule(id)
        // Stop (not reset) the timer so the active screen shows a clean 00:00 from the moment
        // the user lands. The first set check-off later flips state to Running. Resuming an
        // existing session keeps whatever timer state was previously running.
        if (existing == null) timer.stop()
        _events.emit(Event.OpenActive(id))
    }

    /**
     * End the active session immediately — used by the in-list banner when the user wants to
     * clear a phantom session that's lingering with `endedAt = null`. End-time falls back to
     * last logged activity, or now if there was none.
     */
    fun endActiveSession() = viewModelScope.launch {
        val current = repo.activeSession() ?: return@launch
        val endAt = repo.lastActivityAt(current.id) ?: java.time.Instant.now()
        repo.endSession(current.id, endAt)
        idleScheduler.cancel(current.id)
        timer.stop()
    }

    sealed interface Event {
        data class OpenActive(val sessionId: Long) : Event
    }
}
