package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseGroupingTest {

    @Test
    fun `groups follow the shared picker category order`() {
        val grouped = groupExercisesByCategory(
            listOf(
                exercise(5, "Plank", setOf(Muscle.CORE)),
                exercise(4, "Squat", setOf(Muscle.QUADS)),
                exercise(3, "Curl", setOf(Muscle.BICEPS)),
                exercise(2, "Pulldown", setOf(Muscle.LATS)),
                exercise(1, "Bench press", setOf(Muscle.CHEST)),
            ),
        )

        assertEquals(MuscleCategory.entries.toList(), grouped.keys.toList())
    }

    @Test
    fun `groups use broad categories and multi-primary exercise appears once`() {
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

        val grouped = groupExercisesByCategory(exercises)

        assertEquals(
            listOf(MuscleCategory.CHEST_AND_SHOULDERS, MuscleCategory.LEGS_AND_GLUTES),
            grouped.keys.toList(),
        )
        assertEquals(
            listOf(shoulderAndCalves.id, 3L),
            grouped.getValue(MuscleCategory.CHEST_AND_SHOULDERS).map { it.id },
        )
        assertEquals(1, grouped.values.flatten().count { it.id == shoulderAndCalves.id })
    }

    @Test
    fun `same lead muscle falls back to case-insensitive name then id`() {
        val grouped = groupExercisesByCategory(
            listOf(
                exercise(4, "Zulu press", setOf(Muscle.FRONT_DELTS, Muscle.CHEST)),
                exercise(3, "alpha press", setOf(Muscle.TRICEPS, Muscle.FRONT_DELTS)),
                exercise(2, "Same press", setOf(Muscle.FRONT_DELTS)),
                exercise(1, "Same press", setOf(Muscle.FRONT_DELTS)),
            ),
        )

        assertEquals(
            listOf(3L, 1L, 2L, 4L),
            grouped.getValue(MuscleCategory.CHEST_AND_SHOULDERS).map { it.id },
        )
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
