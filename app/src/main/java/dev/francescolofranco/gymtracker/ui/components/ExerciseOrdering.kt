package dev.francescolofranco.gymtracker.ui.components

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.topmost
import java.util.Locale

/** The primary muscle that places an exercise highest in a top-to-bottom body scan. */
internal fun ExerciseEntity.anatomicalLeadMuscle(): Muscle? = primaryMuscles.topmost()

/**
 * Stable exercise order for browsing: highest primary muscle first, then name. Remaining primary
 * muscles do not reorder exercises inside the same section; that keeps each section predictable.
 */
internal val ExerciseTopToBottomComparator: Comparator<ExerciseEntity> =
    compareBy<ExerciseEntity> {
        it.anatomicalLeadMuscle()?.anatomicalRank ?: Int.MAX_VALUE
    }.thenBy {
        it.name.lowercase(Locale.ROOT)
    }.thenBy {
        it.id
    }
