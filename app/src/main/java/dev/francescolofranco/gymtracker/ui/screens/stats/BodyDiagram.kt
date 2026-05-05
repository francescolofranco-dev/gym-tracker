package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.Muscle

private enum class BodyView { FRONT, BACK }

/** Normalised rectangle in 0..1 coordinate space relative to the silhouette panel. */
private data class NRect(val l: Float, val t: Float, val r: Float, val b: Float) {
    fun toPx(size: Size, padX: Float, padY: Float): Rect = Rect(
        left = padX + l * (size.width - 2 * padX),
        top = padY + t * (size.height - 2 * padY),
        right = padX + r * (size.width - 2 * padX),
        bottom = padY + b * (size.height - 2 * padY),
    )
}

private data class MuscleBlock(val muscle: Muscle, val rects: List<NRect>)

/**
 * Front and back muscle layouts in a normalised coordinate system. Blocks are intentionally
 * abstract rectangles rather than anatomical paths — the goal is semantic colour coding, not
 * realistic anatomy. Two rects per muscle = mirrored left/right; both map to the same Muscle.
 */
private val FRONT_BLOCKS = listOf(
    MuscleBlock(Muscle.UPPER_BACK_TRAPS, listOf(NRect(0.40f, 0.10f, 0.60f, 0.16f))), // traps top hint
    MuscleBlock(Muscle.FRONT_DELTS, listOf(NRect(0.30f, 0.18f, 0.42f, 0.26f), NRect(0.58f, 0.18f, 0.70f, 0.26f))),
    MuscleBlock(Muscle.SIDE_DELTS, listOf(NRect(0.22f, 0.18f, 0.30f, 0.28f), NRect(0.70f, 0.18f, 0.78f, 0.28f))),
    MuscleBlock(Muscle.CHEST, listOf(NRect(0.32f, 0.22f, 0.50f, 0.32f), NRect(0.50f, 0.22f, 0.68f, 0.32f))),
    MuscleBlock(Muscle.BICEPS, listOf(NRect(0.18f, 0.28f, 0.28f, 0.42f), NRect(0.72f, 0.28f, 0.82f, 0.42f))),
    MuscleBlock(Muscle.CORE, listOf(NRect(0.36f, 0.32f, 0.64f, 0.50f))),
    MuscleBlock(Muscle.FOREARMS, listOf(NRect(0.14f, 0.42f, 0.24f, 0.58f), NRect(0.76f, 0.42f, 0.86f, 0.58f))),
    MuscleBlock(Muscle.QUADS, listOf(NRect(0.32f, 0.58f, 0.48f, 0.82f), NRect(0.52f, 0.58f, 0.68f, 0.82f))),
    MuscleBlock(Muscle.CALVES, listOf(NRect(0.34f, 0.84f, 0.46f, 0.96f), NRect(0.54f, 0.84f, 0.66f, 0.96f))),
)

private val BACK_BLOCKS = listOf(
    MuscleBlock(Muscle.UPPER_BACK_TRAPS, listOf(NRect(0.36f, 0.16f, 0.64f, 0.30f))),
    MuscleBlock(Muscle.REAR_DELTS, listOf(NRect(0.22f, 0.18f, 0.32f, 0.26f), NRect(0.68f, 0.18f, 0.78f, 0.26f))),
    MuscleBlock(Muscle.LATS, listOf(NRect(0.30f, 0.30f, 0.42f, 0.48f), NRect(0.58f, 0.30f, 0.70f, 0.48f))),
    MuscleBlock(Muscle.TRICEPS, listOf(NRect(0.18f, 0.28f, 0.28f, 0.42f), NRect(0.72f, 0.28f, 0.82f, 0.42f))),
    MuscleBlock(Muscle.FOREARMS, listOf(NRect(0.14f, 0.42f, 0.24f, 0.58f), NRect(0.76f, 0.42f, 0.86f, 0.58f))),
    MuscleBlock(Muscle.GLUTES, listOf(NRect(0.34f, 0.50f, 0.50f, 0.62f), NRect(0.50f, 0.50f, 0.66f, 0.62f))),
    MuscleBlock(Muscle.HAMSTRINGS, listOf(NRect(0.32f, 0.62f, 0.48f, 0.82f), NRect(0.52f, 0.62f, 0.68f, 0.82f))),
    MuscleBlock(Muscle.CALVES, listOf(NRect(0.34f, 0.84f, 0.46f, 0.96f), NRect(0.54f, 0.84f, 0.66f, 0.96f))),
)

