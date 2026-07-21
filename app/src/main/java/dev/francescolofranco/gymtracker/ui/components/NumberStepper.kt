package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dev.francescolofranco.gymtracker.ui.motion.GymMotion

/**
 * Big-target stepper. Tap +/- once to step; press-and-hold to scrub fast.
 * Tap the value chip to open a custom numpad (caller wires `onChipClick`).
 *
 * The stepper itself never opens the system keyboard.
 */
@Composable
fun NumberStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double,
    modifier: Modifier = Modifier,
    fastStep: Double = step * 2,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    valueLabel: String = formatValue(value),
    onChipClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    fun update(delta: Double) {
        val next = (value + delta).coerceIn(min, max)
        if (next != value) onValueChange(next)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease",
            enabled = enabled && value > min,
            onTap = { update(-step) },
            onHoldStep = { update(-fastStep) }
        )

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(onChipClick) {
                    if (onChipClick != null) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onChipClick()
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = "Value $valueLabel" },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = valueLabel,
                transitionSpec = {
                    fadeIn(tween(GymMotion.Quick, easing = GymMotion.EmphasizedEasing))
                        .togetherWith(fadeOut(tween(GymMotion.Quick / 2, easing = GymMotion.ExitEasing)))
                },
                label = "stepper value",
            ) { animatedValue ->
                Text(text = animatedValue, style = MaterialTheme.typography.titleLarge)
            }
        }

        StepperButton(
            icon = Icons.Filled.Add,
            contentDescription = "Increase",
            enabled = enabled && value < max,
            onTap = { update(step) },
            onHoldStep = { update(fastStep) }
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStep: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    // pointerInput / LaunchedEffect coroutines outlive recompositions, so they capture the
    // first onTap / onHoldStep lambdas — which close over the original `value` parameter.
    // Without rememberUpdatedState the second tap would compute the same `next` as the first
    // and the `if (next != value)` guard would swallow it (mirrors the SetRow CompactStepper
    // fix in commit dd7d579).
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHoldStep by rememberUpdatedState(onHoldStep)
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "stepper press",
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            // Initial delay before scrubbing kicks in
            delay(400)
            while (isActive && pressed) {
                currentOnHoldStep()
                delay(80)
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
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
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
                    if (up != null) {
                        val elapsed = up.uptimeMillis - downTime
                        if (elapsed < 400) {
                            scope.launch { currentOnTap() }
                        }
                    }
                }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else "%.1f".format(value)
