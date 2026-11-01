package dev.francescolofranco.gymtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.OswaldFontFamily
import dev.francescolofranco.gymtracker.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

enum class LogoVariant {
    MarkOnly,
    FullLockup,
    Monochrome,
}

private val KdObsidian = Color(0xFF090A0C)
private val KdSurface = Color(0xFF14161B)
private val KdBorder = Color(0xFF22262E)
private val KdChartreuse = Color(0xFFD4FF33)
private val KdOpticalWhite = Color(0xFFF4F5F7)

private val RackClampEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val PulseSurgeEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Kinetic Discipline brand mark & lockup.
 *
 * Geometry is snapped to a 24x24 grid with 2px stroke paths:
 * - Background barbell shaft line (`x1="2" y1="12" x2="22" y2="12"`, opacity 0.4)
 * - Barbell plates & telemetry pulse (`M2 8V16M5 5V19V12H8L10.5 4L13.5 20L16 12H19V5V19M22 8V16`)
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    variant: LogoVariant = LogoVariant.MarkOnly,
    badge: String? = null,
    wordmark: String = "GYM TRACKER",
    showContainer: Boolean = false,
    monochromeTint: Color = DarkOnSurface,
    interactive: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val isMonochrome = variant == LogoVariant.Monochrome
    val isLockup = variant == LogoVariant.FullLockup

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isSurging by remember { mutableStateOf(false) }
    val pulseProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val isActive = isPressed || isHovered || isSurging

    val rackScaleY by animateFloatAsState(
        targetValue = if (isActive) 0.92f else 1f,
        // 👇 Increase durationMillis (currently 180ms) to slow down the vertical squeeze
        animationSpec = tween(durationMillis = 900, easing = RackClampEasing),
        label = "rackScaleY",
    )
    val rackAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.85f else 0.4f,
        // 👇 Increase durationMillis (currently 180ms) to match
        animationSpec = tween(durationMillis = 900),
        label = "rackAlpha",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) KdChartreuse else KdBorder,
        // 👇 Increase durationMillis (currently 180ms) to match
        animationSpec = tween(durationMillis = 900),
        label = "logoBorderColor",
    )

    val triggerMicroInteraction: () -> Unit = {
        scope.launch {
            isSurging = true
            pulseProgress.snapTo(0f)
            pulseProgress.animateTo(
                targetValue = 1f,
                // 👇 Increase durationMillis (currently 360ms) to slow down the wave sweep
                animationSpec = tween(durationMillis = 1200, easing = PulseSurgeEasing),
            )
            delay(240.milliseconds)
            isSurging = false
        }
        onClick?.invoke()
    }

    val gapDp = max(8, (size.value * 0.35f).roundToInt()).dp
    val clickableModifier = if (interactive || onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = triggerMicroInteraction,
        )
    } else {
        Modifier
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gapDp),
        modifier = modifier
            .then(clickableModifier)
            .semantics {
                contentDescription = "Gym Tracker Logo"
                if (onClick != null) {
                    role = Role.Button
                }
            },
    ) {
        val markModifier = if (showContainer) {
            Modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(KdObsidian)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding((size.value * 0.14f).dp)
        } else {
            Modifier.size(size)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = markModifier,
        ) {
            val rackColor = if (isMonochrome) monochromeTint else KdOpticalWhite
            val pulseColor = if (isMonochrome) monochromeTint else KdChartreuse

            val fullPulsePath = remember { Path() }
            val drawnPulsePath = remember { Path() }
            val pathMeasure = remember { PathMeasure() }

            Canvas(modifier = Modifier.matchParentSize()) {
                val scaleFactor = this.size.minDimension / 24f
                val strokePx = 2f * scaleFactor

                // 1. Background horizontal axis:
                // <line x1="2" y1="12" x2="22" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="square" opacity="0.4"/>
                drawLine(
                    color = rackColor.copy(alpha = rackAlpha),
                    start = Offset(2f * scaleFactor, 12f * scaleFactor),
                    end = Offset(22f * scaleFactor, 12f * scaleFactor),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Square,
                )

                // 2. Vertical barbell plates:
                // M2 8V16 | M5 5V19 | M19 5V19 | M22 8V16
                scale(scaleX = 1f, scaleY = rackScaleY, pivot = center) {
                    // Left outer plate (M2 8V16)
                    drawLine(
                        color = pulseColor,
                        start = Offset(2f * scaleFactor, 8f * scaleFactor),
                        end = Offset(2f * scaleFactor, 16f * scaleFactor),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Square,
                    )
                    // Left inner plate (M5 5V19)
                    drawLine(
                        color = pulseColor,
                        start = Offset(5f * scaleFactor, 5f * scaleFactor),
                        end = Offset(5f * scaleFactor, 19f * scaleFactor),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Square,
                    )
                    // Right inner plate (M19 5V19)
                    drawLine(
                        color = pulseColor,
                        start = Offset(19f * scaleFactor, 5f * scaleFactor),
                        end = Offset(19f * scaleFactor, 19f * scaleFactor),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Square,
                    )
                    // Right outer plate (M22 8V16)
                    drawLine(
                        color = pulseColor,
                        start = Offset(22f * scaleFactor, 8f * scaleFactor),
                        end = Offset(22f * scaleFactor, 16f * scaleFactor),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Square,
                    )
                }

                // 3. Center continuous telemetry pulse:
                // M5 12H8L10.5 4L13.5 20L16 12H19
                fullPulsePath.reset()
                fullPulsePath.moveTo(5f * scaleFactor, 12f * scaleFactor)
                fullPulsePath.lineTo(8f * scaleFactor, 12f * scaleFactor)
                fullPulsePath.lineTo(10.5f * scaleFactor, 4f * scaleFactor)
                fullPulsePath.lineTo(13.5f * scaleFactor, 20f * scaleFactor)
                fullPulsePath.lineTo(16f * scaleFactor, 12f * scaleFactor)
                fullPulsePath.lineTo(19f * scaleFactor, 12f * scaleFactor)

                val progress = pulseProgress.value
                val pathToDraw = if (progress >= 0.999f) {
                    fullPulsePath
                } else {
                    drawnPulsePath.reset()
                    pathMeasure.setPath(fullPulsePath, false)
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * progress,
                        destination = drawnPulsePath,
                        startWithMoveTo = true,
                    )
                    drawnPulsePath
                }

                drawPath(
                    path = pathToDraw,
                    color = pulseColor,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Square,
                        join = StrokeJoin.Miter,
                        miter = 4f,
                    ),
                )
            }
        }

        if (isLockup) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val wordmarkFontSize = (size.value * (if (showContainer) 0.56f else 0.68f)).roundToInt().sp
                Text(
                    text = wordmark.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = OswaldFontFamily,
                        fontSize = wordmarkFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (wordmarkFontSize.value * 0.02f).sp,
                    ),
                    color = if (isMonochrome) monochromeTint else KdOpticalWhite,
                    maxLines = 1,
                )
                if (!badge.isNullOrBlank()) {
                    val badgeFontSize = max(10, (size.value * 0.30f).roundToInt()).sp
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(KdSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = badgeFontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (badgeFontSize.value * 0.06f).sp,
                                fontFeatureSettings = "tnum",
                            ),
                            color = KdChartreuse,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}