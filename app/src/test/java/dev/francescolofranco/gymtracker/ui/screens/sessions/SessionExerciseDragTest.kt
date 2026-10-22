package dev.francescolofranco.gymtracker.ui.screens.sessions

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionExerciseDragTest {
    private val ids = listOf(11L, 22L, 33L, 44L)

    @Test fun dragDownInsertsAfterTargetAndPreservesOtherExercises() {
        assertEquals(listOf(22L, 33L, 44L, 11L), reorderedExerciseIds(ids, 11L, 44L))
    }

    @Test fun dragUpInsertsBeforeTargetAndPreservesOtherExercises() {
        assertEquals(listOf(44L, 11L, 22L, 33L), reorderedExerciseIds(ids, 44L, 11L))
    }

    @Test fun cancelledOrStaleDragLeavesOrderUnchanged() {
        assertEquals(ids, reorderedExerciseIds(ids, null, 22L))
        assertEquals(ids, reorderedExerciseIds(ids, 11L, 99L))
        assertEquals(ids, reorderedExerciseIds(ids, 22L, 22L))
    }
}
