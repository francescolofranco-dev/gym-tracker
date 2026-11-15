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

    @Query(
        """
        UPDATE set_log
        SET kg = CASE WHEN :isNegativeWeight THEN -ABS(kg) ELSE ABS(kg) END
        WHERE sessionExerciseId IN (
            SELECT id FROM session_exercise WHERE exerciseId = :exerciseId
        ) AND kg IS NOT NULL AND kg != 0.0
        """
    )
    suspend fun normalizeExerciseSetWeights(exerciseId: Long, isNegativeWeight: Boolean)
}
