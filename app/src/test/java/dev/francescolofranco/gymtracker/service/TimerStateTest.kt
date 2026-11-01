package dev.francescolofranco.gymtracker.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    @Test
    fun runningTimerUsesMonotonicElapsedTime() {
        val state = TimerState.Running(baseElapsedRealtime = 1_000)
        assertEquals(4_000, state.elapsedMillis(now = 5_000))
    }

    @Test
    fun clockGoingBackCannotProduceNegativeElapsedTime() {
        val state = TimerState.Running(baseElapsedRealtime = 5_000)
        assertEquals(0, state.elapsedMillis(now = 4_000))
    }

    @Test
    fun stoppedTimerAlwaysReadsZero() {
        assertEquals(0, TimerState.Stopped.elapsedMillis(now = Long.MAX_VALUE))
        assertFalse(TimerState.Stopped.isRunning)
        assertTrue(TimerState.Running(0).isRunning)
    }

    @Test
    fun overlayHidesWhenAppIsActiveAndInFocus() {
        val running = TimerState.Running(baseElapsedRealtime = 1_000)
        assertTrue(TimerService.shouldShowOverlay(overlayEnabled = true, state = running, appInFocus = false))
        assertFalse(TimerService.shouldShowOverlay(overlayEnabled = true, state = running, appInFocus = true))
        assertFalse(TimerService.shouldShowOverlay(overlayEnabled = false, state = running, appInFocus = false))
        assertFalse(TimerService.shouldShowOverlay(overlayEnabled = true, state = TimerState.Stopped, appInFocus = false))
    }
}
