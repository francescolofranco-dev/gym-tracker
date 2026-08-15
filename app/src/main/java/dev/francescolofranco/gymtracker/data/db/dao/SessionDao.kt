package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.data.db.projections.StatSetRow
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {

    @Query("SELECT * FROM session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT s.*,
               COALESCE(COUNT(DISTINCT se.id), 0) AS exerciseCount,
               COALESCE(SUM(CASE WHEN sl.reps IS NOT NULL AND sl.isSkipped = 0 THEN 1 ELSE 0 END), 0) AS setCount,
               COALESCE(SUM(CASE WHEN sl.reps IS NOT NULL AND sl.isSkipped = 0
                                 THEN sl.reps * COALESCE(sl.kg, 0.0) ELSE 0 END), 0.0) AS totalVolume
        FROM session s
        LEFT JOIN session_exercise se ON se.sessionId = s.id AND se.isSkipped = 0
        LEFT JOIN set_log sl ON sl.sessionExerciseId = se.id
        WHERE s.acceptedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        """
    )
    fun observeAllSummaries(): Flow<List<SessionSummary>>

    // The "active" session can be a draft (acceptedAt = null) or accepted-but-not-ended; both
    // need the same row when the user re-enters from the Start button. The banner / past list
    // filter on acceptedAt separately.
    @Query("SELECT * FROM session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): SessionEntity?

    @Query("UPDATE session SET acceptedAt = :at WHERE id = :id")
    suspend fun accept(id: Long, at: Instant = Instant.now())

    @Query("SELECT * FROM session WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): SessionEntity?

    @Query("SELECT * FROM session WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<SessionEntity?>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("UPDATE session SET endedAt = :at WHERE id = :id")
    suspend fun end(id: Long, at: Instant = Instant.now())

    @Query("UPDATE session SET startedAt = :start, acceptedAt = :start, endedAt = :end WHERE id = :id")
    suspend fun updateTiming(id: Long, start: Instant, end: Instant)

    /**
     * Hard-deletes a session. Foreign keys on session_exercise and set_log are CASCADE so
     * all child rows go too.
     */
    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE session SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Insert
    suspend fun insertExercise(item: SessionExerciseEntity): Long

    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY orderInSession")
    fun observeExercises(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Transaction
    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY orderInSession")
    fun observeExerciseDetails(sessionId: Long): Flow<List<SessionExerciseDetail>>

    @Query("SELECT COALESCE(MAX(orderInSession) + 1, 0) FROM session_exercise WHERE sessionId = :sessionId")
    suspend fun nextOrderInSession(sessionId: Long): Int

    @Query("UPDATE session_exercise SET isSkipped = :skipped WHERE id = :id")
    suspend fun setExerciseSkipped(id: Long, skipped: Boolean)

    @Query(
        """
        UPDATE session_exercise
        SET notes = :notes,
            isNotePinned = :isPinned,
            noteCarryForward = :carryForward
        WHERE id = :id
        """,
    )
    suspend fun updateExerciseNote(
        id: Long,
        notes: String?,
        isPinned: Boolean,
        carryForward: Boolean,
    )

    /**
     * Most recent completed occurrence of an exercise before it is added to [targetSessionId].
     *
     * Looking at the latest occurrence even when it has no eligible note is intentional: a
     * consumed or explicitly cleared note must prevent an older note from resurfacing later.
     */
    @Query(
        """
        SELECT se.* FROM session_exercise se
        JOIN session s ON s.id = se.sessionId
        JOIN session target ON target.id = :targetSessionId
        WHERE se.exerciseId = :exerciseId
          AND se.sessionId != :targetSessionId
          AND s.acceptedAt IS NOT NULL
          AND s.endedAt IS NOT NULL
          AND (
              s.startedAt < target.startedAt OR
              (s.startedAt = target.startedAt AND s.id < target.id)
          )
        ORDER BY s.startedAt DESC, s.id DESC, se.id DESC
        LIMIT 1
        """,
    )
    suspend fun latestExerciseOccurrence(
        exerciseId: Long,
        targetSessionId: Long,
    ): SessionExerciseEntity?

    @Query("DELETE FROM session_exercise WHERE id = :id")
    suspend fun removeExercise(id: Long)

    @Query("SELECT * FROM session_exercise WHERE id = :id LIMIT 1")
    suspend fun sessionExerciseById(id: Long): SessionExerciseEntity?

    @Query("UPDATE session_exercise SET orderInSession = :order WHERE id = :id")
    suspend fun setExerciseOrder(id: Long, order: Int)

    /**
     * Swap orderInSession between two session_exercise rows. Wrapped in a transaction so a
     * crash mid-swap can't leave the list with two rows at the same order (which would make
     * sorting unstable). Caller is responsible for picking the right pair (an exercise + its
     * neighbour) — this is a generic "exchange these two orders" primitive.
     */
    @Transaction
    suspend fun swapExerciseOrder(idA: Long, idB: Long) {
        val a = sessionExerciseById(idA) ?: return
        val b = sessionExerciseById(idB) ?: return
        setExerciseOrder(idA, b.orderInSession)
        setExerciseOrder(idB, a.orderInSession)
    }

    @Query("SELECT * FROM session ORDER BY id")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM session_exercise ORDER BY id")
    suspend fun allSessionExercises(): List<SessionExerciseEntity>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun replaceSessions(items: List<SessionEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun replaceSessionExercises(items: List<SessionExerciseEntity>)

    @Query("DELETE FROM session")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM session_exercise")
    suspend fun deleteAllSessionExercises()

    /** Latest log timestamp across all sets in a session, or null if nothing logged yet. */
    @Query(
        """
        SELECT MAX(sl.loggedAt) FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id
        WHERE se.sessionId = :sessionId AND sl.loggedAt IS NOT NULL
        """
    )
    suspend fun lastActivityAt(sessionId: Long): Instant?

    /**
     * Logged sets in a date range with denormalised exercise + session info.
     * Filters out skipped session-exercises and skipped/unlogged sets.
     */
    @Query(
        """
        SELECT s.id AS sessionId, COALESCE(s.acceptedAt, s.startedAt) AS sessionStartedAt,
               e.id AS exerciseId, e.name AS exerciseName,
               e.primaryMuscles AS primaryMuscles, e.secondaryMuscles AS secondaryMuscles,
               e.isBodyweight AS isBodyweight, e.isUnilateral AS isUnilateral,
               sl.setNumber AS setNumber, sl.side AS side,
               sl.reps AS reps, sl.kg AS kg
        FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id AND se.isSkipped = 0
        JOIN exercise e ON se.exerciseId = e.id
        JOIN session s ON se.sessionId = s.id
        WHERE sl.reps IS NOT NULL AND sl.isSkipped = 0
          AND COALESCE(s.acceptedAt, s.startedAt) >= :startInclusive
          AND COALESCE(s.acceptedAt, s.startedAt) < :endExclusive
        ORDER BY COALESCE(s.acceptedAt, s.startedAt)
        """
    )
    fun observeLoggedSetsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<StatSetRow>>

    /** Snapshot query used by tests, backup previews, and explicit refreshes. */
    @Query(
        """
        SELECT s.id AS sessionId, COALESCE(s.acceptedAt, s.startedAt) AS sessionStartedAt,
               e.id AS exerciseId, e.name AS exerciseName,
               e.primaryMuscles AS primaryMuscles, e.secondaryMuscles AS secondaryMuscles,
               e.isBodyweight AS isBodyweight, e.isUnilateral AS isUnilateral,
               sl.setNumber AS setNumber, sl.side AS side, sl.reps AS reps, sl.kg AS kg
        FROM set_log sl
        JOIN session_exercise se ON sl.sessionExerciseId = se.id AND se.isSkipped = 0
        JOIN exercise e ON se.exerciseId = e.id
        JOIN session s ON se.sessionId = s.id
        WHERE sl.reps IS NOT NULL AND sl.isSkipped = 0
          AND COALESCE(s.acceptedAt, s.startedAt) >= :startInclusive
          AND COALESCE(s.acceptedAt, s.startedAt) < :endExclusive
        ORDER BY COALESCE(s.acceptedAt, s.startedAt)
        """
    )
    suspend fun loggedSetsBetween(startInclusive: Instant, endExclusive: Instant): List<StatSetRow>
}
