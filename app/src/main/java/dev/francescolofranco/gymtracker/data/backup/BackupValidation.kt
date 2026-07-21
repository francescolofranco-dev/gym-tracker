package dev.francescolofranco.gymtracker.data.backup

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity

/** Pure preflight validation, kept outside Room so destructive-restore rules are unit-testable. */
internal fun validateBackupData(
    exercises: List<ExerciseEntity>,
    templates: List<TemplateEntity>,
    templateExercises: List<TemplateExerciseEntity>,
    sessions: List<SessionEntity>,
    sessionExercises: List<SessionExerciseEntity>,
    setLogs: List<SetLogEntity>,
) {
    fun <T> requireUnique(label: String, values: List<T>) {
        if (values.size != values.toSet().size) throw BackupParseException("Backup contains duplicate $label IDs.")
    }
    requireUnique("exercise", exercises.map { it.id })
    requireUnique("template", templates.map { it.id })
    requireUnique("session", sessions.map { it.id })
    requireUnique("session exercise", sessionExercises.map { it.id })
    requireUnique("set", setLogs.map { it.id })

    val exerciseIds = exercises.mapTo(HashSet()) { it.id }
    val templateIds = templates.mapTo(HashSet()) { it.id }
    val sessionIds = sessions.mapTo(HashSet()) { it.id }
    val sessionExerciseIds = sessionExercises.mapTo(HashSet()) { it.id }
    if (exercises.any {
            it.name.isBlank() || it.primaryMuscles.isEmpty() || it.targetSets <= 0 ||
                it.repRangeMin <= 0 || it.repRangeMax < it.repRangeMin
        }
    ) {
        throw BackupParseException("Backup contains an invalid exercise definition.")
    }
    if (templateExercises.any { it.templateId !in templateIds || it.exerciseId !in exerciseIds }) {
        throw BackupParseException("Backup contains a template with a missing exercise.")
    }
    if (sessions.any { it.templateId != null && it.templateId !in templateIds }) {
        throw BackupParseException("Backup contains a session with a missing template.")
    }
    if (sessions.any { it.endedAt != null && it.endedAt < (it.acceptedAt ?: it.startedAt) }) {
        throw BackupParseException("Backup contains a session whose end precedes its start.")
    }
    if (sessionExercises.any { it.sessionId !in sessionIds || it.exerciseId !in exerciseIds }) {
        throw BackupParseException("Backup contains a session with a missing exercise.")
    }
    if (setLogs.any { it.sessionExerciseId !in sessionExerciseIds || (it.reps ?: 0) < 0 || (it.kg ?: 0.0) < 0 }) {
        throw BackupParseException("Backup contains an invalid or orphaned set.")
    }
}
