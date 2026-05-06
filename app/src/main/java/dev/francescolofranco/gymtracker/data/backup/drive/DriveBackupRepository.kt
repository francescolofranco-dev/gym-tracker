package dev.francescolofranco.gymtracker.data.backup.drive

import dev.francescolofranco.gymtracker.data.backup.BackupExporter
import dev.francescolofranco.gymtracker.data.backup.BackupImporter
import dev.francescolofranco.gymtracker.data.backup.BackupSummary
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DriveBackupResult {
    data class Success(val fileId: String, val sizeBytes: Int, val pruned: Int) : DriveBackupResult
    data object NotSignedIn : DriveBackupResult
    data class Error(val message: String) : DriveBackupResult
}

sealed interface DriveRestoreResult {
    data class Success(val summary: BackupSummary) : DriveRestoreResult
    data object NotSignedIn : DriveRestoreResult
    data class Error(val message: String) : DriveRestoreResult
}

/**
 * Orchestrates a Drive snapshot run: ask DriveAuth for a token, build the JSON via the
 * existing [BackupExporter], upload to the appDataFolder, then prune any snapshots beyond
 * [MAX_SNAPSHOTS]. The same flow powers both the daily WorkManager job and the manual
 * "Backup now" button.
 */
@Singleton
class DriveBackupRepository @Inject constructor(
    private val auth: DriveAuth,
    private val client: DriveBackupClient,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val userPrefs: UserPrefs,
) {

    suspend fun runBackup(): DriveBackupResult = withContext(Dispatchers.IO) {
        try {
            val token = auth.freshAccessToken() ?: return@withContext DriveBackupResult.NotSignedIn
            val json = exporter.exportToJson()
            val bytes = json.toByteArray()
            val now = Instant.now()
            val name = "gymtracker-${now.toString().replace(":", "-")}.json"
            val fileId = client.upload(token, name, bytes)

            val pruned = pruneToLast(token, MAX_SNAPSHOTS)
            userPrefs.setLastBackupAt(now)

            DriveBackupResult.Success(fileId = fileId, sizeBytes = bytes.size, pruned = pruned)
        } catch (e: DriveApiException) {
            DriveBackupResult.Error(e.message ?: "Drive request failed.")
        } catch (e: Throwable) {
            DriveBackupResult.Error(e.message ?: "Unknown failure.")
        }
    }

    suspend fun listSnapshots(): List<DriveSnapshot> = withContext(Dispatchers.IO) {
        val token = auth.freshAccessToken() ?: return@withContext emptyList()
        runCatching { client.list(token) }.getOrDefault(emptyList())
    }

    suspend fun restore(snapshotId: String): DriveRestoreResult = withContext(Dispatchers.IO) {
        try {
            val token = auth.freshAccessToken() ?: return@withContext DriveRestoreResult.NotSignedIn
            val bytes = client.download(token, snapshotId)
            val summary = importer.importFromJson(String(bytes))
            DriveRestoreResult.Success(summary)
        } catch (e: DriveApiException) {
            DriveRestoreResult.Error(e.message ?: "Drive request failed.")
        } catch (e: Throwable) {
            DriveRestoreResult.Error(e.message ?: "Unknown failure.")
        }
    }

    private fun pruneToLast(token: String, keep: Int): Int {
        val snapshots = client.list(token)
        if (snapshots.size <= keep) return 0
        val stale = snapshots.drop(keep)  // list() returns newest-first
        stale.forEach { runCatching { client.delete(token, it.id) } }
        return stale.size
    }

    companion object {
        const val MAX_SNAPSHOTS = 7
    }
}
