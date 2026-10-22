package dev.francescolofranco.gymtracker.domain

/**
 * Broad, presentation-only categories used to browse exercises without changing the detailed
 * muscles persisted on each exercise.
 */
enum class MuscleCategory(
    val displayName: String,
    val muscles: Set<Muscle>,
) {
    PUSH(
        displayName = "Push",
        muscles = setOf(
            Muscle.CHEST,
            Muscle.FRONT_DELTS,
            Muscle.SIDE_DELTS,
            Muscle.TRICEPS,
        ),
    ),
    PULL(
        displayName = "Pull",
        muscles = setOf(
            Muscle.LATS,
            Muscle.UPPER_BACK_TRAPS,
            Muscle.LOWER_BACK,
            Muscle.REAR_DELTS,
            Muscle.BICEPS,
            Muscle.FOREARMS,
        ),
    ),
    LEGS(
        displayName = "Legs",
        muscles = setOf(
            Muscle.QUADS,
            Muscle.HAMSTRINGS,
            Muscle.ADDUCTORS,
            Muscle.GLUTES,
            Muscle.CALVES,
        ),
    ),
    CORE(
        displayName = "Core",
        muscles = setOf(Muscle.CORE),
    ),
    ;

    fun containsAny(candidates: Iterable<Muscle>): Boolean = candidates.any(muscles::contains)

    companion object {
        fun containing(muscle: Muscle): MuscleCategory? =
            entries.firstOrNull { muscle in it.muscles }
    }
}
