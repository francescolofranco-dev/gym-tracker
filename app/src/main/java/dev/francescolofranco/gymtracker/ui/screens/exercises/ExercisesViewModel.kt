package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.repository.ExerciseRepository
import dev.francescolofranco.gymtracker.domain.Muscle
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
) : ViewModel() {

    /**
     * Groups exercises under their "lead" primary muscle — the first primary in [Muscle] enum
     * order. Multi-primary exercises only appear once (under their lead) rather than being
     * duplicated across every group they touch, which would clutter the list.
     */
    val grouped: StateFlow<Map<Muscle, List<ExerciseEntity>>> =
        repo.observeActive()
            .map { list ->
                list.groupBy { ex -> ex.primaryMuscles.minByOrNull { it.ordinal } ?: Muscle.CORE }
                    .toSortedMap(compareBy { it.ordinal })
                    .mapValues { (_, items) -> items.sortedBy { it.name.lowercase() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _editing = MutableStateFlow<EditMode>(EditMode.None)
    val editing: StateFlow<EditMode> = _editing.asStateFlow()

    fun openCreate() {
        _editing.value = EditMode.Create(ExerciseFormState())
    }

    fun openDuplicate(source: ExerciseEntity) {
        _editing.value = EditMode.Create(
            initial = source.toFormState().copy(name = "${source.name} (copy)"),
            isDuplicate = true,
        )
    }

    fun openEdit(source: ExerciseEntity) {
        _editing.value = EditMode.Edit(
            exerciseId = source.id,
            initial = source.toFormState(),
        )
    }

    fun closeForm() {
        _editing.value = EditMode.None
    }

    fun save(state: ExerciseFormState) {
        if (state.primaryMuscles.isEmpty()) return
        val mode = _editing.value
        viewModelScope.launch {
            when (mode) {
                is EditMode.Edit -> repo.update(
                    id = mode.exerciseId,
                    name = state.name,
                    primaryMuscles = state.primaryMuscles,
                    secondaryMuscles = state.secondaryMuscles,
                    targetSets = state.targetSets,
                    repRangeMin = state.repRangeMin,
                    repRangeMax = state.repRangeMax,
                    isBodyweight = state.isBodyweight,
                )
                else -> repo.create(
                    name = state.name,
                    primaryMuscles = state.primaryMuscles,
                    secondaryMuscles = state.secondaryMuscles,
                    targetSets = state.targetSets,
                    repRangeMin = state.repRangeMin,
                    repRangeMax = state.repRangeMax,
                    isBodyweight = state.isBodyweight,
                )
            }
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

sealed interface EditMode {
    data object None : EditMode
    data class Create(
        val initial: ExerciseFormState,
        val isDuplicate: Boolean = false,
    ) : EditMode
    data class Edit(
        val exerciseId: Long,
        val initial: ExerciseFormState,
    ) : EditMode
}
