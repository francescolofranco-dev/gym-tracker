package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.MuscleCategory
import dev.francescolofranco.gymtracker.ui.components.GymIcons
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.OswaldFontFamily
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import kotlin.math.abs

@Composable
internal fun MuscleCoverageCard(
    state: StatsUiState,
    onMuscleClick: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverage = remember(state.muscleVolumes, state.previousMuscleVolumes, state.period) {
        Muscle.topToBottom.associateWith { muscle ->
            WeeklyCoverage(
                current = weeklyAverage(state.muscleVolumes[muscle]?.effectiveSets ?: 0.0, state.period),
                previous = weeklyAverage(state.previousMuscleVolumes[muscle]?.effectiveSets ?: 0.0, state.period),
            )
        }
    }
    val counts = MuscleCategory.entries.associateWith { category ->
        category.muscles.count { coverage.getValue(it).current > 0.0 }
    }
    val trained = counts.values.sum()
    val peak = coverage.values.maxOf { maxOf(it.current, it.previous) }.coerceAtLeast(1.0)
    var selectedCategory by rememberSaveable { mutableStateOf<MuscleCategory?>(null) }
    val category = selectedCategory ?: counts.maxBy { it.value }.key

    StatsCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "MUSCLE COVERAGE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = if (trained == 0) "No muscle work logged this period"
                else "$trained of ${coverage.size} muscles worked",
                style = MaterialTheme.typography.bodyMedium,
                color = OliveSubtext,
            )
        }
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MuscleCategory.entries.chunked(2).forEach { categories ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { option ->
                        CoverageGroupTile(
                            category = option,
                            trained = counts.getValue(option),
                            selected = category == option,
                            onClick = { selectedCategory = option },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDeep)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Effective sets per week",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkOnSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CoverageLegend("Current", BrandLime)
                    CoverageLegend("Previous", OliveSubtext)
                }
            }
            coverage.keys.filter { it in category.muscles }.forEach { muscle ->
                key(muscle) {
                    CoverageMuscleRow(muscle, coverage.getValue(muscle), peak, onClick = { onMuscleClick(muscle) })
                }
            }
        }
    }
}

@Composable
private fun CoverageGroupTile(
    category: MuscleCategory,
    trained: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = category.muscles.size
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) BrandLime.copy(alpha = 0.10f) else SurfaceElevated)
            .border(1.dp, if (selected) BrandLime.copy(alpha = 0.65f) else SurfaceChip, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = "${category.displayName}, $trained of $total muscles worked" }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) BrandLime else DarkOnSurface,
            )
            Text(
                text = "$trained/$total",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (selected) BrandLime else OliveSubtext,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(total) { index ->
                Box(
                    Modifier.weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < trained) BrandLime else SurfaceChip),
                )
            }
        }
    }
}

@Composable
private fun CoverageLegend(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(12.dp).height(4.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = OliveSubtext)
    }
}

@Composable
private fun CoverageMuscleRow(
    muscle: Muscle,
    volume: WeeklyCoverage,
    peak: Double,
    onClick: () -> Unit,
) {
    val current = formatCoverageSets(volume.current)
    val previous = formatCoverageSets(volume.previous)
    val delta = volume.current - volume.previous
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClickLabel = "View contributing exercises", onClick = onClick)
            .semantics {
                contentDescription = "${muscle.displayName}, $current current and $previous previous effective sets per week"
            }
            .heightIn(min = 52.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = muscle.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DarkOnSurface,
                )
                Text(
                    text = current,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = OswaldFontFamily),
                    color = if (volume.current > 0.0) DarkOnSurface else OliveSubtext,
                )
                if (abs(delta) >= 0.05) {
                    val sign = if (delta > 0.0) "+" else "−"
                    Text(
                        text = "$sign${formatSetCount(abs(delta))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta > 0.0) BrandLime else OliveSubtext,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CoverageBar(volume.current / peak, BrandLime)
                CoverageBar(volume.previous / peak, OliveSubtext)
            }
        }
        Icon(
            imageVector = GymIcons.ChevronForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = OliveSubtext,
        )
    }
}

@Composable
private fun CoverageBar(fraction: Double, color: Color) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier.fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(SurfaceChip),
    ) {
        Box(Modifier.fillMaxWidth(fraction.toFloat()).height(4.dp).background(color, shape))
    }
}

private data class WeeklyCoverage(val current: Double, val previous: Double)

private fun formatCoverageSets(value: Double): String =
    if (value > 0.0 && value < 0.05) "<0.1" else formatSetCount(value)
