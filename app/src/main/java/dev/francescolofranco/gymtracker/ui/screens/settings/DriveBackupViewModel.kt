package dev.francescolofranco.gymtracker.ui.screens.settings

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.backup.drive.DriveAuth
import dev.francescolofranco.gymtracker.data.backup.drive.DriveAuthorizationOutcome
import dev.francescolofranco.gymtracker.data.backup.drive.DriveBackupRepository
import dev.francescolofranco.gymtracker.data.backup.drive.DriveBackupResult
import dev.francescolofranco.gymtracker.data.backup.drive.DriveRestoreResult
import dev.francescolofranco.gymtracker.data.backup.drive.DriveInspectResult
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import dev.francescolofranco.gymtracker.data.backup.BackupSummary
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class DriveUiState(
    val connected: Boolean = false,
    val checkingConnection: Boolean = true,
    val loadingSnapshots: Boolean = false,
    val authorizationRequest: PendingIntent? = null,
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val snapshots: List<DriveSnapshot> = emptyList(),
    val lastBackupAt: Instant? = null,
    val restorePreview: DriveRestorePreview? = null,
)

data class DriveRestorePreview(val snapshot: DriveSnapshot, val summary: BackupSummary)

@HiltViewModel
class DriveBackupViewModel @Inject constructor(
    private val auth: DriveAuth,
    private val repo: DriveBackupRepository,
    private val prefs: UserPrefs,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        DriveUiState(
            connected = auth.connected.value,
            checkingConnection = !auth.connected.value,
            loadingSnapshots = repo.cachedSnapshots.value == null,
            snapshots = repo.cachedSnapshots.value ?: emptyList(),
        ),
    )
    private val connected: StateFlow<Boolean> = auth.connected
    private val lastBackupAt: StateFlow<Instant?> = prefs.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<DriveUiState> = combineState()

    init {
        viewModelScope.launch {
            if (!auth.connected.value) {
                auth.refreshAuthorization()
            }
            val isConnected = auth.connected.value
            _ui.update {
                it.copy(
                    connected = isConnected,
                    checkingConnection = false,
                    loadingSnapshots = isConnected && it.snapshots.isEmpty(),
                )
            }
            if (isConnected) {
                fetchSnapshots()
            }
        }
    }

    private fun combineState(): StateFlow<DriveUiState> {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(connected, lastBackupAt, repo.cachedSnapshots) { isConnected, last, cached ->
                Triple(isConnected, last, cached)
            }.collect { (isConnected, last, cached) ->
                _ui.update { current ->
                    current.copy(
                        connected = isConnected,
                        lastBackupAt = last,
                        snapshots = cached ?: current.snapshots,
                    )
                }
            }
        }
        return _ui.asStateFlow()
    }

    fun connect() {
        if (_ui.value.running) return
        viewModelScope.launch {
            _ui.update { it.copy(running = true, error = null) }
            try {
                when (val outcome = auth.requestAuthorization()) {
                    is DriveAuthorizationOutcome.Granted -> {
                        _ui.update { it.copy(running = false, loadingSnapshots = it.snapshots.isEmpty()) }
                        fetchSnapshots()
                    }
                    is DriveAuthorizationOutcome.NeedsUserConsent -> {
                        _ui.update {
                            it.copy(
                                running = false,
                                authorizationRequest = outcome.pendingIntent,
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                _ui.update {
                    it.copy(
                        running = false,
                        error = t.message ?: "Google Drive authorization failed.",
                    )
                }
            }
        }
    }

    fun consumeAuthorizationRequest() {
        _ui.update { it.copy(authorizationRequest = null) }
    }

    fun onAuthorizationResult(data: Intent?) = viewModelScope.launch {
        _ui.update { it.copy(running = true, error = null) }
        try {
            auth.handleAuthorizationResult(data)
            _ui.update { it.copy(running = false, loadingSnapshots = it.snapshots.isEmpty()) }
            fetchSnapshots()
        } catch (t: Throwable) {
            _ui.update {
                it.copy(
                    running = false,
                    error = t.message ?: "Google Drive authorization failed.",
                )
            }
        }
    }

    fun disconnect() = viewModelScope.launch {
        if (_ui.value.running) return@launch
        _ui.update { it.copy(running = true, error = null) }
        try {
            auth.revokeAccess()
            repo.clearCachedSnapshots()
            _ui.update { it.copy(running = false, snapshots = emptyList(), loadingSnapshots = false) }
        } catch (t: Throwable) {
            _ui.update {
                it.copy(
                    running = false,
                    error = t.message ?: "Unable to disconnect Google Drive.",
                )
            }
        }
    }

    fun backupNow() = run("Uploading…") {
        when (val res = repo.runBackup()) {
            is DriveBackupResult.Success ->
                "Uploaded ${humanByteCount(res.sizeBytes)}" + if (res.pruned > 0) ", pruned ${res.pruned} old snapshot(s)." else "."
            DriveBackupResult.UpToDate ->
                "Already up to date — no changes since last backup."
            DriveBackupResult.AuthorizationRequired ->
                throw IllegalStateException("Google Drive authorization is required.")
            is DriveBackupResult.Error -> throw IllegalStateException(res.message)
        }.also { refreshSnapshots() }
    }

    fun refreshSnapshots() = viewModelScope.launch {
        fetchSnapshots()
    }

    private suspend fun fetchSnapshots() {
        if (!auth.connected.value) return
        _ui.update { it.copy(loadingSnapshots = it.snapshots.isEmpty()) }
        runCatching { repo.listSnapshots() }
            .onSuccess { list -> _ui.update { it.copy(snapshots = list, loadingSnapshots = false) } }
            .onFailure { _ui.update { it.copy(loadingSnapshots = false) } }
    }

    fun inspectRestore(snapshot: DriveSnapshot) {
        if (_ui.value.running) return
        viewModelScope.launch {
            _ui.update { it.copy(running = true, error = null) }
            _ui.update { current ->
                when (val result = repo.inspect(snapshot.id)) {
                    is DriveInspectResult.Success -> current.copy(
                        running = false,
                        restorePreview = DriveRestorePreview(snapshot, result.summary),
                    )
                    DriveInspectResult.AuthorizationRequired -> current.copy(
                        running = false,
                        error = "Google Drive authorization is required.",
                    )
                    is DriveInspectResult.Error -> current.copy(running = false, error = result.message)
                }
            }
        }
    }

    fun dismissRestorePreview() {
        _ui.update { it.copy(restorePreview = null) }
    }

    fun restorePreview() {
        val preview = _ui.value.restorePreview ?: return
        _ui.update { it.copy(restorePreview = null) }
        restore(preview.snapshot)
    }

    private fun restore(snapshot: DriveSnapshot) = run("Restoring ${snapshot.name}…") {
        when (val res = repo.restore(snapshot.id)) {
            is DriveRestoreResult.Success -> "Restored ${res.summary.sessions} session${if (res.summary.sessions == 1) "" else "s"} from Drive."
            DriveRestoreResult.AuthorizationRequired ->
                throw IllegalStateException("Google Drive authorization is required.")
            is DriveRestoreResult.Error -> throw IllegalStateException(res.message)
        }
    }

    fun consumeMessage() {
        _ui.update { it.copy(message = null, error = null) }
    }

    private fun run(workingMessage: String, block: suspend () -> String) {
        if (_ui.value.running) return
        viewModelScope.launch {
            _ui.update { it.copy(running = true, message = workingMessage, error = null) }
            try {
                val msg = block()
                _ui.update { it.copy(running = false, message = msg, error = null) }
            } catch (t: Throwable) {
                _ui.update { it.copy(running = false, error = t.message ?: "Drive request failed.", message = null) }
            }
        }
    }

    private fun humanByteCount(bytes: Int): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
