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
    /**
     * Persist a draft change. [kgFromExplicitEntry] is true only when the user typed kg in the
     * numpad (vs. just bumping reps on the stepper) — that's the signal the ViewModel uses to
     * decide whether to fire the "first kg" auto-fill across the exercise's other sets. Without
     * this distinction, bumping reps on a fresh exercise would propagate the HINT kg to every
     * pending set, which is not what the user asked for.
     */
    onDraft: (reps: Int?, kg: Double?, kgFromExplicitEntry: Boolean) -> Unit,
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

    // Snapshot the values the numpad was opened against — on dismiss we only fire onDraft
    // when something actually changed and the row is still uncommitted. Logged rows already
    // route through onCommit on each digit, so they don't need the draft pathway.
    var preOpenReps by remember(log.id) { mutableStateOf(reps) }
    var preOpenKg by remember(log.id) { mutableStateOf(kgInternal) }

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

        // Reps delta:
        //  - First-time exercise (no hintReps) → always show "-" so the row reads as
        //    "no reference yet" rather than "no chip at all", matching the kg behaviour.
        //  - Has hint → only show the % after the set is committed; while the user is still
        //    scrubbing the stepper the live delta would just nag.
        val repsDelta = when {
            state.hintReps == null -> percentDeltaOrDash(reps.toDouble(), null)
            isLogged -> percentDeltaOrDash(reps.toDouble(), state.hintReps.toDouble())
            else -> null
        }
        CompactStepper(
            value = reps.toDouble(),
            label = "$reps",
            subLabel = repsDelta?.text,
            subLabelColor = repsDelta?.tone?.color(),
            onValueChange = {
                reps = it.toInt().coerceAtLeast(0)
                if (isLogged) onCommit(reps, kgInternal)
                // Unlogged stepper bumps used to live only in compose state — when the row
                // recycled out of the LazyColumn viewport (or the user left the screen),
                // the change was lost and the chip reset to the hint. Persist as a draft so
                // the value sticks across recomposition and navigation.
                //
                // takeIf mirrors finishNumpad: a zero-with-no-history clears the field, but
                // a non-zero value (or any history) is preserved. kgFromExplicitEntry = false
                // because the user touched reps, not kg.
                else if (editable) onDraft(
                    reps.takeIf { it > 0 || log.reps != null },
                    kgInternal.takeIf { it > 0.0 || log.kg != null },
                    false,
                )
            },
            step = 1.0,
            min = 0.0,
            max = 200.0,
            onChipClick = if (editable) ({ numpadFor = NumpadField.REPS }) else null,
            enabled = editable && !isSkipped,
            modifier = Modifier.weight(1f),
        )

        // Kg: tap-to-numpad chip only (no +/-). Manual digit entry is much faster than
        // stepping 2.5 kg at a time for typical lifting changes. The sub-label is always
        // populated — a dash when there's no previous session to compare against, otherwise
        // ±% vs the matching set so the user can see at a glance whether they're progressing.
        val pctDelta = percentDeltaOrDash(kgInternal, state.hintKg)
        WeightChip(
            label = formatWeightChip(displayKg, state.unit, state.isBodyweight),
            subLabel = pctDelta.text,
            subLabelColor = pctDelta.tone.color(),
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

    fun finishNumpad() {
        // Persist as a draft if anything changed and the row isn't committed yet. Logged rows
        // are already saved per-keystroke through onCommit, so they don't need this.
        // Auto-fill kg across the exercise only when the user actually typed a NEW kg in the
        // numpad (kg branch + value changed from preOpen) — otherwise the hint-derived kg
        // would propagate just because the user opened the numpad and closed it.
        val kgFromExplicit = numpadFor == NumpadField.KG && kgInternal != preOpenKg
        if (!isLogged && (reps != preOpenReps || kgInternal != preOpenKg)) {
            onDraft(
                reps.takeIf { it > 0 || log.reps != null },
                kgInternal.takeIf { it > 0.0 || log.kg != null },
                kgFromExplicit,
            )
        }
        numpadFor = null
    }

    when (numpadFor) {
        NumpadField.REPS -> {
            // Snapshot on first composition of this branch so dismiss can diff.
            LaunchedEffect(numpadFor) {
                preOpenReps = reps
                preOpenKg = kgInternal
            }
            NumpadSheet(
                initialValue = reps.toDouble(),
                allowDecimal = false,
                onValueChange = {
                    reps = it.toInt().coerceAtLeast(0)
                    if (isLogged) onCommit(reps, kgInternal)
                },
                onDismiss = { finishNumpad() },
                label = "Reps · set ${log.setNumber}",
                minValue = 0.0,
                maxValue = 200.0,
            )
        }

        NumpadField.KG -> {
            LaunchedEffect(numpadFor) {
                preOpenReps = reps
                preOpenKg = kgInternal
            }
            NumpadSheet(
                initialValue = displayKg,
                allowDecimal = true,
                onValueChange = { v ->
                    kgInternal = convertToKg(v, state.unit).coerceAtLeast(0.0)
                    if (isLogged) onCommit(reps, kgInternal)
                },
                onDismiss = { finishNumpad() },
                label = "${state.unit.label()} · set ${log.setNumber}",
                minValue = 0.0,
                maxValue = 9999.0,
            )
        }

        null -> Unit
    }
}

