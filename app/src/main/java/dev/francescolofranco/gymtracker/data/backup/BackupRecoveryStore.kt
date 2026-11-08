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
open class BackupRecoveryStore {
    private val recoveryFile: File?
    private val exporter: BackupExporter?

    @Inject
    constructor(
        @ApplicationContext context: Context,
        exporter: BackupExporter,
    ) {
        this.recoveryFile = File(context.filesDir, "backup-before-last-restore.json")
        this.exporter = exporter
    }

    constructor() {
        this.recoveryFile = null
        this.exporter = null
    }

    open suspend fun captureCurrent() = withContext(Dispatchers.IO) {
        val file = recoveryFile ?: return@withContext
        val exp = exporter ?: return@withContext
        val json = exp.exportToJson()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json)
        if (!temporary.renameTo(file)) {
            file.writeText(json)
            temporary.delete()
        }
    }

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        recoveryFile?.takeIf { it.isFile }?.readText()
    }

    fun exists(): Boolean = recoveryFile?.isFile == true
}
