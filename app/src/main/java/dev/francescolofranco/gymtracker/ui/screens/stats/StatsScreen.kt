package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.screens.sessions.convertFromKg
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatWeightNumber
import dev.francescolofranco.gymtracker.ui.screens.sessions.label
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val loadableState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()
    (loadableState as? Loadable.Error)?.let {
        ErrorPane(it.message, viewModel::retry)
        return
    }
    val state = (loadableState as? Loadable.Ready)?.value
    if (state == null) {
        LoadingPane()
        return
    }

    var showExplanation by rememberSaveable { mutableStateOf(false) }
    var showAllMuscles by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "range") {
            HeaderControls(
                period = state.period,
                onPeriodChange = viewModel::setPeriod,
                onExplain = { showExplanation = true },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "kpis") {
            KpiStrip(
                state,
                Modifier.animateItem(
                    fadeInSpec = GymMotion.ItemFadeIn,
                    placementSpec = GymMotion.ItemPlacement,
                    fadeOutSpec = GymMotion.ItemFadeOut,
                ).padding(horizontal = 16.dp),
            )
        }
        item(key = "changes") {
            WhatChangedCard(
                state,
                Modifier.animateItem(
                    fadeInSpec = GymMotion.ItemFadeIn,
                    placementSpec = GymMotion.ItemPlacement,
                    fadeOutSpec = GymMotion.ItemFadeOut,
                ).padding(horizontal = 16.dp),
            )
        }
        item(key = "coverage") {
            MuscleCoverageCard(
                state = state,
                showAll = showAllMuscles,
                onToggleShowAll = { showAllMuscles = !showAllMuscles },
                onMuscleClick = viewModel::selectMuscle,
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "progress") {
            ExerciseProgressCard(
                state,
                Modifier.animateItem(
                    fadeInSpec = GymMotion.ItemFadeIn,
                    placementSpec = GymMotion.ItemPlacement,
                    fadeOutSpec = GymMotion.ItemFadeOut,
                ).padding(horizontal = 16.dp),
            )
        }
    }

    selectedMuscle?.let { muscle ->
        val volume = state.muscleVolumes[muscle] ?: return@let
        MuscleDrillSheet(
            volume = volume,
            previous = state.previousMuscleVolumes[muscle],
            period = state.period,
            onDismiss = { viewModel.selectMuscle(null) },
        )
    }

    if (showExplanation) {
        StatsExplanationDialog(onDismiss = { showExplanation = false })
    }
}

@Composable
private fun HeaderControls(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
    onExplain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PeriodToggle(period, onPeriodChange, Modifier.weight(1f))
        IconButton(onClick = onExplain) {
            Icon(Icons.Outlined.Info, contentDescription = "How statistics work")
        }
    }
}

@Composable
private fun PeriodToggle(
    period: StatsPeriod,
    onChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        StatsPeriod.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = period == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, StatsPeriod.entries.size),
            ) { Text(option.label) }
        }
    }
}

