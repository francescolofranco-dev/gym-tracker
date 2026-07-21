package dev.francescolofranco.gymtracker.data.backup

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class BackupValidationTest {
    private val exercise = ExerciseEntity(
        id = 1,
        name = "Lateral raise",
        primaryMuscles = setOf(Muscle.SIDE_DELTS),
        secondaryMuscles = emptySet(),
        targetSets = 4,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        isUnilateral = true,
        createdAt = Instant.EPOCH,
    )
    private val session = SessionEntity(
        id = 2,
        startedAt = Instant.EPOCH,
        acceptedAt = Instant.EPOCH.plusSeconds(30),
        endedAt = Instant.EPOCH.plusSeconds(300),
    )
    private val sessionExercise = SessionExerciseEntity(
        id = 3,
        sessionId = session.id,
        exerciseId = exercise.id,
        orderInSession = 0,
    )
    private val set = SetLogEntity(id = 4, sessionExerciseId = sessionExercise.id, setNumber = 1, reps = 10, kg = 8.0)

    @Test
    fun validGraphPassesPreflight() {
        validateBackupData(listOf(exercise), emptyList(), emptyList(), listOf(session), listOf(sessionExercise), listOf(set))
    }

    @Test
    fun orphanedSetIsRejected() {
        assertThrows(BackupParseException::class.java) {
            validateBackupData(listOf(exercise), emptyList(), emptyList(), listOf(session), listOf(sessionExercise), listOf(set.copy(sessionExerciseId = 999)))
        }
    }

    @Test
    fun duplicateIdsAreRejected() {
        assertThrows(BackupParseException::class.java) {
            validateBackupData(listOf(exercise, exercise.copy(name = "Duplicate")), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    @Test
    fun invalidExerciseRangeIsRejected() {
        assertThrows(BackupParseException::class.java) {
            validateBackupData(listOf(exercise.copy(repRangeMin = 12, repRangeMax = 8)), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    @Test
    fun endBeforeAcceptedStartIsRejected() {
        assertThrows(BackupParseException::class.java) {
            validateBackupData(
                listOf(exercise),
                emptyList(),
                emptyList(),
                listOf(session.copy(endedAt = Instant.EPOCH.plusSeconds(10))),
                listOf(sessionExercise),
                listOf(set),
            )
        }
    }
}
