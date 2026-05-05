package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    repo: SessionRepository,
) : ViewModel() {

    val rows: StateFlow<List<ExerciseWithRecency>> = repo.observePickerExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
