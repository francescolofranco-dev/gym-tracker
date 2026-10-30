package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.screens.exercises.PersonalRecordType
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    onExit: () -> Unit,
    onCompleted: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel(),
) {
    val loadableContent by viewModel.content.collectAsStateWithLifecycle()
    val content = (loadableContent as? Loadable.Ready)?.value
    val session = content?.session
    val details = content?.details.orEmpty()
    val unit = content?.unit ?: WeightUnit.KG
    val hints by viewModel.hints.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecords.collectAsStateWithLifecycle()
    val keepScreenOn = content?.keepScreenOn ?: false

    (loadableContent as? Loadable.Error)?.let {
        ErrorPane(it.message, viewModel::retry)
        return
    }

    var showPicker by remember { mutableStateOf(false) }
    var showWorkoutActions by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var sessionNotesEditor by remember { mutableStateOf(false) }
    var exerciseNotesTarget by remember { mutableStateOf<SessionExerciseDetail?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ActiveSessionViewModel.Event.Exit -> onExit()
                is ActiveSessionViewModel.Event.OpenCompleted -> onCompleted(event.sessionId)
            }
        }
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

    val plannedTotal = remember(details) {
        details
            .filter { !it.sessionExercise.isSkipped }
            .sumOf { d -> d.setLogs.count { !it.isSkipped } }
    }
    val loggedTotal = remember(details) {
        details
            .filter { !it.sessionExercise.isSkipped }
            .sumOf { d -> d.setLogs.count { it.loggedAt != null && it.reps != null && !it.isSkipped } }
    }
    val progressPct = remember(plannedTotal, loggedTotal) {
        if (plannedTotal == 0) 0 else (loggedTotal * 100 / plannedTotal).coerceIn(0, 100)
    }
    val totalVolumeKg = remember(details) {
        details
            .filter { !it.sessionExercise.isSkipped }
            .sumOf { d ->
                d.setLogs
                    .filter { it.loggedAt != null && it.reps != null && !it.isSkipped }
                    .sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) }
            }
    }

    Scaffold(
        containerColor = SurfaceDeep,
        topBar = {
            Column(modifier = Modifier.background(SurfaceBase)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceBase,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    title = {
                        Column {
                            Text(
                                text = "ACTIVE WORKOUT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                            Text(
                                text = sessionProgressLabel(progressPct, details.size),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = MutedOlive,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { sessionNotesEditor = true },
                            enabled = content != null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp)),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Session notes",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        TextButton(
                            onClick = { confirmEnd = true },
                            enabled = content != null,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = DestructiveCoral),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, DestructiveCoral, RoundedCornerShape(2.dp)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "End",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                            )
                        }
                        IconButton(
                            onClick = { showWorkoutActions = true },
                            enabled = content != null,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceCard)
                                .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Workout actions",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
                HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
            }
        },
        floatingActionButton = {
            if (content != null) {
                ExtendedFloatingActionButton(
                    onClick = { showPicker = true },
                    shape = RoundedCornerShape(4.dp),
                    containerColor = BrandLime,
                    contentColor = DarkOnPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = "Add exercise",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                        )
                    },
                )
            }
        },
    ) { padding ->
        if (content == null) {
            LoadingPane(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val isDraft = session?.acceptedAt == null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDeep)
                .padding(padding),
        ) {
            // Draft state shows a prominent "Start workout" CTA in place of the timer pill —
            // the timer can't start until the session is accepted. Disabled until at least one
            // exercise has been picked, otherwise there's nothing to start.
            AnimatedContent(
                targetState = isDraft,
                transitionSpec = {
                    ((fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                        slideInVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) { it / 3 })
                        .togetherWith(
                            fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                                slideOutVertically(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) { -it / 4 },
                        )) using SizeTransform(clip = false)
                },
                label = "session-header",
            ) { draft ->
                if (draft) {
                    StartWorkoutBanner(
                        enabled = details.isNotEmpty(),
                        onStart = { viewModel.acceptSession() },
                    )
                } else {
                    ActiveSessionMetricsBand(
                        loggedSets = loggedTotal,
                        plannedSets = plannedTotal,
                        totalVolumeKg = totalVolumeKg,
                        unit = unit,
                    )
                }
            }

            if (details.isEmpty()) {
                EmptyActiveSession(modifier = Modifier.fillMaxSize())
                return@Column
            }

            val reorder = rememberSessionExerciseDrag(details, viewModel::reorderExercises)
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = reorder.listState,
                    modifier = Modifier.fillMaxSize().then(reorder.gestures),
                    // Extra bottom space so the last set's ✓ scrolls clear of the FAB instead of
                    // being permanently obscured by it.
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                ) {
                    session?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item(key = "session-notes", contentType = "session-notes") {
                            SessionNotesPreview(notes = notes, onEdit = { sessionNotesEditor = true })
                        }
                        item(key = "session-notes-gap", contentType = "gap") {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    details.forEach { detail ->
                        exerciseSection(
                            detail = detail,
                            headerModifier = reorder.headerModifier(detail.sessionExercise.id),
                            footerModifier = reorder.footerModifier(detail.sessionExercise.id),
                            unit = unit,
                            hints = hints.byExerciseId[detail.exercise.id] ?: emptyList(),
                            personalRecords = personalRecords[detail.exercise.id].orEmpty(),
                            editable = true,
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
                            onEditExerciseNotes = { exerciseNotesTarget = detail },
                            onRemoveExercise = { viewModel.removeSessionExercise(detail.sessionExercise.id) },
                            onOpenExerciseStats = { onOpenExerciseStats(detail.exercise.id) },
                        )
                    }
                }
                SessionExerciseDragPreview(reorder, details)
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            excludeIds = details.map { it.exercise.id }.toSet(),
            onDismiss = { showPicker = false },
            onPick = { e -> viewModel.addExercise(e.id) },
            onUnpick = { e -> viewModel.removeExercise(e.id) },
        )
    }

    if (showWorkoutActions) {
        WorkoutActionsSheet(
            loggedSets = loggedTotal,
            plannedSets = plannedTotal,
            hasNotes = !session?.notes.isNullOrBlank(),
            onDismiss = { showWorkoutActions = false },
            onOpenNotes = {
                showWorkoutActions = false
                sessionNotesEditor = true
            },
            onEndWorkout = {
                showWorkoutActions = false
                confirmEnd = true
            },
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
        ExerciseNoteSheet(
            exerciseName = target.exercise.name,
            initial = target.sessionExercise.notes.orEmpty(),
            initialPinned = target.sessionExercise.pinnedNotes.orEmpty(),
            onCancel = { exerciseNotesTarget = null },
            onConfirm = { notes, pinnedNotes ->
                viewModel.setExerciseNote(target.sessionExercise.id, notes, pinnedNotes)
                exerciseNotesTarget = null
            },
        )
    }
}

