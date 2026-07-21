package dev.francescolofranco.gymtracker.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * One motion language for navigation, lists, and small state changes. Compose automatically
 * honors the system animator-duration scale, including the user's "remove animations" setting.
 */
object GymMotion {
    const val Quick = 140
    const val Standard = 240
    const val Emphasized = 340

    /** Material-style emphasized easing: quick response, then a soft settle. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val ExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    val ItemFadeIn: FiniteAnimationSpec<Float> = tween(
        durationMillis = Standard,
        delayMillis = 35,
        easing = EmphasizedEasing,
    )
    val ItemFadeOut: FiniteAnimationSpec<Float> = tween(
        durationMillis = Quick,
        easing = ExitEasing,
    )
    val ItemPlacement: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMediumLow,
    )
}
