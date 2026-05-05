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
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExporter @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val templateDao: TemplateDao,
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put(BackupSchema.K_SCHEMA_VERSION, BackupSchema.CURRENT_SCHEMA_VERSION)
        root.put(BackupSchema.K_EXPORTED_AT, Instant.now().toString())
        root.put(BackupSchema.K_EXERCISES, JSONArray(exerciseDao.all().map { it.toJson() }))
        root.put(BackupSchema.K_TEMPLATES, JSONArray(templateDao.all().map { it.toJson() }))
        root.put(BackupSchema.K_TEMPLATE_EXERCISES, JSONArray(templateDao.allTemplateExercises().map { it.toJson() }))
        root.put(BackupSchema.K_SESSIONS, JSONArray(sessionDao.allSessions().map { it.toJson() }))
        root.put(BackupSchema.K_SESSION_EXERCISES, JSONArray(sessionDao.allSessionExercises().map { it.toJson() }))
        root.put(BackupSchema.K_SET_LOGS, JSONArray(setLogDao.all().map { it.toJson() }))
        return root.toString(2)
    }
}

private fun ExerciseEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("primaryMuscle", primaryMuscle.name)
    put("secondaryMuscles", JSONArray(secondaryMuscles.map { it.name }))
    put("targetSets", targetSets)
    put("repRangeMin", repRangeMin)
    put("repRangeMax", repRangeMax)
    put("isBodyweight", isBodyweight)
    put("createdAt", createdAt.toString())
    put("deletedAt", deletedAt?.toString() ?: JSONObject.NULL)
}

private fun TemplateEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("createdAt", createdAt.toString())
}

private fun TemplateExerciseEntity.toJson(): JSONObject = JSONObject().apply {
    put("templateId", templateId)
    put("exerciseId", exerciseId)
    put("orderInTemplate", orderInTemplate)
}

private fun SessionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("startedAt", startedAt.toString())
    put("endedAt", endedAt?.toString() ?: JSONObject.NULL)
    put("notes", notes ?: JSONObject.NULL)
    put("templateId", templateId ?: JSONObject.NULL)
}

private fun SessionExerciseEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("sessionId", sessionId)
    put("exerciseId", exerciseId)
    put("orderInSession", orderInSession)
    put("notes", notes ?: JSONObject.NULL)
    put("isSkipped", isSkipped)
}

private fun SetLogEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("sessionExerciseId", sessionExerciseId)
    put("setNumber", setNumber)
    put("reps", reps ?: JSONObject.NULL)
    put("kg", kg ?: JSONObject.NULL)
    put("isSkipped", isSkipped)
    put("loggedAt", loggedAt?.toString() ?: JSONObject.NULL)
}
