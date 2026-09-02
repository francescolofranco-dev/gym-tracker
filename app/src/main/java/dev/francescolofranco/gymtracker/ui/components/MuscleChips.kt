package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.Muscle

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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Muscle.topToBottom.forEach { muscle ->
            val isSelected = selected(muscle)
            FilterChip(
                selected = isSelected,
                onClick = { onTap(muscle) },
                label = { Text(muscle.displayName) },
                enabled = enabled(muscle),
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}
