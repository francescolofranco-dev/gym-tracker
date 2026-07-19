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
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import dev.francescolofranco.gymtracker.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class DriveUiState(
    val connected: Boolean = false,
    val checkingConnection: Boolean = true,
    val authorizationRequest: PendingIntent? = null,
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val snapshots: List<DriveSnapshot> = emptyList(),
    val lastBackupAt: Instant? = null,
)

@HiltViewModel
class DriveBackupViewModel @Inject constructor(
    private val auth: DriveAuth,
    private val repo: DriveBackupRepository,
    private val prefs: UserPrefs,
) : ViewModel() {

    private val _ui = MutableStateFlow(DriveUiState())
    private val connected: StateFlow<Boolean> = auth.connected
    private val lastBackupAt: StateFlow<Instant?> = prefs.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<DriveUiState> = combineState()

    init {
        viewModelScope.launch {
            auth.refreshAuthorization()
            _ui.value = _ui.value.copy(checkingConnection = false)
        }
    }

    private fun combineState(): StateFlow<DriveUiState> {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(connected, lastBackupAt) { isConnected, last ->
                _ui.value.copy(connected = isConnected, lastBackupAt = last)
            }.collect { _ui.value = it }
        }
        return _ui.asStateFlow()
    }

    fun connect() {
        if (_ui.value.running) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(running = true, error = null)
            try {
                when (val outcome = auth.requestAuthorization()) {
                    is DriveAuthorizationOutcome.Granted -> {
                        _ui.value = _ui.value.copy(running = false)
                    }
                    is DriveAuthorizationOutcome.NeedsUserConsent -> {
                        _ui.value = _ui.value.copy(
                            running = false,
                            authorizationRequest = outcome.pendingIntent,
                        )
                    }
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    running = false,
                    error = t.message ?: "Google Drive authorization failed.",
                )
            }
        }
    }

    fun consumeAuthorizationRequest() {
        _ui.value = _ui.value.copy(authorizationRequest = null)
    }

    fun onAuthorizationResult(data: Intent?) = viewModelScope.launch {
        _ui.value = _ui.value.copy(running = true, error = null)
        try {
            auth.handleAuthorizationResult(data)
            _ui.value = _ui.value.copy(running = false)
        } catch (t: Throwable) {
            _ui.value = _ui.value.copy(
                running = false,
                error = t.message ?: "Google Drive authorization failed.",
            )
        }
    }

    fun disconnect() = viewModelScope.launch {
        if (_ui.value.running) return@launch
        _ui.value = _ui.value.copy(running = true, error = null)
        try {
            auth.revokeAccess()
            _ui.value = _ui.value.copy(running = false, snapshots = emptyList())
        } catch (t: Throwable) {
            _ui.value = _ui.value.copy(
                running = false,
                error = t.message ?: "Unable to disconnect Google Drive.",
            )
        }
    }

    fun backupNow() = run("Uploading…") {
        when (val res = repo.runBackup()) {
            is DriveBackupResult.Success ->
                "Uploaded ${humanByteCount(res.sizeBytes)}" + if (res.pruned > 0) ", pruned ${res.pruned} old snapshot(s)." else "."
            DriveBackupResult.AuthorizationRequired ->
                throw IllegalStateException("Google Drive authorization is required.")
            is DriveBackupResult.Error -> throw IllegalStateException(res.message)
        }.also { refreshSnapshots() }
    }

    fun refreshSnapshots() = viewModelScope.launch {
        if (!auth.connected.value) return@launch
        runCatching { repo.listSnapshots() }
            .onSuccess { _ui.value = _ui.value.copy(snapshots = it) }
    }

    fun restore(snapshot: DriveSnapshot) = run("Restoring ${snapshot.name}…") {
        when (val res = repo.restore(snapshot.id)) {
            is DriveRestoreResult.Success -> "Restored ${res.summary.sessions} session${if (res.summary.sessions == 1) "" else "s"} from Drive."
            DriveRestoreResult.AuthorizationRequired ->
                throw IllegalStateException("Google Drive authorization is required.")
            is DriveRestoreResult.Error -> throw IllegalStateException(res.message)
        }
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null, error = null)
    }

    private fun run(workingMessage: String, block: suspend () -> String) {
        if (_ui.value.running) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(running = true, message = workingMessage, error = null)
            try {
                val msg = block()
                _ui.value = _ui.value.copy(running = false, message = msg, error = null)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(running = false, error = t.message ?: "Drive request failed.", message = null)
            }
        }
    }

    private fun humanByteCount(bytes: Int): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
