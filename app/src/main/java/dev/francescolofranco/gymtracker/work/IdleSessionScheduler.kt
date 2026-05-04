package dev.francescolofranco.gymtracker.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules / cancels the auto-end-session worker. Called every time a set is logged so the
 * 3h idle clock stays fresh. The worker itself is the only thing allowed to flip a session to
 * ended without the user pressing End.
 */
@Singleton
class IdleSessionScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(sessionId: Long, after: Duration = IdleSessionWorker.IDLE_THRESHOLD) {
        val request = OneTimeWorkRequestBuilder<IdleSessionWorker>()
            .setInitialDelay(after.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(IdleSessionWorker.KEY_SESSION_ID, sessionId).build())
            .build()
        workManager.enqueueUniqueWork(
            uniqueName(sessionId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(sessionId: Long) {
        workManager.cancelUniqueWork(uniqueName(sessionId))
    }

    private fun uniqueName(sessionId: Long) = "idle-session-$sessionId"
}
