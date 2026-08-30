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
    val firstSetKg: Double?,
    val isBodyweight: Boolean,
)

data class SessionShareSummary(
    val exercises: List<SessionShareExercise>,
) {
    val exerciseCount: Int get() = exercises.size
    val setCount: Int get() = exercises.sumOf(SessionShareExercise::setCount)
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
            val firstSet = completedSets.minWith(
                compareBy({ it.setNumber }, { it.side.ordinal }),
            )
            SessionShareExercise(
                name = detail.exercise.name,
                setCount = completedSets.size,
                firstSetKg = firstSet.kg,
                isBodyweight = detail.exercise.isBodyweight,
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
                append(" · ${exercise.firstSetWeightLabel(unit)}")
                if (index != summary.exercises.lastIndex) appendLine()
            }
        }

        appendLine()
        appendLine()
        append("Tracked with Gym Tracker")
    }
}

fun SessionShareExercise.firstSetWeightLabel(unit: WeightUnit): String {
    val kg = firstSetKg
    if (isBodyweight && (kg == null || kg == 0.0)) return "BW"
    if (kg == null) return "—"

    val weight = "${formatWeightNumber(convertFromKg(kg, unit))} ${unit.label()}"
    return if (isBodyweight) "BW +$weight" else weight
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
