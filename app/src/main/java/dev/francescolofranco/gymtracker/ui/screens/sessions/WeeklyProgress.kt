package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import java.time.Instant
import java.time.ZoneId

/** Today and the six preceding local dates, matching session history. Drafts never count. */
internal fun completedInLastSevenDays(
    sessions: List<SessionEntity>,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    val start = now.atZone(zone).toLocalDate().minusDays(6).atStartOfDay(zone).toInstant()
    return sessions.count {
        val date = it.workoutStartedAt()
        it.acceptedAt != null && it.endedAt != null &&
            !date.isBefore(start) && !date.isAfter(now)
    }
}
