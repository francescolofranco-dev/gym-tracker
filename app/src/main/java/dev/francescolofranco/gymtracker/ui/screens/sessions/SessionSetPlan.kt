package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide

/** Session labels describe the saved rows, never the exercise's mutable default mode. */
internal fun SessionExerciseDetail.setPlanLabel(): String {
    val counts = setLogs.groupingBy { it.side }.eachCount()
    val both = counts[ExerciseSide.BOTH] ?: 0
    val left = counts[ExerciseSide.LEFT] ?: 0
    val right = counts[ExerciseSide.RIGHT] ?: 0
    val reps = "${exercise.repRangeMin}–${exercise.repRangeMax}"
    return when {
        setLogs.isEmpty() -> "No sets"
        left == 0 && right == 0 -> "${both}×$reps · bilateral"
        both == 0 && left == right -> "${left}×$reps/side"
        else -> buildList {
            if (both > 0) add("$both B (both)")
            if (left > 0) add("$left L")
            if (right > 0) add("$right R")
        }.joinToString(" + ") + " · $reps reps"
    }
}
