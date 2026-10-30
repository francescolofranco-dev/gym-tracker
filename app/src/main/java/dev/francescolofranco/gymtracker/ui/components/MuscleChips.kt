package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated

/**
 * Tap-only muscle picker. Caller controls whether the underlying selection
 * model is single (e.g. primary muscle) or multi (e.g. secondaries) by what
 * it does in [onTap] and what [selected] returns.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleChips(
    selected: (Muscle) -> Boolean,
    onTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (Muscle) -> Boolean = { true },
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Muscle.topToBottom.forEach { muscle ->
            val isSelected = selected(muscle)
            val isEnabled = enabled(muscle)
            FilterChip(
                selected = isSelected,
                onClick = { onTap(muscle) },
                label = {
                    Text(
                        text = muscle.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                    )
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) BrandLime else EditorialBorder.copy(alpha = if (isEnabled) 0.5f else 0.2f),
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceElevated,
                    labelColor = OliveSubtext,
                    selectedContainerColor = BrandLime,
                    selectedLabelColor = DarkOnPrimary,
                    disabledContainerColor = SurfaceDeep,
                    disabledLabelColor = MutedOlive.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
