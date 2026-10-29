package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Calendar weeks use the device timezone, matching session history dates. Drafts never count. */
internal fun completedThisWeek(
    sessions: List<SessionEntity>,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    val monday = now.atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = monday.atStartOfDay(zone).toInstant()
    val end = monday.plusWeeks(1).atStartOfDay(zone).toInstant()
    return sessions.count {
        val date = it.workoutStartedAt()
        it.acceptedAt != null && it.endedAt != null &&
            !date.isBefore(start) && date.isBefore(end) && !date.isAfter(now)
    }
}
