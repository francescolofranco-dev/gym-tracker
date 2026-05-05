package dev.francescolofranco.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM template ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<TemplateEntity>

    @Query("SELECT * FROM template WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): TemplateEntity?

    @Query("SELECT * FROM template WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TemplateEntity?>

    @Insert
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    @Query("UPDATE template SET name = :name WHERE id = :id")
    suspend fun renameTemplate(id: Long, name: String)

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

    /**
     * Resolves a template's exercises (ordered, excluding soft-deleted ones). Returns an empty
     * list for unknown template ids.
     */
    @Query(
        """
        SELECT e.* FROM template_exercise te
        JOIN exercise e ON e.id = te.exerciseId
        WHERE te.templateId = :templateId AND e.deletedAt IS NULL
        ORDER BY te.orderInTemplate
        """
    )
    fun observeExercisesFor(templateId: Long): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT e.* FROM template_exercise te
        JOIN exercise e ON e.id = te.exerciseId
        WHERE te.templateId = :templateId AND e.deletedAt IS NULL
        ORDER BY te.orderInTemplate
        """
    )
    suspend fun exercisesFor(templateId: Long): List<ExerciseEntity>
}
