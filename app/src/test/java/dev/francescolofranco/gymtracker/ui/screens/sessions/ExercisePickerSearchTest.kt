package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerSearchTest {

    private val bench = exercise(1, "Bench press", Muscle.CHEST)
    private val squat = exercise(2, "Back squat", Muscle.QUADS)
    private val pulldown = exercise(3, "Lat pulldown", Muscle.LATS)
    private val rows = listOf(bench, squat, pulldown).mapIndexed { index, exercise ->
        ExerciseWithRecency(exercise, Instant.ofEpochSecond(index.toLong()))
    }

    @Test
    fun `all filter stays alphabetical with a blank query`() {
        val result = filterPickerExercises(rows, PickerFilter.All, emptyList(), "  ")

        assertEquals(listOf("Back squat", "Bench press", "Lat pulldown"), result.map { it.name })
    }

    @Test
    fun `recent filter keeps its snapshotted order while searching`() {
        val result = filterPickerExercises(
            rows = rows,
            filter = PickerFilter.Recent,
            recentIds = listOf(pulldown.id, bench.id, squat.id),
            query = "back",
        )

        assertEquals(listOf(pulldown.id, squat.id), result.map { it.id })
    }

    @Test
    fun `muscle filter and query are both applied`() {
        val inclineBench = exercise(4, "Incline bench press", Muscle.CHEST)
        val result = filterPickerExercises(
            rows = rows + ExerciseWithRecency(inclineBench, null),
            filter = PickerFilter.ByMuscle(Muscle.CHEST),
            recentIds = emptyList(),
            query = "incline",
        )

        assertEquals(listOf(inclineBench.id), result.map { it.id })
    }

    @Test
    fun `picker filters round trip through save keys`() {
        val filters = listOf(
            PickerFilter.All,
            PickerFilter.Recent,
            PickerFilter.ByMuscle(Muscle.REAR_DELTS),
        )

        filters.forEach { filter ->
            assertEquals(filter, restorePickerFilter(filter.saveKey()))
        }
        assertEquals(PickerFilter.All, restorePickerFilter("muscle:REMOVED_VALUE"))
    }

    private fun exercise(id: Long, name: String, primaryMuscle: Muscle) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscles = setOf(primaryMuscle),
        secondaryMuscles = if (id == 1L) setOf(Muscle.TRICEPS) else emptySet(),
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        createdAt = Instant.EPOCH,
    )
}
