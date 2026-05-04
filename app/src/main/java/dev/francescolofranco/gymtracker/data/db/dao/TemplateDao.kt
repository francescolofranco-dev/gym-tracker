package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM template ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): TemplateEntity?

    @Insert
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Insert
    suspend fun insertTemplateExercise(item: TemplateExerciseEntity)

    @Query("DELETE FROM template_exercise WHERE templateId = :templateId")
    suspend fun clearTemplateExercises(templateId: Long)

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY orderInTemplate")
    fun observeTemplateExercises(templateId: Long): Flow<List<TemplateExerciseEntity>>

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY orderInTemplate")
    suspend fun templateExercises(templateId: Long): List<TemplateExerciseEntity>
}
