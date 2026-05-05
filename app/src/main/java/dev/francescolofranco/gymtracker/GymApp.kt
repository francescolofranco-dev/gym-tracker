package dev.francescolofranco.gymtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.francescolofranco.gymtracker.service.TimerService
import dev.francescolofranco.gymtracker.work.DailyBackupScheduler
import javax.inject.Inject

@HiltAndroidApp
class GymApp : Application() {

    @Inject lateinit var dailyBackupScheduler: DailyBackupScheduler

    override fun onCreate() {
        super.onCreate()
        TimerService.ensureChannel(this)
        dailyBackupScheduler.schedule()
    }
}
