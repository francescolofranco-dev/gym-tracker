package dev.francescolofranco.gymtracker.data.backup

import androidx.room.withTransaction
import dev.francescolofranco.gymtracker.data.db.AppDatabase
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

class BackupParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class BackupSummary(
    val exercises: Int,
    val templates: Int,
    val templateExercises: Int,
    val sessions: Int,
    val sessionExercises: Int,
    val setLogs: Int,
    val exportedAt: Instant?,
)

@Singleton
class BackupImporter @Inject constructor(
    private val db: AppDatabase,
) {

    /**
     * Replaces the entire local DB with the contents of [json]. Wraps the wipe + re-insert in a
     * single transaction so a failure mid-restore leaves the previous DB intact.
     */
    suspend fun importFromJson(json: String): BackupSummary {
        val root = parseRoot(json)
        val schemaVersion = root.optInt(BackupSchema.K_SCHEMA_VERSION, -1)
        if (schemaVersion < BackupSchema.MIN_SUPPORTED_VERSION || schemaVersion > BackupSchema.CURRENT_SCHEMA_VERSION) {
            throw BackupParseException(
                "Unsupported backup schema $schemaVersion " +
                    "(supported: ${BackupSchema.MIN_SUPPORTED_VERSION}..${BackupSchema.CURRENT_SCHEMA_VERSION}).",
            )
        }
        val exportedAt = root.optStringOrNull(BackupSchema.K_EXPORTED_AT)?.let { runCatching { Instant.parse(it) }.getOrNull() }

        val exercises = parseList(root, BackupSchema.K_EXERCISES) { it.parseExercise() }
        val templates = parseList(root, BackupSchema.K_TEMPLATES) { it.parseTemplate() }
        val templateExercises = parseList(root, BackupSchema.K_TEMPLATE_EXERCISES) { it.parseTemplateExercise() }
        val sessions = parseList(root, BackupSchema.K_SESSIONS) { it.parseSession() }
        val sessionExercises = parseList(root, BackupSchema.K_SESSION_EXERCISES) { it.parseSessionExercise() }
        val setLogs = parseList(root, BackupSchema.K_SET_LOGS) { it.parseSetLog() }

        db.withTransaction {
            // Clear in FK-safe order (children first).
            db.setLogDao().deleteAll()
            db.sessionDao().deleteAllSessionExercises()
            db.sessionDao().deleteAllSessions()
            db.templateDao().deleteAllTemplateExercises()
            db.templateDao().deleteAllTemplates()
            db.exerciseDao().deleteAll()

            // Insert in parent-first order.
            db.exerciseDao().replaceAll(exercises)
            db.templateDao().replaceTemplates(templates)
            db.templateDao().replaceTemplateExercises(templateExercises)
            db.sessionDao().replaceSessions(sessions)
            db.sessionDao().replaceSessionExercises(sessionExercises)
            db.setLogDao().replaceAll(setLogs)
        }

        return BackupSummary(
            exercises = exercises.size,
            templates = templates.size,
            templateExercises = templateExercises.size,
            sessions = sessions.size,
            sessionExercises = sessionExercises.size,
            setLogs = setLogs.size,
            exportedAt = exportedAt,
        )
    }

    private fun parseRoot(json: String): JSONObject = try {
        JSONObject(json)
    } catch (t: Throwable) {
        throw BackupParseException("Backup file is not valid JSON.", t)
    }

    private inline fun <T> parseList(
        root: JSONObject,
        key: String,
        crossinline parse: (JSONObject) -> T,
    ): List<T> {
        val arr = root.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { i ->
            try { parse(arr.getJSONObject(i)) } catch (t: Throwable) {
                throw BackupParseException("Failed to parse $key[$i]: ${t.message}", t)
            }
        }
    }
}

private fun JSONObject.parseExercise(): ExerciseEntity {
    // v2 stores `primaryMuscles` as an array; v1 used a single `primaryMuscle` string.
    // Accept either so backups from earlier releases still import cleanly.
    val primaries: Set<Muscle> = optJSONArray("primaryMuscles")
        ?.let { parseMuscleSet(it) }
        ?: setOf(Muscle.valueOf(getString("primaryMuscle")))
    return ExerciseEntity(
        id = getLong("id"),
        name = getString("name"),
        primaryMuscles = primaries,
        secondaryMuscles = parseMuscleSet(optJSONArray("secondaryMuscles")),
        targetSets = getInt("targetSets"),
        repRangeMin = getInt("repRangeMin"),
        repRangeMax = getInt("repRangeMax"),
        isBodyweight = getBoolean("isBodyweight"),
        createdAt = Instant.parse(getString("createdAt")),
        deletedAt = optStringOrNull("deletedAt")?.let { Instant.parse(it) },
    )
}

private fun JSONObject.parseTemplate(): TemplateEntity = TemplateEntity(
    id = getLong("id"),
    name = getString("name"),
    createdAt = Instant.parse(getString("createdAt")),
)

private fun JSONObject.parseTemplateExercise(): TemplateExerciseEntity = TemplateExerciseEntity(
    templateId = getLong("templateId"),
    exerciseId = getLong("exerciseId"),
    orderInTemplate = getInt("orderInTemplate"),
)

private fun JSONObject.parseSession(): SessionEntity {
    val startedAt = Instant.parse(getString("startedAt"))
    // Old backups (schema v1/v2) had no acceptedAt — treat those sessions as already
    // accepted at their start time so they keep showing in history after restore.
    val acceptedAt = if (has("acceptedAt") && !isNull("acceptedAt")) {
        optStringOrNull("acceptedAt")?.let { Instant.parse(it) }
    } else {
        startedAt
    }
    return SessionEntity(
        id = getLong("id"),
        startedAt = startedAt,
        endedAt = optStringOrNull("endedAt")?.let { Instant.parse(it) },
        notes = optStringOrNull("notes"),
        templateId = if (isNull("templateId")) null else optLong("templateId").takeIf { it != 0L },
        acceptedAt = acceptedAt,
    )
}

private fun JSONObject.parseSessionExercise(): SessionExerciseEntity = SessionExerciseEntity(
    id = getLong("id"),
    sessionId = getLong("sessionId"),
    exerciseId = getLong("exerciseId"),
    orderInSession = getInt("orderInSession"),
    notes = optStringOrNull("notes"),
    isSkipped = getBoolean("isSkipped"),
)

private fun JSONObject.parseSetLog(): SetLogEntity = SetLogEntity(
    id = getLong("id"),
    sessionExerciseId = getLong("sessionExerciseId"),
    setNumber = getInt("setNumber"),
    reps = if (isNull("reps")) null else optInt("reps"),
    kg = if (isNull("kg")) null else optDouble("kg"),
    isSkipped = getBoolean("isSkipped"),
    loggedAt = optStringOrNull("loggedAt")?.let { Instant.parse(it) },
)

private fun parseMuscleSet(arr: JSONArray?): Set<Muscle> {
    if (arr == null) return emptySet()
    return (0 until arr.length()).mapNotNull { i ->
        runCatching { Muscle.valueOf(arr.getString(i)) }.getOrNull()
    }.toSet()
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else optString(key, "").takeIf { it.isNotEmpty() }
