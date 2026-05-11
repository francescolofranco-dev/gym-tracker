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
        // Timer is no longer kicked off here — it auto-resets on the first set check-off, so
        // the user can browse the empty session / pick exercises without the clock running.
        _events.emit(Event.OpenActive(id))
    }

    sealed interface Event {
        data class OpenActive(val sessionId: Long) : Event
    }
}
