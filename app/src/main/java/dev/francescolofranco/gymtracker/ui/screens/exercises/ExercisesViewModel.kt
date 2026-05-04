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

    val grouped: StateFlow<Map<Muscle, List<ExerciseEntity>>> =
        repo.observeActive()
            .map { list ->
                list.groupBy { it.primaryMuscle }
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

    fun closeForm() {
        _editing.value = EditMode.None
    }

    fun save(state: ExerciseFormState) {
        val primary = state.primaryMuscle ?: return
        viewModelScope.launch {
            repo.create(
                name = state.name,
                primaryMuscle = primary,
                secondaryMuscles = state.secondaryMuscles,
                targetSets = state.targetSets,
                repRangeMin = state.repRangeMin,
                repRangeMax = state.repRangeMax,
                isBodyweight = state.isBodyweight,
            )
            _editing.value = EditMode.None
        }
    }

    fun rename(id: Long, newName: String) = viewModelScope.launch {
        repo.rename(id, newName)
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
}
