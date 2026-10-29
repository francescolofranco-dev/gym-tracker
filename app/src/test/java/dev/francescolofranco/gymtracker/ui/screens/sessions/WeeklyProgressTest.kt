package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WeeklyProgressTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val now = Instant.parse("2026-10-29T12:00:00Z")
    private fun completed(at: String) = SessionEntity(
        startedAt = Instant.parse(at), acceptedAt = Instant.parse(at),
        endedAt = Instant.parse(at).plusSeconds(1800),
    )

    @Test fun `counts completed workouts within local Monday week only`() {
        val sessions = listOf(
            completed("2026-10-25T22:59:59Z"), // Sunday locally
            completed("2026-10-25T23:00:00Z"), // Monday locally
            completed("2026-10-28T10:00:00Z"),
            completed("2026-11-01T23:00:00Z"), // Next week
            completed("2026-10-29T14:00:00Z"), // Future
        )
        assertEquals(2, completedThisWeek(sessions, now, zone))
    }

    @Test fun `excludes drafts and unfinished workouts and uses accepted start`() {
        val real = completed("2026-10-28T10:00:00Z")
        assertEquals(1, completedThisWeek(listOf(
            real.copy(acceptedAt = null), real.copy(endedAt = null),
            real.copy(startedAt = Instant.parse("2026-10-20T10:00:00Z")),
        ), now, zone))
    }

    @Test fun `week spans daylight savings transition without fixed day durations`() {
        val sunday = Instant.parse("2026-10-25T22:30:00Z")
        assertEquals(1, completedThisWeek(listOf(
            completed("2026-10-18T22:00:00Z"), // Monday midnight before DST change
            completed("2026-10-18T21:59:59Z"),
        ), sunday, zone))
    }
}
