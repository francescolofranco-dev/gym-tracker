package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.NumberStepper
import dev.francescolofranco.gymtracker.ui.components.NumpadSheet
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import java.util.Locale

data class SetRowState(
    val log: SetLogEntity,
    val targetReps: IntRange,
    val isBodyweight: Boolean,
    val unit: WeightUnit,
    /** Reps to display when log.reps is null (planned). */
    val hintReps: Int?,
    /** kg to display when log.kg is null (planned). */
    val hintKg: Double?,
)

@Composable
fun SetRow(
    state: SetRowState,
    onCommit: (reps: Int, kg: Double) -> Unit,
    onUncommit: () -> Unit,
    onSkipToggle: () -> Unit,
    editable: Boolean = true,
) {
    val log = state.log
    val isLogged = log.loggedAt != null && log.reps != null
    val isSkipped = log.isSkipped

    val initialReps = (log.reps ?: state.hintReps ?: state.targetReps.first).coerceAtLeast(0)
    val initialKgInternal = log.kg ?: state.hintKg ?: 0.0

    var reps by remember(log.id, log.loggedAt, log.reps) { mutableStateOf(initialReps) }
    var kgInternal by remember(log.id, log.loggedAt, log.kg) { mutableStateOf(initialKgInternal) }
    var numpadFor by remember(log.id) { mutableStateOf<NumpadField?>(null) }

    val displayKg = convertFromKg(kgInternal, state.unit)
    val belowRange = isLogged && (log.reps ?: 0) < state.targetReps.first
    val rowAlpha = if (isSkipped) 0.45f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetIndex(number = log.setNumber, belowRange = belowRange)

        StepperBlock(
            label = "Reps",
            modifier = Modifier.weight(1f),
        ) {
            NumberStepper(
                value = reps.toDouble(),
                onValueChange = {
                    reps = it.toInt().coerceAtLeast(0)
                    if (isLogged) onCommit(reps, kgInternal)
                },
                step = 1.0,
                fastStep = 1.0,
                min = 0.0,
                max = 200.0,
                valueLabel = "$reps",
                onChipClick = if (editable) ({ numpadFor = NumpadField.REPS }) else null,
                enabled = editable && !isSkipped,
            )
        }

        StepperBlock(
            label = if (state.isBodyweight) "+${state.unit.label()}" else state.unit.label(),
            modifier = Modifier.weight(1.2f),
        ) {
            NumberStepper(
                value = displayKg,
                onValueChange = { v ->
                    kgInternal = convertToKg(v, state.unit).coerceAtLeast(0.0)
                    if (isLogged) onCommit(reps, kgInternal)
                },
                step = 2.5,
                fastStep = 5.0,
                min = 0.0,
                max = 999.0,
                valueLabel = formatStepperValue(displayKg),
                onChipClick = if (editable) ({ numpadFor = NumpadField.KG }) else null,
                enabled = editable && !isSkipped,
            )
        }

        CheckButton(
            isLogged = isLogged,
            isSkipped = isSkipped,
            editable = editable,
            onTap = {
                if (!editable) return@CheckButton
                if (isLogged) onUncommit() else onCommit(reps, kgInternal)
            },
            onLongPress = { if (editable) onSkipToggle() },
        )
    }

    when (numpadFor) {
        NumpadField.REPS -> NumpadSheet(
            initialValue = reps.toDouble(),
            allowDecimal = false,
            onValueChange = {
                reps = it.toInt().coerceAtLeast(0)
                if (isLogged) onCommit(reps, kgInternal)
            },
            onDismiss = { numpadFor = null },
            label = "Reps · set ${log.setNumber}",
            minValue = 0.0,
            maxValue = 200.0,
        )

        NumpadField.KG -> NumpadSheet(
            initialValue = displayKg,
            allowDecimal = true,
            onValueChange = { v ->
                kgInternal = convertToKg(v, state.unit).coerceAtLeast(0.0)
                if (isLogged) onCommit(reps, kgInternal)
            },
            onDismiss = { numpadFor = null },
            label = "${state.unit.label()} · set ${log.setNumber}",
            minValue = 0.0,
            maxValue = 9999.0,
        )

        null -> Unit
    }
}

@Composable
private fun SetIndex(number: Int, belowRange: Boolean) {
    val bg = if (belowRange) VolumeBlue else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (belowRange) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

@Composable
private fun StepperBlock(
    label: String,
    modifier: Modifier,
    stepper: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        stepper()
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CheckButton(
    isLogged: Boolean,
    isSkipped: Boolean,
    editable: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val bg = when {
        isSkipped -> MaterialTheme.colorScheme.surfaceContainerHighest
        isLogged -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = when {
        isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        isLogged -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val clickModifier = if (editable) {
        Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)
    } else {
        Modifier.clickable(enabled = false, onClick = {})
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (isLogged) "Logged (long-press to skip)" else "Mark logged",
            tint = tint,
        )
    }
}

private fun formatStepperValue(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}"
    else String.format(Locale.US, "%.1f", rounded)
}

private enum class NumpadField { REPS, KG }
