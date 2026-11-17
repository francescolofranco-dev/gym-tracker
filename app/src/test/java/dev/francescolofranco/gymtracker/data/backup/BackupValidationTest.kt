package dev.francescolofranco.gymtracker.data.backup

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun negativeSetWeightWithinBoundsPassesPreflight() {
        validateBackupData(
            listOf(exercise),
            emptyList(),
            emptyList(),
            listOf(session),
            listOf(sessionExercise),
            listOf(set.copy(kg = -25.0)),
        )
    }

    @Test
    fun outOfBoundsOrNonFiniteSetWeightIsRejected() {
        listOf(-10000.0, 10000.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalidKg ->
            assertThrows(BackupParseException::class.java) {
                validateBackupData(
                    listOf(exercise),
                    emptyList(),
                    emptyList(),
                    listOf(session),
                    listOf(sessionExercise),
                    listOf(set.copy(kg = invalidKg)),
                )
            }
        }
    }

    @Test
    fun importingPreV8BackupConvertsAssistedPullUpsFrom70KgOffset() {
        fun backupJson(schemaVersion: Int, kgs: List<Double>): String {
            val setsJson = kgs.mapIndexed { idx, kg ->
                """{"id":${100 + idx},"sessionExerciseId":10,"setNumber":${idx + 1},"side":"BOTH","reps":8,"kg":$kg,"isSkipped":false,"loggedAt":"2026-10-01T10:05:00Z"}"""
            }.joinToString(",")
            return """
                {
                  "schemaVersion": $schemaVersion,
                  "exportedAt": "2026-10-01T11:00:00Z",
                  "exercises": [
                    {
                      "id": 1,
                      "name": "Assisted Pull-Ups",
                      "primaryMuscles": ["LATS"],
                      "secondaryMuscles": [],
                      "targetSets": 3,
                      "repRangeMin": 6,
                      "repRangeMax": 12,
                      "isBodyweight": false,
                      "isUnilateral": false,
                      "isWeightPerSide": false,
                      "createdAt": "2026-09-01T10:00:00Z",
                      "deletedAt": null
                    }
                  ],
                  "templates": [],
                  "templateExercises": [],
                  "sessions": [
                    {
                      "id": 1,
                      "startedAt": "2026-10-01T10:00:00Z",
                      "endedAt": "2026-10-01T10:45:00Z",
                      "notes": null,
                      "templateId": null,
                      "acceptedAt": "2026-10-01T10:00:00Z"
                    }
                  ],
                  "sessionExercises": [
                    {
                      "id": 10,
                      "sessionId": 1,
                      "exerciseId": 1,
                      "orderInSession": 0,
                      "notes": null,
                      "pinnedNotes": null,
                      "noteCarryForward": false,
                      "isSkipped": false
                    }
                  ],
                  "setLogs": [$setsJson]
                }
            """.trimIndent()
        }

        val importer = BackupImporter()

        val v6Parsed = importer.parseBackup(backupJson(schemaVersion = 6, kgs = listOf(35.0, 60.0, 70.0)))
        assertTrue(v6Parsed.exercises.single().isNegativeWeight)
        assertFalse(v6Parsed.exercises.single().isBodyweight)
        assertEquals(listOf(-35.0, -10.0, 0.0), v6Parsed.setLogs.map { it.kg })

        val v7Parsed = importer.parseBackup(backupJson(schemaVersion = 7, kgs = listOf(-35.0, -60.0, -70.0)))
        assertEquals(listOf(-35.0, -10.0, 0.0), v7Parsed.setLogs.map { it.kg })

        val v8Parsed = importer.parseBackup(backupJson(schemaVersion = 8, kgs = listOf(-35.0, -10.0, 0.0)))
        assertFalse(v8Parsed.exercises.single().isNegativeWeight)
        assertEquals(listOf(-35.0, -10.0, 0.0), v8Parsed.setLogs.map { it.kg })
    }
}
