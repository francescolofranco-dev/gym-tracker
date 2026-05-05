package dev.francescolofranco.gymtracker.ui.screens.sessions

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

fun formatWeight(kg: Double, unit: WeightUnit): String {
    val converted = convertFromKg(kg, unit)
    val rounded = (converted * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()} ${unit.label()}"
    else String.format(Locale.US, "%.1f %s", rounded, unit.label())
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

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun formatSessionDate(at: Instant): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val sessionDay = at.atZone(zone).toLocalDate()
    return when (sessionDay) {
        today -> "Today, ${timeFormat.format(at)}"
        today.minusDays(1) -> "Yesterday, ${timeFormat.format(at)}"
        else -> "${dateFormat.format(at)}, ${timeFormat.format(at)}"
    }
}
