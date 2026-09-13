package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity

@Dao
interface ExerciseModeDao {
    /** Ended sessions retain their original rows, including unfinished and skipped sets. */
    @Query("""
        SELECT sl.* FROM set_log sl
        JOIN session_exercise se ON se.id = sl.sessionExerciseId
        JOIN session s ON s.id = se.sessionId
        WHERE se.exerciseId = :exerciseId AND s.endedAt IS NULL AND se.isSkipped = 0
        ORDER BY sl.sessionExerciseId, sl.setNumber, sl.id
    """)
    suspend fun openSessionSets(exerciseId: Long): List<SetLogEntity>
}
