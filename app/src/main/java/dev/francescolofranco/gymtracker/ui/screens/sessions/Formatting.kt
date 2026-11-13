package dev.francescolofranco.gymtracker.ui.screens.sessions

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import dev.francescolofranco.gymtracker.domain.WeightUnit
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val LBS_PER_KG = 2.2046226218

fun WeightUnit.label(): String = when (this) {
    WeightUnit.KG -> "kg"
    WeightUnit.LBS -> "lbs"
}

fun convertFromKg(kg: Double, unit: WeightUnit): Double = when (unit) {
    WeightUnit.KG -> kg
    WeightUnit.LBS -> kg * LBS_PER_KG
}

fun convertToKg(value: Double, unit: WeightUnit): Double = when (unit) {
    WeightUnit.KG -> value
    WeightUnit.LBS -> value / LBS_PER_KG
}

/**
 * Renders a weight number with up to 3 decimals, trimming trailing zeros so 12 reads as "12",
 * 12.5 as "12.5", and 12.345 as "12.345". The numpad allows 3 fractional digits so the display
 * needs to match — otherwise the user enters "2.345" and the chip rounds it back to "2.3".
 */
fun formatWeightNumber(value: Double): String {
    val normalized = if (kotlin.math.abs(value) < 0.0005) 0.0 else value
    if (normalized % 1.0 == 0.0) return normalized.toInt().toString()
    return String.format(Locale.US, "%.3f", normalized).trimEnd('0').trimEnd('.')
}

fun formatTotalVolume(kg: Double, unit: WeightUnit): String {
    val converted = convertFromKg(kg, unit)
    val rounded = converted.roundToInt()
    return "$rounded ${unit.label()}"
}

fun formatDuration(d: Duration): String {
    val totalSeconds = d.seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** Use Android's formatter so the explicit phone clock setting overrides locale defaults. */
@Composable
fun formatSessionDate(at: Instant): String = formatSessionDate(
    at = at,
    systemTimeFormat = DateFormat.getTimeFormat(LocalContext.current),
    locale = LocalConfiguration.current.locales[0],
)

internal fun formatSessionDate(
    at: Instant,
    systemTimeFormat: java.text.DateFormat,
    zone: ZoneId = systemTimeFormat.timeZone.toZoneId(),
    today: LocalDate = LocalDate.now(zone),
    locale: Locale = Locale.getDefault(),
): String {
    val sessionDay = at.atZone(zone).toLocalDate()
    val time = systemTimeFormat.format(Date.from(at))
    val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale).withZone(zone)
    return when (sessionDay) {
        today -> "Today, $time"
        today.minusDays(1) -> "Yesterday, $time"
        else -> "${dateFormat.format(at)}, $time"
    }
}
