package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WorkoutExerciseBlockTest {
    @Test fun pairsAtTheLeadPositionAndPreservesBothExerciseIdentities() {
        val partner = detail(2, supersetLeadId = 1)
        val other = detail(3)
        val lead = detail(1)
        val blocks = listOf(partner, other, lead).workoutExerciseBlocks()

        assertEquals(listOf(3L, 1L), blocks.map { it.id })
        assertEquals(listOf(lead, partner), blocks.last().exercises)
        val reordered = orderedExerciseBlocks(blocks, listOf(1, 3))
        assertEquals(listOf(lead, partner, other), reordered.flatMap { it.exercises })
    }

    @Test fun alternatesByRoundWithBothSidesTogetherAndKeepsExtraSets() {
        val lead = detail(1, sets = listOf(
            set(12, 1, 1, ExerciseSide.RIGHT),
            set(11, 1, 1, ExerciseSide.LEFT),
            set(13, 1, 2, ExerciseSide.LEFT),
            set(14, 1, 2, ExerciseSide.RIGHT),
        ))
        val partner = detail(2, supersetLeadId = 1, sets = listOf(set(21, 2, 1)))

        assertEquals(
            listOf(11L, 12L, 21L, 13L, 14L),
            WorkoutExerciseBlock(lead, partner).rows.map { it.log.id },
        )
    }

    @Test fun highlightsPartnerAfterAnExistingLeadLogAndSkipsResolvedRows() {
        val lead = detail(1, sets = listOf(
            set(11, 1, 1).copy(reps = 10, loggedAt = Instant.EPOCH),
            set(12, 1, 2).copy(isSkipped = true),
        ))
        val partner = detail(2, supersetLeadId = 1, sets = listOf(set(21, 2, 1), set(22, 2, 2)))

        assertEquals(21L, WorkoutExerciseBlock(lead, partner).nextSetId)
        val loggedPartner = partner.copy(setLogs = listOf(
            partner.setLogs.first().copy(reps = 12, loggedAt = Instant.EPOCH),
            partner.setLogs.last(),
        ))
        assertEquals(22L, WorkoutExerciseBlock(lead, loggedPartner).nextSetId)
    }

    private fun detail(
        id: Long,
        supersetLeadId: Long? = null,
        sets: List<SetLogEntity> = emptyList(),
    ) = SessionExerciseDetail(
        sessionExercise = SessionExerciseEntity(
            id = id, sessionId = 1, exerciseId = id, orderInSession = 0, supersetLeadId = supersetLeadId,
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
        setLogs = sets,
    )

    private fun set(id: Long, exerciseId: Long, number: Int, side: ExerciseSide = ExerciseSide.BOTH) =
        SetLogEntity(id = id, sessionExerciseId = exerciseId, setNumber = number, side = side)
}
