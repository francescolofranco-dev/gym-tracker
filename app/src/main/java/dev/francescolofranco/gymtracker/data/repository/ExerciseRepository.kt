package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSessionPoint
import dev.francescolofranco.gymtracker.domain.Muscle
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao
) {
    fun observeActive(): Flow<List<ExerciseEntity>> = dao.observeActive()

    fun observeById(id: Long): Flow<ExerciseEntity?> = dao.observeById(id)

    fun observeHistory(id: Long): Flow<List<ExerciseSessionPoint>> = dao.observeHistory(id)

    suspend fun byId(id: Long): ExerciseEntity? = dao.byId(id)

    suspend fun create(
        name: String,
        primaryMuscles: Set<Muscle>,
        secondaryMuscles: Set<Muscle>,
        targetSets: Int,
        repRangeMin: Int,
        repRangeMax: Int,
        isBodyweight: Boolean,
    ): Long = dao.insert(
        ExerciseEntity(
            name = name.trim(),
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles - primaryMuscles,
            targetSets = targetSets,
            repRangeMin = repRangeMin,
            repRangeMax = repRangeMax,
            isBodyweight = isBodyweight,
            createdAt = Instant.now(),
        )
    )

    suspend fun rename(id: Long, newName: String) = dao.rename(id, newName.trim())

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun restore(id: Long) = dao.restore(id)
}
