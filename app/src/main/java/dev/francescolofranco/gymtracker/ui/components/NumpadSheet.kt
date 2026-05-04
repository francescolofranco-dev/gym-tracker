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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                        buffer = applyToken(buffer, tapped, allowDecimal)
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
            // Cap length to keep things sane: 6 chars is plenty (e.g. "999.99")
            if (candidate.length > 6) buffer else candidate
        }
    }
}

private fun bufferToDouble(buffer: String): Double? {
    if (buffer.isEmpty() || buffer == ".") return 0.0
    return buffer.toDoubleOrNull()
}

private fun initialBuffer(value: Double, allowDecimal: Boolean): String {
    if (value == 0.0) return ""
    return if (allowDecimal) {
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    } else {
        value.toInt().toString()
    }
}