@Composable
fun BodyDiagram(
    colors: Map<Muscle, Color>,
    onMuscleTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BodyPanel(
                view = BodyView.FRONT,
                blocks = FRONT_BLOCKS,
                colors = colors,
                onMuscleTap = onMuscleTap,
                modifier = Modifier.weight(1f).aspectRatio(0.5f),
            )
            BodyPanel(
                view = BodyView.BACK,
                blocks = BACK_BLOCKS,
                colors = colors,
                onMuscleTap = onMuscleTap,
                modifier = Modifier.weight(1f).aspectRatio(0.5f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Front", style = MaterialTheme.typography.labelMedium)
            Text("Back", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BodyPanel(
    view: BodyView,
    blocks: List<MuscleBlock>,
    colors: Map<Muscle, Color>,
    onMuscleTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val viewLabel = if (view == BodyView.FRONT) "Front" else "Back"

    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(blocks, colors) {
                    awaitEachTap { offset ->
                        val size = Size(this.size.width.toFloat(), this.size.height.toFloat())
                        val pad = 8f
                        val muscle = blocks
                            .asSequence()
                            .flatMap { b -> b.rects.asSequence().map { b.muscle to it.toPx(size, pad, pad) } }
                            .firstOrNull { (_, r) -> r.contains(offset) }?.first
                        if (muscle != null) onMuscleTap(muscle)
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSilhouette(outline = outline, view = view, panelSize = size)
                val padX = 8f
                val padY = 8f
                blocks.forEach { block ->
                    val color = colors[block.muscle] ?: Color.Gray
                    block.rects.forEach { nrect ->
                        val r = nrect.toPx(size, padX, padY)
                        drawRoundRect(
                            color = color.copy(alpha = 0.85f),
                            topLeft = Offset(r.left, r.top),
                            size = Size(r.width, r.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                        )
                    }
                }
            }
            // Accessibility / readability: label the panel.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            ) {
                Text(
                    text = viewLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitEachTap(
    onTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val up = waitForUpOrCancellation()
        if (up != null) onTap(down.position)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSilhouette(
    outline: Color,
    view: BodyView,
    panelSize: Size,
) {
    val w = panelSize.width
    val h = panelSize.height
    val stroke = Stroke(width = 3f)

    // Head
    drawCircle(
        color = outline,
        radius = w * 0.10f,
        center = Offset(w * 0.5f, h * 0.07f),
        style = stroke,
    )

    val torso = Path().apply {
        moveTo(w * 0.30f, h * 0.16f)
        lineTo(w * 0.70f, h * 0.16f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.52f)
        close()
    }
    drawPath(torso, outline, style = stroke)

    val leftArm = Path().apply {
        moveTo(w * 0.30f, h * 0.17f)
        lineTo(w * 0.16f, h * 0.42f)
        lineTo(w * 0.13f, h * 0.58f)
    }
    drawPath(leftArm, outline, style = stroke)

    val rightArm = Path().apply {
        moveTo(w * 0.70f, h * 0.17f)
        lineTo(w * 0.84f, h * 0.42f)
        lineTo(w * 0.87f, h * 0.58f)
    }
    drawPath(rightArm, outline, style = stroke)

    val pelvis = Path().apply {
        moveTo(w * 0.34f, h * 0.52f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.62f, h * 0.60f)
        lineTo(w * 0.38f, h * 0.60f)
        close()
    }
    drawPath(pelvis, outline, style = stroke)

    val leftLeg = Path().apply {
        moveTo(w * 0.40f, h * 0.60f)
        lineTo(w * 0.36f, h * 0.96f)
    }
    val rightLeg = Path().apply {
        moveTo(w * 0.60f, h * 0.60f)
        lineTo(w * 0.64f, h * 0.96f)
    }
    drawPath(leftLeg, outline, style = stroke)
    drawPath(rightLeg, outline, style = stroke)

    // (We intentionally don't differentiate front vs back outline shape — only the muscle
    // overlays differ. A future polish pass can swap in distinct silhouettes.)
    @Suppress("UNUSED_EXPRESSION") view
}
