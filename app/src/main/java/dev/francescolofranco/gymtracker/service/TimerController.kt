package dev.francescolofranco.gymtracker.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [TimerService] static API. Injectable so ViewModels stay testable.
 */
@Singleton
class TimerController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val state: StateFlow<TimerState> get() = TimerService.state

    fun reset() = TimerService.send(context, TimerService.ACTION_RESET)
    fun stop() = TimerService.send(context, TimerService.ACTION_STOP)
    fun ensureStarted() = TimerService.send(context, TimerService.ACTION_ENSURE)
}
