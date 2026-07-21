package dev.francescolofranco.gymtracker.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps one app-private snapshot immediately before any destructive restore. */
@Singleton
class BackupRecoveryStore @Inject constructor(
    @ApplicationContext context: Context,
    private val exporter: BackupExporter,
) {
    private val recoveryFile = File(context.filesDir, "backup-before-last-restore.json")

    suspend fun captureCurrent() = withContext(Dispatchers.IO) {
        val json = exporter.exportToJson()
        val temporary = File(recoveryFile.parentFile, "${recoveryFile.name}.tmp")
        temporary.writeText(json)
        if (!temporary.renameTo(recoveryFile)) {
            recoveryFile.writeText(json)
            temporary.delete()
        }
    }

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        recoveryFile.takeIf { it.isFile }?.readText()
    }

    fun exists(): Boolean = recoveryFile.isFile
}
