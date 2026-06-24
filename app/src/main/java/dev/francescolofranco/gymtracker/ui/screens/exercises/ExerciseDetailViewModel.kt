package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import dev.francescolofranco.gymtracker.data.repository.ExerciseRepository
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.nav.ExerciseRoutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: ExerciseRepository,
    userPrefs: UserPrefs,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedState.get<Long>(ExerciseRoutes.DETAIL_ARG))

    val exercise: StateFlow<ExerciseEntity?> = repo.observeById(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Per-session progress points (oldest first), derived from the full committed-set stream. Every
     * metric the detail screen shows — volume, estimated 1RM, top set, reps — comes off this one flow.
     */
    val progress: StateFlow<List<SessionProgressPoint>> = repo.observeSetHistory(exerciseId)
        .map { aggregateSessions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unit: StateFlow<WeightUnit> = userPrefs.unit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUnit.KG)

    /**
     * Save the edited form back into the exercise. Stats queries join through the exercise
     * table at read time so muscle reassignments retroactively re-attribute every historical
     * set — no DB migration needed for the "I mis-categorised this exercise" case.
     */
    fun save(state: ExerciseFormState) = viewModelScope.launch {
        if (state.primaryMuscles.isEmpty() || state.name.isBlank()) return@launch
        repo.update(
            id = exerciseId,
            name = state.name,
            primaryMuscles = state.primaryMuscles,
            secondaryMuscles = state.secondaryMuscles,
            targetSets = state.targetSets,
            repRangeMin = state.repRangeMin,
            repRangeMax = state.repRangeMax,
            isBodyweight = state.isBodyweight,
        )
    }
}
