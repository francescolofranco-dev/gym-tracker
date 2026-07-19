package dev.francescolofranco.gymtracker.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.francescolofranco.gymtracker.data.backup.drive.DriveBackupRepository
import dev.francescolofranco.gymtracker.data.backup.drive.DriveBackupResult
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily JSON snapshot job. Runs once a day shortly after 03:00. Today the worker just produces
 * the JSON and logs its size; the cloud upload will plug into `doWork` once the Drive
 * credentials are wired (Phase 7B). Scheduling is idempotent — `KEEP` policy means the job
 * picks up where it left off across restarts.
 */
class DailyBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val ep = EntryPointAccessors.fromApplication(applicationContext, DailyBackupEntryPoint::class.java)
            when (val outcome = ep.driveBackupRepository().runBackup()) {
                is DriveBackupResult.Success -> {
                    Log.i(TAG, "Daily Drive backup uploaded (${outcome.sizeBytes}B, pruned ${outcome.pruned}).")
                    Result.success()
                }
                DriveBackupResult.AuthorizationRequired -> {
                    // Not an error — the user hasn't authorized Drive or revoked the grant.
                    Log.i(TAG, "Drive not connected; skipping daily backup.")
                    Result.success()
                }
                is DriveBackupResult.Error -> {
                    Log.w(TAG, "Daily Drive backup failed: ${outcome.message}")
                    Result.retry()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Daily backup failed: ${t.message}", t)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "daily-backup"
        private const val TAG = "DailyBackupWorker"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyBackupEntryPoint {
    fun driveBackupRepository(): DriveBackupRepository
}

@Singleton
class DailyBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Schedules a daily run at ~03:00 local time. Safe to call repeatedly (KEEP policy). */
    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelay = millisUntilNext0300()
        val request = PeriodicWorkRequestBuilder<DailyBackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyBackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(DailyBackupWorker.UNIQUE_NAME)
    }

    private fun millisUntilNext0300(): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val today0300 = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(3, 0))
        val target = if (now.isBefore(today0300)) today0300 else today0300.plusDays(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(0)
    }
}
