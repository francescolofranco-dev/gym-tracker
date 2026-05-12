package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.SetLogDao
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val setLogDao: SetLogDao,
    private val exerciseDao: ExerciseDao,
    private val templateDao: dev.francescolofranco.gymtracker.data.db.dao.TemplateDao,
) {

    fun observeAllSummaries(): Flow<List<SessionSummary>> = sessionDao.observeAllSummaries()

    fun observeActive(): Flow<SessionEntity?> = sessionDao.observeActive()

    fun observeSession(id: Long): Flow<SessionEntity?> = sessionDao.observeById(id)

    fun observeExerciseDetails(sessionId: Long): Flow<List<SessionExerciseDetail>> =
        sessionDao.observeExerciseDetails(sessionId)

    fun observePickerExercises(): Flow<List<ExerciseWithRecency>> =
        exerciseDao.observeActiveByRecency()

    suspend fun activeSession(): SessionEntity? = sessionDao.activeSession()

    /**
     * Start a new session. If [templateId] is non-null, also seeds the session with the
     * template's (non-deleted) exercises in their template order, each pre-filling planned
     * sets from the user's previous session for that exercise.
     */
    suspend fun startSession(templateId: Long? = null): Long {
        val sessionId = sessionDao.insert(
            SessionEntity(startedAt = Instant.now(), templateId = templateId)
        )
        if (templateId != null) {
            templateDao.exercisesFor(templateId).forEach { exercise ->
                addExerciseToSession(sessionId, exercise.id)
            }
        }
        return sessionId
    }

    suspend fun endSession(id: Long, at: Instant = Instant.now()) = sessionDao.end(id, at)

    /** Hard-delete a session and cascade-remove its exercises + set logs. */
    suspend fun deleteSession(id: Long) = sessionDao.deleteById(id)

    suspend fun updateSessionNotes(id: Long, notes: String?) = sessionDao.updateNotes(id, notes)

    suspend fun lastActivityAt(sessionId: Long): Instant? = sessionDao.lastActivityAt(sessionId)

    /**
     * Append an exercise to a session. Pre-fills [targetSets] planned set rows whose values come
     * from the previous session for that exercise, falling back to the rep range minimum when
     * there's no history.
     */
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long): Long {
        val exercise = exerciseDao.byId(exerciseId)
            ?: error("Exercise $exerciseId not found")
        val order = sessionDao.nextOrderInSession(sessionId)
        val sessionExerciseId = sessionDao.insertExercise(
            SessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                orderInSession = order,
            )
        )
        // Planned sets stay null (reps/kg) until the user taps ✓. The UI reads
        // `lastSessionSets` separately to suggest stepper values.
        for (i in 1..exercise.targetSets) {
            setLogDao.upsert(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = i,
                )
            )
        }
        return sessionExerciseId
    }

    suspend fun lastSessionSets(exerciseId: Long, limit: Int): List<SetLogEntity> =
        setLogDao.lastSessionSets(exerciseId, limit)

    suspend fun removeSessionExercise(sessionExerciseId: Long) =
        sessionDao.removeExercise(sessionExerciseId)

    suspend fun setSessionExerciseSkipped(sessionExerciseId: Long, skipped: Boolean) =
        sessionDao.setExerciseSkipped(sessionExerciseId, skipped)

    suspend fun updateSessionExerciseNotes(sessionExerciseId: Long, notes: String?) =
        sessionDao.updateExerciseNotes(sessionExerciseId, notes)

    /** Commit a logged set (✓). Pass null reps/kg to revert a logged set to "planned". */
    suspend fun logSet(setLogId: Long, reps: Int?, kg: Double?, at: Instant? = Instant.now()) {
        val current = setLogDao.byId(setLogId) ?: return
        setLogDao.update(
            current.copy(
                reps = reps,
                kg = kg,
                loggedAt = if (reps == null) null else at,
                isSkipped = false,
            )
        )
    }

    suspend fun setSetSkipped(setLogId: Long, skipped: Boolean) {
        val current = setLogDao.byId(setLogId) ?: return
        setLogDao.update(current.copy(isSkipped = skipped))
    }
}
