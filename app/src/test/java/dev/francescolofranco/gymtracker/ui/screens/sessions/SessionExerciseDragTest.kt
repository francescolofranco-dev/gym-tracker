package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SessionExerciseDragTest {
    private val ids = listOf(11L, 22L, 33L, 44L)

    private fun detail(id: Long) = SessionExerciseDetail(
        sessionExercise = SessionExerciseEntity(
            id = id,
            sessionId = 1L,
            exerciseId = id,
            orderInSession = 0,
        ),
        exercise = ExerciseEntity(
            id = id,
            name = "Exercise $id",
            primaryMuscles = emptySet(),
            secondaryMuscles = emptySet(),
            targetSets = 3,
            repRangeMin = 8,
            repRangeMax = 12,
            isBodyweight = false,
            createdAt = Instant.EPOCH,
        ),
        setLogs = emptyList(),
    )

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

    @Test fun orderedExerciseDetailsAppliesPendingOrderImmediately() {
        val details = listOf(detail(11L), detail(22L), detail(33L))
        val reordered = orderedExerciseDetails(details, listOf(33L, 11L, 22L))
        assertEquals(listOf(33L, 11L, 22L), reordered.map { it.sessionExercise.id })
    }

    @Test fun orderedExerciseDetailsFallsBackWhenPendingOrderIsNullOrStale() {
        val details = listOf(detail(11L), detail(22L), detail(33L))
        assertEquals(details, orderedExerciseDetails(details, null))
        assertEquals(details, orderedExerciseDetails(details, listOf(33L, 11L)))
        assertEquals(details, orderedExerciseDetails(details, listOf(33L, 11L, 99L)))
    }

    @Test fun targetNudgeDirectionReflectsDropSideAndIgnoresSelfOrInactive() {
        assertEquals(1, targetNudgeDirection(draggedId = 11L, targetId = 22L, dropAfter = false, sectionId = 22L))
        assertEquals(-1, targetNudgeDirection(draggedId = 11L, targetId = 22L, dropAfter = true, sectionId = 22L))
        assertEquals(0, targetNudgeDirection(draggedId = 11L, targetId = 22L, dropAfter = true, sectionId = 11L))
        assertEquals(0, targetNudgeDirection(draggedId = 11L, targetId = 22L, dropAfter = true, sectionId = 33L))
        assertEquals(0, targetNudgeDirection(draggedId = null, targetId = 22L, dropAfter = false, sectionId = 22L))
    }
}
