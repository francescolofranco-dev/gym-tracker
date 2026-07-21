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

    private fun row(
        unilateral: Boolean = false,
        side: ExerciseSide = ExerciseSide.BOTH,
        secondaries: Set<Muscle> = emptySet(),
    ) = StatSetRow(
        sessionId = 1,
        sessionStartedAt = Instant.EPOCH,
        exerciseId = 1,
        exerciseName = "Press",
        primaryMuscles = setOf(Muscle.CHEST),
        secondaryMuscles = secondaries,
        isBodyweight = false,
        isUnilateral = unilateral,
        side = side,
        reps = 10,
        kg = 100.0,
    )
}
