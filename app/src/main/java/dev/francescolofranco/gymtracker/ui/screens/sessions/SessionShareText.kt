package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionShareExercise(
    val name: String,
    val setCount: Int,
    val volumeKg: Double,
)

data class SessionShareSummary(
    val exercises: List<SessionShareExercise>,
) {
    val exerciseCount: Int get() = exercises.size
    val setCount: Int get() = exercises.sumOf(SessionShareExercise::setCount)
    val volumeKg: Double get() = exercises.sumOf(SessionShareExercise::volumeKg)
}

/**
 * Creates the one canonical summary used by the completion screen and WhatsApp copy.
 * Draft values are deliberately ignored: a set only counts once it was checked off.
 */
fun summarizeFinishedSession(details: List<SessionExerciseDetail>): SessionShareSummary {
    val exercises = details
        .filterNot { it.sessionExercise.isSkipped }
        .mapNotNull { detail ->
            val completedSets = detail.setLogs.filter { set ->
                set.loggedAt != null && set.reps != null && !set.isSkipped
            }
            if (completedSets.isEmpty()) return@mapNotNull null
            SessionShareExercise(
                name = detail.exercise.name,
                setCount = completedSets.size,
                volumeKg = completedSets.sumOf { set -> set.reps!! * (set.kg ?: 0.0) },
            )
        }
    return SessionShareSummary(exercises)
}

fun buildSessionShareText(
    session: SessionEntity,
    details: List<SessionExerciseDetail>,
    unit: WeightUnit,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val summary = summarizeFinishedSession(details)
    val date = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        .withZone(zoneId)
        .format(session.workoutStartedAt())
    val headlineStats = buildList {
        add(plural(summary.exerciseCount, "exercise", "exercises"))
        add(plural(summary.setCount, "set", "sets"))
        if (summary.volumeKg > 0.0) add("${formatTotalVolume(summary.volumeKg, unit)} volume")
    }.joinToString(" · ")

    return buildString {
        appendLine("Workout complete! 💪")
        appendLine("$date · ${formatShareDuration(session.workoutDuration())}")
        append(headlineStats)

        if (summary.exercises.isNotEmpty()) {
            appendLine()
            appendLine()
            summary.exercises.forEachIndexed { index, exercise ->
                append("• ${exercise.name} — ${plural(exercise.setCount, "set", "sets")}")
                if (exercise.volumeKg > 0.0) {
                    append(" · ${formatTotalVolume(exercise.volumeKg, unit)}")
                }
                if (index != summary.exercises.lastIndex) appendLine()
            }
        }

        appendLine()
        appendLine()
        append("Tracked with Gym Tracker")
    }
}

fun formatShareDuration(duration: Duration): String {
    val totalMinutes = duration.seconds.coerceAtLeast(0) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        totalMinutes > 0 -> "${totalMinutes}m"
        else -> "<1m"
    }
}

private fun plural(value: Int, singular: String, plural: String): String =
    "$value ${if (value == 1) singular else plural}"
