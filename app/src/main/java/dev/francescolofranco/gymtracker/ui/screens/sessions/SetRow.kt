package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.NumpadSheet
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.RegressionAmber
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Shared column geometry so the header labels and every set row line up into a strict grid.
private val ColIndexWidth = 40.dp
private val ColCheckWidth = 52.dp
private const val ColWeightWeight = 1f
private const val ColRepsWeight = 1.3f

data class SetRowState(
    val log: SetLogEntity,
    val targetReps: IntRange,
    val isBodyweight: Boolean,
    val unit: WeightUnit,
    /** Reps to display when log.reps is null (planned). */
    val hintReps: Int?,
    /** kg to display when log.kg is null (planned). */
    val hintKg: Double?,
    /** True when this is the current set to log (first un-logged, non-skipped). */
    val isActive: Boolean = false,
)

/** Column headers (# / WEIGHT / REPS) drawn once above the set rows; matches the row grid. */
@Composable
fun SetTableHeader() {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", style = style, color = color, modifier = Modifier.width(ColIndexWidth))
        Text(
            "WEIGHT",
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(ColWeightWeight),
        )
        Text(
            "REPS",
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(ColRepsWeight),
        )
        Spacer(Modifier.width(ColCheckWidth))
    }
}

@Composable
fun SetRow(
    state: SetRowState,
    onCommit: (reps: Int, kg: Double) -> Unit,
    onUncommit: () -> Unit,
    /**
     * Persist a draft change.
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

    // hintReps/hintKg are keyed here too: they arrive asynchronously (DB round trip), and a row
    // that first composes before the hint loads must re-derive its initial value once it does.
    // Safe for rows with an existing draft — log.reps/log.kg take precedence over the hint in the
    // initializer above, so an in-progress edit is never overwritten by a later hint update.
    var reps by remember(log.id, log.loggedAt, log.reps, state.hintReps) { mutableIntStateOf(initialReps) }
    var kgInternal by remember(log.id, log.loggedAt, log.kg, state.hintKg) { mutableDoubleStateOf(initialKgInternal) }
    var numpadFor by remember(log.id) { mutableStateOf<NumpadField?>(null) }

    // Snapshot the values the numpad was opened against — on dismiss we only fire onDraft
    // when something actually changed and the row is still uncommitted. Logged rows already
    // route through onCommit on each digit, so they don't need the draft pathway.
    var preOpenReps by remember(log.id) { mutableIntStateOf(reps) }
    var preOpenKg by remember(log.id) { mutableDoubleStateOf(kgInternal) }

    val displayKg = convertFromKg(kgInternal, state.unit)
    val belowRange = isLogged && log.reps < state.targetReps.first
    val rowAlpha = if (isSkipped) 0.45f else 1f
    val isActiveSet = state.isActive && !isLogged && !isSkipped

    // Deltas vs the matching set last session. Weight always renders ("-" when no reference);
    // reps only renders the % once committed (while scrubbing an un-logged set the live % nags).
    val kgDelta = percentDeltaOrDash(kgInternal, state.hintKg)
    val kgDeltaColor = kgDelta.tone.color()
    val repsDelta = when {
        state.hintReps == null -> percentDeltaOrDash(reps.toDouble(), null)
        isLogged -> percentDeltaOrDash(reps.toDouble(), state.hintReps.toDouble())
        else -> null
    }
    val repsDeltaColor = (repsDelta?.tone ?: PercentTone.Same).color()

    val indexColor = when {
        isActiveSet -> MaterialTheme.colorScheme.primary
        belowRange -> VolumeBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val animatedRowAlpha by animateFloatAsState(
        targetValue = rowAlpha,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "set row alpha",
    )
    val animatedIndexColor by animateColorAsState(
        targetValue = indexColor,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "active set",
    )

    fun bumpReps(delta: Int) {
        val next = (reps + delta).coerceIn(0, 200)
        if (next == reps) return
        reps = next
        if (isLogged) {
            onCommit(reps, kgInternal)
        } else if (editable) {
            // Persist a stepper bump as a draft so it survives recomposition / navigation.
            onDraft(
                reps.takeIf { it > 0 || log.reps != null },
                kgInternal.takeIf { it > 0.0 || log.kg != null },
                false,
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(animatedRowAlpha)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // # column
        Cell(modifier = Modifier.width(ColIndexWidth), captionText = null, horizontalAlignment = Alignment.Start) {
            Text(
                text = if (log.side.shortLabel.isBlank()) "${log.setNumber}" else "${log.setNumber} ${log.side.shortLabel}",
                style = MaterialTheme.typography.titleMedium.asNumber(),
                color = animatedIndexColor,
            )
        }

        // WEIGHT column — big value + small unit, tap to open the kg numpad.
        Cell(
            modifier = Modifier.weight(ColWeightWeight),
            captionText = kgDelta.text,
            captionColor = kgDeltaColor,
        ) {
            WeightValue(
                number = formatWeightChip(displayKg, state.isBodyweight),
                unit = state.unit.label(),
                onClick = if (editable) ({ numpadFor = NumpadField.KG }) else null,
                enabled = editable && !isSkipped,
            )
        }

        // REPS column — ghost - / + keys around the tappable value.
        Cell(
            modifier = Modifier.weight(ColRepsWeight),
            captionText = repsDelta?.text,
            captionColor = repsDeltaColor,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            ) {
                GhostStepper(
                    icon = Icons.Filled.Remove,
                    contentDescription = "Decrease reps",
                    enabled = editable && !isSkipped && reps > 0,
                    onTap = { bumpReps(-1) },
                    onHoldStep = { bumpReps(-2) },
                )
                RepsValue(
                    reps = reps,
                    onTap = if (editable) ({ numpadFor = NumpadField.REPS }) else null,
                    enabled = editable && !isSkipped,
                )
                GhostStepper(
                    icon = Icons.Filled.Add,
                    contentDescription = "Increase reps",
                    enabled = editable && !isSkipped && reps < 200,
                    onTap = { bumpReps(1) },
                    onHoldStep = { bumpReps(2) },
                )
            }
        }

        // Check column
        Cell(modifier = Modifier.width(ColCheckWidth), captionText = null) {
            CheckSquare(
                isLogged = isLogged,
                isSkipped = isSkipped,
                editable = editable,
                onTap = {
                    if (!editable) return@CheckSquare
                    if (isLogged) onUncommit() else onCommit(reps, kgInternal)
                },
                onLongPress = { if (editable) onSkipToggle() },
            )
        }
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

/**
 * One grid cell: the [content] sits on a shared top line (so values, ghost keys and the check
 * align across columns) with an optional ±% [captionText] on a baseline below. The caption slot
 * is always reserved so every cell is the same height.
 */
@Composable
private fun Cell(
    modifier: Modifier,
    captionText: String?,
    captionColor: Color = Color.Unspecified,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.Center) {
            content()
        }
        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (captionText != null) {
                Text(
                    text = captionText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    color = captionColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Big weight value with a small unit suffix ("20 kg"); tap to open the kg numpad. */
@Composable
private fun WeightValue(
    number: String,
    unit: String,
    onClick: (() -> Unit)?,
    enabled: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
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
            .semantics {
                contentDescription = "Weight $number $unit"
                if (enabled && onClick != null) {
                    role = Role.Button
                    this.onClick("Edit weight") { onClick.invoke(); true }
                }
            },
    ) {
        AnimatedNumber(number)
        Spacer(Modifier.width(2.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** The reps value — a big tappable numeral that opens the reps numpad. */
@Composable
private fun RepsValue(reps: Int, onTap: (() -> Unit)?, enabled: Boolean) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(34.dp)
            .pointerInput(onTap, enabled) {
                if (onTap == null || !enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onTap()
                    }
                }
            }
            .semantics {
                contentDescription = "Reps $reps"
                if (enabled && onTap != null) {
                    role = Role.Button
                    this.onClick("Edit reps") { onTap.invoke(); true }
                }
            },
    ) {
        AnimatedNumber(reps.toString())
    }
}

@Composable
private fun AnimatedNumber(value: String) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            fadeIn(tween(GymMotion.Quick, easing = GymMotion.EmphasizedEasing))
                .togetherWith(fadeOut(tween(GymMotion.Quick / 2, easing = GymMotion.ExitEasing)))
        },
        label = "number change",
    ) { animatedValue ->
        Text(
            text = animatedValue,
            style = MaterialTheme.typography.titleLarge.asNumber(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Flat "ghost" +/- key: just the cyan glyph, no filled background, on a 40dp touch target. Tap to
 * step once, press-and-hold to scrub.
 */
@Composable
private fun GhostStepper(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStep: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    // Long-lived coroutines capture the first lambdas; route through "latest" refs so every call
    // observes live state (otherwise repeat taps recompute the same value and look stuck).
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHoldStep by rememberUpdatedState(onHoldStep)
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "stepper press",
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(400.milliseconds)
            while (isActive && pressed) {
                currentOnHoldStep()
                delay(80.milliseconds)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
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
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Tap-to-toggle check, a rounded square. Logged sets fill solid green with a white tick;
 * un-logged sets are a muted square. Long-press toggles the skipped state.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CheckSquare(
    isLogged: Boolean,
    isSkipped: Boolean,
    editable: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val bg = when {
        isSkipped -> MaterialTheme.colorScheme.surfaceContainerHighest
        isLogged -> VolumeGreen
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (isLogged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val animatedBg by animateColorAsState(
        targetValue = bg,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "set status background",
    )
    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
        label = "set status icon",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isLogged) 1f else 0.78f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "set check",
    )
    val clickModifier = if (editable) {
        Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)
    } else {
        Modifier.clickable(enabled = false, onClick = {})
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .then(clickModifier)
            .semantics {
                role = Role.Checkbox
                contentDescription = "Set status"
                stateDescription = when {
                    isSkipped -> "Skipped"
                    isLogged -> "Logged"
                    else -> "Not logged"
                }
                if (editable) {
                    customActions = listOf(
                        CustomAccessibilityAction(if (isSkipped) "Unskip set" else "Skip set") {
                            onLongPress()
                            true
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = animatedTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
        )
    }
}

/**
 * Tight, font-padding-free style for the big numerals so they sit on the true vertical centre of
 * their cell and line up with the ghost keys. Compose's default includeFontPadding adds space
 * above the glyph, which leaves numbers floating high.
 */
private fun TextStyle.asNumber(): TextStyle = copy(
    fontWeight = FontWeight.SemiBold,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

private fun formatWeightChip(value: Double, isBodyweight: Boolean): String {
    val prefix = if (isBodyweight) "+" else ""
    return "$prefix${formatWeightNumber(value)}"
}

private enum class PercentTone { Up, Down, Same }

private data class PercentDelta(val text: String, val tone: PercentTone)

@Composable
private fun PercentTone.color(): Color = when (this) {
    // Green = progress, amber = regression. Amber is its own token (not the brand primary, now
    // cyan, nor Material error red) so the up/down deltas keep the green/amber semantics without
    // colliding with the primary action colour.
    PercentTone.Up -> VolumeGreen
    PercentTone.Down -> RegressionAmber
    PercentTone.Same -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Percentage change vs the matching set from the previous session. Always returns a renderable
 * value so the caption layout doesn't jump as logs come and go:
 *  - "+N%" / "-N%" when the rounded change is non-zero,
 *  - "0%" (neutral tone) when current matches prior,
 *  - "-" (neutral tone) when there's no prior data to compare against.
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
