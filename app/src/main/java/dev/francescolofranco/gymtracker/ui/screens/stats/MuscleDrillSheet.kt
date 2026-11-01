package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
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
        containerColor = SurfaceControl,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceChip),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(volumeColor(currentTotal)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = volume.muscle.displayName.uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            text = "AVERAGE EFFECTIVE SETS PER WEEK · ${period.label.uppercase()}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.66.sp,
                            ),
                            color = OliveSubtext,
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .clickable(
                            role = Role.Button,
                            onClick = onDismiss,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = OliveSubtext,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceElevated)
                    .padding(8.dp),
            ) {
                CountBlock(
                    label = "DIRECT /WK",
                    count = currentDirect,
                    delta = currentDirect - previousDirect,
                    hasPrevious = previous != null,
                    highlightValue = false,
                    modifier = Modifier.weight(1f),
                )
                CountBlock(
                    label = "INDIRECT /WK",
                    count = currentIndirect,
                    delta = currentIndirect - previousIndirect,
                    hasPrevious = previous != null,
                    highlightValue = false,
                    modifier = Modifier.weight(1f),
                )
                CountBlock(
                    label = "EFFECTIVE /WK",
                    count = currentTotal,
                    delta = currentTotal - previousTotal,
                    hasPrevious = previous != null,
                    highlightValue = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "CONTRIBUTING EXERCISES · AVG/WEEK",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                ),
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
                Column {
                    volume.contributingExercises.forEachIndexed { index, exercise ->
                        if (index > 0) {
                            HorizontalDivider(color = SurfaceChip.copy(alpha = 0.4f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                                Text(
                                    text = if (exercise.isPrimary) "PRIMARY" else "SECONDARY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        letterSpacing = 0.66.sp,
                                    ),
                                    color = OliveSubtext,
                                )
                            }
                            Text(
                                text = "${formatSetCount(weeklyAverage(exercise.sets, period))}/wk",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
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
    highlightValue: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
            Text(
                text = formatSetCount(count),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = when {
                    highlightValue -> BrandLime
                    count <= 0.0 -> OliveSubtext
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = OliveSubtext,
            )
            if (hasPrevious) {
                val text = when {
                    delta > 0.05 -> "+${formatSetCount(delta)}"
                    delta < -0.05 -> "−${formatSetCount(abs(delta))}"
                    else -> "0"
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.66.sp,
                    ),
                    color = when {
                        delta > 0.05 -> BrandLime
                        else -> OliveSubtext
                    },
                )
            }
    }
}
