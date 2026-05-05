package dev.francescolofranco.gymtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.francescolofranco.gymtracker.service.TimerService

@HiltAndroidApp
class GymApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TimerService.ensureChannel(this)
    }
}
