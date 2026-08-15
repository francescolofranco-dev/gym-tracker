package dev.francescolofranco.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercise",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val orderInSession: Int,
    val notes: String? = null,
    /** Pinned notes are copied to every future session that contains this exercise. */
    val isNotePinned: Boolean = false,
    /** An unpinned note is copied once; the copied row sets this back to false. */
    val noteCarryForward: Boolean = false,
    val isSkipped: Boolean = false
)
