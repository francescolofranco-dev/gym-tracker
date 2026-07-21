package dev.francescolofranco.gymtracker.data.db.projections

import dev.francescolofranco.gymtracker.domain.ExerciseSide
import java.time.Instant

/**
 * One committed set of an exercise, denormalised with the date of the session it belongs to.
 * The per-exercise progress screen reads the full ordered stream of these and derives every
 * metric in Kotlin (volume, estimated 1RM, top set, reps) — see
 * ui/screens/exercises/ExerciseProgress.kt.
 */
data class ExerciseSetRow(
    val sessionId: Long,
    val sessionStartedAt: Instant,
    val reps: Int,
    val kg: Double?,
    val setNumber: Int,
    val side: ExerciseSide = ExerciseSide.BOTH,
)
