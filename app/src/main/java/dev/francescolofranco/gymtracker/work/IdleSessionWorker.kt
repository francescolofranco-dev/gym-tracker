package dev.francescolofranco.gymtracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.francescolofranco.gymtracker.data.repository.SessionRepository
import dev.francescolofranco.gymtracker.service.TimerController
import java.time.Duration
import java.time.Instant
import dev.francescolofranco.gymtracker.domain.workoutStartedAt

/**
 * Auto-ends an active session that's been idle past [IDLE_THRESHOLD]. Re-scheduled on every
 * set log. If the session has activity within the threshold when this worker fires, it
 * reschedules itself for the remaining time rather than ending early.
 *
 * Uses an EntryPoint to grab the repository because the project does not include hilt-work.
 */
class IdleSessionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1)
        if (sessionId <= 0) return Result.success()

        val ep = EntryPointAccessors.fromApplication(applicationContext, IdleSessionEntryPoint::class.java)
        val repo = ep.sessionRepository()

        val active = repo.activeSession() ?: return Result.success()
        if (active.id != sessionId) return Result.success()

        val lastActivity = repo.lastActivityAt(sessionId) ?: active.workoutStartedAt()
        val now = Instant.now()
        val idleFor = Duration.between(lastActivity, now)
        return if (idleFor >= IDLE_THRESHOLD) {
            repo.endSession(sessionId, lastActivity)
            // Auto-end means no one's holding the timer — tear the notification down too.
            ep.timerController().stop()
            Result.success()
        } else {
            // Activity happened recently — defer by rescheduling for the remaining idle time.
            val scheduler = ep.idleSessionScheduler()
            scheduler.schedule(sessionId, IDLE_THRESHOLD - idleFor)
            Result.success()
        }
    }

    companion object {
        const val KEY_SESSION_ID = "sessionId"
        val IDLE_THRESHOLD: Duration = Duration.ofHours(3)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IdleSessionEntryPoint {
    fun sessionRepository(): SessionRepository
    fun idleSessionScheduler(): IdleSessionScheduler
    fun timerController(): TimerController
}
