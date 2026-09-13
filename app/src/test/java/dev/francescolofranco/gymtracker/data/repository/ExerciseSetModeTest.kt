package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExerciseSetModeTest {
    private fun row(id: Long = 1, number: Int = 1, side: ExerciseSide = ExerciseSide.BOTH) =
        SetLogEntity(id = id, sessionExerciseId = 10, setNumber = number, side = side)

    @Test fun emptyBilateralSetsBecomePairsWithoutInventingValues() {
        val result = reconcileSetMode((1..3).map { row(it.toLong(), it) }, true)
        assertEquals(6, result.size)
        result.groupBy { it.setNumber }.forEach { (number, pair) ->
            assertEquals(listOf(ExerciseSide.LEFT, ExerciseSide.RIGHT), pair.map { it.side })
            assertEquals(number.toLong(), pair[0].id)
            assertEquals(0L, pair[1].id)
            assertTrue(pair.all { it.reps == null && it.kg == null && it.loggedAt == null })
        }
    }

    @Test fun draftsCompletedAndSkippedSetsKeepTheirMeaning() {
        val protected = listOf(
            row(1, 1).copy(reps = 8),
            row(2, 2).copy(kg = 0.0),
            row(3, 3).copy(reps = 10, kg = 20.0, loggedAt = Instant.EPOCH),
            row(4, 4).copy(isSkipped = true),
            row(5, 5).copy(loggedAt = Instant.EPOCH),
        )
        val result = reconcileSetMode(protected + row(6, 6), true)
        assertEquals(protected, result.take(5))
        assertEquals(7, result.size)
    }

    @Test fun switchingBackOnlyCollapsesUntouchedCompletePairs() {
        val left = row(side = ExerciseSide.LEFT)
        val right = row(2, side = ExerciseSide.RIGHT)
        assertEquals(listOf(row()), reconcileSetMode(listOf(left, right), false))
        for (entered in listOf(left.copy(reps = 8), left.copy(kg = 10.0), left.copy(isSkipped = true))) {
            val pair = listOf(entered, right)
            assertEquals(pair, reconcileSetMode(pair, false))
        }
        assertEquals(listOf(left), reconcileSetMode(listOf(left), false))
    }

    @Test fun repeatedConversionDoesNotAddDuplicateSides() {
        val converted = reconcileSetMode(listOf(row()), true)
        assertEquals(converted, reconcileSetMode(converted, true))
        assertEquals(listOf(row()), reconcileSetMode(reconcileSetMode(converted, false), false))
    }

    @Test fun occurrencesWithTheSameSetNumberAreIndependent() {
        val entered = row().copy(reps = 8)
        val empty = row(2).copy(sessionExerciseId = 11)
        val result = reconcileSetMode(listOf(entered, empty), true)
        assertEquals(entered, result.first())
        assertEquals(2, result.count { it.sessionExerciseId == 11L })
    }

    @Test fun unexpectedMixedGroupsArePreserved() {
        val mixed = listOf(row(), row(2, side = ExerciseSide.LEFT), row(3, side = ExerciseSide.RIGHT))
        assertEquals(mixed, reconcileSetMode(mixed, true))
        assertEquals(mixed, reconcileSetMode(mixed, false))
    }
}
