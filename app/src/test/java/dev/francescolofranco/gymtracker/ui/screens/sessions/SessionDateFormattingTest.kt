package dev.francescolofranco.gymtracker.ui.screens.sessions

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDateFormattingTest {
    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun `session dates respect both phone clock formats across date labels`() {
        for ((pattern, time) in listOf("h:mm a" to "7:24 PM", "HH:mm" to "19:24")) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            for ((date, label) in listOf(
                "2026-11-12" to "Today",
                "2026-11-11" to "Yesterday",
                "2026-11-10" to "Tue 10 Nov",
            )) {
                assertEquals(
                    "$label, $time",
                    formatSessionDate(
                        Instant.parse("${date}T19:24:00Z"), formatter,
                        today = today, locale = Locale.US,
                    ),
                )
            }
        }
    }

    @Test
    fun `time and relative date use the phone time zone at midnight and noon`() {
        val formatter = SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+02:00")
        }
        assertEquals(
            "Today, 12:24 AM",
            formatSessionDate(Instant.parse("2026-11-11T22:24:00Z"), formatter, today = today),
        )
        assertEquals(
            "Today, 12:24 PM",
            formatSessionDate(Instant.parse("2026-11-12T10:24:00Z"), formatter, today = today),
        )
    }
}
