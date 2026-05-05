package dev.francescolofranco.gymtracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.backup.BackupOutcome
import dev.francescolofranco.gymtracker.data.backup.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repo: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun suggestedFilename(): String = repo.suggestedFilename()

    fun export(uri: Uri) = run("Backing up…") { repo.exportToUri(uri) }

    fun import(uri: Uri) = run("Restoring…") { repo.importFromUri(uri) }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    private fun run(workingMessage: String, block: suspend () -> BackupOutcome) {
        if (_state.value.running) return
        viewModelScope.launch {
            _state.value = BackupUiState(running = true, message = workingMessage)
            val outcome = block()
            _state.value = when (outcome) {
                is BackupOutcome.Success -> BackupUiState(running = false, message = outcome.message)
                is BackupOutcome.Failure -> BackupUiState(running = false, error = outcome.error)
            }
        }
    }
}
