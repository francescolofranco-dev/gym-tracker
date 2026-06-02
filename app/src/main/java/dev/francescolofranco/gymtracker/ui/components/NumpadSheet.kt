package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen

/**
 * Custom on-screen numpad. Updates the value live as digits are entered;
 * dismissing the sheet (swipe / tap outside) commits whatever's in the buffer.
 * No "OK" / "Done" affordance — tap outside to dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumpadSheet(
    initialValue: Double,
    allowDecimal: Boolean,
    onValueChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    label: String? = null,
    minValue: Double = 0.0,
    maxValue: Double = 9999.0
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Buffer mirrors the decimal/integer input state. Coerce on commit.
    var buffer by remember { mutableStateOf(initialBuffer(initialValue, allowDecimal)) }
    // Pristine flag: the buffer was seeded from the existing value, the user hasn't typed yet.
    // First digit press wipes the seed instead of appending — saves the "tap backspace 3 times
    // before typing the new weight" dance the user complained about.
    var pristine by remember { mutableStateOf(true) }

    LaunchedEffect(buffer) {
        bufferToDouble(buffer)?.let { v ->
            onValueChange(v.coerceIn(minValue, maxValue))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: label on the left, prominent ✓ on the right. Tap-outside still
            // dismisses, but a deliberate confirm button means the user doesn't have to swipe
            // or tap empty space — the cursor flow goes type → tap ✓ → done.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VolumeGreen)
                        .clickable { onDismiss() }
                        .semantics { contentDescription = "Confirm value" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
            Text(
                text = buffer.ifEmpty { "0" },
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
            )

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (allowDecimal) "." else "", "0", BACKSPACE_TOKEN)
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { token ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.6f)
                        ) {
                            if (token.isNotEmpty()) {
                                NumpadKey(
                                    token = token,
                                    onTap = { tapped ->
                                        // First non-backspace press while pristine wipes the seed
                                        // so the user can just start typing the new value.
                                        // Backspace stays "edit existing" so the user can correct
                                        // a typo in the seed if they want.
                                        val seed = if (pristine && tapped != BACKSPACE_TOKEN) "" else buffer
                                        buffer = applyToken(seed, tapped, allowDecimal)
                                        pristine = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(token: String, onTap: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onTap(token)
            },
        contentAlignment = Alignment.Center
    ) {
        if (token == BACKSPACE_TOKEN) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace"
            )
        } else {
            Text(
                text = token,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

private const val BACKSPACE_TOKEN = "<x"

private fun applyToken(buffer: String, token: String, allowDecimal: Boolean): String {
    return when (token) {
        BACKSPACE_TOKEN -> buffer.dropLast(1)
        "." -> if (!allowDecimal || buffer.contains(".")) buffer
        else if (buffer.isEmpty()) "0." else "$buffer."
        else -> {
            val candidate = buffer + token
            // Cap fractional precision at 3 (user wants "up to 3 decimal precision"),
            // and integer side at 4 so we still fit "9999" or "9999.999".
            val dotIdx = candidate.indexOf('.')
            val intLen = if (dotIdx >= 0) dotIdx else candidate.length
            val fracLen = if (dotIdx >= 0) candidate.length - dotIdx - 1 else 0
            if (intLen > MAX_INTEGER_DIGITS || fracLen > MAX_FRACTION_DIGITS) buffer else candidate
        }
    }
}

private const val MAX_INTEGER_DIGITS = 4
private const val MAX_FRACTION_DIGITS = 3

private fun bufferToDouble(buffer: String): Double? {
    if (buffer.isEmpty() || buffer == ".") return 0.0
    return buffer.toDoubleOrNull()
}

private fun initialBuffer(value: Double, allowDecimal: Boolean): String {
    if (value == 0.0) return ""
    return if (allowDecimal) {
        if (value % 1.0 == 0.0) value.toInt().toString()
        // Round to 3 decimals + strip trailing zeros so the seed matches what the buffer
        // is allowed to contain (MAX_FRACTION_DIGITS). Otherwise floating-point noise
        // like 2.5000000004 would seed an unrepresentable buffer.
        else String.format(java.util.Locale.US, "%.3f", value)
            .trimEnd('0').trimEnd('.')
    } else {
        value.toInt().toString()
    }
}
