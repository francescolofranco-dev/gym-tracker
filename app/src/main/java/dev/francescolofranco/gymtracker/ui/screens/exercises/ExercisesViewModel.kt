package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.repository.ExerciseRepository
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import dev.francescolofranco.gymtracker.ui.components.ExerciseTopToBottomComparator
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.RetryableViewModel
import dev.francescolofranco.gymtracker.ui.components.anatomicalLeadMuscle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repo: ExerciseRepository,
) : RetryableViewModel() {

    /**
     * Groups exercises under the broad category of their anatomically highest primary muscle.
     * Multi-primary exercises only appear once rather than being duplicated across categories.
     */
    val grouped: StateFlow<Loadable<Map<MuscleCategory, List<ExerciseEntity>>>> =
        repo.observeActive()
            .map(::groupExercisesByCategory)
            .asLoadableState(viewModelScope)

    private val _editing = MutableStateFlow<EditMode>(EditMode.None)
    val editing: StateFlow<EditMode> = _editing.asStateFlow()

    fun openCreate() {
        _editing.value = EditMode.Create(ExerciseFormState())
    }

    fun closeForm() {
        _editing.value = EditMode.None
    }

    fun save(state: ExerciseFormState) {
        if (state.primaryMuscles.isEmpty()) return
        viewModelScope.launch {
            repo.create(
                name = state.name,
                primaryMuscles = state.primaryMuscles,
                secondaryMuscles = state.secondaryMuscles,
                targetSets = state.targetSets,
                repRangeMin = state.repRangeMin,
                repRangeMax = state.repRangeMax,
                isBodyweight = state.isBodyweight,
                isUnilateral = state.isUnilateral,
            )
            _editing.value = EditMode.None
        }
    }

    fun softDelete(id: Long) = viewModelScope.launch {
        repo.softDelete(id)
    }

    fun restore(id: Long) = viewModelScope.launch {
        repo.restore(id)
    }
}

internal fun groupExercisesByCategory(
    exercises: List<ExerciseEntity>,
): Map<MuscleCategory, List<ExerciseEntity>> = exercises
    .groupBy { exercise ->
        exercise.anatomicalLeadMuscle()
            ?.let(MuscleCategory::containing)
            ?: MuscleCategory.CORE
    }
    .toSortedMap(compareBy { it.ordinal })
    .mapValues { (_, items) -> items.sortedWith(ExerciseTopToBottomComparator) }

sealed interface EditMode {
    data object None : EditMode
    data class Create(val initial: ExerciseFormState) : EditMode
}
