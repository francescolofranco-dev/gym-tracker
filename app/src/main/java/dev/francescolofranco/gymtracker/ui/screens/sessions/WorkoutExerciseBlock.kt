package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail

/** A single exercise or a superset, positioned at the first exercise's place in the workout. */
internal data class WorkoutExerciseBlock(
    val lead: SessionExerciseDetail,
    val partner: SessionExerciseDetail? = null,
) {
    val id = lead.sessionExercise.id
    val exercises = listOfNotNull(lead, partner)
    val rows = exercises.flatMap { exercise ->
        exercise.setLogs.map { WorkoutSet(exercise, it) }
    }.sortedWith(compareBy({ it.log.setNumber }, { it.exercise.sessionExercise.id != id }, { it.log.side.ordinal }))
    val nextSetId = rows.firstOrNull { (exercise, log) ->
        !exercise.sessionExercise.isSkipped && !log.isSkipped &&
            (log.loggedAt == null || log.reps == null)
    }?.log?.id

    val canPair = partner == null && !lead.sessionExercise.isSkipped
    // Header and gap, plus the columns and each set row when there are sets.
    val itemCount = 2 + rows.size + if (rows.isEmpty()) 0 else 1
    val title = if (partner == null) lead.exercise.name else "Superset"
}

internal data class WorkoutSet(val exercise: SessionExerciseDetail, val log: SetLogEntity)

internal fun List<SessionExerciseDetail>.workoutExerciseBlocks(): List<WorkoutExerciseBlock> {
    val byId = associateBy { it.sessionExercise.id }
    val partners = filter { it.sessionExercise.supersetLeadId in byId }
        .associateBy { it.sessionExercise.supersetLeadId }
    val partnerIds = partners.values.mapTo(HashSet()) { it.sessionExercise.id }
    return filterNot { it.sessionExercise.id in partnerIds }.map { lead ->
        WorkoutExerciseBlock(lead, partners[lead.sessionExercise.id])
    }
}
