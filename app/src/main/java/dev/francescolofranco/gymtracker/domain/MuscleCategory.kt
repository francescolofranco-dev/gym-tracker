package dev.francescolofranco.gymtracker.domain

/**
 * Broad, presentation-only categories used to browse exercises without changing the detailed
 * muscles persisted on each exercise.
 */
enum class MuscleCategory(
    val displayName: String,
    val muscles: Set<Muscle>,
) {
    CHEST_AND_SHOULDERS(
        displayName = "Chest & shoulders",
        muscles = setOf(
            Muscle.CHEST,
            Muscle.FRONT_DELTS,
            Muscle.SIDE_DELTS,
            Muscle.REAR_DELTS,
        ),
    ),
    BACK(
        displayName = "Back",
        muscles = setOf(
            Muscle.LATS,
            Muscle.UPPER_BACK_TRAPS,
            Muscle.LOWER_BACK,
        ),
    ),
    ARMS(
        displayName = "Arms",
        muscles = setOf(
            Muscle.BICEPS,
            Muscle.TRICEPS,
            Muscle.FOREARMS,
        ),
    ),
    CORE(
        displayName = "Core",
        muscles = setOf(Muscle.CORE),
    ),
    LEGS_AND_GLUTES(
        displayName = "Legs & glutes",
        muscles = setOf(
            Muscle.QUADS,
            Muscle.HAMSTRINGS,
            Muscle.ADDUCTORS,
            Muscle.GLUTES,
            Muscle.CALVES,
        ),
    ),
    ;

    fun containsAny(candidates: Iterable<Muscle>): Boolean = candidates.any(muscles::contains)

    companion object {
        fun containing(muscle: Muscle): MuscleCategory? =
            entries.firstOrNull { muscle in it.muscles }
    }
}
