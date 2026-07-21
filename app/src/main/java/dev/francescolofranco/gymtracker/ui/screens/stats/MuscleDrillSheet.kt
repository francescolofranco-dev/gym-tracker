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
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleDrillSheet(
    volume: MuscleVolume,
    previous: MuscleVolume?,
    period: StatsPeriod,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentDirect = weeklyAverage(volume.directSets, period)
    val currentIndirect = weeklyAverage(volume.indirectSets, period)
    val currentTotal = weeklyAverage(volume.effectiveSets, period)
    val previousDirect = weeklyAverage(previous?.directSets ?: 0.0, period)
    val previousIndirect = weeklyAverage(previous?.indirectSets ?: 0.0, period)
    val previousTotal = weeklyAverage(previous?.effectiveSets ?: 0.0, period)

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
                        .background(volumeColor(currentTotal)),
                )
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(
                        volume.muscle.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        "Average effective sets per week · ${period.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CountBlock("Direct", currentDirect, currentDirect - previousDirect, previous != null)
                CountBlock("Indirect", currentIndirect, currentIndirect - previousIndirect, previous != null)
                CountBlock("Effective", currentTotal, currentTotal - previousTotal, previous != null)
            }

            HorizontalDivider()
            Text(
                "Contributing exercises · avg/week",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (volume.contributingExercises.isEmpty()) {
                Text(
                    "No sets logged for this muscle in the selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                volume.contributingExercises.forEach { exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (exercise.isPrimary) "Primary" else "Secondary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${formatSetCount(weeklyAverage(exercise.sets, period))}/wk",
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
private fun CountBlock(label: String, count: Double, delta: Double, hasPrevious: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatSetCount(count),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text("$label /wk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hasPrevious) {
            val text = when {
                delta > 0 -> "+${formatSetCount(delta)}"
                delta < 0 -> "−${formatSetCount(abs(delta))}"
                else -> "0"
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    delta > 0 -> MaterialTheme.colorScheme.tertiary
                    delta < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
