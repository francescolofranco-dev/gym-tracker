package dev.francescolofranco.gymtracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NumpadSheetTest {

    @Test
    fun `toggling sign from zero keeps negative prefix for subsequent digits`() {
        var state = initialNumpadState(0.0, allowDecimal = true)
        assertEquals("", state.buffer)
        assertEquals("0", displayBuffer(state.buffer))

        state = state.onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("-", state.buffer)
        assertEquals("-0", displayBuffer(state.buffer))
        assertEquals(0.0, bufferToDouble(state.buffer)!!, 1e-9)

        state = state.onToken("2", allowDecimal = true, allowNegative = true)
            .onToken("5", allowDecimal = true, allowNegative = true)
        assertEquals("-25", state.buffer)
        assertEquals(-25.0, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `toggling sign while pristine flips seed and preserves negative on overwrite`() {
        var state = initialNumpadState(25.0, allowDecimal = true)
        assertEquals("25", state.buffer)

        state = state.onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("-25", state.buffer)
        assertEquals(-25.0, bufferToDouble(state.buffer)!!, 1e-9)

        state = state.onToken("1", allowDecimal = true, allowNegative = true)
            .onToken("5", allowDecimal = true, allowNegative = true)
        assertEquals("-15", state.buffer)
        assertEquals(-15.0, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `typing digit while pristine on negative seed wipes seed and sign`() {
        var state = initialNumpadState(-25.0, allowDecimal = true)
        assertEquals("-25", state.buffer)

        state = state.onToken("1", allowDecimal = true, allowNegative = true)
            .onToken("0", allowDecimal = true, allowNegative = true)
        assertEquals("10", state.buffer)
        assertEquals(10.0, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `toggling sign twice on negative seed restores negative`() {
        var state = initialNumpadState(-25.0, allowDecimal = true)

        state = state.onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("25", state.buffer)
        assertEquals(25.0, bufferToDouble(state.buffer)!!, 1e-9)

        state = state.onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("-25", state.buffer)
    }

    @Test
    fun `decimal after lone minus becomes negative zero point`() {
        var state = initialNumpadState(0.0, allowDecimal = true)
            .onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
            .onToken(".", allowDecimal = true, allowNegative = true)
        assertEquals("-0.", state.buffer)
        assertEquals(-0.0, bufferToDouble(state.buffer)!!, 1e-9)

        state = state.onToken("5", allowDecimal = true, allowNegative = true)
        assertEquals("-0.5", state.buffer)
        assertEquals(-0.5, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `digit caps apply to magnitude ignoring minus sign`() {
        var state = initialNumpadState(0.0, allowDecimal = true)
            .onToken(SIGN_TOKEN, allowDecimal = true, allowNegative = true)
        listOf("9", "9", "9", "9", "8", ".", "1", "2", "3", "4").forEach { token ->
            state = state.onToken(token, allowDecimal = true, allowNegative = true)
        }
        assertEquals("-9999.123", state.buffer)
        assertEquals(-9999.123, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `backspace through negative sign returns to zero`() {
        var state = initialNumpadState(-2.0, allowDecimal = true)
            .onToken(BACKSPACE_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("-", state.buffer)
        assertEquals(0.0, bufferToDouble(state.buffer)!!, 1e-9)

        state = state.onToken(BACKSPACE_TOKEN, allowDecimal = true, allowNegative = true)
        assertEquals("", state.buffer)
        assertEquals(0.0, bufferToDouble(state.buffer)!!, 1e-9)
    }

    @Test
    fun `sign token is ignored when allowNegative is false`() {
        val state = initialNumpadState(10.0, allowDecimal = false)
            .onToken(SIGN_TOKEN, allowDecimal = false, allowNegative = false)
        assertEquals("10", state.buffer)
    }

    @Test
    fun `near zero negative seed formats as empty buffer`() {
        assertEquals("", initialBuffer(-0.0, allowDecimal = true))
        assertEquals("", initialBuffer(-0.0001, allowDecimal = true))
    }
}
