package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.service.TimerController
import dev.francescolofranco.gymtracker.service.TimerState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TimerPillViewModel @Inject constructor(
    private val controller: TimerController,
) : ViewModel() {

    val state: StateFlow<TimerState> = controller.state

    /** Only reachable while the timer is running — the composable gates clicks accordingly. */
    fun reset() = controller.reset()
}
