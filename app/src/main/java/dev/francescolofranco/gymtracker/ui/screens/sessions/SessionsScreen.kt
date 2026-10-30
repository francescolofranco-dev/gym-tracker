package dev.francescolofranco.gymtracker.ui.screens.sessions

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SubtleBorder
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onOpenActive: (Long) -> Unit,
    onOpenCompleted: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenTemplates: () -> Unit = {},
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()

    var editGoal by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var pendingStart by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // Whether granted or denied, run the deferred start. If denied, the timer service
        // still runs but the persistent notification just won't be visible.
        pendingStart?.invoke()
        pendingStart = null
    }
    fun startWithPermission(start: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            start(); return
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            start()
        } else {
            pendingStart = start
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SessionsViewModel.Event.OpenActive -> onOpenActive(event.sessionId)
                is SessionsViewModel.Event.OpenCompleted -> onOpenCompleted(event.sessionId)
            }
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
            title = { Text("End this session?") },
            text = {
                Text(
                    "It will move to the past sessions list with whatever's been logged. " +
                        "You can delete it from there if it's a phantom from earlier testing.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    viewModel.endActiveSession()
                }) { Text("End") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(containerColor = SurfaceDeep) { padding ->
        when (val current = content) {
            Loadable.Loading -> LoadingPane(modifier = Modifier.padding(padding))
            is Loadable.Error -> ErrorPane(current.message, viewModel::retry, Modifier.padding(padding))
            is Loadable.Ready -> {
                val (active, past, unit, suggestion, weeklyGoal, templates) = current.value
                if (showTemplatePicker) {
                    TemplatePickerSheet(
                        templates = templates,
                        suggestion = suggestion,
                        onDismiss = { showTemplatePicker = false },
                        onSelectTemplate = { template ->
                            showTemplatePicker = false
                            startWithPermission { viewModel.startWithTemplate(template.id) }
                        },
                        onManageTemplates = {
                            showTemplatePicker = false
                            onOpenTemplates()
                        },
                    )
                }
                if (editGoal) {
                    AlertDialog(
                        onDismissRequest = { editGoal = false },
                        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
                        containerColor = SurfaceCard,
                        title = { Text("Weekly session goal") },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                Text("Completed sessions, Monday through Sunday.")
                                (1..7).forEach { goal ->
                                    TextButton(onClick = { viewModel.setWeeklyGoal(goal); editGoal = false }) {
                                        Text("$goal session${if (goal == 1) "" else "s"}${if (goal == weeklyGoal) " · Selected" else ""}")
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { editGoal = false }) { Text("Cancel") } },
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDeep)
                        .padding(padding),
                ) {
                    // Only surface a "Session in progress" banner once the user has actually
                    // started. Drafts stay invisible here; the user re-enters via Start.
                    AnimatedVisibility(
                        visible = active?.acceptedAt != null,
                        enter = fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                            expandVertically(tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing)),
                        exit = fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                            shrinkVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)),
                    ) {
                        active?.let { session ->
                            ActiveSessionBanner(
                                session = session,
                                onResume = { onOpenActive(session.id) },
                                onEnd = { confirmEnd = true },
                            )
                        }
                    }

                    if (past.isEmpty() && active == null) {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            WeeklyProgress(past, weeklyGoal, onEditGoal = { editGoal = true })
                            EmptyState(modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                    } else {
                        val grouped = remember(past) { groupPastSessionsByMonth(past) }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            item(key = "weekly-progress") {
                                WeeklyProgress(past, weeklyGoal, onEditGoal = { editGoal = true })
                            }
                            grouped.forEach { (label, group) ->
                                item(key = "h-$label") {
                                    MonthHeader(label = label, sessionCount = group.size)
                                }
                                itemsIndexed(items = group, key = { _, it -> it.session.id }) { index, summary ->
                                    PastSessionRow(
                                        summary = summary,
                                        unit = unit,
                                        isFirst = index == 0,
                                        isLast = index == group.lastIndex,
                                        onClick = { onOpenDetail(summary.session.id) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = GymMotion.ItemFadeIn,
                                            placementSpec = GymMotion.ItemPlacement,
                                            fadeOutSpec = GymMotion.ItemFadeOut,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDeep)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { startWithPermission { viewModel.startBlankSession() } },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandLime,
                                contentColor = DarkOnPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                        ) {
                            Icon(
                                imageVector = if (active != null) Icons.Filled.PlayArrow else Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    active != null -> "RESUME WORKOUT"
                                    else -> "START WORKOUT"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp,
                                ),
                            )
                        }
                        AnimatedVisibility(
                            visible = active == null,
                            enter = fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                                expandVertically(tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing)),
                            exit = fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                                shrinkVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)),
                        ) {
                            TemplateLaunchButton(
                                suggestion = suggestion,
                                templateCount = templates.size,
                                onClick = { showTemplatePicker = true },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateLaunchButton(
    suggestion: TemplateEntity?,
    templateCount: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = BrandLime,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "START WORKOUT FROM A TEMPLATE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.55.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (suggestion != null) {
                Text(
                    text = suggestion.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = BrandLime,
                    maxLines = 1,
                )
            } else if (templateCount > 0) {
                Text(
                    text = "$templateCount",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SlateSubtext,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = BrandLime,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatePickerSheet(
    templates: List<TemplateEntity>,
    suggestion: TemplateEntity?,
    onDismiss: () -> Unit,
    onSelectTemplate: (TemplateEntity) -> Unit,
    onManageTemplates: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderedTemplates = remember(templates, suggestion) {
        if (suggestion == null) templates
        else listOfNotNull(templates.firstOrNull { it.id == suggestion.id }) +
            templates.filter { it.id != suggestion.id }
    }

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
                        text = "START FROM TEMPLATE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                    Text(
                        text = "SELECT A TEMPLATE TO LAUNCH WORKOUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = SlateSubtext,
                    )
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

            if (orderedTemplates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "NO TEMPLATES YET",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Create reusable routines (e.g. Upper Body A, Push, Pull, Legs) to start workouts in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSubtext,
                    )
                    Button(
                        onClick = onManageTemplates,
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandLime,
                            contentColor = DarkOnPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "CREATE TEMPLATE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    orderedTemplates.forEach { template ->
                        val isSuggested = suggestion?.id == template.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = if (isSuggested) BrandLime.copy(alpha = 0.65f) else EditorialBorder,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .clickable { onSelectTemplate(template) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSuggested) BrandLime.copy(alpha = 0.16f) else SurfaceCard),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isSuggested) BrandLime else SlateSubtext,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = template.name.uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.4.sp,
                                        ),
                                    )
                                    if (isSuggested) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(BrandLime)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = "NEXT UP",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp,
                                                ),
                                                color = DarkOnPrimary,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isSuggested) "Suggested next in your rotation" else "Tap to start workout with this template",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = SlateSubtext,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "START",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                    ),
                                    color = BrandLime,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = BrandLime,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDeep)
                            .border(1.dp, EditorialBorder.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable(onClick = onManageTemplates)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MutedOlive,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "MANAGE WORKOUT TEMPLATES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                ),
                                color = SlateSubtext,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MutedOlive,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    session: SessionEntity,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    val elapsed = rememberElapsed(session.workoutStartedAt())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, BrandLime, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onResume),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
                        text = "SESSION IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.0.sp,
                        ),
                        color = BrandLime,
                    )
                }
                Text(
                    text = formatDuration(elapsed),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(
                onClick = onEnd,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = DestructiveCoral,
                ),
            ) {
                Text(
                    text = "END",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Resume",
                tint = BrandLime,
                modifier = Modifier
                    .clickable(onClick = onResume)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun PastSessionRow(
    summary: SessionSummary,
    unit: WeightUnit,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ended = summary.session.endedAt
    val duration = if (ended != null) summary.session.workoutDuration() else Duration.ZERO
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 4.dp else 0.dp,
        topEnd = if (isFirst) 4.dp else 0.dp,
        bottomStart = if (isLast) 4.dp else 0.dp,
        bottomEnd = if (isLast) 4.dp else 0.dp,
    )
    val convertedVolume = convertFromKg(summary.totalVolume, unit).roundToInt()
    val formattedVolumeNumber = remember(convertedVolume) {
        NumberFormat.getIntegerInstance(Locale.US).format(convertedVolume)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(SurfaceBase)
            .border(1.dp, EditorialBorder, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatSessionDate(summary.session.workoutStartedAt()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildList {
                        add("${summary.exerciseCount} exercise${if (summary.exerciseCount == 1) "" else "s"}")
                        add("${summary.setCount} set${if (summary.setCount == 1) "" else "s"}")
                        if (ended != null) add(formatDuration(duration))
                    }.joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SlateSubtext,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = formattedVolumeNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = unit.label().uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MutedOlive,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = "TOTAL VOLUME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.72.sp,
                        ),
                        color = MutedOlive,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MutedOlive,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "NO SESSIONS YET",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Tap Start Workout to begin tracking your first session.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateSubtext,
            )
        }
    }
}