@Composable
private fun KpiStrip(state: StatsUiState, modifier: Modifier = Modifier) {
    val current = state.currentPeriod
    val previous = state.previousPeriod
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Kpi(
            label = "Workouts",
            value = current.sessions.toString(),
            delta = signedNumber(current.sessions - previous.sessions),
            modifier = Modifier.weight(1f),
        )
        KpiDivider()
        Kpi(
            label = "Logged sets",
            value = current.loggedSets.toString(),
            delta = signedNumber(current.loggedSets - previous.loggedSets),
            modifier = Modifier.weight(1f),
        )
        KpiDivider()
        Kpi(
            label = "Training time",
            value = formatTrainingTime(current.trainingTime.seconds),
            delta = signedDuration(current.trainingTime.seconds - previous.trainingTime.seconds),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Kpi(label: String, value: String, delta: SignedValue, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                    slideInVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) { it / 3 })
                    .togetherWith(
                        fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                            slideOutVertically(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) { -it / 3 },
                    )
            },
            label = "stat-$label",
        ) { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChangeLabel(delta.text, delta.direction, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun KpiDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun WhatChangedCard(state: StatsUiState, modifier: Modifier = Modifier) {
    val insights = remember(state) { buildInsights(state) }
    StatsCard(modifier) {
        Text("What changed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        insights.forEach { insight ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = insight.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(28.dp),
                )
                Text(insight.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun buildInsights(state: StatsUiState): List<Insight> {
    if (state.currentPeriod.sessions == 0) {
        return listOf(Insight("—", "No workouts logged in this period."))
    }
    val result = mutableListOf<Insight>()
    if (state.personalRecords.count > 0) {
        val suffix = if (state.personalRecords.count == 1) "record" else "records"
        result += Insight("🏆", "${state.personalRecords.count} personal $suffix")
    }
    state.muscleChanges.filter { it.deltaWeeklySets < 0 }.minByOrNull { it.deltaWeeklySets }?.let {
        result += Insight("↓", "${it.muscle.displayName} received ${abs(it.percentChange ?: 0.0).roundToInt()}% less work")
    }
    state.muscleChanges.filter { it.deltaWeeklySets > 0 }.maxByOrNull { it.deltaWeeklySets }?.let {
        result += Insight("↑", "${it.muscle.displayName} received ${abs(it.percentChange ?: 0.0).roundToInt()}% more work")
    }
    if (result.isEmpty()) result += Insight("→", "Training stayed close to the previous ${state.period.days} days.")
    return result.take(3)
}

@Composable
private fun MuscleCoverageCard(
    state: StatsUiState,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onMuscleClick: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(state.muscleVolumes, state.previousMuscleVolumes, state.period) {
        Muscle.entries.mapNotNull { muscle ->
            val current = state.muscleVolumes[muscle] ?: return@mapNotNull null
            val previous = state.previousMuscleVolumes[muscle]
            val currentWeekly = weeklyAverage(current.effectiveSets, state.period)
            val previousWeekly = weeklyAverage(previous?.effectiveSets ?: 0.0, state.period)
            CoverageRow(current, currentWeekly, previousWeekly)
                .takeIf { currentWeekly > 0.0 || previousWeekly > 0.0 }
        }.sortedByDescending { it.currentWeekly }
    }
    val visibleRows = if (showAll) rows else rows.take(6)

    StatsCard(modifier) {
        Text("Muscle coverage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            "Average effective sets per week · tap for details",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (rows.isEmpty()) {
            Text(
                "No completed sets in this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            visibleRows.forEach { row ->
                WeeklyMuscleRow(row, onClick = { onMuscleClick(row.volume.muscle) })
            }
            if (rows.size > 6) {
                TextButton(onClick = onToggleShowAll, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (showAll) "Show less" else "Show all ${rows.size}")
                }
            }
        }
    }
}

@Composable
private fun WeeklyMuscleRow(row: CoverageRow, onClick: () -> Unit) {
    val delta = row.currentWeekly - row.previousWeekly
    val fraction = (row.currentWeekly / Muscle.WEEKLY_MAX).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
        label = "muscle coverage",
    )
    val color = volumeColor(row.currentWeekly)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${row.volume.muscle.displayName}, ${formatSetCount(row.currentWeekly)} effective sets per week"
            }
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.volume.muscle.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${formatSetCount(row.currentWeekly)}/wk",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            if (abs(delta) >= 0.05) {
                ChangeLabel(formatSignedSets(delta), direction(delta), Modifier.padding(start = 8.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun ExerciseProgressCard(state: StatsUiState, modifier: Modifier = Modifier) {
    StatsCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Exercise progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            if (state.personalRecords.count > 0) {
                Text(
                    "${state.personalRecords.count} PR${if (state.personalRecords.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            "Best performance vs the previous ${state.period.days} days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.exerciseProgress.isEmpty()) {
            Text(
                "No comparable exercise performances yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            state.exerciseProgress.take(5).forEach { signal ->
                ProgressRow(
                    signal = signal,
                    unit = state.unit,
                    hasPr = signal.exerciseId in state.personalRecords.exerciseIds,
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(signal: ExerciseProgressSignal, unit: WeightUnit, hasPr: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(signal.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (signal.isBodyweight) "Best set" else "Estimated 1RM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (signal.isBodyweight) {
                "${signal.currentValue.roundToInt()} reps"
            } else {
                "${formatWeightNumber(convertFromKg(signal.currentValue, unit))} ${unit.label()}"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        if (hasPr) {
            Text(
                "PR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        } else if (signal.percentChange != null) {
            ChangeLabel(
                text = formatPercent(signal.percentChange),
                direction = direction(signal.percentChange),
                modifier = Modifier.padding(start = 8.dp),
            )
        } else {
            Text(
                "New",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun StatsExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dev.francescolofranco.gymtracker.ui.theme.DialogShape,
        title = { Text("How statistics work") },
        text = {
            Text(
                "The selected range is compared with the immediately preceding range of the same length. " +
                    "Logged sets counts every completed row, including left and right rows separately. " +
                    "Muscle coverage uses effective sets: a primary muscle gets 1, a secondary muscle 0.5, " +
                    "and each unilateral side 0.5. Coverage is normalized to an average week so 7d, 28d, " +
                    "and 90d remain comparable. Exercise progress compares estimated 1RM for loaded movements " +
                    "and best-set reps for bodyweight movements. Estimated 1RM uses the Epley formula.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ChangeLabel(text: String, direction: Direction, modifier: Modifier = Modifier) {
    val color = when (direction) {
        Direction.Up -> MaterialTheme.colorScheme.tertiary
        Direction.Down -> MaterialTheme.colorScheme.error
        Direction.Flat -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arrow = when (direction) {
        Direction.Up -> "▲"
        Direction.Down -> "▼"
        Direction.Flat -> "→"
    }
    Text("$arrow $text", style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier, maxLines = 1)
}

private data class Insight(val symbol: String, val text: String)
private data class CoverageRow(val volume: MuscleVolume, val currentWeekly: Double, val previousWeekly: Double)
private data class SignedValue(val text: String, val direction: Direction)
private enum class Direction { Up, Down, Flat }

private fun direction(value: Int): Direction = direction(value.toDouble())
private fun direction(value: Long): Direction = direction(value.toDouble())
private fun direction(value: Double): Direction = when {
    value > 0 -> Direction.Up
    value < 0 -> Direction.Down
    else -> Direction.Flat
}

private fun signedNumber(value: Int): SignedValue = SignedValue(
    text = when {
        value > 0 -> "+$value"
        value < 0 -> value.toString()
        else -> "0"
    },
    direction = direction(value),
)

private fun signedDuration(seconds: Long): SignedValue = SignedValue(
    text = when {
        seconds > 0 -> "+${formatTrainingTime(seconds)}"
        seconds < 0 -> "−${formatTrainingTime(abs(seconds))}"
        else -> "0m"
    },
    direction = direction(seconds),
)

private fun formatTrainingTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatSignedSets(value: Double): String =
    if (value > 0) "+${formatSetCount(value)}" else "−${formatSetCount(abs(value))}"

private fun formatPercent(value: Double): String {
    val rounded = abs(value).roundToInt()
    return if (value > 0) "+$rounded%" else if (value < 0) "−$rounded%" else "0%"
}

internal fun formatSetCount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
