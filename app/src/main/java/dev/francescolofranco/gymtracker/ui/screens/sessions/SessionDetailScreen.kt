package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import kotlinx.coroutines.launch
import java.time.Duration

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val hints by viewModel.hints.collectAsStateWithLifecycle()

    var sessionNotesEditor by remember { mutableStateOf(false) }
    var exerciseNotesTarget by remember { mutableStateOf<SessionExerciseDetail?>(null) }
    var deleteStage by remember { mutableStateOf(DeleteStage.None) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = session
                    Column {
                        Text(
                            text = if (current == null) "Session" else formatSessionDate(current.startedAt),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        if (current?.endedAt != null) {
                            val duration = Duration.between(current.startedAt, current.endedAt)
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { sessionNotesEditor = true }) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Session notes")
                    }
                    IconButton(onClick = { deleteStage = DeleteStage.First }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (details.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Text(text = "No exercises were logged.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                session?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(text = notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                items(items = details, key = { it.sessionExercise.id }) { detail ->
                    ExerciseCard(
                        detail = detail,
                        unit = unit,
                        hints = hints.byExerciseId[detail.exercise.id] ?: emptyList(),
                        editable = true,
                        onCommitSet = { setLogId, reps, kg -> viewModel.logSet(setLogId, reps, kg) },
                        onUncommitSet = { setLogId -> viewModel.unlogSet(setLogId) },
                        onToggleSetSkipped = { setLogId, currentlySkipped ->
                            viewModel.toggleSetSkipped(setLogId, currentlySkipped)
                        },
                        onSkipExercise = { skipped ->
                            viewModel.setExerciseSkipped(detail.sessionExercise.id, skipped)
                        },
                        onEditExerciseNotes = { exerciseNotesTarget = detail },
                        onRemoveExercise = { /* read-mostly: no removal from past sessions */ },
                    )
                }
            }
        }
    }

    if (sessionNotesEditor) {
        NotesDialog(
            title = "Session notes",
            initial = session?.notes.orEmpty(),
            onCancel = { sessionNotesEditor = false },
            onConfirm = {
                viewModel.setSessionNotes(it)
                sessionNotesEditor = false
            },
        )
    }

    exerciseNotesTarget?.let { target ->
        NotesDialog(
            title = "${target.exercise.name} note",
            initial = target.sessionExercise.notes.orEmpty(),
            onCancel = { exerciseNotesTarget = null },
            onConfirm = {
                viewModel.setExerciseNotes(target.sessionExercise.id, it)
                exerciseNotesTarget = null
            },
        )
    }

    when (deleteStage) {
        DeleteStage.None -> Unit

        DeleteStage.First -> AlertDialog(
            onDismissRequest = { deleteStage = DeleteStage.None },
            title = { Text("Delete this session?") },
            text = {
                Text(
                    "Every set you logged in this session will be permanently removed. " +
                        "Your exercise library is unaffected.",
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteStage = DeleteStage.Final }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { deleteStage = DeleteStage.None }) { Text("Cancel") }
            },
        )

        DeleteStage.Final -> AlertDialog(
            onDismissRequest = { deleteStage = DeleteStage.None },
            title = { Text("Are you sure?") },
            text = {
                Text(
                    "This is the last chance. The session and every set logged in it will be " +
                        "deleted forever — there's no undo.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteStage = DeleteStage.None
                    scope.launch {
                        viewModel.deleteSession()
                        onBack()
                    }
                }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteStage = DeleteStage.None }) { Text("Cancel") }
            },
        )
    }
}

private enum class DeleteStage { None, First, Final }

@Composable
private fun NotesDialog(
    title: String,
    initial: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
