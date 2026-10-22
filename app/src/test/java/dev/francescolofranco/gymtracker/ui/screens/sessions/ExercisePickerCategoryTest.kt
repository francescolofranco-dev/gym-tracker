package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import dev.francescolofranco.gymtracker.ui.screens.exercises.groupExercisesByCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerCategoryTest {

    private val pullover = exercise(
        id = 1,
        name = "Pullover",
        primaries = setOf(Muscle.CHEST, Muscle.LATS),
    )
    private val squat = exercise(
        id = 2,
        name = "Back squat",
        primaries = setOf(Muscle.QUADS),
        secondaries = setOf(Muscle.CORE),
    )
    private val rows = listOf(pullover, squat).map { ExerciseWithRecency(it, null) }

    @Test
    fun `exercise is available from every category represented by a primary muscle`() {
        val chestResults = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.PUSH),
            "",
        )
        val pullResults = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.PULL),
            "",
        )

        assertEquals(listOf(pullover.id), chestResults.map { it.id })
        assertEquals(listOf(pullover.id), pullResults.map { it.id })
    }

    @Test
    fun `secondary muscles do not opt an exercise into a category`() {
        val results = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.CORE),
            "",
        )

        assertEquals(emptyList<ExerciseEntity>(), results)
    }

    @Test
    fun `only represented categories are offered and a selected empty category stays visible`() {
        assertEquals(
            listOf(
                MuscleCategory.PUSH,
                MuscleCategory.PULL,
                MuscleCategory.LEGS,
            ),
            availablePickerCategories(rows, selected = null),
        )
        assertEquals(
            listOf(
                MuscleCategory.PUSH,
                MuscleCategory.PULL,
                MuscleCategory.LEGS,
                MuscleCategory.CORE,
            ),
            availablePickerCategories(rows, selected = MuscleCategory.CORE),
        )
    }

    @Test
    fun `picker search uses the same split as the exercises tab for every muscle`() {
        val exercises = Muscle.entries.mapIndexed { index, muscle ->
            exercise(index.toLong(), "Test exercise ${muscle.name}", setOf(muscle))
        }
        val grouped = groupExercisesByCategory(exercises)
        val pickerRows = exercises.map { ExerciseWithRecency(it, null) }

        assertEquals(grouped.keys.toList(), availablePickerCategories(pickerRows, null))
        grouped.forEach { (category, expected) ->
            assertEquals(
                expected.map { it.id },
                filterPickerExercises(pickerRows, PickerFilter.ByCategory(category), "Test")
                    .map { it.id },
            )
        }
    }

    @Test
    fun `each muscle belongs to exactly one workout group`() {
        assertEquals(Muscle.entries.toSet(), MuscleCategory.entries.flatMap { it.muscles }.toSet())
        assertEquals(Muscle.entries.size, MuscleCategory.entries.sumOf { it.muscles.size })
        assertEquals(MuscleCategory.PUSH, MuscleCategory.containing(Muscle.TRICEPS))
        assertEquals(MuscleCategory.PULL, MuscleCategory.containing(Muscle.REAR_DELTS))
        assertEquals(MuscleCategory.LEGS, MuscleCategory.containing(Muscle.QUADS))
        assertEquals(MuscleCategory.CORE, MuscleCategory.containing(Muscle.CORE))
    }

    private fun exercise(
        id: Long,
        name: String,
        primaries: Set<Muscle>,
        secondaries: Set<Muscle> = emptySet(),
    ) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscles = primaries,
        secondaryMuscles = secondaries,
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        createdAt = Instant.EPOCH,
    )
}
