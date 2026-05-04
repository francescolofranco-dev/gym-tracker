package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SessionDao {

    @Query("SELECT * FROM session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

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

    @Query("UPDATE session SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Insert
    suspend fun insertExercise(item: SessionExerciseEntity): Long

    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY orderInSession")
    fun observeExercises(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("UPDATE session_exercise SET isSkipped = :skipped WHERE id = :id")
    suspend fun setExerciseSkipped(id: Long, skipped: Boolean)

    @Query("UPDATE session_exercise SET notes = :notes WHERE id = :id")
    suspend fun updateExerciseNotes(id: Long, notes: String?)

    @Query("DELETE FROM session_exercise WHERE id = :id")
    suspend fun removeExercise(id: Long)
}
