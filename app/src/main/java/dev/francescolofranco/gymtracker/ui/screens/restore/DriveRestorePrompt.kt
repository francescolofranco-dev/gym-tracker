package dev.francescolofranco.gymtracker.ui.screens.restore

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.francescolofranco.gymtracker.data.backup.drive.DriveBackupRepository
import dev.francescolofranco.gymtracker.data.backup.drive.DriveRestoreResult
import dev.francescolofranco.gymtracker.data.backup.drive.DriveSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface PromptState {
    data object Idle : PromptState
    data object Checking : PromptState
    data class Offer(val snapshot: DriveSnapshot) : PromptState
    data object Restoring : PromptState
    data class Done(val message: String) : PromptState
    data object Dismissed : PromptState
}

@HiltViewModel
class DriveRestorePromptViewModel @Inject constructor(
    private val repo: DriveBackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PromptState>(PromptState.Idle)
    internal val state: StateFlow<PromptState> = _state.asStateFlow()

    fun checkOnce() {
        if (_state.value !is PromptState.Idle) return
        _state.value = PromptState.Checking
        viewModelScope.launch {
            val snap = repo.suggestRestoreIfFreshInstall()
            _state.value = if (snap != null) PromptState.Offer(snap) else PromptState.Dismissed
        }
    }

    fun skip() = viewModelScope.launch {
        repo.markOfferConsumed()
        _state.value = PromptState.Dismissed
    }

    fun accept(snapshot: DriveSnapshot) {
        _state.value = PromptState.Restoring
        viewModelScope.launch {
            val result = repo.restore(snapshot.id)
            repo.markOfferConsumed()
            _state.value = when (result) {
                is DriveRestoreResult.Success ->
                    PromptState.Done("Restored ${result.summary.sessions} session${if (result.summary.sessions == 1) "" else "s"} from Drive.")
                is DriveRestoreResult.Error -> PromptState.Done("Restore failed: ${result.message}")
                DriveRestoreResult.NotSignedIn -> PromptState.Done("Drive sign-in expired.")
            }
        }
    }

    fun acknowledge() {
        _state.value = PromptState.Dismissed
    }
}

/**
 * One-shot Drive-restore offer for fresh-install + signed-in users. Mounted at the top of
 * GymApp so it overlays whatever screen is current. Once the user accepts, skips, or sees an
 * error, the underlying flag in UserPrefs is set so we never ask again on this device.
 */
@Composable
fun DriveRestorePrompt(viewModel: DriveRestorePromptViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.checkOnce() }

    when (val s = state) {
        PromptState.Idle, PromptState.Checking, PromptState.Dismissed -> Unit

        is PromptState.Offer -> AlertDialog(
            onDismissRequest = { viewModel.skip() },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            title = { Text("Restore from Drive?") },
            text = {
                Text(
                    "Found a recent backup in your Google Drive (${s.snapshot.name}). " +
                        "Restore it now? You can also restore later from Settings → Google Drive.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.accept(s.snapshot) }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skip() }) { Text("Skip") }
            },
        )

        PromptState.Restoring -> AlertDialog(
            onDismissRequest = { },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            confirmButton = { },
            title = { Text("Restoring…") },
            text = {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            },
        )

        is PromptState.Done -> AlertDialog(
            onDismissRequest = { viewModel.acknowledge() },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            title = { Text("Drive restore") },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledge() }) { Text("OK") }
            },
        )
    }
}
