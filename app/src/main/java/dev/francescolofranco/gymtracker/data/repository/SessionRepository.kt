package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.dao.ExerciseDao
import dev.francescolofranco.gymtracker.data.db.dao.SessionDao
import dev.francescolofranco.gymtracker.data.db.dao.SetLogDao
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseWithRecency
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.ExerciseSide
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

    /** Lightweight session-table invalidation without re-aggregating every logged set. */
    fun observeAllSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    fun observeActive(): Flow<SessionEntity?> = sessionDao.observeActive()

    fun observeSession(id: Long): Flow<SessionEntity?> = sessionDao.observeById(id)

    fun observeExerciseDetails(sessionId: Long): Flow<List<SessionExerciseDetail>> =
        sessionDao.observeExerciseDetails(sessionId)

    fun observePickerExercises(): Flow<List<ExerciseWithRecency>> =
        exerciseDao.observeActiveByRecency()

    fun observeExerciseSetHistory(exerciseId: Long): Flow<List<ExerciseSetRow>> =
        exerciseDao.observeSetHistory(exerciseId)

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

    /** Returns whether the ended session was accepted (as opposed to a setup-only draft). */
    suspend fun endSession(id: Long, at: Instant = Instant.now()): Boolean =
        sessionDao.endAndReturnWasAccepted(id, at)

    suspend fun updateSessionTiming(id: Long, start: Instant, end: Instant) {
        require(!end.isBefore(start)) { "Session end cannot be before its start." }
        sessionDao.updateTiming(id, start, end)
    }

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
        val note = noteForNextSession(
            sessionDao.latestExerciseOccurrence(
                exerciseId = exerciseId,
                targetSessionId = sessionId,
            ),
        )
        val sessionExerciseId = sessionDao.insertExercise(
            SessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                orderInSession = order,
                notes = note?.text,
                isNotePinned = note?.isPinned == true,
                // A normal note has now reached its one subsequent session. A pinned note keeps
                // travelling until the user unpins it.
                noteCarryForward = note?.carryForward == true,
            )
        )
        // Planned sets stay null (reps/kg) until the user taps ✓. The UI reads
        // `lastSessionSets` separately to suggest stepper values.
        val sides = if (exercise.isUnilateral) {
            listOf(ExerciseSide.LEFT, ExerciseSide.RIGHT)
        } else {
            listOf(ExerciseSide.BOTH)
        }
        for (i in 1..exercise.targetSets) {
            sides.forEach { side ->
                setLogDao.upsert(
                    SetLogEntity(
                        sessionExerciseId = sessionExerciseId,
                        setNumber = i,
                        side = side,
                    )
                )
            }
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

    /**
     * Last logged value for one specific set number, searched independently across all ended
     * sessions (see [SetLogDao.lastLoggedSetBefore]).
     */
    suspend fun lastLoggedSetBefore(
        exerciseId: Long,
        setNumber: Int,
        side: ExerciseSide,
        beforeStartedAt: Instant,
    ): SetLogEntity? {
        // Exercises created before unilateral tracking have historical rows stored as BOTH.
        // When the user later marks one unilateral, let each new side inherit that old row
        // until a side-specific value exists. Bilateral rows never borrow one arbitrary side.
        historyLookupSides(side).forEach { candidateSide ->
            setLogDao.lastLoggedSetBefore(
                exerciseId = exerciseId,
                setNumber = setNumber,
                side = candidateSide,
                beforeStartedAtEpochMs = beforeStartedAt.toEpochMilli(),
            )?.let { return it }
        }
        return null
    }

    suspend fun removeSessionExercise(sessionExerciseId: Long) =
        sessionDao.removeExercise(sessionExerciseId)

    /**
     * Swap the position of two session-exercise rows. Used by the move up / move down menu
     * items; the caller (a ViewModel) is responsible for finding the adjacent neighbour and
     * passing both IDs.
     */
    suspend fun swapSessionExerciseOrder(idA: Long, idB: Long) =
        sessionDao.swapExerciseOrder(idA, idB)

    suspend fun setSessionExerciseSkipped(sessionExerciseId: Long, skipped: Boolean) =
        sessionDao.setExerciseSkipped(sessionExerciseId, skipped)

    suspend fun updateSessionExerciseNote(
        sessionExerciseId: Long,
        notes: String?,
        isPinned: Boolean,
    ) {
        val current = sessionDao.sessionExerciseById(sessionExerciseId) ?: return
        val state = savedExerciseNote(current, notes, isPinned)
        sessionDao.updateExerciseNote(
            id = sessionExerciseId,
            notes = state.text,
            isPinned = state.isPinned,
            carryForward = state.carryForward,
        )
    }

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

    /**
     * True if any set OTHER than [excludeSetLogId] has a non-null kg. Used to decide whether
     * the "first explicit kg" auto-fill should fire: we want to propagate when the source set
     * is the only one with kg, even if the source already had a hint-derived draft kg written.
     * Passing 0 (or any non-matching id) effectively asks "does any set have kg" for the
     * legacy non-exclusion case.
     */
    suspend fun anyOtherSetHasKg(sessionExerciseId: Long, excludeSetLogId: Long): Boolean {
        val sets = setLogDao.forSessionExercise(sessionExerciseId)
        return sets.any { it.id != excludeSetLogId && it.kg != null }
    }

    suspend fun setSetSkipped(setLogId: Long, skipped: Boolean) {
        val current = setLogDao.byId(setLogId) ?: return
        setLogDao.update(current.copy(isSkipped = skipped))
    }
}

/** Side-specific history first, then legacy bilateral history as a compatibility fallback. */
internal fun historyLookupSides(side: ExerciseSide): List<ExerciseSide> = when (side) {
    ExerciseSide.BOTH -> listOf(ExerciseSide.BOTH)
    ExerciseSide.LEFT -> listOf(ExerciseSide.LEFT, ExerciseSide.BOTH)
    ExerciseSide.RIGHT -> listOf(ExerciseSide.RIGHT, ExerciseSide.BOTH)
}

internal data class ExerciseNoteState(
    val text: String?,
    val isPinned: Boolean,
    val carryForward: Boolean,
)

/**
 * Normalise editor input and resolve its next-session lifetime. An unchanged Save preserves the
 * current lifetime, so simply reading a carried note cannot renew it indefinitely. Any actual
 * edit makes an unpinned note relevant for one more occurrence; unpinning follows that same rule.
 */
internal fun savedExerciseNote(
    current: SessionExerciseEntity,
    notes: String?,
    isPinned: Boolean,
): ExerciseNoteState {
    val text = notes?.trim()?.takeIf { it.isNotEmpty() }
    val pinned = text != null && isPinned
    val carryForward = when {
        text == null -> false
        pinned -> true
        text == current.notes && !current.isNotePinned -> current.noteCarryForward
        else -> true
    }
    return ExerciseNoteState(
        text = text,
        isPinned = pinned,
        carryForward = carryForward,
    )
}

/** Resolve the previous occurrence's note without letting an expired note reappear. */
internal fun noteForNextSession(previous: SessionExerciseEntity?): ExerciseNoteState? {
    previous ?: return null
    val text = previous.notes?.takeIf { it.isNotBlank() } ?: return null
    if (!previous.isNotePinned && !previous.noteCarryForward) return null
    return ExerciseNoteState(
        text = text,
        isPinned = previous.isNotePinned,
        carryForward = previous.isNotePinned,
    )
}
