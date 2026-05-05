package dev.francescolofranco.gymtracker.data.db.projections

import java.time.Instant

/** One point on the per-exercise volume-over-time chart, plus what to show in the history list. */
data class ExerciseSessionPoint(
    val sessionId: Long,
    val sessionStartedAt: Instant,
    val sessionEndedAt: Instant?,
    val setsLogged: Int,
    val volumeKg: Double,
)
