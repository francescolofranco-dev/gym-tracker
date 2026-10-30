package dev.francescolofranco.gymtracker.ui.screens.exercises

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.domain.MuscleCategory
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
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.RegressionAmber
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
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
                            text = (exercise?.name ?: "Exercise").uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.45.sp,
                            ),
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(exercise = ex) }
            item {
                ProgressSection(
                    points = progress,
                    isBodyweight = ex.isBodyweight,
                    unit = unit,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (progress.isNotEmpty()) {
                item {
                    PersonalRecordsCard(
                        summary = personalRecordSummary(progress),
                        isBodyweight = ex.isBodyweight,
                        unit = unit,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HISTORY",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.45.sp,
                        ),
                    )
                    if (historyRows.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(TagShape)
                                .background(SurfaceChip)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${historyRows.size} SESSION${if (historyRows.size == 1) "" else "S"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.66.sp,
                                ),
                                color = BrandLime,
                            )
                        }
                    }
                }
            }
            if (historyRows.isEmpty()) {
                item { EmptyHistory(modifier = Modifier.padding(horizontal = 16.dp)) }
            } else {
                items(items = historyRows, key = { it.first.sessionId }) { (point, previous) ->
                    Column(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = GymMotion.ItemFadeIn,
                                placementSpec = GymMotion.ItemPlacement,
                                fadeOutSpec = GymMotion.ItemFadeOut,
                            )
                            .padding(horizontal = 16.dp),
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
    val category = exercise.primaryMuscles.sortedTopToBottom()
        .firstOrNull()
        ?.let(MuscleCategory::containing)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (category != null) {
                        Box(
                            modifier = Modifier
                                .clip(TagShape)
                                .background(SurfaceChip)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = category.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.55.sp,
                                ),
                                color = BrandLime,
                            )
                        }
                    }
                    val qualifiers = buildList {
                        if (exercise.isBodyweight) add("BODYWEIGHT")
                        if (exercise.isUnilateral) add("UNILATERAL")
                        if (exercise.deletedAt != null) add("DELETED")
                    }
                    if (qualifiers.isNotEmpty()) {
                        Text(
                            text = qualifiers.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.66.sp,
                            ),
                            color = OliveSubtext,
                        )
                    }
                }
                Text(
                    text = exercise.name.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "TARGET DOSING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                    ),
                    color = OliveSubtext,
                )
                Box(
                    modifier = Modifier
                        .clip(TagShape)
                        .background(SurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${exercise.targetSets} × ${exercise.repRangeMin}–${exercise.repRangeMax}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                        ),
                        color = BrandLime,
                    )
                }
            }
        }
        val primaries = exercise.primaryMuscles.sortedTopToBottom().joinToString(" + ") { it.displayName }
        val secondaries = exercise.secondaryMuscles.sortedTopToBottom().joinToString(" • ") { it.displayName }
        Text(
            text = buildString {
                append("Primary: ")
                append(primaries)
                if (secondaries.isNotEmpty()) {
                    append("  •  ")
                    append(secondaries)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = OliveSubtext,
        )
    }
}