@Composable
private fun ActiveSessionMetricsBand(
    loggedSets: Int,
    plannedSets: Int,
    totalVolumeKg: Double,
    unit: WeightUnit,
) {
    val convertedVolume = convertFromKg(totalVolumeKg, unit).roundToInt()
    val formattedVol = remember(convertedVolume) {
        NumberFormat.getIntegerInstance(Locale.US).format(convertedVolume)
    }
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceBase)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerPill(
                modifier = Modifier.weight(1.1f),
                compact = true,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceCard)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.72.sp,
                    ),
                    color = MutedOlive,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "$loggedSets / $plannedSets",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "SETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = SlateSubtext,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceCard)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "TOTAL VOL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 0.72.sp,
                    ),
                    color = MutedOlive,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formattedVol,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = unit.label().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = SlateSubtext,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
    }
}

/**
 * Replaces the timer pill while the session is still a draft. Until the user taps Start
 * workout the session is invisible from the home screen banner and the past list, so this
 * banner is the only place that acknowledges work is in progress.
 */
@Composable
private fun StartWorkoutBanner(enabled: Boolean, onStart: () -> Unit) {
    Button(
        onClick = onStart,
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandLime,
            contentColor = DarkOnPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "START WORKOUT",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
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
            tint = BrandLime,
        )
        Text(text = "EMPTY SESSION", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Tap Add exercise to add the first one.",
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
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "SESSION NOTES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = BrandLime,
            )
            Text(text = notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun exerciseCountLabel(count: Int): String =
    "$count exercise${if (count == 1) "" else "s"}"

private fun sessionProgressLabel(progressPct: Int, exerciseCount: Int): String =
    if (exerciseCount == 0) exerciseCountLabel(0) else "$progressPct% · ${exerciseCountLabel(exerciseCount)}"

@Composable
private fun Modifier.completionRail(isComplete: Boolean): Modifier {
    val surface = SurfaceCard
    return if (isComplete) {
        background(BrandLime)
            .padding(start = 3.dp)
            .background(surface)
    } else {
        background(surface)
    }
}

/**
 * Adds one exercise as granular lazy items. A whole [ExerciseCard] used to be one very tall lazy
 * item, so crossing an exercise boundary forced Compose to create its header and every set row in
 * a single frame. Keeping each set as its own keyed item spreads that work across prefetch frames.
 */
internal fun LazyListScope.exerciseSection(
    headerModifier: Modifier = Modifier,
    footerModifier: Modifier = Modifier,
    detail: SessionExerciseDetail,
    unit: WeightUnit,
    hints: List<HintRow>,
    personalRecords: Set<PersonalRecordType> = emptySet(),
    editable: Boolean,
    onCommitSet: (setLogId: Long, reps: Int, kg: Double) -> Unit,
    onUncommitSet: (setLogId: Long) -> Unit,
    onDraftSet: (setLogId: Long, reps: Int?, kg: Double?, kgFromExplicitEntry: Boolean) -> Unit,
    onToggleSetSkipped: (setLogId: Long, currentlySkipped: Boolean) -> Unit,
    onSkipExercise: (skipped: Boolean) -> Unit,
    onEditExerciseNotes: () -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenExerciseStats: () -> Unit,
) {
    val exercise = detail.exercise
    val sets = detail.setLogs.sortedWith(compareBy({ it.setNumber }, { it.side.ordinal }))
    val loggedSets = sets.count { it.loggedAt != null && it.reps != null && !it.isSkipped }
    val plannedSets = sets.count { !it.isSkipped }
    val incomplete = !detail.sessionExercise.isSkipped && loggedSets < plannedSets
    val isComplete = !detail.sessionExercise.isSkipped &&
        plannedSets > 0 && loggedSets >= plannedSets
    val sectionId = detail.sessionExercise.id

    item(
        key = "exercise-$sectionId-header",
        contentType = "exercise-header",
    ) {
        ExerciseSectionHeader(
            modifier = headerModifier,
            detail = detail,
            hints = hints,
            personalRecords = personalRecords,
            isComplete = isComplete,
            incomplete = incomplete,
            hasSets = sets.isNotEmpty(),
            editable = editable,
            onSkipExercise = onSkipExercise,
            onEditExerciseNotes = onEditExerciseNotes,
            onRemoveExercise = onRemoveExercise,
            onOpenExerciseStats = onOpenExerciseStats,
        )
    }

    if (sets.isNotEmpty()) {
        item(
            key = "exercise-$sectionId-columns",
            contentType = "set-columns",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .completionRail(isComplete)
                    .border(width = 1.dp, color = EditorialBorder),
            ) {
                SetTableHeader()
            }
        }

        val activeSetId = if (editable) {
            sets.firstOrNull { !it.isSkipped && !(it.loggedAt != null && it.reps != null) }?.id
        } else {
            null
        }
        items(
            items = sets,
            key = { "set-${it.id}" },
            contentType = { if (it.id == sets.last().id) "set-row-last" else "set-row" },
        ) { log ->
            val hint = hints.firstOrNull { it.setNumber == log.setNumber && it.side == log.side }
            val isLast = log.id == sets.last().id
            val shape = if (isLast) {
                RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(0.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .completionRail(isComplete)
                    .border(1.dp, EditorialBorder, shape),
            ) {
                SetRow(
                    state = SetRowState(
                        log = log,
                        targetReps = exercise.repRangeMin..exercise.repRangeMax,
                        isBodyweight = exercise.isBodyweight,
                        unit = unit,
                        hintReps = hint?.reps,
                        hintKg = hint?.kg,
                        isActive = log.id == activeSetId,
                        showBilateralLabel = sets.any { it.side != ExerciseSide.BOTH },
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

    item(
        key = "exercise-$sectionId-gap",
        contentType = "gap",
    ) {
        Spacer(footerModifier.fillMaxWidth().height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSectionHeader(
    modifier: Modifier = Modifier,
    detail: SessionExerciseDetail,
    hints: List<HintRow>,
    personalRecords: Set<PersonalRecordType>,
    isComplete: Boolean,
    incomplete: Boolean,
    hasSets: Boolean,
    editable: Boolean,
    onSkipExercise: (skipped: Boolean) -> Unit,
    onEditExerciseNotes: () -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenExerciseStats: () -> Unit,
) {
    val exercise = detail.exercise
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = if (hasSets) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    } else {
        RoundedCornerShape(4.dp)
    }
    val pinnedNote = detail.sessionExercise.pinnedNotes?.takeIf { it.isNotBlank() }
    val sessionNote = detail.sessionExercise.notes?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceElevated)
            .border(1.dp, EditorialBorder, shape)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ExerciseNameAndBadges(
                    name = exercise.name,
                    isComplete = isComplete,
                    isFirstTime = hints.isEmpty(),
                    personalRecords = personalRecords,
                    note = sessionNote,
                    pinnedNote = pinnedNote,
                    isNoteCarried = !detail.sessionExercise.noteCarryForward,
                    onEditNote = onEditExerciseNotes,
                )
                Text(
                    text = buildList {
                        add(exercise.primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName })
                        if (exercise.isBodyweight) add("BW")
                        add(detail.setPlanLabel())
                        if (detail.sessionExercise.isSkipped) add("Skipped")
                        else if (incomplete) add("Incomplete")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = OliveSubtext,
                )
            }
            IconButton(
                onClick = onOpenExerciseStats,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceControl)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp)),
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = "Exercise stats",
                    tint = SlateSubtext,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceControl)
                        .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Exercise options",
                        tint = SlateSubtext,
                        modifier = Modifier.size(16.dp),
                    )
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
                    DropdownMenuItem(
                        text = { Text(if (detail.sessionExercise.isSkipped) "Unskip" else "Skip exercise") },
                        leadingIcon = { Icon(Icons.Filled.NotInterested, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSkipExercise(!detail.sessionExercise.isSkipped)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                when {
                                    detail.sessionExercise.notes.isNullOrBlank() &&
                                        detail.sessionExercise.pinnedNotes.isNullOrBlank() -> "Add notes"
                                    else -> "Edit notes"
                                },
                            )
                        },
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    detail: SessionExerciseDetail,
    unit: WeightUnit,
    hints: List<HintRow>,
    modifier: Modifier = Modifier,
    personalRecords: Set<PersonalRecordType> = emptySet(),
    editable: Boolean,
    onCommitSet: (setLogId: Long, reps: Int, kg: Double) -> Unit,
    onUncommitSet: (setLogId: Long) -> Unit,
    onDraftSet: (setLogId: Long, reps: Int?, kg: Double?, kgFromExplicitEntry: Boolean) -> Unit,
    onToggleSetSkipped: (setLogId: Long, currentlySkipped: Boolean) -> Unit,
    onSkipExercise: (skipped: Boolean) -> Unit,
    onEditExerciseNotes: () -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenExerciseStats: () -> Unit,
) {
    val exercise = detail.exercise
    val sets = detail.setLogs.sortedWith(compareBy({ it.setNumber }, { it.side.ordinal }))
    val loggedSets = sets.count { it.loggedAt != null && it.reps != null && !it.isSkipped }
    val plannedSets = sets.count { !it.isSkipped }
    val incomplete = !detail.sessionExercise.isSkipped && loggedSets < plannedSets
    // Completion uses the same accent as logged set checks.
    val isComplete = !detail.sessionExercise.isSkipped &&
        plannedSets > 0 && loggedSets >= plannedSets
    val borderWidth by animateDpAsState(
        targetValue = if (isComplete) 1.5.dp else 0.dp,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "exercise completion border",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isComplete) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "exercise completion color",
    )

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                ExerciseNameAndBadges(
                    name = exercise.name,
                    isComplete = isComplete,
                    isFirstTime = hints.isEmpty(),
                    personalRecords = personalRecords,
                    note = detail.sessionExercise.notes?.takeIf { it.isNotBlank() },
                    pinnedNote = detail.sessionExercise.pinnedNotes?.takeIf { it.isNotBlank() },
                    isNoteCarried = !detail.sessionExercise.noteCarryForward,
                    onEditNote = onEditExerciseNotes,
                )
                Text(
                    text = buildList {
                        add(exercise.primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName })
                        if (exercise.isBodyweight) add("BW")
                        add(detail.setPlanLabel())
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
                    DropdownMenuItem(
                        text = { Text(if (detail.sessionExercise.isSkipped) "Unskip" else "Skip exercise") },
                        leadingIcon = { Icon(Icons.Filled.NotInterested, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSkipExercise(!detail.sessionExercise.isSkipped)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                when {
                                    detail.sessionExercise.notes.isNullOrBlank() &&
                                        detail.sessionExercise.pinnedNotes.isNullOrBlank() -> "Add notes"
                                    else -> "Edit notes"
                                },
                            )
                        },
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
                val hint = hints.firstOrNull { it.setNumber == log.setNumber && it.side == log.side }
                SetRow(
                    state = SetRowState(
                        log = log,
                        targetReps = exercise.repRangeMin..exercise.repRangeMax,
                        isBodyweight = exercise.isBodyweight,
                        unit = unit,
                        hintReps = hint?.reps,
                        hintKg = hint?.kg,
                        isActive = log.id == activeSetId,
                        showBilateralLabel = sets.any { it.side != ExerciseSide.BOTH },
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
 * Keeps an exercise name readable when completion, record, and note badges all appear together.
 * A [Row] with a weighted name allowed the badges to consume nearly all of the name's width,
 * causing short names such as "Lat Pullover" to wrap one character per line. A [FlowRow] keeps
 * the normal inline layout when it fits and wraps badges to a new line when it does not.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseNameAndBadges(
    name: String,
    isComplete: Boolean,
    isFirstTime: Boolean,
    personalRecords: Set<PersonalRecordType>,
    note: String?,
    pinnedNote: String?,
    isNoteCarried: Boolean,
    onEditNote: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            ),
        )
        when {
            isComplete -> CompletedBadge()
            isFirstTime -> FirstTimeBadge()
        }
        if (personalRecords.isNotEmpty()) {
            PersonalRecordBadge(personalRecords)
        }
        if (pinnedNote != null || note != null) {
            // Keep the two note types together when the header wraps, while still allowing
            // them to wrap independently on narrow screens or with larger system text.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (pinnedNote != null) {
                    NoteIndicator(
                        note = pinnedNote,
                        isPinned = true,
                        isCarried = false,
                        onEdit = onEditNote,
                    )
                }
                if (note != null) {
                    NoteIndicator(
                        note = note,
                        isPinned = false,
                        isCarried = isNoteCarried,
                        onEdit = onEditNote,
                    )
                }
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
            .clip(RoundedCornerShape(2.dp))
            .background(BrandLime)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "COMPLETED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = DarkOnPrimary,
        )
    }
}

@Composable
private fun FirstTimeBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceControl)
            .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "FIRST TIME",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = SlateSubtext,
        )
    }
}

