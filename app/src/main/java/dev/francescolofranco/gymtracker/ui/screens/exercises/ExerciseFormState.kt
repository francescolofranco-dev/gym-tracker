package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.Muscle

data class ExerciseFormState(
    val name: String = "",
    val primaryMuscle: Muscle? = null,
    val secondaryMuscles: Set<Muscle> = emptySet(),
    val targetSets: Int = 4,
    val repRangeMin: Int = 8,
    val repRangeMax: Int = 12,
    val isBodyweight: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
                primaryMuscle != null &&
                targetSets in 1..MAX_SETS &&
                repRangeMin in 1..MAX_REPS &&
                repRangeMax in repRangeMin..MAX_REPS

    companion object {
        const val MAX_SETS = 10
        const val MAX_REPS = 50
    }
}

fun ExerciseEntity.toFormState(): ExerciseFormState = ExerciseFormState(
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    targetSets = targetSets,
    repRangeMin = repRangeMin,
    repRangeMax = repRangeMax,
    isBodyweight = isBodyweight,
)
