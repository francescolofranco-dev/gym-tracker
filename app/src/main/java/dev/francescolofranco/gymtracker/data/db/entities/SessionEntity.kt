package dev.francescolofranco.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("startedAt"), Index("endedAt"), Index("templateId")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val notes: String? = null,
    val templateId: Long? = null,
    /**
     * When non-null, the user has explicitly hit "Start workout" — the session is real and
     * shows in the in-progress banner / past-session history. Null = a draft created on
     * tap-Start that's still in the exercise-selection phase; drafts are invisible everywhere
     * outside the live setup screen.
     */
    val acceptedAt: Instant? = null,
)
