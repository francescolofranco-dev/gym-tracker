package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PersonalRecordsTest {
    @Test
    fun firstSessionEstablishesBaselineWithoutBadge() {
        val points = aggregateSessions(listOf(row(1, 0, 8, 80.0)))
        assertTrue(detectPersonalRecords(points, false)[1L].orEmpty().isEmpty())
    }

    @Test
    fun strongerSessionEarnsEstimatedOneRmRecord() {
        val points = aggregateSessions(listOf(row(1, 0, 8, 80.0), row(2, 7, 8, 85.0)))
        assertTrue(PersonalRecordType.ESTIMATED_1RM in detectPersonalRecords(points, false)[2L].orEmpty())
    }

    @Test
    fun moreRepsAtSameWeightEarnsRepRecord() {
        val points = aggregateSessions(listOf(row(1, 0, 8, 80.0), row(2, 7, 10, 80.0)))
        assertTrue(PersonalRecordType.REPS_AT_WEIGHT in detectPersonalRecords(points, false)[2L].orEmpty())
    }

    @Test
    fun firstUseOfNewWeightIsNotCalledRepRecord() {
        val points = aggregateSessions(listOf(row(1, 0, 8, 80.0), row(2, 7, 8, 85.0)))
        val records = detectPersonalRecords(points, false)[2L].orEmpty()
        assertFalse(PersonalRecordType.REPS_AT_WEIGHT in records)
    }

    @Test
    fun bodyweightBestSetCreatesRepRecord() {
        val points = aggregateSessions(listOf(row(1, 0, 8, null), row(2, 7, 10, null)))
        val records = detectPersonalRecords(points, true)[2L].orEmpty()
        assertTrue(PersonalRecordType.BODYWEIGHT_REPS in records)
    }

    @Test
    fun unilateralRepRecordsCompareTheSameSide() {
        val points = aggregateSessions(
            listOf(
                row(1, 0, 12, 8.0, ExerciseSide.RIGHT),
                row(1, 0, 8, 8.0, ExerciseSide.LEFT),
                row(2, 7, 12, 8.0, ExerciseSide.RIGHT),
                row(2, 7, 10, 8.0, ExerciseSide.LEFT),
            ),
        )
        assertTrue(PersonalRecordType.REPS_AT_WEIGHT in detectPersonalRecords(points, false)[2L].orEmpty())
    }

    @Test
    fun summaryReturnsAllTimeMaxima() {
        val points = aggregateSessions(listOf(row(1, 0, 8, 80.0), row(2, 7, 10, 85.0)))
        val summary = personalRecordSummary(points)
        assertEquals(10, summary.bestReps)
        assertEquals(850.0, summary.bestVolumeKg, 1e-9)
    }

    private fun row(
        sessionId: Long,
        day: Long,
        reps: Int,
        kg: Double?,
        side: ExerciseSide = ExerciseSide.BOTH,
    ) = ExerciseSetRow(
        sessionId = sessionId,
        sessionStartedAt = Instant.EPOCH.plusSeconds(day * 86_400),
        reps = reps,
        kg = kg,
        setNumber = 1,
        side = side,
    )
}
