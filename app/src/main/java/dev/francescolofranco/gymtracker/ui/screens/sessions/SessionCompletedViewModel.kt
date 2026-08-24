package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.RetryableViewModel
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class SessionCompletedContent(
    val session: SessionEntity?,
    val details: List<SessionExerciseDetail>,
    val unit: WeightUnit,
)

@HiltViewModel
class SessionCompletedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repo: SessionRepository,
    userPrefs: UserPrefs,
) : RetryableViewModel() {

    private val sessionId: Long = checkNotNull(savedState.get<Long>(SessionRoutes.COMPLETE_ARG))

    val content: StateFlow<Loadable<SessionCompletedContent>> = combine(
        repo.observeSession(sessionId),
        repo.observeExerciseDetails(sessionId),
        userPrefs.unit,
    ) { session, details, unit ->
        SessionCompletedContent(session, details, unit)
    }.asLoadableState(viewModelScope)
}
