package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleChipsTest {

    @Test
    fun `selected enabled secondary chip uses tinted fill, lime label, and lime border`() {
        val spec = muscleChipSelectedSpec(
            isSecondary = true,
            isSelected = true,
            isEnabled = true,
        )

        assertEquals(SecondarySelectedFill, spec.containerColor)
        assertEquals(BrandLime.copy(alpha = 0.20f), spec.containerColor)
        assertEquals(BrandLime, spec.labelColor)

        val border = spec.border
        assertNotNull(border)
        assertEquals(1.5.dp, border!!.width)
        assertTrue(border.brush is SolidColor)
        assertEquals(BrandLime, (border.brush as SolidColor).value)
    }

    @Test
    fun `unselected secondary chip has no border`() {
        val spec = muscleChipSelectedSpec(
            isSecondary = true,
            isSelected = false,
            isEnabled = true,
        )

        assertNull(spec.border)
    }

    @Test
    fun `disabled secondary chip has no border even when selected`() {
        val spec = muscleChipSelectedSpec(
            isSecondary = true,
            isSelected = true,
            isEnabled = false,
        )

        assertNull(spec.border)
    }

    @Test
    fun `selected enabled primary chip uses solid lime fill, dark label, and no border`() {
        val primarySpec = muscleChipSelectedSpec(
            isSecondary = false,
            isSelected = true,
            isEnabled = true,
        )
        val secondarySpec = muscleChipSelectedSpec(
            isSecondary = true,
            isSelected = true,
            isEnabled = true,
        )

        assertEquals(BrandLime, primarySpec.containerColor)
        assertEquals(DarkOnPrimary, primarySpec.labelColor)
        assertNull(primarySpec.border)
        assertNotEquals(primarySpec.containerColor, secondarySpec.containerColor)
        assertNotEquals(primarySpec.labelColor, secondarySpec.labelColor)
    }
}
