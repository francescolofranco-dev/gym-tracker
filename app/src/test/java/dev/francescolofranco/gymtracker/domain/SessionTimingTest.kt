package dev.francescolofranco.gymtracker.domain

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SessionTimingTest {
    @Test
    fun workoutStartsWhenDraftIsAccepted() {
        val session = session(started = 0, accepted = 120, ended = 720)
        assertEquals(Instant.EPOCH.plusSeconds(120), session.workoutStartedAt())
    }

    @Test
    fun legacySessionFallsBackToCreationTime() {
        val session = session(started = 30, accepted = null, ended = 90)
        assertEquals(Instant.EPOCH.plusSeconds(30), session.workoutStartedAt())
    }

    @Test
    fun durationExcludesDraftSetupTime() {
        val session = session(started = 0, accepted = 120, ended = 720)
        assertEquals(Duration.ofMinutes(10), session.workoutDuration())
    }

    @Test
    fun activeDurationUsesSuppliedNow() {
        val session = session(started = 0, accepted = 60, ended = null)
        assertEquals(Duration.ofMinutes(4), session.workoutDuration(Instant.EPOCH.plusSeconds(300)))
    }

    private fun session(started: Long, accepted: Long?, ended: Long?) = SessionEntity(
        id = 1,
        startedAt = Instant.EPOCH.plusSeconds(started),
        acceptedAt = accepted?.let { Instant.EPOCH.plusSeconds(it) },
        endedAt = ended?.let { Instant.EPOCH.plusSeconds(it) },
    )
}
