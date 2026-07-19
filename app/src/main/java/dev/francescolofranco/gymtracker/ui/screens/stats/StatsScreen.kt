package dev.francescolofranco.gymtracker.ui.screens.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatDuration
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatTotalVolume
import kotlin.math.roundToInt

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val loadableState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()
    val state = (loadableState as? Loadable.Ready)?.value
    if (state == null) {
        LoadingPane()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WeekModeToggle(
                mode = state.weekMode,
                onChange = viewModel::setWeekMode,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            RegionalRadar(
                volumes = state.muscleVolumes,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            VolumeByMuscleCard(
                volumes = state.muscleVolumes,
                previous = state.previousMuscleVolumes,
                unit = state.unit,
                onMuscleClick = { viewModel.selectMuscle(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            SummaryCards(
                state = state,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }

    selectedMuscle?.let { m ->
        val volume = state.muscleVolumes[m] ?: return@let
        val prev = state.previousMuscleVolumes[m]
        MuscleDrillSheet(
            volume = volume,
            previous = prev,
            unit = state.unit,
            onDismiss = { viewModel.selectMuscle(null) },
        )
    }
}

@Composable
private fun WeekModeToggle(
    mode: WeekMode,
    onChange: (WeekMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == WeekMode.ROLLING_7,
            onClick = { onChange(WeekMode.ROLLING_7) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text("Rolling 7d") }
        SegmentedButton(
            selected = mode == WeekMode.MON_SUN,
            onClick = { onChange(WeekMode.MON_SUN) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text("Mon–Sun") }
    }
}

/**
 * Per-muscle kg tonnage this period — replaces the single pooled "Total volume" number from
 * before with one row per muscle that actually got work, sorted descending. The bar is sized
 * relative to the biggest contributor this week so the visual encodes proportional load.
 * A delta chip shows week-over-week swing for each muscle that has prior data.
 */
@Composable
private fun VolumeByMuscleCard(
    volumes: Map<Muscle, MuscleVolume>,
    previous: Map<Muscle, MuscleVolume>,
    unit: WeightUnit,
    onMuscleClick: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(volumes) {
        volumes.values
            .filter { it.totalVolumeKg > 0 }
            .sortedByDescending { it.totalVolumeKg }
    }
    val maxKg = rows.firstOrNull()?.totalVolumeKg ?: 1.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Volume by muscle (this week)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        if (rows.isEmpty()) {
            Text(
                text = "No tonnage logged yet this week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            rows.forEach { v ->
                MuscleVolumeBar(
                    volume = v,
                    previousKg = previous[v.muscle]?.totalVolumeKg ?: 0.0,
                    maxKg = maxKg,
                    unit = unit,
                    onClick = { onMuscleClick(v.muscle) },
                )
            }
        }
    }
}

@Composable
private fun MuscleVolumeBar(
    volume: MuscleVolume,
    previousKg: Double,
    maxKg: Double,
    unit: WeightUnit,
    onClick: () -> Unit,
) {
    val fraction = (volume.totalVolumeKg / maxKg).coerceIn(0.0, 1.0).toFloat()
    val deltaKg = volume.totalVolumeKg - previousKg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = volume.muscle.displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatTotalVolume(volume.totalVolumeKg, unit),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            if (previousKg > 0 || deltaKg.toInt() != 0) {
                DeltaChip(
                    text = formatKgDelta(deltaKg, unit),
                    direction = direction(deltaKg),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun SummaryCards(state: StatsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(
            title = "Sessions",
            week = "${state.week.sessions}",
            weekDelta = (state.week.sessions - state.previousWeek.sessions).intDeltaOrNull(),
            month = "${state.month.sessions}",
            year = "${state.year.sessions}",
        )
        SummaryCard(
            title = "Total volume",
            week = formatTotalVolume(state.week.volumeKg, state.unit),
            weekDelta = formatKgDeltaChip(state.week.volumeKg - state.previousWeek.volumeKg, state.unit),
            month = formatTotalVolume(state.month.volumeKg, state.unit),
            year = formatTotalVolume(state.year.volumeKg, state.unit),
        )
        SummaryCard(
            title = "Avg session duration",
            week = state.week.avgDuration?.let { formatDuration(it) } ?: "—",
            weekDelta = null,
            month = state.month.avgDuration?.let { formatDuration(it) } ?: "—",
            year = state.year.avgDuration?.let { formatDuration(it) } ?: "—",
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    week: String,
    weekDelta: DeltaInfo?,
    month: String,
    year: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            StatLine(label = "This week", value = week, delta = weekDelta)
            StatLine(label = "This month", value = month, delta = null)
            StatLine(label = "This year", value = year, delta = null)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, delta: DeltaInfo?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        if (delta != null) {
            DeltaChip(
                text = delta.text,
                direction = delta.direction,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Visual chip for a week-over-week delta. */
@Composable
private fun DeltaChip(text: String, direction: Direction, modifier: Modifier = Modifier) {
    val (bg, fg) = when (direction) {
        Direction.Up -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        Direction.Down -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Direction.Flat -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arrow = when (direction) {
        Direction.Up -> "▲"
        Direction.Down -> "▼"
        Direction.Flat -> "→"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "$arrow $text",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

internal enum class Direction { Up, Down, Flat }

internal data class DeltaInfo(val text: String, val direction: Direction)

private fun direction(delta: Double): Direction = when {
    delta > 0 -> Direction.Up
    delta < 0 -> Direction.Down
    else -> Direction.Flat
}

internal fun direction(delta: Int): Direction = when {
    delta > 0 -> Direction.Up
    delta < 0 -> Direction.Down
    else -> Direction.Flat
}

private fun Int.intDeltaOrNull(): DeltaInfo? {
    if (this == 0) return null
    val sign = if (this > 0) "+$this" else "$this"
    return DeltaInfo(sign, direction(this))
}

private fun formatKgDelta(delta: Double, unit: WeightUnit): String {
    val abs = formatTotalVolume(kotlin.math.abs(delta), unit)
    return if (delta > 0) "+$abs" else if (delta < 0) "-$abs" else "0"
}

private fun formatKgDeltaChip(delta: Double, unit: WeightUnit): DeltaInfo? {
    if (delta.roundToInt() == 0) return null
    return DeltaInfo(formatKgDelta(delta, unit), direction(delta))
}
