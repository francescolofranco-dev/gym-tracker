package dev.francescolofranco.gymtracker.ui.screens.stats

import dev.francescolofranco.gymtracker.data.db.projections.StatSetRow
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import dev.francescolofranco.gymtracker.domain.Muscle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class VolumeMathTest {
    @Test
    fun bilateralSetCountsAsOneEffectiveDirectSet() {
        val chest = computeMuscleVolumes(listOf(row()))[Muscle.CHEST]!!
        assertEquals(1.0, chest.directSets, 1e-9)
        assertEquals(1.0, chest.effectiveSets, 1e-9)
    }

    @Test
    fun leftAndRightUnilateralSetsEqualOneEffectiveSet() {
        val rows = listOf(
            row(unilateral = true, side = ExerciseSide.LEFT),
            row(unilateral = true, side = ExerciseSide.RIGHT),
        )
        val chest = computeMuscleVolumes(rows)[Muscle.CHEST]!!
        assertEquals(1.0, chest.directSets, 1e-9)
        assertEquals(2_000.0, chest.totalVolumeKg, 1e-9)
    }

    @Test
    fun secondaryMuscleGetsHalfEffectiveCredit() {
        val triceps = computeMuscleVolumes(listOf(row(secondaries = setOf(Muscle.TRICEPS))))[Muscle.TRICEPS]!!
        assertEquals(1.0, triceps.indirectSets, 1e-9)
        assertEquals(0.5, triceps.effectiveSets, 1e-9)
        assertEquals(500.0, triceps.totalVolumeKg, 1e-9)
    }

    @Test
    fun selectedPeriodHasExpectedDuration() {
        val now = Instant.parse("2026-07-21T12:00:00Z")
        val range = periodRange(StatsPeriod.DAYS_28, now)
        assertEquals(Duration.ofDays(28), Duration.between(range.startInclusive, range.endExclusive))
    }

    @Test
    fun previousPeriodIsAdjacentAndSameLength() {
        val current = periodRange(StatsPeriod.DAYS_90, Instant.parse("2026-07-21T12:00:00Z"))
        val previous = previousOf(current)
        assertEquals(previous.endExclusive, current.startInclusive)
        assertEquals(
            Duration.between(current.startInclusive, current.endExclusive),
            Duration.between(previous.startInclusive, previous.endExclusive),
        )
    }

    @Test
    fun `twenty eight day workload is normalized to a weekly average`() {
        assertEquals(7.0, weeklyAverage(28.0, StatsPeriod.DAYS_28), 1e-9)
    }

    @Test
    fun `exercise progress compares best performance with previous equal period`() {
        val previous = listOf(row(kg = 100.0, reps = 10))
        val current = listOf(row(kg = 110.0, reps = 10))

        val signal = computeExerciseProgress(current, previous).single()

        assertEquals(10.0, signal.percentChange!!, 1e-9)
    }

    @Test
    fun `personal records are counted only inside selected period`() {
        val currentStart = Instant.parse("2026-07-14T12:00:00Z")
        val rows = listOf(
            row(sessionId = 1, startedAt = currentStart.minusSeconds(86_400), kg = 100.0),
            row(sessionId = 2, startedAt = currentStart.plusSeconds(86_400), kg = 110.0),
        )
        val range = DateRange(currentStart, currentStart.plusSeconds(7 * 86_400))

        val activity = personalRecordActivity(rows, range)

        assertEquals(2, activity.count) // Estimated 1RM and session-volume PRs.
        assertEquals(setOf(1L), activity.exerciseIds)
    }

    private fun row(
        unilateral: Boolean = false,
        side: ExerciseSide = ExerciseSide.BOTH,
        secondaries: Set<Muscle> = emptySet(),
        sessionId: Long = 1,
        startedAt: Instant = Instant.EPOCH,
        reps: Int = 10,
        kg: Double = 100.0,
    ) = StatSetRow(
        sessionId = sessionId,
        sessionStartedAt = startedAt,
        exerciseId = 1,
        exerciseName = "Press",
        primaryMuscles = setOf(Muscle.CHEST),
        secondaryMuscles = secondaries,
        isBodyweight = false,
        isUnilateral = unilateral,
        setNumber = 1,
        side = side,
        reps = reps,
        kg = kg,
    )
}
