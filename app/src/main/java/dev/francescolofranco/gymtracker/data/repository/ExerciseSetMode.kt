package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.ExerciseSide

/** Only untouched sets can change meaning without interpreting the user's workout data. */
internal fun reconcileSetMode(sets: List<SetLogEntity>, unilateral: Boolean): List<SetLogEntity> =
    sets.groupBy { it.sessionExerciseId to it.setNumber }.values.flatMap { group ->
        if (group.any { it.reps != null || it.kg != null || it.loggedAt != null || it.isSkipped }) {
            group
        } else if (unilateral && group.size == 1 && group.single().side == ExerciseSide.BOTH) {
            val original = group.single()
            listOf(
                original.copy(side = ExerciseSide.LEFT),
                original.copy(id = 0, side = ExerciseSide.RIGHT),
            )
        } else if (!unilateral && group.size == 2 &&
            group.map { it.side }.toSet() == setOf(ExerciseSide.LEFT, ExerciseSide.RIGHT)
        ) {
            listOf(group.first { it.side == ExerciseSide.LEFT }.copy(side = ExerciseSide.BOTH))
        } else {
            group
        }
    }
