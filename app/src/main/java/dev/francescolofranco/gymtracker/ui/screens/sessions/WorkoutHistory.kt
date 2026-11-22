package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal const val BRIGHTEST_DAY_SETS = 20

internal fun completedWorkoutsByMonth(
    summaries: List<SessionSummary>,
    zone: ZoneId,
): Map<YearMonth, List<SessionSummary>> = summaries
    .filter { it.session.acceptedAt != null && it.session.endedAt != null }
    .groupBy { YearMonth.from(it.session.workoutStartedAt().atZone(zone)) }
    .mapValues { (_, workouts) -> workouts.sortedByDescending { it.session.workoutStartedAt() } }

internal fun workoutHistoryMonths(currentMonth: YearMonth, recordedMonths: Set<YearMonth>): List<YearMonth> {
    val oldestMonth = minOf(currentMonth, recordedMonths.minOrNull() ?: currentMonth)
    return generateSequence(currentMonth) { month ->
        month.takeIf { it > oldestMonth }?.minusMonths(1)
    }.toList()
}

internal data class WorkoutDay(val date: LocalDate, val loggedSets: Long?)

internal fun loggedSetsBrightness(loggedSets: Long): Float {
    val fraction = loggedSets.coerceIn(0L, BRIGHTEST_DAY_SETS.toLong()) / BRIGHTEST_DAY_SETS.toFloat()
    return 0.36f + 0.64f * fraction
}

internal fun workoutCalendarDays(
    month: YearMonth,
    workouts: List<SessionSummary>,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): List<WorkoutDay?> {
    val firstDate = month.atDay(1)
    val leadingDays = (firstDate.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val setsByDate = workouts.groupingBy { it.session.workoutStartedAt().atZone(zone).toLocalDate() }
        .fold(0L) { sets, workout -> sets + workout.setCount }
    return List(42) { index ->
        val day = index - leadingDays + 1
        if (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            WorkoutDay(date, setsByDate[date])
        } else {
            null
        }
    }
}
