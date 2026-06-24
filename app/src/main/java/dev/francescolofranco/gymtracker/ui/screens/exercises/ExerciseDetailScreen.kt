package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.screens.sessions.convertFromKg
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatSessionDate
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatWeightNumber
import dev.francescolofranco.gymtracker.ui.screens.sessions.label
import dev.francescolofranco.gymtracker.ui.theme.RegressionAmber
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    var showEdit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = exercise != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit exercise")
                    }
                },
            )
        },
    ) { padding ->
        val ex = exercise
        if (ex == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This exercise is no longer available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        // Pair each session with the most recent *earlier* session that has a value for the headline
        // metric, so the per-row delta bridges over any zero-load session instead of vanishing, then
        // show newest first. Memoised so it isn't rebuilt on every unit / edit-sheet recomposition.
        val headline = headlineMetric(ex.isBodyweight)
        val historyRows = remember(progress, headline) {
            var lastWithValue: SessionProgressPoint? = null
            val acc = ArrayList<Pair<SessionProgressPoint, SessionProgressPoint?>>(progress.size)
            for (p in progress) {
                acc.add(p to lastWithValue)
                if (p.valueFor(headline) != null) lastWithValue = p
            }
            acc.asReversed()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(exercise = ex) }
            item { ProgressSection(points = progress, isBodyweight = ex.isBodyweight, unit = unit) }
            item {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (historyRows.isEmpty()) {
                item { EmptyHistory() }
            } else {
                items(items = historyRows, key = { it.first.sessionId }) { (point, previous) ->
                    HistoryRow(
                        point = point,
                        previous = previous,
                        isBodyweight = ex.isBodyweight,
                        unit = unit,
                        onClick = { onOpenSession(point.sessionId) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                }
            }
        }
    }

    if (showEdit) {
        val current = exercise
        if (current != null) {
            ExerciseFormSheet(
                initial = current.toFormState(),
                title = "Edit exercise",
                onDismiss = { showEdit = false },
                onSave = { state ->
                    viewModel.save(state)
                    showEdit = false
                },
            )
        }
    }
}

@Composable
private fun Header(exercise: ExerciseEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = exercise.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        Text(
            text = buildList {
                add(exercise.primaryMuscles.sortedBy { it.ordinal }.joinToString(" + ") { it.displayName })
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    add(exercise.secondaryMuscles.sortedBy { it.ordinal }.joinToString(", ") { it.displayName })
                }
            }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildList {
                add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}")
                if (exercise.isBodyweight) add("Bodyweight")
                if (exercise.deletedAt != null) add("Deleted")
            }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressSection(
    points: List<SessionProgressPoint>,
    isBodyweight: Boolean,
    unit: WeightUnit,
) {
    val metrics = remember(isBodyweight) { metricsFor(isBodyweight) }
    var selectedIndex by rememberSaveable(isBodyweight) { mutableStateOf(0) }
    val metric = metrics[selectedIndex.coerceIn(0, metrics.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            val latest = points.lastOrNull { it.valueFor(metric) != null }?.valueFor(metric)
            if (latest != null) {
                Text(
                    text = formatMetricValue(latest, metric, unit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Overload verdict is judged on the canonical headline metric (est. 1RM / best reps),
        // independent of whichever metric is currently charted.
        val headline = headlineMetric(isBodyweight)
        val headlineSeries = remember(points, headline) { points.mapNotNull { it.valueFor(headline) } }
        val trend = remember(headlineSeries) { overloadTrend(headlineSeries) }
        if (trend.trend != OverloadTrend.NotEnoughData) {
            OverloadVerdict(trend)
        }

        if (metrics.size > 1) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                metrics.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = i == selectedIndex,
                        onClick = { selectedIndex = i },
                        shape = SegmentedButtonDefaults.itemShape(i, metrics.size),
                    ) { Text(m.label) }
                }
            }
        }

        val series = remember(points, metric) { points.mapNotNull { it.valueFor(metric) } }
        when {
            points.isEmpty() -> {
                Text(
                    text = "No completed sessions yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            series.size < 2 -> {
                Text(
                    text = "Log a second session to see the trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                TrendLineChart(values = series.map { displayValue(it, metric, unit) })
            }
        }
    }
}

@Composable
private fun OverloadVerdict(trend: TrendResult) {
    // The saturated volume tokens read fine as an icon/tint accent but fail text contrast on the
    // near-white light surface, so the label itself is drawn in onSurface; colour + arrow shape
    // carry the up/down/flat meaning redundantly.
    val accent = when (trend.trend) {
        OverloadTrend.Progressing -> VolumeGreen
        OverloadTrend.Regressing -> RegressionAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon: ImageVector = when (trend.trend) {
        OverloadTrend.Progressing -> Icons.AutoMirrored.Filled.TrendingUp
        OverloadTrend.Regressing -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = verdictLabel(trend),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HistoryRow(
    point: SessionProgressPoint,
    previous: SessionProgressPoint?,
    isBodyweight: Boolean,
    unit: WeightUnit,
    onClick: () -> Unit,
) {
    val headline = headlineMetric(isBodyweight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = formatSessionDate(point.startedAt), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = buildList {
                    add("${point.setsLogged} set${if (point.setsLogged == 1) "" else "s"}")
                    if (isBodyweight) {
                        add("best ${point.bestReps} reps")
                    } else if (point.topSetKg != null && point.topSetReps != null) {
                        add("${formatWeightNumber(convertFromKg(point.topSetKg, unit))} ${unit.label()} × ${point.topSetReps}")
                    }
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val value = point.valueFor(headline)
        if (value != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMetricValue(value, headline, unit),
                    style = MaterialTheme.typography.titleMedium,
                )
                DeltaLabel(current = value, previous = previous?.valueFor(headline))
            }
        }
    }
}

@Composable
private fun DeltaLabel(current: Double, previous: Double?) {
    if (previous == null || previous == 0.0) return
    val pct = (current - previous) / previous * 100
    val rounded = pct.roundToInt()
    val (text, color) = when {
        rounded > 0 -> "+$rounded%" to VolumeGreen
        rounded < 0 -> "$rounded%" to RegressionAmber
        else -> "±0%" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Log this exercise in a session to see history here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun displayValue(raw: Double, metric: ProgressMetric, unit: WeightUnit): Double =
    if (metric.isWeight) convertFromKg(raw, unit) else raw

private fun formatMetricValue(raw: Double, metric: ProgressMetric, unit: WeightUnit): String =
    if (metric.isWeight) {
        "${formatWeightNumber(convertFromKg(raw, unit))} ${unit.label()}"
    } else {
        "${raw.roundToInt()} reps"
    }

private fun verdictLabel(trend: TrendResult): String = when (trend.trend) {
    OverloadTrend.Progressing -> {
        val pct = trend.percentChange
        if (pct != null && pct >= 1) "Progressing · up ${pct.roundToInt()}% over ${trend.sessions} sessions"
        else "Progressing"
    }
    OverloadTrend.Regressing -> {
        val pct = trend.percentChange
        if (pct != null && pct <= -1) "Regressing · down ${abs(pct).roundToInt()}% over ${trend.sessions} sessions"
        else "Regressing · stalled or deloading"
    }
    OverloadTrend.Holding -> "Holding steady"
    OverloadTrend.NotEnoughData -> ""
}
