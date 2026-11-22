package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutHistoryTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun `completed workouts use their local workout start for the month and ordering`() {
        val firstDecember = workout(1, "2025-12-31T20:00:00Z")
        val newYear = workout(2, "2025-12-31T21:00:00Z", acceptedAt = "2025-12-31T23:30:00Z")
        val secondDecember = workout(3, "2025-12-31T22:00:00Z")
        val lastDecember = workout(4, "2025-12-31T18:00:00Z", acceptedAt = "2025-12-31T22:15:00Z")
        val draft = workout(5, "2026-01-01T10:00:00Z", acceptedAt = null, completed = false)
        val active = workout(6, "2026-01-01T12:00:00Z", completed = false)

        val history = completedWorkoutsByMonth(
            listOf(secondDecember, newYear, firstDecember, lastDecember, draft, active),
            zone,
        )

        assertEquals(
            mapOf(
                YearMonth.of(2025, 12) to listOf(lastDecember, secondDecember, firstDecember),
                YearMonth.of(2026, 1) to listOf(newYear),
            ),
            history,
        )
    }

    @Test
    fun `leap day can contain multiple workouts and adjacent month cells stay blank`() {
        val days = workoutCalendarDays(
            month = YearMonth.of(2024, 2),
            workouts = listOf(workout(1, "2024-02-28T23:20:00Z"), workout(2, "2024-02-29T15:00:00Z")),
            zone = zone,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(42, days.size)
        assertNull(days[2])
        assertEquals(WorkoutDay(LocalDate.of(2024, 2, 1), 0), days[3])
        assertEquals(WorkoutDay(LocalDate.of(2024, 2, 29), 2), days[31])
        assertNull(days[32])
    }

    @Test
    fun `Sunday first calendars leave room for months that span six weeks`() {
        val days = workoutCalendarDays(YearMonth.of(2026, 8), emptyList(), zone, DayOfWeek.SUNDAY)

        assertNull(days[5])
        assertEquals(WorkoutDay(LocalDate.of(2026, 8, 1), 0), days[6])
        assertEquals(WorkoutDay(LocalDate.of(2026, 8, 31), 0), days[36])
        assertNull(days[37])
    }

    private fun workout(
        id: Long,
        startedAt: String,
        acceptedAt: String? = startedAt,
        completed: Boolean = true,
    ): SessionSummary = SessionSummary(
        session = SessionEntity(
            id = id,
            startedAt = Instant.parse(startedAt),
            acceptedAt = acceptedAt?.let(Instant::parse),
            endedAt = if (completed) Instant.parse(requireNotNull(acceptedAt)).plusSeconds(3600) else null,
        ),
        exerciseCount = 1,
        setCount = 3,
        totalVolume = 100.0,
    )
}
