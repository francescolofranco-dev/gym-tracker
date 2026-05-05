package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSessionPoint
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY id")
    suspend fun all(): List<ExerciseEntity>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun replaceAll(items: List<ExerciseEntity>)

    @Query("DELETE FROM exercise")
    suspend fun deleteAll()

    /**
     * Active exercises ordered by most-recently-used first, then alphabetically.
     * SQLite has no NULLS LAST; the `IS NULL` boolean coerces to 0/1 so non-null sorts ahead.
     */
    @Query(
        """
        SELECT e.*, MAX(s.startedAt) AS lastUsedAt
        FROM exercise e
        LEFT JOIN session_exercise se ON se.exerciseId = e.id
        LEFT JOIN session s ON se.sessionId = s.id
        WHERE e.deletedAt IS NULL
        GROUP BY e.id
        ORDER BY (lastUsedAt IS NULL), lastUsedAt DESC, e.name COLLATE NOCASE
        """
    )
    fun observeActiveByRecency(): Flow<List<ExerciseWithRecency>>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercise SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("UPDATE exercise SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: Long, at: Instant = Instant.now())

    @Query("UPDATE exercise SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    /**
     * Per-session aggregate for an exercise: one row per session that included a non-skipped
     * occurrence of the exercise, with set count + total volume. Sessions with no logged sets
     * for the exercise are filtered out (HAVING setsLogged > 0).
     */
    @Query(
        """
        SELECT s.id AS sessionId,
               s.startedAt AS sessionStartedAt,
               s.endedAt AS sessionEndedAt,
               COUNT(sl.id) AS setsLogged,
               COALESCE(SUM(sl.reps * COALESCE(sl.kg, 0.0)), 0.0) AS volumeKg
        FROM session s
        JOIN session_exercise se ON se.sessionId = s.id AND se.isSkipped = 0
        LEFT JOIN set_log sl ON sl.sessionExerciseId = se.id
            AND sl.reps IS NOT NULL AND sl.isSkipped = 0
        WHERE se.exerciseId = :exerciseId
        GROUP BY s.id
        HAVING setsLogged > 0
        ORDER BY s.startedAt ASC
        """
    )
    fun observeHistory(exerciseId: Long): Flow<List<ExerciseSessionPoint>>
}
