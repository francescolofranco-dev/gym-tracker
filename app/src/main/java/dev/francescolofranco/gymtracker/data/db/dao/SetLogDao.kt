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

    @Query("SELECT * FROM set_log WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): SetLogEntity?

    @Query("SELECT * FROM set_log ORDER BY id")
    suspend fun all(): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(items: List<SetLogEntity>)

    @Query("DELETE FROM set_log")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setLog: SetLogEntity): Long

    @Update
    suspend fun update(setLog: SetLogEntity)

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Reps/kg from the last completed session of an exercise (used to pre-fill stepper values).
     * Returns up to [limit] sets ordered by their original setNumber.
     */
    @Query(
        """
        SELECT sl.* FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId
          AND sl.reps IS NOT NULL
          AND sl.isSkipped = 0
          AND se.sessionId = (
              SELECT s.id FROM session s
              JOIN session_exercise se2 ON se2.sessionId = s.id
              JOIN set_log sl2 ON sl2.sessionExerciseId = se2.id
              WHERE se2.exerciseId = :exerciseId
                AND sl2.reps IS NOT NULL
                AND sl2.isSkipped = 0
              ORDER BY s.startedAt DESC
              LIMIT 1
          )
        ORDER BY sl.setNumber ASC
        LIMIT :limit
        """
    )
    suspend fun lastSessionSets(exerciseId: Long, limit: Int): List<SetLogEntity>

    /**
     * Sets from the most-recent COMPLETED session that started strictly before the anchor.
     * Two filters do the heavy lifting:
     *  - `s.endedAt IS NOT NULL` excludes the currently-active session (whose anchor we'd
     *    otherwise reflect onto itself, producing the "0% always" bug the user hit).
     *  - `s.startedAt < :beforeStartedAtEpochMs` lets the past-session detail screen anchor
     *    to the session immediately before the one being viewed.
     * For active sessions, callers pass the active session's startedAt as the anchor.
     */
    @Query(
        """
        SELECT sl.* FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId
          AND sl.reps IS NOT NULL
          AND sl.isSkipped = 0
          AND se.sessionId = (
              SELECT s.id FROM session s
              JOIN session_exercise se2 ON se2.sessionId = s.id
              JOIN set_log sl2 ON sl2.sessionExerciseId = se2.id
              WHERE se2.exerciseId = :exerciseId
                AND sl2.reps IS NOT NULL
                AND sl2.isSkipped = 0
                AND s.startedAt < :beforeStartedAtEpochMs
                AND s.endedAt IS NOT NULL
              ORDER BY s.startedAt DESC
              LIMIT 1
          )
        ORDER BY sl.setNumber ASC
        LIMIT :limit
        """
    )
    suspend fun lastSessionSetsBefore(
        exerciseId: Long,
        beforeStartedAtEpochMs: Long,
        limit: Int,
    ): List<SetLogEntity>

    /**
     * Most recent logged value for ONE specific set number, searched independently across all
     * ended sessions before the anchor — not confined to a single session's rows.
     *
     * [lastSessionSetsBefore] anchors to one session and returns whatever it logged; if that
     * session only completed set 1 before being abandoned, callers used to fall back to reusing
     * set 1's value as the hint for sets 2-4 too. That made the "vs last time" % badge compare
     * e.g. today's set 3 against the *previous session's set 1* instead of the last time set 3
     * was actually performed — which can flip the sign of the delta (looks like a regression when
     * it's really an improvement). This query instead resolves each set number on its own.
     */
    @Query(
        """
        SELECT sl.* FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id
        JOIN session s ON s.id = se.sessionId
        WHERE se.exerciseId = :exerciseId
          AND sl.setNumber = :setNumber
          AND sl.reps IS NOT NULL
          AND sl.isSkipped = 0
          AND s.startedAt < :beforeStartedAtEpochMs
          AND s.endedAt IS NOT NULL
        ORDER BY s.startedAt DESC
        LIMIT 1
        """
    )
    suspend fun lastLoggedSetBefore(
        exerciseId: Long,
        setNumber: Int,
        beforeStartedAtEpochMs: Long,
    ): SetLogEntity?
}
