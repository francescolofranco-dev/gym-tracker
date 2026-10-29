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
    override fun onResume() {
        super.onResume()
        // Refresh after returning from Android's overlay permission screen.
        if (TimerService.state.value is TimerState.Running) {
            TimerService.send(this, TimerService.ACTION_ENSURE)
        }
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
