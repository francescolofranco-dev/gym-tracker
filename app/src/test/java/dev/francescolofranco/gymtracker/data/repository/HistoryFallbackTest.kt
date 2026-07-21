package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.domain.ExerciseSide
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFallbackTest {

    @Test
    fun `left side falls back to legacy bilateral history`() {
        assertEquals(
            listOf(ExerciseSide.LEFT, ExerciseSide.BOTH),
            historyLookupSides(ExerciseSide.LEFT),
        )
    }

    @Test
    fun `right side falls back to legacy bilateral history`() {
        assertEquals(
            listOf(ExerciseSide.RIGHT, ExerciseSide.BOTH),
            historyLookupSides(ExerciseSide.RIGHT),
        )
    }

    @Test
    fun `bilateral history never borrows one side`() {
        assertEquals(
            listOf(ExerciseSide.BOTH),
            historyLookupSides(ExerciseSide.BOTH),
        )
    }
}
