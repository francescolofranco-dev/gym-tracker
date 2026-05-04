package dev.francescolofranco.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.francescolofranco.gymtracker.domain.Muscle
import java.time.Instant

@Entity(
    tableName = "exercise",
    indices = [Index("primaryMuscle"), Index("deletedAt")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: Set<Muscle>,
    val targetSets: Int,
    val repRangeMin: Int,
    val repRangeMax: Int,
    val isBodyweight: Boolean,
    val createdAt: Instant,
    val deletedAt: Instant? = null
)
