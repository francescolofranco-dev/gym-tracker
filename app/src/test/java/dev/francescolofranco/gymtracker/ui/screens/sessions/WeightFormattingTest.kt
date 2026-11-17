package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.screens.exercises.epley1Rm
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightFormattingTest {
    @Test
    fun `weights and estimated max display at most two decimals`() {
        assertEquals("8.56", formatWeightNumber(8.555))
        assertEquals("12.83", formatWeightNumber(epley1Rm(8.555, 15)))
        assertEquals("18.86", formatWeightNumber(convertFromKg(8.555, WeightUnit.LBS)))
        assertEquals("-8.56", formatWeightNumber(-8.555))
    }

    @Test
    fun `rounding trims trailing zeros and normalizes negative zero`() {
        assertEquals("12", formatWeightNumber(12.0))
        assertEquals("12.5", formatWeightNumber(12.5))
        assertEquals("13", formatWeightNumber(12.999))
        assertEquals("0", formatWeightNumber(-0.004))
        assertEquals("-0.01", formatWeightNumber(-0.006))
    }
}
