package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.sortedTopToBottom
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.screens.sessions.convertFromKg
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatSessionDate
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatWeightNumber
import dev.francescolofranco.gymtracker.ui.screens.sessions.label
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.RegressionAmber
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.TagShape
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
    val loadableContent by viewModel.content.collectAsStateWithLifecycle()
    val content = (loadableContent as? Loadable.Ready)?.value
    val exercise = content?.exercise
    var showEdit by remember { mutableStateOf(false) }

    (loadableContent as? Loadable.Error)?.let {
        ErrorPane(it.message, viewModel::retry)
        return
    }

    Scaffold(
        containerColor = SurfaceDeep,
        topBar = {
            Column(modifier = Modifier.background(SurfaceBase)) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBase),
                    title = {
                        Text(
                            text = exercise?.name ?: "Exercise",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    },
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
                HorizontalDivider(color = EditorialBorder.copy(alpha = 0.35f), thickness = 1.dp)
            }
        },
    ) { padding ->
        if (content == null) {
            LoadingPane(modifier = Modifier.padding(padding))
            return@Scaffold
        }

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
                    color = OliveSubtext,
                )
            }
            return@Scaffold
        }
        val progress = content.progress
        val unit = content.unit
        val recordsBySession = remember(progress, ex.isBodyweight) {
            detectPersonalRecords(progress, ex.isBodyweight)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(exercise = ex) }
            item { ProgressSection(points = progress, isBodyweight = ex.isBodyweight, unit = unit) }
            if (progress.isNotEmpty()) {
                item {
                    PersonalRecordsCard(
                        summary = personalRecordSummary(progress),
                        isBodyweight = ex.isBodyweight,
                        unit = unit,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = OliveSubtext,
                    )
                    if (historyRows.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(TagShape)
                                .background(SurfaceElevated)
                                .border(1.dp, EditorialBorder.copy(alpha = 0.25f), TagShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${historyRows.size} SESSION${if (historyRows.size == 1) "" else "S"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OliveSubtext,
                            )
                        }
                    }
                }
            }
            if (historyRows.isEmpty()) {
                item { EmptyHistory() }
            } else {
                items(items = historyRows, key = { it.first.sessionId }) { (point, previous) ->
                    Column(
                        modifier = Modifier.animateItem(
                            fadeInSpec = GymMotion.ItemFadeIn,
                            placementSpec = GymMotion.ItemPlacement,
                            fadeOutSpec = GymMotion.ItemFadeOut,
                        ),
                    ) {
                        HistoryRow(
                            point = point,
                            previous = previous,
                            isBodyweight = ex.isBodyweight,
                            unit = unit,
                            records = recordsBySession[point.sessionId].orEmpty(),
                            onClick = { onOpenSession(point.sessionId) },
                        )
                    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.3f), CardShape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = buildList {
                    add(exercise.primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName })
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        add(exercise.secondaryMuscles.sortedTopToBottom().joinToString(", ") { it.displayName })
                    }
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = OliveSubtext,
            )
            Text(
                text = buildList {
                    add("${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}")
                    if (exercise.isBodyweight) add("Bodyweight")
                    if (exercise.isUnilateral) add("Unilateral · sets per side")
                    if (exercise.deletedAt != null) add("Deleted")
                }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = OliveSubtext,
            )
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceElevated)
                .border(1.dp, EditorialBorder.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "TARGET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = OliveSubtext,
            )
            Text(
                text = "${exercise.targetSets}×${exercise.repRangeMin}–${exercise.repRangeMax}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandLime,
            )
        }
    }
}

@Composable
private fun ProgressSection(
    points: List<SessionProgressPoint>,
    isBodyweight: Boolean,
    unit: WeightUnit,
) {
    val metrics = remember(isBodyweight) { metricsFor(isBodyweight) }
    var selectedIndex by rememberSaveable(isBodyweight) { mutableIntStateOf(0) }
    val metric = metrics[selectedIndex.coerceIn(0, metrics.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.3f), CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            val latest = points.lastOrNull { it.valueFor(metric) != null }?.valueFor(metric)
            if (latest != null) {
                Text(
                    text = formatMetricValue(latest, metric, unit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandLime,
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
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = BrandLime,
                            activeContentColor = DarkOnPrimary,
                            inactiveContainerColor = SurfaceElevated,
                            inactiveContentColor = OliveSubtext,
                            activeBorderColor = BrandLime,
                            inactiveBorderColor = EditorialBorder.copy(alpha = 0.4f),
                        ),
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
                    color = OliveSubtext,
                )
            }
            series.size < 2 -> {
                Text(
                    text = "Log a second session to see the trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OliveSubtext,
                )
            }
            else -> {
                TrendLineChart(
                    values = series.map { displayValue(it, metric, unit) },
                    contentDescription = "${metric.label} trend",
                )
            }
        }
    }
}

@Composable
private fun OverloadVerdict(trend: TrendResult) {
    val accent = when (trend.trend) {
        OverloadTrend.Progressing -> VolumeGreen
        OverloadTrend.Regressing -> RegressionAmber
        else -> OliveSubtext
    }
    val icon: ImageVector = when (trend.trend) {
        OverloadTrend.Progressing -> Icons.AutoMirrored.Filled.TrendingUp
        OverloadTrend.Regressing -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = verdictLabel(trend),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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
    records: Set<PersonalRecordType>,
    onClick: () -> Unit,
) {
    val headline = headlineMetric(isBodyweight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = formatSessionDate(point.startedAt),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            )
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
                color = OliveSubtext,
            )
            if (records.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(TagShape)
                        .background(BrandLime.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = records.joinToString(" · ") { it.shortLabel },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandLime,
                    )
                }
            }
        }
        val value = point.valueFor(headline)
        if (value != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMetricValue(value, headline, unit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                DeltaLabel(current = value, previous = previous?.valueFor(headline))
            }
        }
    }
}

@Composable
private fun PersonalRecordsCard(
    summary: PersonalRecordSummary,
    isBodyweight: Boolean,
    unit: WeightUnit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.3f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Personal records",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Box(
                modifier = Modifier
                    .clip(TagShape)
                    .background(BrandLime.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "ALL-TIME BEST",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = BrandLime,
                )
            }
        }
        if (isBodyweight) {
            RecordLine("Best set", "${summary.bestReps} reps")
        } else {
            summary.bestE1rmKg?.let {
                RecordLine("Estimated 1RM", formatMetricValue(it, ProgressMetric.Est1Rm, unit))
            }
            RecordLine("Session volume", formatMetricValue(summary.bestVolumeKg, ProgressMetric.Volume, unit))
        }
        Text(
            "PR badges in history mark sessions that beat an earlier result; the first session establishes the baseline.",
            style = MaterialTheme.typography.bodySmall,
            color = OliveSubtext,
        )
    }
}

@Composable
private fun RecordLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceElevated)
            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OliveSubtext,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = BrandLime,
        )
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
        else -> "±0%" to OliveSubtext
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = color,
    )
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Log this exercise in a session to see history here.",
            style = MaterialTheme.typography.bodyMedium,
            color = OliveSubtext,
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
