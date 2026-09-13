package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSetPlanTest {
    private fun detail(unilateral: Boolean, vararg sides: ExerciseSide) = SessionExerciseDetail(
        sessionExercise = SessionExerciseEntity(sessionId = 1, exerciseId = 1, orderInSession = 0),
        exercise = ExerciseEntity(name = "Curl", primaryMuscles = emptySet(),
            secondaryMuscles = emptySet(), isBodyweight = false, createdAt = java.time.Instant.EPOCH,
            targetSets = 5, repRangeMin = 8, repRangeMax = 12, isUnilateral = unilateral),
        setLogs = sides.mapIndexed { index, side ->
            SetLogEntity(sessionExerciseId = 1, setNumber = index + 1, side = side)
        },
    )

    @Test fun historicalBilateralRowsIgnoreNewUnilateralDefaultAndTargetCount() {
        assertEquals("2×8–12 · bilateral", detail(true, ExerciseSide.BOTH, ExerciseSide.BOTH).setPlanLabel())
    }

    @Test fun historicalUnilateralRowsIgnoreNewBilateralDefault() {
        assertEquals("1×8–12/side", detail(false, ExerciseSide.LEFT, ExerciseSide.RIGHT).setPlanLabel())
    }

    @Test fun mixedSessionExplainsBothAndSeparateSides() {
        assertEquals("1 B (both) + 1 L + 1 R · 8–12 reps",
            detail(true, ExerciseSide.BOTH, ExerciseSide.LEFT, ExerciseSide.RIGHT).setPlanLabel())
    }

    @Test fun incompletePairsDoNotClaimEqualSetsPerSide() {
        assertEquals("1 L · 8–12 reps", detail(true, ExerciseSide.LEFT).setPlanLabel())
        assertEquals("No sets", detail(true).setPlanLabel())
    }
}
