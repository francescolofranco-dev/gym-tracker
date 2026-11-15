package dev.francescolofranco.gymtracker.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class UiZoomTest {
    @Test
    fun zoomScaleIsFivePercent() {
        assertEquals(1.05f, UI_ZOOM_SCALE, 0.0001f)
    }

    @Test
    fun scaledDensityIncreasesLogicalDensityByFivePercent() {
        assertEquals(1.05f, scaledDensity(1.0f), 0.0001f)
        assertEquals(2.10f, scaledDensity(2.0f), 0.0001f)
        assertEquals(2.8875f, scaledDensity(2.75f), 0.0001f)
        assertEquals(3.15f, scaledDensity(3.0f), 0.0001f)
        assertEquals(3.675f, scaledDensity(3.5f), 0.0001f)
    }

    @Test
    fun scaledDensityDpiIncreasesCommonBucketsByFivePercent() {
        assertEquals(336, scaledDensityDpi(320))
        assertEquals(441, scaledDensityDpi(420))
        assertEquals(462, scaledDensityDpi(440))
        assertEquals(504, scaledDensityDpi(480))
        assertEquals(588, scaledDensityDpi(560))
    }
}
