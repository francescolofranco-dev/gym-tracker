package dev.francescolofranco.gymtracker.ui.screens.sessions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.TemplateEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onOpenActive: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()

    var confirmEnd by remember { mutableStateOf(false) }

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

    Scaffold { padding ->
        when (val current = content) {
            Loadable.Loading -> LoadingPane(modifier = Modifier.padding(padding))
            is Loadable.Ready -> {
                val (active, past, unit, suggestion) = current.value
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Only surface a "Session in progress" banner once the user has actually
                    // started. Drafts stay invisible here; the user re-enters via Start.
                    active?.takeIf { it.acceptedAt != null }?.let { session ->
                        ActiveSessionBanner(
                            session = session,
                            onResume = { onOpenActive(session.id) },
                            onEnd = { confirmEnd = true },
                        )
                    }

                    if (past.isEmpty() && active == null) {
                        EmptyState(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    } else {
                        val grouped = remember(past) { groupPastSessionsByMonth(past) }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            grouped.forEach { (label, group) ->
                                item(key = "h-$label") { MonthHeader(label = label) }
                                items(items = group, key = { it.session.id }) { summary ->
                                    PastSessionRow(
                                        summary = summary,
                                        unit = unit,
                                        onClick = { onOpenDetail(summary.session.id) },
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (active == null && suggestion != null) {
                            SuggestionBanner(
                                template = suggestion,
                                onUseTemplate = {
                                    startWithPermission { viewModel.startWithTemplate(suggestion.id) }
                                },
                            )
                        }
                        Button(
                            onClick = { startWithPermission { viewModel.startBlankSession() } },
                            shape = dev.francescolofranco.gymtracker.ui.theme.ButtonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    active != null -> "Resume session"
                                    suggestion != null -> "Start blank"
                                    else -> "Start session"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionBanner(template: TemplateEntity, onUseTemplate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onUseTemplate)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's suggestion",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "${template.name} — based on your rotation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = "Use",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    session: SessionEntity,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    val elapsed = rememberElapsed(session.startedAt)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onResume),
            ) {
                Text(
                    text = "Session in progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = formatDuration(elapsed),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            TextButton(
                onClick = onEnd,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text("End")
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Resume",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = onResume).padding(8.dp),
            )
        }
    }
}

@Composable
private fun PastSessionRow(summary: SessionSummary, unit: WeightUnit, onClick: () -> Unit) {
    val ended = summary.session.endedAt
    val duration = if (ended != null) Duration.between(summary.session.startedAt, ended) else Duration.ZERO

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = formatSessionDate(summary.session.startedAt),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = buildList {
                    add("${summary.exerciseCount} exercise${if (summary.exerciseCount == 1) "" else "s"}")
                    add("${summary.setCount} set${if (summary.setCount == 1) "" else "s"}")
                    if (summary.totalVolume > 0) add(formatTotalVolume(summary.totalVolume, unit))
                    if (ended != null) add(formatDuration(duration))
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text(text = "No sessions yet", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Tap Start session to begin tracking your first workout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

private fun groupPastSessionsByMonth(items: List<SessionSummary>): List<Pair<String, List<SessionSummary>>> {
    if (items.isEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    val byMonth = LinkedHashMap<YearMonth, MutableList<SessionSummary>>()
    items.forEach { s ->
        val ym = YearMonth.from(s.session.startedAt.atZone(zone))
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
