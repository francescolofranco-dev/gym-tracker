package dev.francescolofranco.gymtracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.backup.BackupOutcome
import dev.francescolofranco.gymtracker.data.backup.BackupPreviewOutcome
import dev.francescolofranco.gymtracker.data.backup.BackupRepository
import dev.francescolofranco.gymtracker.data.backup.BackupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val preview: RestorePreview? = null,
    val hasRecovery: Boolean = false,
)

data class RestorePreview(val uri: Uri, val summary: BackupSummary)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState(hasRecovery = repo.hasRecovery()))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun suggestedFilename(): String = repo.suggestedFilename()

    fun export(uri: Uri) = run("Backing up…") { repo.exportToUri(uri) }

    fun inspect(uri: Uri) {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true, message = "Checking backup…", error = null)
            _state.value = when (val outcome = repo.inspectUri(uri)) {
                is BackupPreviewOutcome.Success -> _state.value.copy(
                    running = false,
                    message = null,
                    preview = RestorePreview(uri, outcome.summary),
                )
                is BackupPreviewOutcome.Failure -> _state.value.copy(
                    running = false,
                    message = null,
                    error = outcome.error,
                )
            }
        }
    }

    fun dismissPreview() {
        _state.value = _state.value.copy(preview = null)
    }

    fun restorePreview() {
        val preview = _state.value.preview ?: return
        _state.value = _state.value.copy(preview = null)
        run("Restoring…") { repo.importFromUri(preview.uri) }
    }

    fun restoreRecovery() = run("Recovering…") { repo.restoreRecovery() }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    private fun run(workingMessage: String, block: suspend () -> BackupOutcome) {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.value = _state.value.copy(running = true, message = workingMessage, error = null)
            val outcome = block()
            _state.value = when (outcome) {
                is BackupOutcome.Success -> _state.value.copy(
                    running = false,
                    message = outcome.message,
                    error = null,
                    hasRecovery = repo.hasRecovery(),
                )
                is BackupOutcome.Failure -> _state.value.copy(running = false, message = null, error = outcome.error)
            }
        }
    }
}
