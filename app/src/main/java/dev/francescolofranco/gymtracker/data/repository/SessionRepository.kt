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

    /**
     * Promote a draft session to an accepted (real) one. Until the session has [acceptedAt],
     * it's invisible from the home screen banner and the past sessions list — it's just a
     * setup workspace. Idempotent: re-accepting an already-accepted session is a no-op.
     */
    suspend fun acceptSession(id: Long, at: Instant = Instant.now()) {
        val current = sessionDao.byId(id) ?: return
        if (current.acceptedAt == null) sessionDao.accept(id, at)
    }

    /**
     * Hard-delete a session and cascade-remove its exercises + set logs. Returns `true` if
     * the deleted row was the active session (endedAt = null) so callers can tear down the
     * timer notification accordingly.
     */
    suspend fun deleteSession(id: Long): Boolean {
        val wasActive = sessionDao.byId(id)?.endedAt == null
        sessionDao.deleteById(id)
        return wasActive
    }

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

    /**
     * Hint sets anchored to a specific session's [beforeStartedAt]: returns sets from the
     * previous session for this exercise that started strictly before the anchor. Used by the
     * past-session detail screen so % deltas reference the workout immediately before that
     * session, not the globally-latest one (which would be wrong when viewing history).
     */
    suspend fun lastSessionSetsBefore(
        exerciseId: Long,
        beforeStartedAt: Instant,
        limit: Int,
    ): List<SetLogEntity> = setLogDao.lastSessionSetsBefore(
        exerciseId = exerciseId,
        beforeStartedAtEpochMs = beforeStartedAt.toEpochMilli(),
        limit = limit,
    )

    suspend fun removeSessionExercise(sessionExerciseId: Long) =
        sessionDao.removeExercise(sessionExerciseId)

    suspend fun setSessionExerciseSkipped(sessionExerciseId: Long, skipped: Boolean) =
        sessionDao.setExerciseSkipped(sessionExerciseId, skipped)

    suspend fun updateSessionExerciseNotes(sessionExerciseId: Long, notes: String?) =
        sessionDao.updateExerciseNotes(sessionExerciseId, notes)

    /**
     * Revert a logged set back to "planned" while keeping the reps/kg the user had entered.
     * Just clears [SetLogEntity.loggedAt] (and isSkipped). The previous values stay so the UI
     * shows them as the pending stepper state — the user can re-commit with one tap or
     * tweak them without losing what they typed.
     */
    suspend fun unlogSet(setLogId: Long) {
        val current = setLogDao.byId(setLogId) ?: return
        setLogDao.update(current.copy(loggedAt = null, isSkipped = false))
    }

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

    /**
     * Persist a set's reps/kg WITHOUT committing it (loggedAt stays null). Used so values the
     * user typed in the numpad survive navigating away — previously the typed-but-uncommitted
     * value lived only in compose state, so visiting another screen and coming back wiped it.
     * No-ops on an already-committed set: if a set is logged, use [logSet] to update the
     * canonical values.
     */
    suspend fun saveSetDraft(setLogId: Long, reps: Int?, kg: Double?) {
        val current = setLogDao.byId(setLogId) ?: return
        if (current.loggedAt != null) return
        setLogDao.update(current.copy(reps = reps, kg = kg))
    }

    /**
     * One-shot bulk apply: copy [kg] into every NOT-YET-LOGGED set in a given session
     * exercise. Used by the auto-fill flow where typing a kg into the first set propagates
     * it to the rest. Reps are intentionally left untouched — the user wants a single weight
     * across sets, not a single reps count. Skips logged sets so committed history isn't
     * trampled.
     */
    suspend fun applyKgToPendingSets(sessionExerciseId: Long, kg: Double) {
        val sets = setLogDao.forSessionExercise(sessionExerciseId)
        sets.forEach { s ->
            if (s.loggedAt == null) {
                setLogDao.update(s.copy(kg = kg))
            }
        }
    }

    /** True if no set in this session-exercise has a non-null kg yet (committed or draft). */
    suspend fun hasAnyKg(sessionExerciseId: Long): Boolean {
        val sets = setLogDao.forSessionExercise(sessionExerciseId)
        return sets.any { it.kg != null }
    }

    suspend fun setSetSkipped(setLogId: Long, skipped: Boolean) {
        val current = setLogDao.byId(setLogId) ?: return
        setLogDao.update(current.copy(isSkipped = skipped))
    }
}
