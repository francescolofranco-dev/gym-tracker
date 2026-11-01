package dev.francescolofranco.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.francescolofranco.gymtracker.ui.GymApp
import dev.francescolofranco.gymtracker.ui.theme.GymTrackerTheme
import dev.francescolofranco.gymtracker.service.TimerService
import dev.francescolofranco.gymtracker.service.TimerState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        TimerService.setAppInFocus(hasWindowFocus())
    }

    override fun onResume() {
        super.onResume()
        TimerService.setAppInFocus(true)
        // Refresh after returning from Android's overlay permission screen.
        if (TimerService.state.value is TimerState.Running) {
            TimerService.send(this, TimerService.ACTION_ENSURE)
        }
    }

    override fun onPause() {
        TimerService.setAppInFocus(false)
        super.onPause()
    }

    override fun onStop() {
        TimerService.setAppInFocus(false)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        TimerService.setAppInFocus(
            hasFocus && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackerTheme {
                GymApp()
            }
        }
    }
}
