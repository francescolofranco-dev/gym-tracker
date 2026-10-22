package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
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
            PickerFilter.ByCategory(PickerCategory.PUSH),
            "",
        )
        val pullResults = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(PickerCategory.PULL),
            "",
        )

        assertEquals(listOf(pullover.id), chestResults.map { it.id })
        assertEquals(listOf(pullover.id), pullResults.map { it.id })
    }

    @Test
    fun `secondary muscles do not opt an exercise into a category`() {
        val results = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(PickerCategory.CORE),
            "",
        )

        assertEquals(emptyList<ExerciseEntity>(), results)
    }

    @Test
    fun `only represented categories are offered and a selected empty category stays visible`() {
        assertEquals(
            listOf(
                PickerCategory.PUSH,
                PickerCategory.PULL,
                PickerCategory.LEGS,
            ),
            availablePickerCategories(rows, selected = null),
        )
        assertEquals(
            listOf(
                PickerCategory.PUSH,
                PickerCategory.PULL,
                PickerCategory.LEGS,
                PickerCategory.CORE,
            ),
            availablePickerCategories(rows, selected = PickerCategory.CORE),
        )
    }

    @Test
    fun `each muscle belongs to exactly one workout group`() {
        assertEquals(Muscle.entries.toSet(), PickerCategory.entries.flatMap { it.muscles }.toSet())
        assertEquals(Muscle.entries.size, PickerCategory.entries.sumOf { it.muscles.size })
        assertEquals(PickerCategory.PUSH, PickerCategory.containing(Muscle.TRICEPS))
        assertEquals(PickerCategory.PULL, PickerCategory.containing(Muscle.REAR_DELTS))
        assertEquals(PickerCategory.LEGS, PickerCategory.containing(Muscle.QUADS))
        assertEquals(PickerCategory.CORE, PickerCategory.containing(Muscle.CORE))
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
