package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
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
    val setsLabel: String,
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
                setsLabel = completedSetsLabel(completedSets),
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
                append("• ${exercise.name} — ${exercise.setsLabel}")
                append(" · ${exercise.firstSetWeightLabel(unit)}")
                if (index != summary.exercises.lastIndex) appendLine()
            }
        }

        appendLine()
        appendLine()
        append("Tracked with Gym Tracker")
    }
}

private fun completedSetsLabel(completedSets: List<SetLogEntity>): String {
    val sortedSets = completedSets.sortedWith(compareBy({ it.setNumber }, { it.side.ordinal }))
    val counts = sortedSets.groupingBy { it.side }.eachCount()
    val both = counts[ExerciseSide.BOTH] ?: 0
    val left = counts[ExerciseSide.LEFT] ?: 0
    val right = counts[ExerciseSide.RIGHT] ?: 0

    val bothReps = sortedSets.filter { it.side == ExerciseSide.BOTH }.mapNotNull { it.reps }
    val leftReps = sortedSets.filter { it.side == ExerciseSide.LEFT }.mapNotNull { it.reps }
    val rightReps = sortedSets.filter { it.side == ExerciseSide.RIGHT }.mapNotNull { it.reps }

    return when {
        left == 0 && right == 0 -> {
            if (bothReps.distinct().size <= 1) {
                val reps = bothReps.firstOrNull() ?: 0
                "${both}×$reps ${repWord(reps)}"
            } else {
                "${plural(both, "set", "sets")} (${bothReps.joinToString(", ")} reps)"
            }
        }
        both == 0 && left == right && left > 0 -> {
            if (leftReps == rightReps) {
                if (leftReps.distinct().size <= 1) {
                    val reps = leftReps.firstOrNull() ?: 0
                    "${left}×$reps ${repWord(reps)}/side"
                } else {
                    "${plural(left, "set", "sets")}/side (${leftReps.joinToString(", ")} reps)"
                }
            } else {
                val leftDesc = if (leftReps.distinct().size <= 1) "${leftReps.firstOrNull() ?: 0}" else leftReps.joinToString(", ")
                val rightDesc = if (rightReps.distinct().size <= 1) "${rightReps.firstOrNull() ?: 0}" else rightReps.joinToString(", ")
                "${plural(left, "set", "sets")}/side (L $leftDesc, R $rightDesc reps)"
            }
        }
        both == 0 && right == 0 && left > 0 -> {
            if (leftReps.distinct().size <= 1) {
                val reps = leftReps.firstOrNull() ?: 0
                "$left L × $reps ${repWord(reps)}"
            } else {
                "$left L ${if (left == 1) "set" else "sets"} (${leftReps.joinToString(", ")} reps)"
            }
        }
        both == 0 && left == 0 && right > 0 -> {
            if (rightReps.distinct().size <= 1) {
                val reps = rightReps.firstOrNull() ?: 0
                "$right R × $reps ${repWord(reps)}"
            } else {
                "$right R ${if (right == 1) "set" else "sets"} (${rightReps.joinToString(", ")} reps)"
            }
        }
        else -> {
            val parts = buildList {
                if (both > 0) add("$both B")
                if (left > 0) add("$left L")
                if (right > 0) add("$right R")
            }.joinToString(" + ")
            val repsDetail = buildList {
                if (bothReps.isNotEmpty()) {
                    val desc = if (bothReps.distinct().size <= 1) "${bothReps.first()}" else bothReps.joinToString(", ")
                    add("B $desc")
                }
                if (leftReps.isNotEmpty()) {
                    val desc = if (leftReps.distinct().size <= 1) "${leftReps.first()}" else leftReps.joinToString(", ")
                    add("L $desc")
                }
                if (rightReps.isNotEmpty()) {
                    val desc = if (rightReps.distinct().size <= 1) "${rightReps.first()}" else rightReps.joinToString(", ")
                    add("R $desc")
                }
            }.joinToString(", ")
            val setsWord = if (sortedSets.size == 1) "set" else "sets"
            if (repsDetail.isBlank()) {
                "$parts $setsWord"
            } else {
                "$parts $setsWord ($repsDetail reps)"
            }
        }
    }
}

private fun repWord(count: Int): String = if (count == 1) "rep" else "reps"

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
