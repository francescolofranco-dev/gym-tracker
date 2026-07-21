package dev.francescolofranco.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import java.time.Instant

@Entity(
    tableName = "set_log",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionExerciseId")]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val side: ExerciseSide = ExerciseSide.BOTH,
    val reps: Int? = null,
    val kg: Double? = null,
    val isSkipped: Boolean = false,
    val loggedAt: Instant? = null
)