@Composable
private fun MonthHeader(label: String, sessionCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "MONTHLY LOG",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.55.sp,
                ),
                color = MutedOlive,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceCard)
                .border(1.dp, EditorialBorder, RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = null,
                tint = BrandLime,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "$sessionCount SESSION${if (sessionCount == 1) "" else "S"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.55.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

private fun groupPastSessionsByMonth(items: List<SessionSummary>): List<Pair<String, List<SessionSummary>>> {
    if (items.isEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    val byMonth = LinkedHashMap<YearMonth, MutableList<SessionSummary>>()
    items.forEach { s ->
        val ym = YearMonth.from(s.session.workoutStartedAt().atZone(zone))
        byMonth.getOrPut(ym) { mutableListOf() }.add(s)
    }
    return byMonth.entries.map { (ym, group) ->
        ym.format(monthFormatter).replaceFirstChar { it.titlecase() } to group
    }
}

@Composable
private fun rememberElapsed(start: Instant): Duration {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(start) {
        while (true) {
            now = Instant.now()
            delay(1_000.milliseconds)
        }
    }
    return remember(now, start) { Duration.between(start, now) }
}

/** Decorative marks are replaced by actual progress, with the target available at the point of use. */
@Composable
private fun WeeklyProgress(past: List<SessionSummary>, goal: Int, onEditGoal: () -> Unit) {
    // Recompute on composition so returning home after midnight observes the new calendar week.
    val count = completedThisWeek(past.map { it.session })
    val progress = (count.toFloat() / goal).coerceIn(0f, 1f)
    val remaining = (goal - count).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceBase)
            .border(1.dp, EditorialBorder, RoundedCornerShape(4.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(14.dp)
                        .background(BrandLime),
                )
                Text(
                    text = "WEEKLY SESSION GOAL",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = onEditGoal)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "EDIT GOAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.88.sp,
                    ),
                    color = BrandLime,
                )
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = BrandLime,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .semantics { contentDescription = "$count of $goal weekly sessions completed" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(80.dp)) {
                    val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Butt)
                    drawArc(SubtleBorder, -90f, 360f, false, style = stroke)
                    if (progress > 0f) drawArc(BrandLime, -90f, 360f * progress, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$count/$goal",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "SESSIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 0.64.sp,
                        ),
                        color = MutedOlive,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = when (remaining) {
                        0 -> "WEEKLY GOAL REACHED. KEEP BUILDING."
                        1 -> "ONE MORE SESSION TO YOUR WEEKLY GOAL"
                        else -> "$remaining MORE SESSIONS TO YOUR WEEKLY GOAL"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Keep your training momentum going",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SlateSubtext,
                )
            }
        }
    }
}
