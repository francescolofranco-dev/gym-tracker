package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseGroupingTest {

    @Test
    fun `groups run top to bottom and multi-primary exercise appears under highest muscle once`() {
        val shoulderAndCalves = exercise(
            id = 1,
            name = "Standing reach",
            primaries = linkedSetOf(Muscle.CALVES, Muscle.FRONT_DELTS),
        )
        val exercises = listOf(
            exercise(2, "Calf raise", setOf(Muscle.CALVES)),
            exercise(3, "Bench press", setOf(Muscle.CHEST)),
            shoulderAndCalves,
        )

        val grouped = groupExercisesTopToBottom(exercises)

        assertEquals(
            listOf(Muscle.FRONT_DELTS, Muscle.CHEST, Muscle.CALVES),
            grouped.keys.toList(),
        )
        assertEquals(listOf(shoulderAndCalves.id), grouped.getValue(Muscle.FRONT_DELTS).map { it.id })
        assertEquals(1, grouped.values.flatten().count { it.id == shoulderAndCalves.id })
    }

    @Test
    fun `same lead muscle falls back to case-insensitive name then id`() {
        val grouped = groupExercisesTopToBottom(
            listOf(
                exercise(4, "Zulu press", setOf(Muscle.FRONT_DELTS, Muscle.CHEST)),
                exercise(3, "alpha press", setOf(Muscle.TRICEPS, Muscle.FRONT_DELTS)),
                exercise(2, "Same press", setOf(Muscle.FRONT_DELTS)),
                exercise(1, "Same press", setOf(Muscle.FRONT_DELTS)),
            ),
        )

        assertEquals(listOf(3L, 1L, 2L, 4L), grouped.getValue(Muscle.FRONT_DELTS).map { it.id })
    }

    private fun exercise(
        id: Long,
        name: String,
        primaries: Set<Muscle>,
    ) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscles = primaries,
        secondaryMuscles = emptySet(),
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        createdAt = Instant.EPOCH,
    )
}
