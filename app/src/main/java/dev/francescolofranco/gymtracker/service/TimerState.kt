package dev.francescolofranco.gymtracker.service

import android.os.SystemClock

/**
 * Single source of truth for the workout timer. The base time is anchored to
 * [SystemClock.elapsedRealtime] (monotonic, immune to wall-clock changes).
 *
 * STOPPED — counter shows 00:00; the service stays alive so the QS tile / FAB / notification
 * stay snappy without re-binding.
 *
 * RUNNING — counter ticks from `baseElapsedRealtime`.
 */
sealed interface TimerState {
    data object Stopped : TimerState
    data class Running(val baseElapsedRealtime: Long) : TimerState

    fun elapsedMillis(now: Long = SystemClock.elapsedRealtime()): Long = when (this) {
        Stopped -> 0L
        is Running -> (now - baseElapsedRealtime).coerceAtLeast(0)
    }

    val isRunning: Boolean get() = this is Running

    companion object {
        fun runningNow(): Running = Running(SystemClock.elapsedRealtime())
    }
}
