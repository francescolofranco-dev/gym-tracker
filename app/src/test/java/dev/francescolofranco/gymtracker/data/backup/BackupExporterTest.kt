package dev.francescolofranco.gymtracker.data.backup

import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.SetLogDao
import dev.francescolofranco.gymtracker.data.db.dao.TemplateDao
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant

class BackupExporterTest {

    private val exercises = mutableListOf<ExerciseEntity>()
    private val templates = mutableListOf<TemplateEntity>()
    private val templateExercises = mutableListOf<TemplateExerciseEntity>()
    private val sessions = mutableListOf<SessionEntity>()
    private val sessionExercises = mutableListOf<SessionExerciseEntity>()
    private val setLogs = mutableListOf<SetLogEntity>()

    private val exerciseDao: ExerciseDao = createProxy { method ->
        if (method == "all") exercises else null
    }
    private val templateDao: TemplateDao = createProxy { method ->
        when (method) {
            "all" -> templates
            "allTemplateExercises" -> templateExercises
            else -> null
        }
    }
    private val sessionDao: SessionDao = createProxy { method ->
        when (method) {
            "allSessions" -> sessions
            "allSessionExercises" -> sessionExercises
            else -> null
        }
    }
    private val setLogDao: SetLogDao = createProxy { method ->
        if (method == "all") setLogs else null
    }

    private val exporter = BackupExporter(exerciseDao, templateDao, sessionDao, setLogDao)

    private val sampleExercise = ExerciseEntity(
        id = 1,
        name = "Bench Press",
        primaryMuscles = setOf(Muscle.CHEST),
        secondaryMuscles = setOf(Muscle.TRICEPS),
        targetSets = 3,
        repRangeMin = 8,
        repRangeMax = 12,
        isBodyweight = false,
        isUnilateral = false,
        createdAt = Instant.EPOCH,
    )
    private val sampleSession = SessionEntity(
        id = 10,
        startedAt = Instant.EPOCH,
        acceptedAt = Instant.EPOCH.plusSeconds(10),
        endedAt = Instant.EPOCH.plusSeconds(3600),
    )
    private val sampleSessionExercise = SessionExerciseEntity(
        id = 20,
        sessionId = 10,
        exerciseId = 1,
        orderInSession = 0,
    )
    private val sampleSet = SetLogEntity(
        id = 30,
        sessionExerciseId = 20,
        setNumber = 1,
        reps = 10,
        kg = 100.0,
    )

    @Test
    fun sameDataWithDifferentExportTimesProducesIdenticalContentHash() = runBlocking {
        exercises.add(sampleExercise)
        sessions.add(sampleSession)
        sessionExercises.add(sampleSessionExercise)
        setLogs.add(sampleSet)

        val export1 = exporter.export(Instant.ofEpochSecond(1000))
        val export2 = exporter.export(Instant.ofEpochSecond(999999))

        assertNotEquals(export1.json, export2.json)
        assertEquals(export1.contentHash, export2.contentHash)
    }

    @Test
    fun computeContentHashMatchesExportContentHash() = runBlocking {
        exercises.add(sampleExercise)
        sessions.add(sampleSession)

        val export = exporter.export(Instant.now())
        val computed = BackupExporter.computeContentHash(export.json)

        assertEquals(export.contentHash, computed)
    }

    @Test
    fun addingSessionChangesContentHash() = runBlocking {
        exercises.add(sampleExercise)
        val hashBefore = exporter.export().contentHash

        sessions.add(sampleSession)
        val hashAfter = exporter.export().contentHash

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun modifyingSetLogWeightChangesContentHash() = runBlocking {
        exercises.add(sampleExercise)
        sessions.add(sampleSession)
        sessionExercises.add(sampleSessionExercise)
        setLogs.add(sampleSet)
        val hashBefore = exporter.export().contentHash

        setLogs[0] = sampleSet.copy(kg = 102.5)
        val hashAfter = exporter.export().contentHash

        assertNotEquals(hashBefore, hashAfter)
    }

    @Test
    fun modifyingExerciseNotesChangesContentHash() = runBlocking {
        exercises.add(sampleExercise)
        sessions.add(sampleSession)
        sessionExercises.add(sampleSessionExercise)
        val hashBefore = exporter.export().contentHash

        sessionExercises[0] = sampleSessionExercise.copy(notes = "Pause at bottom")
        val hashAfter = exporter.export().contentHash

        assertNotEquals(hashBefore, hashAfter)
    }

    private inline fun <reified T> createProxy(crossinline handler: (String) -> Any?): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ ->
            handler(method.name)
        } as T
    }
}
