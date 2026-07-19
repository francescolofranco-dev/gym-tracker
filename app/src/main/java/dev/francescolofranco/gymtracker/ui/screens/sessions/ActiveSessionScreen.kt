package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.WeightUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    onExit: () -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val hints by viewModel.hints.collectAsStateWithLifecycle()
    val exitRequested by viewModel.exitRequested.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var sessionNotesEditor by remember { mutableStateOf(false) }
    var exerciseNotesTarget by remember { mutableStateOf<SessionExerciseDetail?>(null) }

    LaunchedEffect(exitRequested) {
        if (exitRequested) onExit()
    }

    // Honour the "keep screen on during session" pref. Adds the FLAG_KEEP_SCREEN_ON window
    // flag while this screen is mounted AND the toggle is on; cleared on dispose so leaving
    // the screen lets the device sleep normally.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(keepScreenOn) {
        val window = (view.context as? android.app.Activity)?.window
        if (keepScreenOn && window != null) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            // Per-session progress = logged non-skipped sets / sum of planned sets across all
            // non-skipped exercises. Skipped exercises drop out of both numerator and
            // denominator so they don't drag the headline down.
            val progressPct = remember(details) {
                val plannedTotal = details
                    .filter { !it.sessionExercise.isSkipped }
                    .sumOf { it.exercise.targetSets }
                val loggedTotal = details
                    .filter { !it.sessionExercise.isSkipped }
                    .sumOf { d -> d.setLogs.count { it.loggedAt != null && it.reps != null && !it.isSkipped } }
                if (plannedTotal == 0) 0 else (loggedTotal * 100 / plannedTotal).coerceIn(0, 100)
            }
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Session")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = sessionProgressLabel(progressPct, details.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
        val isDraft = session?.acceptedAt == null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Draft state shows a prominent "Start workout" CTA in place of the timer pill —
            // the timer can't start until the session is accepted. Disabled until at least one
            // exercise has been picked, otherwise there's nothing to start.
            if (isDraft) {
                StartWorkoutBanner(
                    enabled = details.isNotEmpty(),
                    onStart = { viewModel.acceptSession() },
                )
            } else {
                TimerPill()
            }

            if (details.isEmpty()) {
                EmptyActiveSession(modifier = Modifier.fillMaxSize())
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Extra bottom space so the last set's ✓ scrolls clear of the FAB instead of
                // being permanently obscured by it.
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                session?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item {
                        SessionNotesPreview(notes = notes, onEdit = { sessionNotesEditor = true })
                    }
                }
                itemsIndexed(items = details, key = { _, d -> d.sessionExercise.id }) { index, detail ->
                    ExerciseCard(
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
                        onRemoveExercise = { viewModel.removeSessionExercise(detail.sessionExercise.id) },
                        onOpenExerciseStats = { onOpenExerciseStats(detail.exercise.id) },
                    )
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            excludeIds = details.map { it.exercise.id }.toSet(),
            onDismiss = { showPicker = false },
            onPick = { e -> viewModel.addExercise(e.id) },
        )
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
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

/**
 * Replaces the timer pill while the session is still a draft. Until the user taps Start
 * workout the session is invisible from the home screen banner and the past list, so this
 * banner is the only place that acknowledges work is in progress.
 */
@Composable
private fun StartWorkoutBanner(enabled: Boolean, onStart: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onStart,
        enabled = enabled,
        shape = dev.francescolofranco.gymtracker.ui.theme.ButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Start workout",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (enabled) "Begins the timer and counts this in your history."
                else "Pick an exercise first.",
                style = MaterialTheme.typography.labelSmall,
            )
        }
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

private fun exerciseCountLabel(count: Int): String =
    "$count exercise${if (count == 1) "" else "s"}"

