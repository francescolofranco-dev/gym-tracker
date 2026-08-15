package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EditCalendar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val loadableContent by viewModel.content.collectAsStateWithLifecycle()
    val content = (loadableContent as? Loadable.Ready)?.value
    val session = content?.session
    val details = content?.details.orEmpty()
    val unit = content?.unit ?: WeightUnit.KG
    val hints by viewModel.hints.collectAsStateWithLifecycle()

    (loadableContent as? Loadable.Error)?.let {
        ErrorPane(it.message, viewModel::retry)
        return
    }

    var sessionNotesEditor by remember { mutableStateOf(false) }
    var exerciseNotesTarget by remember { mutableStateOf<SessionExerciseDetail?>(null) }
    var deleteStage by remember { mutableStateOf(DeleteStage.None) }
    var timingEditor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val current = session
                    Column {
                        Text(
                            text = if (current == null) "Session" else formatSessionDate(current.workoutStartedAt()),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        if (current?.endedAt != null) {
                            val duration = current.workoutDuration()
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
                    IconButton(onClick = { timingEditor = true }, enabled = session?.endedAt != null) {
                        Icon(Icons.Filled.EditCalendar, contentDescription = "Edit session date and time")
                    }
                    IconButton(onClick = { sessionNotesEditor = true }, enabled = content != null) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Session notes")
                    }
                    IconButton(onClick = { deleteStage = DeleteStage.First }, enabled = content != null) {
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
        if (content == null) {
            LoadingPane(modifier = Modifier.padding(padding))
        } else if (details.isEmpty()) {
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
            ) {
                session?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item(key = "session-notes", contentType = "session-notes") {
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
                    item(key = "session-notes-gap", contentType = "gap") {
                        Spacer(Modifier.height(12.dp))
                    }
                }
                details.forEachIndexed { index, detail ->
                    exerciseSection(
                        detail = detail,
                        unit = unit,
                        hints = hints.byExerciseId[detail.exercise.id] ?: emptyList(),
                        editable = true,
                        canMoveUp = index > 0,
                        canMoveDown = index < details.lastIndex,
                        onCommitSet = { setLogId, reps, kg -> viewModel.logSet(setLogId, reps, kg) },
                        onUncommitSet = { setLogId -> viewModel.unlogSet(setLogId) },
                        onDraftSet = { setLogId, reps, kg, kgExplicit ->
                            viewModel.saveSetDraft(detail.sessionExercise.id, setLogId, reps, kg, kgExplicit)
                        },
                        onToggleSetSkipped = { setLogId, currentlySkipped ->
                            viewModel.toggleSetSkipped(setLogId, currentlySkipped)
                        },
                        onSkipExercise = { skipped ->
                            viewModel.setExerciseSkipped(detail.sessionExercise.id, skipped)
                        },
                        onMoveUp = { viewModel.moveSessionExercise(detail.sessionExercise.id, -1) },
                        onMoveDown = { viewModel.moveSessionExercise(detail.sessionExercise.id, +1) },
                        onEditExerciseNotes = { exerciseNotesTarget = detail },
                        onRemoveExercise = { /* read-mostly: no removal from past sessions */ },
                        onOpenExerciseStats = { onOpenExerciseStats(detail.exercise.id) },
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
        ExerciseNoteDialog(
            title = "${target.exercise.name} note",
            initial = target.sessionExercise.notes.orEmpty(),
            initiallyPinned = target.sessionExercise.isNotePinned,
            onCancel = { exerciseNotesTarget = null },
            onConfirm = { notes, isPinned ->
                viewModel.setExerciseNote(target.sessionExercise.id, notes, isPinned)
                exerciseNotesTarget = null
            },
        )
    }

    if (timingEditor) {
        session?.endedAt?.let { end ->
            TimingDialog(
                initialStart = session.workoutStartedAt(),
                initialEnd = end,
                onCancel = { timingEditor = false },
                onConfirm = { start, updatedEnd ->
                    viewModel.setSessionTiming(start, updatedEnd)
                    timingEditor = false
                },
            )
        }
    }

    when (deleteStage) {
        DeleteStage.None -> Unit

        DeleteStage.First -> AlertDialog(
            onDismissRequest = { deleteStage = DeleteStage.None },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
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
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
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
private fun TimingDialog(
    initialStart: Instant,
    initialEnd: Instant,
    onCancel: () -> Unit,
    onConfirm: (Instant, Instant) -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    var start by remember(initialStart) { mutableStateOf(initialStart) }
    var end by remember(initialEnd) { mutableStateOf(initialEnd) }
    val valid = !end.isBefore(start)

    fun pickDateTime(initial: Instant, onPicked: (Instant) -> Unit) {
        val current = initial.atZone(zone)
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                android.app.TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onPicked(
                            java.time.ZonedDateTime.of(year, month + 1, day, hour, minute, 0, 0, zone).toInstant(),
                        )
                    },
                    current.hour,
                    current.minute,
                    android.text.format.DateFormat.is24HourFormat(context),
                ).show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onCancel,
        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
        title = { Text("Edit session timing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimingRow("Started", start) { pickDateTime(start) { start = it } }
                TimingRow("Ended", end) { pickDateTime(end) { end = it } }
                Text(
                    if (valid) "Duration: ${formatDuration(Duration.between(start, end))}"
                    else "End time must be after start time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(start, end) }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun TimingRow(label: String, value: Instant, onEdit: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.systemDefault())
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatter.format(value), style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = onEdit) { Text("Change") }
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
        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
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
