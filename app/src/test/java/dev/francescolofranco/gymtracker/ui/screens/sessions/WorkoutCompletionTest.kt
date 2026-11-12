package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionTest {
    private val pending = SetLogEntity(sessionExerciseId = 1, setNumber = 1)
    private val logged = pending.copy(reps = 10, loggedAt = Instant.EPOCH)

    private fun exercise(vararg sets: SetLogEntity, skipped: Boolean = false) = SessionExerciseDetail(
        sessionExercise = SessionExerciseEntity(
            sessionId = 1, exerciseId = 1, orderInSession = 0, isSkipped = skipped,
        ),
        exercise = ExerciseEntity(
            name = "Curl", primaryMuscles = emptySet(), secondaryMuscles = emptySet(),
            isBodyweight = false, createdAt = Instant.EPOCH,
            targetSets = 2, repRangeMin = 8, repRangeMax = 12,
        ),
        setLogs = sets.toList(),
    )

    @Test fun requiresEveryExerciseEvenWhenLastInOrderIsComplete() {
        assertFalse(listOf(exercise(pending), exercise(logged)).isWorkoutComplete())
        assertTrue(listOf(exercise(logged), exercise(logged)).isWorkoutComplete())
    }

    @Test fun draftsAndUnconfirmedSetsAreNotComplete() {
        assertFalse(listOf(exercise(pending.copy(reps = 10, kg = 20.0))).isWorkoutComplete())
        assertFalse(listOf(exercise(logged.copy(reps = null))).isWorkoutComplete())
    }

    @Test fun requiresBothUnilateralSidesAndReopensAfterUndo() {
        val left = logged.copy(side = ExerciseSide.LEFT)
        val right = pending.copy(side = ExerciseSide.RIGHT)
        assertFalse(listOf(exercise(left, right)).isWorkoutComplete())
        assertTrue(listOf(exercise(left, right.copy(reps = 10, loggedAt = Instant.EPOCH))).isWorkoutComplete())
        assertFalse(listOf(exercise(left, right)).isWorkoutComplete())
    }

    @Test fun skippedWorkDoesNotBlockLoggedWork() {
        assertTrue(listOf(exercise(logged, pending.copy(isSkipped = true)),
            exercise(pending, skipped = true)).isWorkoutComplete())
    }

    @Test fun emptyOrEntirelySkippedWorkoutsDoNotCelebrate() {
        assertFalse(emptyList<SessionExerciseDetail>().isWorkoutComplete())
        assertFalse(listOf(exercise()).isWorkoutComplete())
        assertFalse(listOf(exercise(logged), exercise()).isWorkoutComplete())
        assertFalse(listOf(exercise(pending.copy(isSkipped = true))).isWorkoutComplete())
        assertFalse(listOf(exercise(logged, skipped = true)).isWorkoutComplete())
    }
}
