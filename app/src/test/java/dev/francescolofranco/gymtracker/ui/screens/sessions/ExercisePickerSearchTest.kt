package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
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
    fun `all filter follows anatomy before exercise name`() {
        val result = filterPickerExercises(rows, PickerFilter.All, emptyList(), "  ")

        assertEquals(listOf("Bench press", "Lat pulldown", "Back squat"), result.map { it.name })
    }

    @Test
    fun `search results retain anatomical order`() {
        val frontRaise = exercise(4, "Front raise", Muscle.FRONT_DELTS)
        val calfRaise = exercise(5, "Calf raise", Muscle.CALVES)

        val result = filterPickerExercises(
            rows = listOf(ExerciseWithRecency(calfRaise, null), ExerciseWithRecency(frontRaise, null)),
            filter = PickerFilter.All,
            recentIds = emptyList(),
            query = "raise",
        )

        assertEquals(listOf(frontRaise.id, calfRaise.id), result.map { it.id })
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
    fun `category filter and query are both applied`() {
        val inclineBench = exercise(4, "Incline bench press", Muscle.CHEST)
        val shoulderPress = exercise(
            id = 5,
            name = "Zulu shoulder press",
            primaryMuscles = linkedSetOf(Muscle.CHEST, Muscle.FRONT_DELTS),
        )
        val result = filterPickerExercises(
            rows = rows + ExerciseWithRecency(inclineBench, null) + ExerciseWithRecency(shoulderPress, null),
            filter = PickerFilter.ByCategory(MuscleCategory.CHEST_AND_SHOULDERS),
            recentIds = emptyList(),
            query = "press",
        )

        assertEquals(listOf(shoulderPress.id, bench.id, inclineBench.id), result.map { it.id })
    }

    @Test
    fun `picker filters round trip through save keys`() {
        val filters = listOf(
            PickerFilter.All,
            PickerFilter.Recent,
            PickerFilter.ByCategory(MuscleCategory.CHEST_AND_SHOULDERS),
        )

        filters.forEach { filter ->
            assertEquals(filter, restorePickerFilter(filter.saveKey()))
        }
        assertEquals(PickerFilter.All, restorePickerFilter("category:REMOVED_VALUE"))
        assertEquals(
            PickerFilter.ByCategory(MuscleCategory.CHEST_AND_SHOULDERS),
            restorePickerFilter("muscle:REAR_DELTS"),
        )
    }

    private fun exercise(id: Long, name: String, primaryMuscle: Muscle) =
        exercise(id, name, setOf(primaryMuscle))

    private fun exercise(
        id: Long,
        name: String,
        primaryMuscles: Set<Muscle>,
    ) = ExerciseEntity(
        id = id,
        name = name,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = if (id == 1L) setOf(Muscle.TRICEPS) else emptySet(),
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        createdAt = Instant.EPOCH,
    )
}
