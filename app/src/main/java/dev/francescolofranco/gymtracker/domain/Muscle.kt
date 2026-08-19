package dev.francescolofranco.gymtracker.domain

enum class Muscle(val displayName: String) {
    CHEST("Chest"),
    LATS("Back (Lats)"),
    UPPER_BACK_TRAPS("Back (Upper / Traps)"),
    LOWER_BACK("Back (Lower)"),
    FRONT_DELTS("Shoulders (Front)"),
    SIDE_DELTS("Shoulders (Side)"),
    REAR_DELTS("Shoulders (Rear)"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    ADDUCTORS("Adductors"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core / Abs");

    companion object {
        const val WEEKLY_MIN = 3
        const val WEEKLY_MAX = 10
    }
}

enum class WeightUnit { KG, LBS }

enum class WeekMode { ROLLING_7, MON_SUN }

/** Which side performed a set. Bilateral exercises use [BOTH]. */
enum class ExerciseSide(val shortLabel: String, val displayName: String) {
    BOTH("", "Both sides"),
    LEFT("L", "Left"),
    RIGHT("R", "Right"),
}
