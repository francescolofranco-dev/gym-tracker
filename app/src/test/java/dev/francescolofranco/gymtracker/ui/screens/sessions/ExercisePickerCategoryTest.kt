package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerCategoryTest {

    private val chestAndTriceps = exercise(
        id = 1,
        name = "Bench press",
        primaries = setOf(Muscle.CHEST, Muscle.TRICEPS),
    )
    private val squat = exercise(
        id = 2,
        name = "Back squat",
        primaries = setOf(Muscle.QUADS),
        secondaries = setOf(Muscle.CORE),
    )
    private val rows = listOf(chestAndTriceps, squat).map { ExerciseWithRecency(it, null) }

    @Test
    fun `exercise is available from every category represented by a primary muscle`() {
        val chestResults = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.CHEST_AND_SHOULDERS),
            emptyList(),
            "",
        )
        val armResults = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.ARMS),
            emptyList(),
            "",
        )

        assertEquals(listOf(chestAndTriceps.id), chestResults.map { it.id })
        assertEquals(listOf(chestAndTriceps.id), armResults.map { it.id })
    }

    @Test
    fun `secondary muscles do not opt an exercise into a category`() {
        val results = filterPickerExercises(
            rows,
            PickerFilter.ByCategory(MuscleCategory.CORE),
            emptyList(),
            "",
        )

        assertEquals(emptyList<ExerciseEntity>(), results)
    }

    @Test
    fun `only represented categories are offered and a selected empty category stays visible`() {
        assertEquals(
            listOf(
                MuscleCategory.CHEST_AND_SHOULDERS,
                MuscleCategory.ARMS,
                MuscleCategory.LEGS_AND_GLUTES,
            ),
            availablePickerCategories(rows, selected = null),
        )
        assertEquals(
            listOf(
                MuscleCategory.CHEST_AND_SHOULDERS,
                MuscleCategory.ARMS,
                MuscleCategory.CORE,
                MuscleCategory.LEGS_AND_GLUTES,
            ),
            availablePickerCategories(rows, selected = MuscleCategory.CORE),
        )
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
