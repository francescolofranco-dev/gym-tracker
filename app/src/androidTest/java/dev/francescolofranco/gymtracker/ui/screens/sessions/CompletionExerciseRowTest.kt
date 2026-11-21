package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.theme.GymTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CompletionExerciseRowTest(private val widthDp: Int, private val fontScale: Float) {
    @get:Rule val compose = createComposeRule()

    @Test fun longNameAndUnilateralRepsKeepReadableWidths() {
        val name = "Standing calf raise with a long exercise name"
        val sets = "4 sets/side (L 20, 15, 14, 15, R 20, 15, 15, 15 reps)"
        compose.setContent {
            GymTrackerTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.width(widthDp.dp)) {
                        CompletionExerciseRow(
                            SessionShareExercise(name, 8, sets, 10.0, false),
                            WeightUnit.KG,
                        )
                    }
                }
            }
        }
        val nameBounds = compose.onNodeWithText(name.uppercase()).fetchSemanticsNode().boundsInRoot
        val setsBounds = compose.onNodeWithText(sets).fetchSemanticsNode().boundsInRoot
        val weightBounds = compose.onNodeWithText("10 kg").fetchSemanticsNode().boundsInRoot
        val availableWidth = weightBounds.right - nameBounds.left
        assertTrue("Name must retain readable width", nameBounds.width >= availableWidth / 2)
        assertTrue("Set details must sit below the name and weight",
            setsBounds.top >= maxOf(nameBounds.bottom, weightBounds.bottom))
        assertTrue("Set details must have the full row width", setsBounds.width >= availableWidth - 1f)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0}dp, fontScale={1}")
        fun sizes() = listOf(arrayOf<Any>(240, 1f), arrayOf<Any>(320, 1f),
            arrayOf<Any>(240, 1.5f), arrayOf<Any>(320, 1.5f))
    }
}
