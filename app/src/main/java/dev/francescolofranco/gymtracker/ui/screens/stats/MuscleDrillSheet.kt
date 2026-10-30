package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import dev.francescolofranco.gymtracker.ui.theme.CardShape
import dev.francescolofranco.gymtracker.ui.theme.DestructiveCoral
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EditorialBorder),
            )
        },
    ) {
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
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = volume.muscle.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Average effective sets per week · ${period.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OliveSubtext,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountBlock("Direct", currentDirect, currentDirect - previousDirect, previous != null, Modifier.weight(1f))
                CountBlock("Indirect", currentIndirect, currentIndirect - previousIndirect, previous != null, Modifier.weight(1f))
                CountBlock("Effective", currentTotal, currentTotal - previousTotal, previous != null, Modifier.weight(1f))
            }

            HorizontalDivider(color = EditorialBorder.copy(alpha = 0.25f))
            Text(
                text = "Contributing exercises · avg/week",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = OliveSubtext,
            )
            if (volume.contributingExercises.isEmpty()) {
                Text(
                    text = "No sets logged for this muscle in the selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OliveSubtext,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                volume.contributingExercises.forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(SurfaceBase)
                            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = if (exercise.isPrimary) "Primary" else "Secondary",
                                style = MaterialTheme.typography.labelSmall,
                                color = OliveSubtext,
                            )
                        }
                        Text(
                            text = "${formatSetCount(weeklyAverage(exercise.sets, period))}/wk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CountBlock(
    label: String,
    count: Double,
    delta: Double,
    hasPrevious: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(SurfaceElevated)
            .border(1.dp, EditorialBorder.copy(alpha = 0.25f), CardShape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatSetCount(count),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = "$label /wk",
            style = MaterialTheme.typography.labelSmall,
            color = OliveSubtext,
        )
        if (hasPrevious) {
            val text = when {
                delta > 0 -> "+${formatSetCount(delta)}"
                delta < 0 -> "−${formatSetCount(abs(delta))}"
                else -> "0"
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    delta > 0 -> VolumeGreen
                    delta < 0 -> DestructiveCoral
                    else -> OliveSubtext
                },
            )
        }
    }
}