@Composable
private fun ProgressSection(
    points: List<SessionProgressPoint>,
    isBodyweight: Boolean,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val metrics = remember(isBodyweight) { metricsFor(isBodyweight) }
    var selectedIndex by rememberSaveable(isBodyweight) { mutableIntStateOf(0) }
    val metric = metrics[selectedIndex.coerceIn(0, metrics.lastIndex)]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CURRENT BENCHMARK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.55.sp,
                    ),
                    color = OliveSubtext,
                )
                Text(
                    text = "PROGRESSION",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.55.sp,
                    ),
                )
            }
            val latest = points.lastOrNull { it.valueFor(metric) != null }?.valueFor(metric)
            if (latest != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val numberPart = if (metric.isWeight) {
                            formatWeightNumber(convertFromKg(latest, unit))
                        } else {
                            latest.roundToInt().toString()
                        }
                        val unitPart = if (metric.isWeight) unit.label().uppercase() else "REPS"
                        Text(
                            text = "$numberPart ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.84).sp,
                            ),
                            color = BrandLime,
                        )
                        Text(
                            text = unitPart,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = OliveSubtext,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Text(
                        text = "RECENT SESSION ${metric.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.66.sp,
                        ),
                        color = OliveSubtext,
                    )
                }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TagShape)
                    .background(SurfaceDeep)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                metrics.forEachIndexed { i, m ->
                    val isSelected = i == selectedIndex
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(TagShape)
                            .background(if (isSelected) SurfaceChip else Color.Transparent)
                            .clickable { selectedIndex = i }
                            .semantics {
                                role = Role.RadioButton
                                selected = isSelected
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(12.dp),
                            )
                        }
                        Text(
                            text = m.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.66.sp,
                            ),
                            color = if (isSelected) BrandLime else OliveSubtext,
                        )
                    }
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
                val firstVal = series.first()
                val lastVal = series.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BASELINE ${formatMetricValue(firstVal, metric, unit).uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.66.sp,
                        ),
                        color = OliveSubtext,
                    )
                    Text(
                        text = "LATEST ${formatMetricValue(lastVal, metric, unit).uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.66.sp,
                        ),
                        color = BrandLime,
                    )
                }
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
        OverloadTrend.Progressing -> BrandLime
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
            .clip(TagShape)
            .background(SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = verdictLabel(trend),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = accent,
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
            .clip(TagShape)
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatSessionDate(point.startedAt),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                if (records.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(TagShape)
                            .background(BrandLime)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = records.joinToString(" · ") { it.shortLabel },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = DarkOnPrimary,
                        )
                    }
                }
            }
            Text(
                text = buildList {
                    add("${point.setsLogged} set${if (point.setsLogged == 1) "" else "s"}")
                    if (isBodyweight) {
                        add("best ${point.bestReps} reps")
                    } else if (point.topSetKg != null && point.topSetReps != null) {
                        add("${formatWeightNumber(convertFromKg(point.topSetKg, unit))} ${unit.label()} × ${point.topSetReps}")
                    }
                }.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = OliveSubtext,
            )
        }
        val value = point.valueFor(headline)
        if (value != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMetricValue(value, headline, unit),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = BrandLime,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "PERSONAL RECORDS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.45.sp,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .clip(TagShape)
                    .background(SurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "ALL-TIME BEST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.66.sp,
                    ),
                    color = BrandLime,
                )
            }
        }
        if (isBodyweight) {
            RecordTile(
                label = "BEST SET",
                number = "${summary.bestReps}",
                unit = "REPS",
                caption = "Most reps in a single set",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                summary.bestE1rmKg?.let { e1rm ->
                    RecordTile(
                        label = "ESTIMATED 1RM",
                        number = formatWeightNumber(convertFromKg(e1rm, unit)),
                        unit = unit.label().uppercase(),
                        caption = "Calculated via Epley formula",
                        modifier = Modifier.weight(1f),
                    )
                }
                RecordTile(
                    label = "SESSION VOLUME",
                    number = formatWeightNumber(convertFromKg(summary.bestVolumeKg, unit)),
                    unit = unit.label().uppercase(),
                    caption = "Best single-session load",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TagShape)
                .background(SurfaceDeep)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MutedOlive,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(14.dp),
            )
            Text(
                "PR badges in history mark sessions that beat an earlier result; the first session establishes the baseline.",
                style = MaterialTheme.typography.bodySmall,
                color = OliveSubtext,
            )
        }
    }
}

@Composable
private fun RecordTile(
    label: String,
    number: String,
    unit: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TagShape)
            .background(SurfaceElevated)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.66.sp,
            ),
            color = OliveSubtext,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.84).sp,
                ),
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = BrandLime,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
            ),
            color = OliveSubtext,
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
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceCard)
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
