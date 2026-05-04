package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {

    @Query("SELECT * FROM set_log WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    fun observeForSessionExercise(sessionExerciseId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_log WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    suspend fun forSessionExercise(sessionExerciseId: Long): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setLog: SetLogEntity): Long

    @Update
    suspend fun update(setLog: SetLogEntity)

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Latest set logs for a given exercise across the user's history (used for "last time" pre-fill).
     * Returns at most [limit] sets ordered by their original setNumber.
     */
    @Query(
        """
        SELECT sl.* FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id
        JOIN session s ON se.sessionId = s.id
        WHERE se.exerciseId = :exerciseId
          AND sl.reps IS NOT NULL
          AND sl.isSkipped = 0
        ORDER BY s.startedAt DESC, sl.setNumber ASC
        LIMIT :limit
        """
    )
    suspend fun lastPerformed(exerciseId: Long, limit: Int): List<SetLogEntity>
}
