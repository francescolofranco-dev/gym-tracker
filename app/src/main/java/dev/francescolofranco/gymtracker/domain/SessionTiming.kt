package dev.francescolofranco.gymtracker.domain

import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import java.time.Duration
import java.time.Instant

/** The real workout starts when a draft is accepted, not when its setup screen was opened. */
fun SessionEntity.workoutStartedAt(): Instant = acceptedAt ?: startedAt

fun SessionEntity.workoutDuration(now: Instant = Instant.now()): Duration =
    Duration.between(workoutStartedAt(), endedAt ?: now).coerceAtLeast(Duration.ZERO)
