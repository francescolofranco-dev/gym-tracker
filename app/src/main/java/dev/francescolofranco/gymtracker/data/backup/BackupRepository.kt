package dev.francescolofranco.gymtracker.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface BackupOutcome {
    data class Success(val message: String) : BackupOutcome
    data class Failure(val error: String) : BackupOutcome
}

sealed interface BackupPreviewOutcome {
    data class Success(val summary: BackupSummary) : BackupPreviewOutcome
    data class Failure(val error: String) : BackupPreviewOutcome
}

/**
 * Facade that the UI / WorkManager call into. Composes the JSON pipeline with content-URI IO
 * (the SAF picker on Android hands us a `content://` URI; we read/write through ContentResolver).
 *
 * Drive integration lives in a separate impl that's wired in once the user has set up an OAuth
 * client; until then [latestBackupTimestamp] reflects only manual exports.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val recoveryStore: BackupRecoveryStore,
) {
    /** Writes the JSON backup to a SAF document URI. */
    suspend fun exportToUri(uri: Uri): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val json = exporter.exportToJson()
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                ?: return@withContext BackupOutcome.Failure("Could not open destination.")
            BackupOutcome.Success("Backup saved (${humanByteCount(json.length)}).")
        } catch (t: Throwable) {
            BackupOutcome.Failure(t.message ?: "Export failed.")
        }
    }

    /** Parses and validates a document without changing local data. */
    suspend fun inspectUri(uri: Uri): BackupPreviewOutcome = withContext(Dispatchers.IO) {
        try {
            val json = readUri(uri) ?: return@withContext BackupPreviewOutcome.Failure("Could not open backup file.")
            BackupPreviewOutcome.Success(importer.inspectJson(json))
        } catch (e: BackupParseException) {
            BackupPreviewOutcome.Failure(e.message ?: "Backup file is invalid.")
        } catch (t: Throwable) {
            BackupPreviewOutcome.Failure(t.message ?: "Could not inspect backup.")
        }
    }

    /** Reads a JSON backup, captures a recovery snapshot, then applies it transactionally. */
    suspend fun importFromUri(uri: Uri): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val json = readUri(uri)
                ?: return@withContext BackupOutcome.Failure("Could not open backup file.")
            importer.inspectJson(json)
            recoveryStore.captureCurrent()
            val summary = importer.importFromJson(json)
            BackupOutcome.Success(
                buildString {
                    append("Restored ${summary.sessions} session${plural(summary.sessions)}, ")
                    append("${summary.exercises} exercise${plural(summary.exercises)}, ")
                    append("${summary.templates} template${plural(summary.templates)}.")
                    summary.exportedAt?.let { append(" Snapshot from ${it}.") }
                    append(" A safety copy of the previous data was kept in the app.")
                },
            )
        } catch (e: BackupParseException) {
            BackupOutcome.Failure(e.message ?: "Backup file is invalid.")
        } catch (t: Throwable) {
            BackupOutcome.Failure(t.message ?: "Restore failed.")
        }
    }

    suspend fun restoreRecovery(): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val json = recoveryStore.read()
                ?: return@withContext BackupOutcome.Failure("No restore safety copy is available.")
            val summary = importer.importFromJson(json)
            BackupOutcome.Success("Recovered ${summary.sessions} session${plural(summary.sessions)} from before the last restore.")
        } catch (t: Throwable) {
            BackupOutcome.Failure(t.message ?: "Recovery failed.")
        }
    }

    fun hasRecovery(): Boolean = recoveryStore.exists()

    fun suggestedFilename(now: Instant = Instant.now()): String =
        "gymtracker-backup-${now.toString().replace(":", "-")}.json"

    private fun plural(n: Int) = if (n == 1) "" else "s"

    private fun readUri(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

    private fun humanByteCount(bytes: Int): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
