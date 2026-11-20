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
        ),
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["supersetLeadId"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true,
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index("supersetLeadId", unique = true)]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val orderInSession: Int,
    val notes: String? = null,
    /** Legacy column retained for database compatibility; new writes leave it false. */
    val isNotePinned: Boolean = false,
    /** Independent note copied to every future occurrence. */
    val pinnedNotes: String? = null,
    /** An unpinned note is copied once; the copied row sets this back to false. */
    val noteCarryForward: Boolean = false,
    val isSkipped: Boolean = false,
    /** The first exercise in this superset, stored on the second exercise only. */
    val supersetLeadId: Long? = null,
)
