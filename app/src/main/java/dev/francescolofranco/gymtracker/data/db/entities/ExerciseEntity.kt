package dev.francescolofranco.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant

@Entity(
    tableName = "exercise",
    indices = [Index("deletedAt")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /**
     * 1-3 primary movers. Compound exercises like Bulgarian split squats legitimately hit
     * quads, glutes, and hamstrings as primaries; the old single-`primaryMuscle` model forced
     * an arbitrary pick. The form caps selection at 3 to keep the "everything is primary"
     * antipattern at bay.
     */
    val primaryMuscles: Set<Muscle>,
    val secondaryMuscles: Set<Muscle>,
    val targetSets: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val isBodyweight: Boolean,
    /** Target sets are interpreted per side when this is true. */
    val isUnilateral: Boolean = false,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)
