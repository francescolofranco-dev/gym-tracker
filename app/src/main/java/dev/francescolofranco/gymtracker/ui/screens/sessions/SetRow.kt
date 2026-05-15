package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.NumpadSheet
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
            .height(56.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetIndex(number = log.setNumber, belowRange = belowRange, isLogged = isLogged)

        CompactStepper(
            value = reps.toDouble(),
            label = "$reps",
            onValueChange = {
                reps = it.toInt().coerceAtLeast(0)
                if (isLogged) onCommit(reps, kgInternal)
            },
            step = 1.0,
            min = 0.0,
            max = 200.0,
            onChipClick = if (editable) ({ numpadFor = NumpadField.REPS }) else null,
            enabled = editable && !isSkipped,
            modifier = Modifier.weight(1f),
        )

        // Kg: tap-to-numpad chip only (no +/-). Manual digit entry is much faster than
        // stepping 2.5 kg at a time for typical lifting changes. A small percentage sub-label
        // shows progress vs the last session's matching set when there's a hint to compare to.
        val pctDelta = computePercentDelta(kgInternal, state.hintKg)
        WeightChip(
            label = formatWeightChip(displayKg, state.unit, state.isBodyweight),
            subLabel = pctDelta?.text,
            subLabelColor = pctDelta?.let {
                if (it.positive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            },
            onClick = if (editable) ({ numpadFor = NumpadField.KG }) else null,
            enabled = editable && !isSkipped,
            modifier = Modifier.weight(1.2f),
        )

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
private fun SetIndex(number: Int, belowRange: Boolean, isLogged: Boolean) {
    val bg = when {
        belowRange -> VolumeBlue
        isLogged -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val fg = when {
        belowRange -> MaterialTheme.colorScheme.onPrimary
        isLogged -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = fg,
        )
    }
}

/**
 * Compact stepper used inside a set row: 36dp -/+ buttons, chip with no min width so the whole
 * row fits inside a phone column. Tap +/- to step, long-press to scrub fast, tap the chip to
 * open the numpad. Vertical center-aligned at row height 48dp.
 */
@Composable
private fun CompactStepper(
    value: Double,
    label: String,
    onValueChange: (Double) -> Unit,
    step: Double,
    fastStep: Double = step * 2,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    onChipClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    fun update(delta: Double) {
        val next = (value + delta).coerceIn(min, max)
        if (next != value) onValueChange(next)
    }
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperKey(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease",
            enabled = enabled && value > min,
            onTap = { update(-step) },
            onHoldStep = { update(-fastStep) },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(onChipClick, enabled) {
                    if (onChipClick == null || !enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onChipClick()
                        }
                    }
                }
                .semantics { contentDescription = "Value $label" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
            )
        }

        StepperKey(
            icon = Icons.Filled.Add,
            contentDescription = "Increase",
            enabled = enabled && value < max,
            onTap = { update(step) },
            onHoldStep = { update(fastStep) },
        )
    }
}

/**
 * Read-and-tap chip for kg / lbs — no +/- buttons, just shows the value and opens the numpad
 * on tap. Matches CompactStepper's height/styling so the row stays aligned.
 */
@Composable
private fun WeightChip(
    label: String,
    subLabel: String?,
    subLabelColor: androidx.compose.ui.graphics.Color?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(onClick, enabled) {
                if (onClick == null || !enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onClick()
                    }
                }
            }
            .semantics { contentDescription = "Weight $label, tap to edit" },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = subLabelColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StepperKey(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStep: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    // The pointerInput / LaunchedEffect coroutines are long-lived — they capture onTap and
    // onHoldStep at first launch. Without these "latest" references, the second tap would
    // call the original closure that still sees the original `value`, producing the same
    // result as the first tap (and looking like the buttons stopped working). Routing every
    // call through the current refs makes each invocation observe the live state.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHoldStep by rememberUpdatedState(onHoldStep)

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(400)
            while (isActive && pressed) {
                currentOnHoldStep()
                delay(80)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    val downTime = down.uptimeMillis
                    pressed = true
                    val up = waitForUpOrCancellation()
                    pressed = false
                    if (up != null && (up.uptimeMillis - downTime) < 400) {
                        scope.launch { currentOnTap() }
                    }
                }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        isLogged -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = when {
        isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        isLogged -> MaterialTheme.colorScheme.onPrimary
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
            .clip(RoundedCornerShape(14.dp))
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

private fun formatWeightChip(value: Double, unit: WeightUnit, isBodyweight: Boolean): String {
    val rounded = (value * 10).toInt() / 10.0
    val number = if (rounded % 1.0 == 0.0) "${rounded.toInt()}"
    else String.format(Locale.US, "%.1f", rounded)
    val prefix = if (isBodyweight) "+" else ""
    return "$prefix$number ${unit.label()}"
}

private data class PercentDelta(val text: String, val positive: Boolean)

/**
 * Percentage change vs the matching set from the last session. Returned only when there's a
 * non-trivial hint (>0) and the rounded change is at least 1% — otherwise the sub-label adds
 * noise without signal.
 */
private fun computePercentDelta(currentKg: Double, hintKg: Double?): PercentDelta? {
    if (hintKg == null || hintKg <= 0.0) return null
    val pct = ((currentKg - hintKg) / hintKg) * 100.0
    val rounded = pct.toInt()
    if (rounded == 0) return null
    val sign = if (rounded > 0) "+" else ""
    return PercentDelta(text = "$sign$rounded%", positive = rounded > 0)
}

private enum class NumpadField { REPS, KG }
