package dev.francescolofranco.gymtracker.data.backup

/**
 * Versioned wire format for the JSON backup. Bump [CURRENT_SCHEMA_VERSION] when a structural
 * change requires migration logic on import.
 *
 * The format is intentionally portable (timestamps as ISO-8601, muscles as enum names) so
 * exports survive across schema migrations and can be eyeballed without tooling.
 */
object BackupSchema {
    /**
     * Wire format version.
     *  - v1: single `primaryMuscle` per exercise.
     *  - v2: list `primaryMuscles` per exercise (1-3 entries). Importer accepts both.
     *  - v3: unilateral exercise metadata and set sides.
     *  - v4: one-session carryover and pinning metadata for exercise notes.
     */
    const val CURRENT_SCHEMA_VERSION = 4
    const val MIN_SUPPORTED_VERSION = 1

    const val K_SCHEMA_VERSION = "schemaVersion"
    const val K_EXPORTED_AT = "exportedAt"
    const val K_EXERCISES = "exercises"
    const val K_TEMPLATES = "templates"
    const val K_TEMPLATE_EXERCISES = "templateExercises"
    const val K_SESSIONS = "sessions"
    const val K_SESSION_EXERCISES = "sessionExercises"
    const val K_SET_LOGS = "setLogs"
}