@Composable
private fun PersonalRecordBadge(records: Set<PersonalRecordType>) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(BrandLime)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = records.joinToString(" · ") { it.shortLabel },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = DarkOnPrimary,
        )
    }
}

/**
 * A quiet note badge with a full-size touch target around its compact painted surface.
 * The persistent tooltip keeps the complete note and its Edit action available on tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteIndicator(
    note: String,
    isPinned: Boolean,
    isCarried: Boolean,
    onEdit: () -> Unit,
) {
    val label = when {
        isPinned -> "Pinned note"
        isCarried -> "From last session"
        else -> "Note"
    }
    val badgeLabel = when {
        isPinned -> "Pinned"
        isCarried -> "Previous note"
        else -> "Note"
    }
    val containerColor = if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (isPinned) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            RichTooltip(
                title = { Text(label) },
                action = {
                    TextButton(
                        onClick = {
                            scope.launch { tooltipState.dismiss() }
                            onEdit()
                        },
                    ) { Text("Edit") }
                },
            ) {
                Text(
                    text = note,
                    modifier = Modifier
                        .sizeIn(maxHeight = 240.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        },
        state = tooltipState,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClickLabel = "Read $label") {
                    scope.launch { tooltipState.show() }
                }
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clearAndSetSemantics { contentDescription = "$label: $note" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutActionsSheet(
    loggedSets: Int,
    plannedSets: Int,
    hasNotes: Boolean,
    onDismiss: () -> Unit,
    onOpenNotes: () -> Unit,
    onEndWorkout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "WORKOUT ACTIONS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandLime),
                        )
                        Text(
                            text = "ACTIVE SESSION · $loggedSets/$plannedSets SETS LOGGED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp,
                            ),
                            color = OliveSubtext,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = SlateSubtext,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .clickable(onClick = onOpenNotes)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceControl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = BrandLime,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WORKOUT NOTES",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = if (hasNotes) "Edit session readiness, cues & fatigue notes"
                            else "Add session readiness, cues & fatigue notes",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OliveSubtext,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = SlateSubtext,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(DestructiveCoral.copy(alpha = 0.08f))
                        .border(1.dp, DestructiveCoral.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onEndWorkout)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DestructiveCoral.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            tint = DestructiveCoral,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "END WORKOUT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = DestructiveCoral,
                        )
                        Text(
                            text = "Finish session and save logged sets to history",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = DestructiveCoral.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = DestructiveCoral,
                    )
                }
            }
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