private fun sessionProgressLabel(progressPct: Int, exerciseCount: Int): String =
    if (exerciseCount == 0) exerciseCountLabel(0) else "$progressPct% · ${exerciseCountLabel(exerciseCount)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    detail: SessionExerciseDetail,
    unit: WeightUnit,
    hints: List<HintRow>,
    editable: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCommitSet: (setLogId: Long, reps: Int, kg: Double) -> Unit,
    onUncommitSet: (setLogId: Long) -> Unit,
    onDraftSet: (setLogId: Long, reps: Int?, kg: Double?, kgFromExplicitEntry: Boolean) -> Unit,
    onToggleSetSkipped: (setLogId: Long, currentlySkipped: Boolean) -> Unit,
    onSkipExercise: (skipped: Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditExerciseNotes: () -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenExerciseStats: () -> Unit,
) {
    val exercise = detail.exercise
    val sets = detail.setLogs.sortedBy { it.setNumber }
    val loggedSets = sets.count { it.loggedAt != null && it.reps != null && !it.isSkipped }
    val incomplete = !detail.sessionExercise.isSkipped && loggedSets < exercise.targetSets
    // Completed = every planned set is logged. A subtle green tint on the card body confirms
    // the exercise is done without shouting — matches the SetIndex / CheckButton green.
    val isComplete = !detail.sessionExercise.isSkipped &&
        exercise.targetSets > 0 && loggedSets >= exercise.targetSets
    // Completed exercises get a calm green outline + a "Done" tag rather than a heavy green fill
    // wash, which read as too loud over the grey set rows.
    val cardBorder = if (isComplete) {
        BorderStroke(1.5.dp, dev.francescolofranco.gymtracker.ui.theme.VolumeGreen)
    } else {
        null
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (cardBorder != null) {
                    Modifier.border(cardBorder, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // "First time" — empty hints means this exercise has never been logged
                    // before. The badge gives the user a heads-up that there's no historical
                    // reference (so the dash sub-labels on the kg/reps chips are expected,
                    // not a glitch).
                    if (isComplete) {
                        Spacer(Modifier.width(8.dp))
                        CompletedBadge()
                    } else if (hints.isEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        FirstTimeBadge()
                    }
                    // Note flag — its own slot so it can sit alongside the Completed/First-time
                    // badge. Tapping it reveals the note; editing routes through the overflow menu.
                    val note = detail.sessionExercise.notes?.takeIf { it.isNotBlank() }
                    if (note != null) {
                        Spacer(Modifier.width(8.dp))
                        NoteIndicator(note = note, onEdit = onEditExerciseNotes)
                    }
                }
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
                IconButton(onClick = onOpenExerciseStats) {
                    Icon(Icons.Filled.BarChart, contentDescription = "Exercise stats")
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Exercise options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("View stats") },
                        leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenExerciseStats()
                        },
                    )
                    // Move up / down: greyed out at the edges instead of hidden so the menu
                    // stays a predictable shape across rows.
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                        enabled = canMoveUp,
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                        enabled = canMoveDown,
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        },
                    )
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SetTableHeader()

        // The "active" set is the first one still to log; SetRow tints its index. Only while
        // editable — a finished session shown in the detail view has no current set.
        val activeSetId = if (editable) {
            sets.firstOrNull { !it.isSkipped && !(it.loggedAt != null && it.reps != null) }?.id
        } else {
            null
        }

        Column {
            sets.forEach { log ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Each hint is already resolved per set-number (see ActiveSessionViewModel.hints),
                // so no cross-set fallback here — reusing e.g. set 1's hint for set 3 used to make
                // the "vs last time" delta compare against the wrong set, sometimes flipping its
                // sign (looked like a regression when it was really an improvement).
                val hint = hints.firstOrNull { it.setNumber == log.setNumber }
                SetRow(
                    state = SetRowState(
                        log = log,
                        targetReps = exercise.repRangeMin..exercise.repRangeMax,
                        isBodyweight = exercise.isBodyweight,
                        unit = unit,
                        hintReps = hint?.reps,
                        hintKg = hint?.kg,
                        isActive = log.id == activeSetId,
                    ),
                    onCommit = { reps, kg -> onCommitSet(log.id, reps, kg) },
                    onUncommit = { onUncommitSet(log.id) },
                    onDraft = { reps, kg, kgExplicit -> onDraftSet(log.id, reps, kg, kgExplicit) },
                    onSkipToggle = { onToggleSetSkipped(log.id, log.isSkipped) },
                    editable = editable,
                )
            }
        }
    }
}

/**
 * Compact pill rendered next to an exercise name when there's no prior session to compare
 * against — keeps the sub-label dashes on the kg/reps chips from feeling like missing data.
 */
@Composable
private fun CompletedBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "Completed",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun FirstTimeBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "First time",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/**
 * A compact, tappable note flag next to an exercise name. Tapping it pops a rich tooltip showing
 * the note text plus an Edit action; the tooltip is persistent so it stays open for reading until
 * the user taps away. Editing still routes through the overflow menu's "Edit note" via [onEdit].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteIndicator(note: String, onEdit: () -> Unit) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = { Text("Note") },
                action = {
                    TextButton(
                        onClick = {
                            scope.launch { tooltipState.dismiss() }
                            onEdit()
                        },
                    ) { Text("Edit") }
                },
            ) { Text(note) }
        },
        state = tooltipState,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { scope.launch { tooltipState.show() } }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                // Carry the note text in the description so screen readers can read it directly:
                // the tooltip's content lives in a popup that isn't part of this anchor's semantics.
                contentDescription = "Note: $note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
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
        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text("Add a note") },
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
