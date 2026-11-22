package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal fun completedWorkoutsByMonth(
    summaries: List<SessionSummary>,
    zone: ZoneId,
): Map<YearMonth, List<SessionSummary>> = summaries
    .filter { it.session.acceptedAt != null && it.session.endedAt != null }
    .groupBy { YearMonth.from(it.session.workoutStartedAt().atZone(zone)) }
    .mapValues { (_, workouts) -> workouts.sortedByDescending { it.session.workoutStartedAt() } }

internal data class WorkoutDay(val date: LocalDate, val workoutCount: Int)

internal fun workoutCalendarDays(
    month: YearMonth,
    workouts: List<SessionSummary>,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): List<WorkoutDay?> {
    val firstDate = month.atDay(1)
    val leadingDays = (firstDate.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val counts = workouts.groupingBy { it.session.workoutStartedAt().atZone(zone).toLocalDate() }.eachCount()
    return List(42) { index ->
        val day = index - leadingDays + 1
        if (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            WorkoutDay(date, counts[date] ?: 0)
        } else {
            null
        }
    }
}
