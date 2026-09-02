package dev.francescolofranco.gymtracker.domain

enum class Muscle(
    val displayName: String,
    /** Stable visual position from the top of the body to the bottom. */
    val anatomicalRank: Int,
) {
    // Keep declaration order independent from presentation order. Enum names are persisted by
    // Room and backups; an explicit rank lets the UX evolve without coupling it to storage.
    CHEST("Chest", 4),
    LATS("Back (Lats)", 5),
    UPPER_BACK_TRAPS("Back (Upper / Traps)", 0),
    LOWER_BACK("Back (Lower)", 10),
    FRONT_DELTS("Shoulders (Front)", 1),
    SIDE_DELTS("Shoulders (Side)", 2),
    REAR_DELTS("Shoulders (Rear)", 3),
    BICEPS("Biceps", 6),
    TRICEPS("Triceps", 7),
    FOREARMS("Forearms", 8),
    QUADS("Quads", 13),
    HAMSTRINGS("Hamstrings", 14),
    ADDUCTORS("Adductors", 12),
    GLUTES("Glutes", 11),
    CALVES("Calves", 15),
    CORE("Core / Abs", 9);

    companion object {
        const val WEEKLY_MIN = 3
        const val WEEKLY_MAX = 10

        /** Canonical display order for body-oriented navigation and labels. */
        val topToBottom: List<Muscle> by lazy { entries.sortedBy { it.anatomicalRank } }
    }
}

/** The anatomically highest muscle in this collection, independent of set or enum order. */
fun Iterable<Muscle>.topmost(): Muscle? = minByOrNull { it.anatomicalRank }

fun Iterable<Muscle>.sortedTopToBottom(): List<Muscle> = sortedBy { it.anatomicalRank }

enum class WeightUnit { KG, LBS }

enum class WeekMode { ROLLING_7, MON_SUN }

/** Which side performed a set. Bilateral exercises use [BOTH]. */
enum class ExerciseSide(val shortLabel: String, val displayName: String) {
    BOTH("", "Both sides"),
    LEFT("L", "Left"),
    RIGHT("R", "Right"),
}
