package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ExerciseEntity>>

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
}
