package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatTotalVolume
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleDrillSheet(
    volume: MuscleVolume,
    previous: MuscleVolume?,
    unit: WeightUnit,
    period: StatsPeriod,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val color = volumeColor(volume.total, period.days)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = volume.muscle.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountBlock(
                    label = "Direct",
                    count = volume.directSets,
                    delta = (volume.directSets - (previous?.directSets ?: volume.directSets)).setDeltaIfPresent(previous != null),
                )
                CountBlock(
                    label = "Indirect",
                    count = volume.indirectSets,
                    delta = (volume.indirectSets - (previous?.indirectSets ?: volume.indirectSets)).setDeltaIfPresent(previous != null),
                )
                CountBlock(
                    label = "Total",
                    count = volume.total,
                    delta = (volume.total - (previous?.total ?: volume.total)).setDeltaIfPresent(previous != null),
                )
            }

            if (volume.totalVolumeKg > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tonnage · ${period.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTotalVolume(volume.totalVolumeKg, unit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    val deltaKg = volume.totalVolumeKg - (previous?.totalVolumeKg ?: 0.0)
                    if (previous != null && deltaKg.roundToInt() != 0) {
                        SmallDeltaChip(
                            text = formatKgDelta(deltaKg, unit),
                            direction = directionOf(deltaKg),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Contributing exercises · ${period.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (volume.contributingExercises.isEmpty()) {
                Text(
                    text = "No sets logged for this muscle in the selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                volume.contributingExercises.forEach { ce ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = ce.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (ce.isPrimary) "Primary" else "Secondary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${formatSetCount(ce.sets)} effective sets",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CountBlock(label: String, count: Double, delta: SmallDelta?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatSetCount(count),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (delta != null) {
            SmallDeltaChip(text = delta.text, direction = delta.direction, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun SmallDeltaChip(text: String, direction: Direction, modifier: Modifier = Modifier) {
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
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = "$arrow $text",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

private data class SmallDelta(val text: String, val direction: Direction)

private fun Double.setDeltaIfPresent(hasPrevious: Boolean): SmallDelta? {
    if (!hasPrevious) return null
    if (this == 0.0) return SmallDelta("0", Direction.Flat)
    val formatted = formatSetCount(kotlin.math.abs(this))
    val sign = if (this > 0) "+$formatted" else "-$formatted"
    return SmallDelta(sign, directionOf(this))
}

private fun directionOf(delta: Int): Direction = when {
    delta > 0 -> Direction.Up
    delta < 0 -> Direction.Down
    else -> Direction.Flat
}

private fun directionOf(delta: Double): Direction = when {
    delta > 0 -> Direction.Up
    delta < 0 -> Direction.Down
    else -> Direction.Flat
}

private fun formatKgDelta(delta: Double, unit: WeightUnit): String {
    val abs = formatTotalVolume(abs(delta), unit)
    return if (delta > 0) "+$abs" else if (delta < 0) "-$abs" else "0"
}
