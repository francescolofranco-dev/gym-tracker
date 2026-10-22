package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.domain.Muscle

/** Workout groups for the session picker; detailed muscle assignments stay on each exercise. */
internal enum class PickerCategory(val displayName: String, val muscles: Set<Muscle>) {
    PUSH("Push", setOf(Muscle.CHEST, Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.TRICEPS)),
    PULL("Pull", setOf(Muscle.LATS, Muscle.UPPER_BACK_TRAPS, Muscle.LOWER_BACK, Muscle.REAR_DELTS, Muscle.BICEPS, Muscle.FOREARMS)),
    LEGS("Legs", setOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.ADDUCTORS, Muscle.GLUTES, Muscle.CALVES)),
    CORE("Core", setOf(Muscle.CORE));

    fun containsAny(candidates: Iterable<Muscle>): Boolean = candidates.any(muscles::contains)

    companion object {
        fun containing(muscle: Muscle): PickerCategory? = entries.firstOrNull { muscle in it.muscles }
    }
}
