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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep

internal val SecondarySelectedFill = BrandLime.copy(alpha = 0.20f)
internal val SecondarySelectedBorderColor = BrandLime
internal val SecondarySelectedBorderWidth = 1.5.dp

internal data class MuscleChipSelectedSpec(
    val containerColor: Color,
    val labelColor: Color,
    val border: BorderStroke?,
)

internal fun muscleChipSelectedSpec(
    isSecondary: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
): MuscleChipSelectedSpec = if (isSecondary) {
    MuscleChipSelectedSpec(
        containerColor = SecondarySelectedFill,
        labelColor = BrandLime,
        border = if (isSelected && isEnabled) {
            BorderStroke(SecondarySelectedBorderWidth, SecondarySelectedBorderColor)
        } else {
            null
        },
    )
} else {
    MuscleChipSelectedSpec(
        containerColor = BrandLime,
        labelColor = DarkOnPrimary,
        border = null,
    )
}

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
    isSecondary: Boolean = false,
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
            val selectedSpec = muscleChipSelectedSpec(
                isSecondary = isSecondary,
                isSelected = isSelected,
                isEnabled = isEnabled,
            )
            FilterChip(
                selected = isSelected,
                onClick = { onTap(muscle) },
                label = {
                    Text(
                        text = muscle.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(4.dp),
                border = selectedSpec.border,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceChip,
                    labelColor = DarkOnSurface,
                    selectedContainerColor = selectedSpec.containerColor,
                    selectedLabelColor = selectedSpec.labelColor,
                    disabledContainerColor = SurfaceDeep,
                    disabledSelectedContainerColor = SurfaceDeep,
                    disabledLabelColor = MutedOlive.copy(alpha = 0.5f),
                ),
            )
        }
    }
}
