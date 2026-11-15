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
        val result = filterPickerExercises(rows, PickerFilter.All, "  ")

        assertEquals(listOf("Bench press", "Lat pulldown", "Back squat"), result.map { it.name })
    }

    @Test
    fun `search results retain anatomical order`() {
        val frontRaise = exercise(4, "Front raise", Muscle.FRONT_DELTS)
        val calfRaise = exercise(5, "Calf raise", Muscle.CALVES)

        val result = filterPickerExercises(
            rows = listOf(ExerciseWithRecency(calfRaise, null), ExerciseWithRecency(frontRaise, null)),
            filter = PickerFilter.All,
            query = "raise",
        )

        assertEquals(listOf(frontRaise.id, calfRaise.id), result.map { it.id })
    }

    @Test
    fun `removed recent filter restores to all`() {
        assertEquals(PickerFilter.All, restorePickerFilter("recent"))
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
            filter = PickerFilter.ByCategory(MuscleCategory.PUSH),
            query = "press",
        )

        assertEquals(listOf(shoulderPress.id, bench.id, inclineBench.id), result.map { it.id })
    }

    @Test
    fun `picker filters round trip through save keys`() {
        val filters = listOf(PickerFilter.All) +
            MuscleCategory.entries.map { PickerFilter.ByCategory(it) }

        filters.forEach { filter ->
            assertEquals(filter, restorePickerFilter(filter.saveKey()))
        }
        assertEquals(PickerFilter.All, restorePickerFilter("category:REMOVED_VALUE"))
        listOf("CHEST_AND_SHOULDERS", "BACK", "ARMS", "LEGS_AND_GLUTES").forEach { oldCategory ->
            assertEquals(PickerFilter.All, restorePickerFilter("category:$oldCategory"))
        }
        assertEquals(
            PickerFilter.ByCategory(MuscleCategory.PULL),
            restorePickerFilter("muscle:REAR_DELTS"),
        )
    }

    @Test
    fun `resolveSelectedExerciseIds applies add and remove overrides over excludeIds`() {
        val resolved = resolveSelectedExerciseIds(
            excludeIds = setOf(1L, 2L),
            selectionOverrides = mapOf(
                1L to false,
                3L to true,
                4L to false,
            ),
        )

        assertEquals(setOf(2L, 3L), resolved)
    }

    @Test
    fun `pickerConfirmLabel reflects selection diff from initial exercises`() {
        val initial = setOf(1L, 2L)

        assertEquals("DONE", pickerConfirmLabel(emptySet(), emptySet()))
        assertEquals("DONE", pickerConfirmLabel(initial, setOf(1L, 2L)))
        assertEquals("ADD 1 EXERCISE", pickerConfirmLabel(initial, setOf(1L, 2L, 3L)))
        assertEquals("ADD 2 EXERCISES", pickerConfirmLabel(initial, setOf(1L, 2L, 3L, 4L)))
        assertEquals("UPDATE EXERCISES", pickerConfirmLabel(initial, setOf(1L)))
        assertEquals("UPDATE EXERCISES", pickerConfirmLabel(initial, setOf(1L, 3L)))
        assertEquals("UPDATE EXERCISES", pickerConfirmLabel(initial, setOf(1L, 3L, 4L)))

        val pickedThenUnpicked = resolveSelectedExerciseIds(
            excludeIds = initial + 3L,
            selectionOverrides = mapOf(3L to false),
        )
        assertEquals("DONE", pickerConfirmLabel(initial, pickedThenUnpicked))
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
