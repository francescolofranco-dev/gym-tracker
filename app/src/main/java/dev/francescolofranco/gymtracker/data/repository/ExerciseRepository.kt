package dev.francescolofranco.gymtracker.data.repository

import androidx.room.withTransaction
import dev.francescolofranco.gymtracker.data.db.AppDatabase
import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import dev.francescolofranco.gymtracker.domain.Muscle
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    private val db: AppDatabase,
) {
    fun observeActive(): Flow<List<ExerciseEntity>> = dao.observeActive()

    fun observeById(id: Long): Flow<ExerciseEntity?> = dao.observeById(id)

    fun observeSetHistory(id: Long): Flow<List<ExerciseSetRow>> = dao.observeSetHistory(id)

    suspend fun byId(id: Long): ExerciseEntity? = dao.byId(id)

    suspend fun create(
        name: String,
        primaryMuscles: Set<Muscle>,
        secondaryMuscles: Set<Muscle>,
        targetSets: Int,
        repRangeMin: Int,
        repRangeMax: Int,
        isBodyweight: Boolean,
        isUnilateral: Boolean,
    ): Long = dao.insert(
        ExerciseEntity(
            name = name.trim(),
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles - primaryMuscles,
            targetSets = targetSets,
            repRangeMin = repRangeMin,
            repRangeMax = repRangeMax,
            isBodyweight = isBodyweight,
            isUnilateral = isUnilateral,
            createdAt = Instant.now(),
        )
    )

    suspend fun rename(id: Long, newName: String) = dao.rename(id, newName.trim())

    /**
     * Update every editable field of an existing exercise. Because stats queries join through
     * the exercise table at read time (no muscle attribution is cached on session_exercise or
     * set_log), an edit here transparently re-attributes every historical set to the new
     * muscle assignment — exactly what the user wants when they realise an exercise was
     * mis-categorised. Preserves id, createdAt, and deletedAt.
     */
    suspend fun update(
        id: Long,
        name: String,
        primaryMuscles: Set<Muscle>,
        secondaryMuscles: Set<Muscle>,
        targetSets: Int,
        repRangeMin: Int,
        repRangeMax: Int,
        isBodyweight: Boolean,
        isUnilateral: Boolean,
    ) = db.withTransaction {
        val current = dao.byId(id) ?: return@withTransaction
        if (current.isUnilateral != isUnilateral) {
            val original = db.exerciseModeDao().openSessionSets(id)
            val reconciled = reconcileSetMode(original, isUnilateral)
            val retainedIds = reconciled.map { it.id }.toSet()
            val setDao = db.setLogDao()
            original.filter { it.id !in retainedIds }.forEach { setDao.delete(it.id) }
            val originalsById = original.associateBy { it.id }
            reconciled.forEach { row ->
                if (row.id == 0L) setDao.upsert(row)
                else if (row != originalsById[row.id]) setDao.update(row)
            }
        }
        dao.update(
            current.copy(
                name = name.trim(),
                primaryMuscles = primaryMuscles,
                secondaryMuscles = secondaryMuscles - primaryMuscles,
                targetSets = targetSets,
                repRangeMin = repRangeMin,
                repRangeMax = repRangeMax,
                isBodyweight = isBodyweight,
                isUnilateral = isUnilateral,
            ),
        )
    }

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun restore(id: Long) = dao.restore(id)
}
