package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SubtleBorder
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated

/**
 * Custom on-screen numpad. Updates the value live as digits are entered;
 * dismissing the sheet (swipe / tap outside) commits whatever's in the buffer.
 * Done, swipe, and tap-outside all dismiss without changing live-entry behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumpadSheet(
    initialValue: Double,
    allowDecimal: Boolean,
    onValueChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    label: String? = null,
    unitLabel: String? = null,
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
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Keep the existing live-entry flow with an explicit, readable dismissal action.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (label != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandLime),
                        )
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.88.sp,
                            ),
                            color = SlateSubtext,
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = DarkOnPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceBase)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = buffer.ifEmpty { "0" },
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 48.sp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .background(BrandLime),
                    )
                    if (!unitLabel.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = unitLabel.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = BrandLime,
                        )
                    }
                }
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (allowDecimal) "." else "", "0", BACKSPACE_TOKEN)
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { token ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
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
    val isBackspace = token == BACKSPACE_TOKEN
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (isBackspace) SurfaceControl else SurfaceElevated)
            .border(1.dp, if (isBackspace) EditorialBorder else SubtleBorder, shape)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onTap(token)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = SlateSubtext,
            )
        } else {
            Text(
                text = token,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
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
