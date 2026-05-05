package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.TemplateDao
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.TemplateWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val dao: TemplateDao,
    private val sessionDao: SessionDao,
) {
    fun observeAll(): Flow<List<TemplateEntity>> = dao.observeAll()

    fun observeWithExercises(templateId: Long): Flow<TemplateWithExercises?> =
        combine(dao.observeById(templateId), dao.observeExercisesFor(templateId)) { template, exercises ->
            template?.let { TemplateWithExercises(it, exercises) }
        }

    suspend fun exercisesFor(templateId: Long): List<ExerciseEntity> = dao.exercisesFor(templateId)

    suspend fun create(name: String, exerciseIds: List<Long>): Long {
        val templateId = dao.insertTemplate(
            TemplateEntity(name = name.trim(), createdAt = Instant.now())
        )
        exerciseIds.forEachIndexed { index, exerciseId ->
            dao.insertTemplateExercise(
                TemplateExerciseEntity(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderInTemplate = index,
                )
            )
        }
        return templateId
    }

    suspend fun update(templateId: Long, name: String, exerciseIds: List<Long>) {
        dao.renameTemplate(templateId, name.trim())
        dao.clearTemplateExercises(templateId)
        exerciseIds.forEachIndexed { index, exerciseId ->
            dao.insertTemplateExercise(
                TemplateExerciseEntity(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderInTemplate = index,
                )
            )
        }
    }

    suspend fun delete(templateId: Long) = dao.deleteTemplate(templateId)

    /**
     * Cyclic rotation suggestion: pick the template that comes after the most recent
     * template-driven session in the (alphabetical) template ordering. Falls back to the first
     * template when there's no template-driven history yet.
     */
    fun observeNextSuggestion(): Flow<TemplateEntity?> =
        combine(dao.observeAll(), sessionDao.observeAll()) { templates, sessions ->
            if (templates.isEmpty()) null
            else {
                val lastTemplateId = sessions.firstOrNull { it.templateId != null }?.templateId
                if (lastTemplateId == null) {
                    templates.first()
                } else {
                    val idx = templates.indexOfFirst { it.id == lastTemplateId }
                    if (idx == -1) templates.first()
                    else templates[(idx + 1) % templates.size]
                }
            }
        }
}
