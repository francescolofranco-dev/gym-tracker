package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.WeightUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionId: Long,
    onExit: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val hints by viewModel.hints.collectAsStateWithLifecycle()
    val exitRequested by viewModel.exitRequested.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var sessionNotesEditor by remember { mutableStateOf(false) }
    var exerciseNotesTarget by remember { mutableStateOf<SessionExerciseDetail?>(null) }

    LaunchedEffect(exitRequested) {
        if (exitRequested) onExit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session") },
                actions = {
                    IconButton(onClick = { sessionNotesEditor = true }) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Session notes")
                    }
                    TextButton(onClick = { confirmEnd = true }) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("End")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Select exercise") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TimerPill()

            if (details.isEmpty()) {
                EmptyActiveSession(modifier = Modifier.fillMaxSize())
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                session?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item {
                        SessionNotesPreview(notes = notes, onEdit = { sessionNotesEditor = true })
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
                        onRemoveExercise = { viewModel.removeSessionExercise(detail.sessionExercise.id) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            onDismiss = { showPicker = false },
            onPick = { e -> viewModel.addExercise(e.id) },
        )
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End session?") },
            text = { Text("This finalises the session. You can still edit sets afterwards.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    viewModel.endSession()
                }) { Text("End") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Cancel") }
            },
        )
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
}

@Composable
private fun EmptyActiveSession(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = "Empty session", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Tap Select exercise to add the first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionNotesPreview(notes: String, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    detail: SessionExerciseDetail,
    unit: WeightUnit,
    hints: List<HintRow>,
    editable: Boolean,
    onCommitSet: (setLogId: Long, reps: Int, kg: Double) -> Unit,
    onUncommitSet: (setLogId: Long) -> Unit,
    onToggleSetSkipped: (setLogId: Long, currentlySkipped: Boolean) -> Unit,
    onSkipExercise: (skipped: Boolean) -> Unit,
    onEditExerciseNotes: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val exercise = detail.exercise
    val sets = detail.setLogs.sortedBy { it.setNumber }
    val loggedSets = sets.count { it.loggedAt != null && it.reps != null && !it.isSkipped }
    val incomplete = !detail.sessionExercise.isSkipped && loggedSets < exercise.targetSets

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = buildList {
                        add(exercise.primaryMuscles.sortedBy { it.ordinal }.joinToString(" + ") { it.displayName })
                        if (exercise.isBodyweight) add("BW")
                        add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}")
                        if (detail.sessionExercise.isSkipped) add("Skipped")
                        else if (incomplete) add("Incomplete")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Exercise options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (detail.sessionExercise.isSkipped) "Unskip" else "Skip exercise") },
                        leadingIcon = { Icon(Icons.Filled.NotInterested, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSkipExercise(!detail.sessionExercise.isSkipped)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (detail.sessionExercise.notes.isNullOrBlank()) "Add note" else "Edit note") },
                        leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditExerciseNotes()
                        },
                    )
                    if (editable) {
                        DropdownMenuItem(
                            text = { Text("Remove from session") },
                            onClick = {
                                menuExpanded = false
                                onRemoveExercise()
                            },
                        )
                    }
                }
            }
        }

        detail.sessionExercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

        sets.forEach { log ->
            val hint = hints.firstOrNull { it.setNumber == log.setNumber }
            SetRow(
                state = SetRowState(
                    log = log,
                    targetReps = exercise.repRangeMin..exercise.repRangeMax,
                    isBodyweight = exercise.isBodyweight,
                    unit = unit,
                    hintReps = hint?.reps,
                    hintKg = hint?.kg,
                ),
                onCommit = { reps, kg -> onCommitSet(log.id, reps, kg) },
                onUncommit = { onUncommitSet(log.id) },
                onSkipToggle = { onToggleSetSkipped(log.id, log.isSkipped) },
                editable = editable,
            )
        }
    }
}

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

