package dev.francescolofranco.gymtracker.data.db.projections

import androidx.room.Embedded
import androidx.room.Relation
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import java.time.Instant

/** Past-session list row aggregates: cheap to compute in SQL, expensive to do in Kotlin. */
data class SessionSummary(
    @Embedded val session: SessionEntity,
    val exerciseCount: Int,
    val setCount: Int,
    val totalVolume: Double,
)

/** Active-session view: SessionExercise paired with its template Exercise and logged sets. */
data class SessionExerciseDetail(
    @Embedded val sessionExercise: SessionExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val setLogs: List<SetLogEntity>,
)

/** Exercise picker row: include last-used time so we can sort most-recent first. */
data class ExerciseWithRecency(
    @Embedded val exercise: ExerciseEntity,
    val lastUsedAt: Instant?,
)

/**
 * Flat row used for stats aggregation: one entry per logged set, joined with the parent
 * session and exercise so the volume math can be done in Kotlin without per-set queries.
 */
data class StatSetRow(
    val sessionId: Long,
    val sessionStartedAt: Instant,
    val exerciseId: Long,
    val exerciseName: String,
    val primaryMuscles: Set<dev.francescolofranco.gymtracker.domain.Muscle>,
    val secondaryMuscles: Set<dev.francescolofranco.gymtracker.domain.Muscle>,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    val side: dev.francescolofranco.gymtracker.domain.ExerciseSide,
    val reps: Int,
    val kg: Double?,
)
