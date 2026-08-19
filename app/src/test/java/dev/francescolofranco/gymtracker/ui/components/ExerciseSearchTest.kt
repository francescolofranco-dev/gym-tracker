package dev.francescolofranco.gymtracker.ui.components

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchTest {

    @Test
    fun `blank query matches every exercise`() {
        assertTrue(benchPress().matchesExerciseQuery("   "))
    }

    @Test
    fun `name matching ignores case accents and extra whitespace`() {
        val exercise = benchPress(name = "Développé couché")

        assertTrue(exercise.matchesExerciseQuery("  DEVELOPPE   couche "))
    }

    @Test
    fun `query terms can match across name and muscles`() {
        assertTrue(benchPress().matchesExerciseQuery("barbell chest"))
        assertTrue(benchPress().matchesExerciseQuery("press triceps"))
    }

    @Test
    fun `bodyweight and unilateral aliases are searchable`() {
        val splitSquat = exercise(
            name = "Bulgarian split squat",
            primaryMuscles = setOf(Muscle.QUADS, Muscle.GLUTES),
            isBodyweight = true,
            isUnilateral = true,
        )

        assertTrue(splitSquat.matchesExerciseQuery("bw single side"))
    }

    @Test
    fun `all query terms must match`() {
        assertFalse(benchPress().matchesExerciseQuery("barbell hamstrings"))
    }

    private fun benchPress(name: String = "Barbell bench press") = exercise(
        name = name,
        primaryMuscles = setOf(Muscle.CHEST),
        secondaryMuscles = setOf(Muscle.TRICEPS, Muscle.FRONT_DELTS),
    )

    private fun exercise(
        name: String,
        primaryMuscles: Set<Muscle>,
        secondaryMuscles: Set<Muscle> = emptySet(),
        isBodyweight: Boolean = false,
        isUnilateral: Boolean = false,
    ) = ExerciseEntity(
        id = 1,
        name = name,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = isBodyweight,
        isUnilateral = isUnilateral,
        createdAt = Instant.EPOCH,
    )
}
