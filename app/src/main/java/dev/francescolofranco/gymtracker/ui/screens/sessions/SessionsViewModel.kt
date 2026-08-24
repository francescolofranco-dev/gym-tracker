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
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.RetryableViewModel
import dev.francescolofranco.gymtracker.work.IdleSessionScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsContent(
    val active: SessionEntity?,
    val past: List<SessionSummary>,
    val unit: WeightUnit,
    val suggestion: TemplateEntity?,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val templates: TemplateRepository,
    private val idleScheduler: IdleSessionScheduler,
    private val timer: TimerController,
    userPrefs: UserPrefs,
) : RetryableViewModel() {

    @OptIn(FlowPreview::class)
    val content: StateFlow<Loadable<SessionsContent>> = combine(
        repo.observeActive(),
        repo.observeAllSummaries(),
        userPrefs.unit,
        templates.observeNextSuggestion(),
    ) { active, past, unit, suggestion ->
        SessionsContent(active, past, unit, suggestion)
    }.debounce(24L)
        .asLoadableState(viewModelScope)

    // Unlike a replay=0 SharedFlow, the channel retains navigation events while the Activity is
    // briefly being recreated, without replaying them after they have been handled.
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun startBlankSession() = startWithTemplate(templateId = null)

    fun startWithTemplate(templateId: Long?) = viewModelScope.launch {
        val existing = repo.activeSession()
        val id = existing?.id ?: repo.startSession(templateId = templateId)
        idleScheduler.schedule(id)
        // Stop (not reset) the timer so the active screen shows a clean 00:00 from the moment
        // the user lands. The first set check-off later flips state to Running. Resuming an
        // existing session keeps whatever timer state was previously running.
        if (existing == null) timer.stop()
        _events.send(Event.OpenActive(id))
    }

    /**
     * End the active session immediately — used by the in-list banner when the user wants to
     * clear a session that's lingering with `endedAt = null`. A user-triggered end always uses
     * the current time; only the idle worker backdates to the last activity.
     */
    fun endActiveSession() = viewModelScope.launch {
        val current = repo.activeSession() ?: return@launch
        repo.endSession(current.id, java.time.Instant.now())
        idleScheduler.cancel(current.id)
        timer.stop()
        _events.send(Event.OpenCompleted(current.id))
    }

    sealed interface Event {
        data class OpenActive(val sessionId: Long) : Event
        data class OpenCompleted(val sessionId: Long) : Event
    }
}
