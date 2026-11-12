package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail

/** Skips resolve planned work, but only an actually logged set earns a completion prompt. */
internal fun List<SessionExerciseDetail>.isWorkoutComplete(): Boolean {
    val activeExercises = filterNot { it.sessionExercise.isSkipped }
    return activeExercises.any { detail ->
        detail.setLogs.any { !it.isSkipped && it.loggedAt != null && it.reps != null }
    } && activeExercises.all { detail ->
        detail.setLogs.isNotEmpty() && detail.setLogs.all {
            it.isSkipped || (it.loggedAt != null && it.reps != null)
        }
    }
}
