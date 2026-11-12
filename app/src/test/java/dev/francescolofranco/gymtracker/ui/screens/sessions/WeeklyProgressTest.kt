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

    @Test fun `counts today and previous six local dates across calendar weeks`() {
        val sessions = listOf(
            completed("2026-10-22T21:59:59Z"), // Before the window
            completed("2026-10-22T22:00:00Z"), // Friday midnight locally
            completed("2026-10-25T12:00:00Z"), // Previous calendar week
            completed("2026-10-28T10:00:00Z"),
            completed("2026-10-29T12:00:00Z"), // Now is included
            completed("2026-10-29T12:00:01Z"), // Future
        )
        assertEquals(4, completedInLastSevenDays(sessions, now, zone))
    }

    @Test fun `Monday keeps sessions from the preceding calendar week`() {
        assertEquals(1, completedInLastSevenDays(
            listOf(completed("2026-10-25T12:00:00Z")),
            Instant.parse("2026-10-26T12:00:00Z"), zone,
        ))
    }

    @Test fun `window advances at local midnight`() {
        val sessions = listOf(completed("2026-10-22T22:00:00Z"))
        assertEquals(1, completedInLastSevenDays(sessions, Instant.parse("2026-10-29T22:59:59Z"), zone))
        assertEquals(0, completedInLastSevenDays(sessions, Instant.parse("2026-10-29T23:00:00Z"), zone))
    }

    @Test fun `excludes drafts and unfinished workouts and uses accepted start`() {
        val real = completed("2026-10-28T10:00:00Z")
        assertEquals(1, completedInLastSevenDays(listOf(
            real.copy(acceptedAt = null), real.copy(endedAt = null),
            real.copy(startedAt = Instant.parse("2026-10-20T10:00:00Z")),
        ), now, zone))
    }

    @Test fun `window spans daylight savings transition without fixed day durations`() {
        val sunday = Instant.parse("2026-10-25T22:30:00Z")
        assertEquals(1, completedInLastSevenDays(listOf(
            completed("2026-10-18T22:00:00Z"), // Monday midnight before DST change
            completed("2026-10-18T21:59:59Z"),
        ), sunday, zone))
    }

    @Test fun `spring daylight savings uses local dates`() {
        assertEquals(1, completedInLastSevenDays(listOf(
            completed("2026-03-22T23:00:00Z"), // Monday midnight locally
            completed("2026-03-22T22:59:59Z"),
        ), Instant.parse("2026-03-29T20:00:00Z"), zone))
    }
}
