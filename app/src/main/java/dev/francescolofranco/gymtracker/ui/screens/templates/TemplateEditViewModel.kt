package dev.francescolofranco.gymtracker.ui.screens.templates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.repository.ExerciseRepository
import dev.francescolofranco.gymtracker.data.repository.TemplateRepository
import dev.francescolofranco.gymtracker.ui.nav.TemplateRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateEditState(
    val name: String = "",
    val exercises: List<ExerciseEntity> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank() && exercises.isNotEmpty()
}

@HiltViewModel
class TemplateEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val templates: TemplateRepository,
    exercises: ExerciseRepository,
) : ViewModel() {

    private val templateId: Long = savedState.get<Long>(TemplateRoutes.EDIT_ARG) ?: 0L
    val isNew: Boolean = templateId <= 0L

    private val _state = MutableStateFlow(TemplateEditState())
    val state: StateFlow<TemplateEditState> = _state.asStateFlow()

    val availableExercises: StateFlow<List<ExerciseEntity>> = exercises.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch {
                templates.observeWithExercises(templateId).collect { ts ->
                    if (ts != null) {
                        _state.value = _state.value.copy(
                            name = ts.template.name,
                            exercises = ts.exercises,
                        )
                    }
                }
            }
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun addExercise(e: ExerciseEntity) {
        if (_state.value.exercises.any { it.id == e.id }) return
        _state.value = _state.value.copy(exercises = _state.value.exercises + e)
    }

    fun remove(exerciseId: Long) {
        _state.value = _state.value.copy(
            exercises = _state.value.exercises.filterNot { it.id == exerciseId },
        )
    }

    fun moveUp(exerciseId: Long) {
        val list = _state.value.exercises.toMutableList()
        val idx = list.indexOfFirst { it.id == exerciseId }
        if (idx > 0) {
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            _state.value = _state.value.copy(exercises = list)
        }
    }

    fun moveDown(exerciseId: Long) {
        val list = _state.value.exercises.toMutableList()
        val idx = list.indexOfFirst { it.id == exerciseId }
        if (idx >= 0 && idx < list.size - 1) {
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            _state.value = _state.value.copy(exercises = list)
        }
    }

    fun save() {
        val current = _state.value
        if (!current.isValid || current.saving) return
        viewModelScope.launch {
            _state.value = current.copy(saving = true)
            val ids = current.exercises.map { it.id }
            if (isNew) templates.create(current.name, ids)
            else templates.update(templateId, current.name, ids)
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }
}