@Composable
private fun SetIndex(number: Int, belowRange: Boolean, isLogged: Boolean) {
    // Green = logged is consistent with the body diagram's "in-range" colour and matches the
    // CheckButton below, so a glance down the index column tells the user how many sets are
    // in the bank. Below-range still wins the colour slot since it's the more actionable
    // state ("you fell short of the rep target on that one").
    val bg = when {
        belowRange -> VolumeBlue
        isLogged -> dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val fg = when {
        belowRange -> androidx.compose.ui.graphics.Color.White
        isLogged -> androidx.compose.ui.graphics.Color.White
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
    subLabel: String? = null,
    subLabelColor: androidx.compose.ui.graphics.Color? = null,
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
        // Match the SetIndex circle so "logged" reads as the same green affordance across the row.
        isLogged -> dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = when {
        isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
        isLogged -> androidx.compose.ui.graphics.Color.White
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
    val prefix = if (isBodyweight) "+" else ""
    return "$prefix${formatWeightNumber(value)} ${unit.label()}"
}

private enum class PercentTone { Up, Down, Same }

private data class PercentDelta(val text: String, val tone: PercentTone)

@Composable
private fun PercentTone.color(): androidx.compose.ui.graphics.Color = when (this) {
    // Green = progress, amber = regression. Amber is its own token (not the brand primary, now
    // cyan, nor Material error red) so the up/down deltas keep the green/amber semantics the
    // user asked for without colliding with the primary action colour.
    PercentTone.Up -> dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
    PercentTone.Down -> dev.francescolofranco.gymtracker.ui.theme.RegressionAmber
    PercentTone.Same -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Percentage change vs the matching set from the previous session. Always returns a renderable
 * value so the chip layout doesn't jump as logs come and go:
 *  - "+N%" / "-N%" when the rounded change is non-zero,
 *  - "0%" (neutral tone) when current matches prior,
 *  - "-" (neutral tone) when there's no prior data to compare against. The dash is the
 *    explicit "first-time / no reference" affordance the user asked for.
 */
private fun percentDeltaOrDash(current: Double, hint: Double?): PercentDelta {
    if (hint == null || hint <= 0.0) return PercentDelta("-", PercentTone.Same)
    val pct = ((current - hint) / hint) * 100.0
    val rounded = pct.toInt()
    return when {
        rounded > 0 -> PercentDelta("+$rounded%", PercentTone.Up)
        rounded < 0 -> PercentDelta("$rounded%", PercentTone.Down)
        else -> PercentDelta("0%", PercentTone.Same)
    }
}

private enum class NumpadField { REPS, KG }
