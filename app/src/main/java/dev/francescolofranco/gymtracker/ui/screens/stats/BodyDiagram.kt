package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.Muscle

private enum class BodyView { FRONT, BACK }

/** Normalised rectangle in 0..1 coordinate space relative to the silhouette panel. */
private data class NRect(val l: Float, val t: Float, val r: Float, val b: Float)

private data class MuscleBlock(val muscle: Muscle, val rects: List<NRect>)

/**
 * Front and back muscle layouts in a normalised coordinate system. Blocks are intentionally
 * abstract rectangles rather than anatomical paths — the goal is semantic colour coding, not
 * realistic anatomy. Two rects per muscle = mirrored left/right; both map to the same Muscle.
 */
private val FRONT_BLOCKS = listOf(
    MuscleBlock(Muscle.FRONT_DELTS, listOf(NRect(0.28f, 0.18f, 0.42f, 0.24f), NRect(0.58f, 0.18f, 0.72f, 0.24f))),
    MuscleBlock(Muscle.SIDE_DELTS, listOf(NRect(0.18f, 0.18f, 0.26f, 0.28f), NRect(0.74f, 0.18f, 0.82f, 0.28f))),
    MuscleBlock(Muscle.CHEST, listOf(NRect(0.32f, 0.26f, 0.50f, 0.36f), NRect(0.50f, 0.26f, 0.68f, 0.36f))),
    MuscleBlock(Muscle.BICEPS, listOf(NRect(0.18f, 0.30f, 0.28f, 0.44f), NRect(0.72f, 0.30f, 0.82f, 0.44f))),
    MuscleBlock(Muscle.CORE, listOf(NRect(0.36f, 0.38f, 0.64f, 0.54f))),
    MuscleBlock(Muscle.FOREARMS, listOf(NRect(0.14f, 0.46f, 0.24f, 0.60f), NRect(0.76f, 0.46f, 0.86f, 0.60f))),
    MuscleBlock(Muscle.QUADS, listOf(NRect(0.30f, 0.60f, 0.45f, 0.82f), NRect(0.55f, 0.60f, 0.70f, 0.82f))),
    // Adductors sit on the inner thigh, anatomically distinct from quads / hamstrings. Single
    // central block between the two quad rects — narrower and a touch shorter so it doesn't
    // dominate the layout.
    MuscleBlock(Muscle.ADDUCTORS, listOf(NRect(0.46f, 0.62f, 0.54f, 0.78f))),
    MuscleBlock(Muscle.CALVES, listOf(NRect(0.34f, 0.84f, 0.46f, 0.96f), NRect(0.54f, 0.84f, 0.66f, 0.96f))),
)

private val BACK_BLOCKS = listOf(
    MuscleBlock(Muscle.UPPER_BACK_TRAPS, listOf(NRect(0.36f, 0.14f, 0.64f, 0.28f))),
    MuscleBlock(Muscle.REAR_DELTS, listOf(NRect(0.22f, 0.18f, 0.34f, 0.26f), NRect(0.66f, 0.18f, 0.78f, 0.26f))),
    MuscleBlock(Muscle.LATS, listOf(NRect(0.28f, 0.30f, 0.42f, 0.50f), NRect(0.58f, 0.30f, 0.72f, 0.50f))),
    MuscleBlock(Muscle.TRICEPS, listOf(NRect(0.16f, 0.30f, 0.26f, 0.44f), NRect(0.74f, 0.30f, 0.84f, 0.44f))),
    MuscleBlock(Muscle.FOREARMS, listOf(NRect(0.12f, 0.46f, 0.22f, 0.60f), NRect(0.78f, 0.46f, 0.88f, 0.60f))),
    MuscleBlock(Muscle.GLUTES, listOf(NRect(0.34f, 0.54f, 0.50f, 0.66f), NRect(0.50f, 0.54f, 0.66f, 0.66f))),
    MuscleBlock(Muscle.HAMSTRINGS, listOf(NRect(0.32f, 0.68f, 0.48f, 0.82f), NRect(0.52f, 0.68f, 0.68f, 0.82f))),
    MuscleBlock(Muscle.CALVES, listOf(NRect(0.34f, 0.84f, 0.46f, 0.96f), NRect(0.54f, 0.84f, 0.66f, 0.96f))),
)

/**
 * Hand-built Compose body diagram. Each muscle is one or more absolutely-positioned tappable
 * Boxes (the symmetric ones get mirrored L/R rects, both pointing at the same Muscle).
 *
 * Accessibility: the first rect of each muscle carries a self-contained semantic description
 * ("Chest, 4 sets, in the 3-10 weekly range. Tap for details."). Mirror rects clear their
 * semantics so Talkback doesn't double-announce the same muscle.
 */
@Composable
fun BodyDiagram(
    volumes: Map<Muscle, MuscleVolume>,
    onMuscleTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BodyPanel(
                view = BodyView.FRONT,
                blocks = FRONT_BLOCKS,
                volumes = volumes,
                onMuscleTap = onMuscleTap,
                modifier = Modifier.weight(1f).aspectRatio(0.55f),
            )
            BodyPanel(
                view = BodyView.BACK,
                blocks = BACK_BLOCKS,
                volumes = volumes,
                onMuscleTap = onMuscleTap,
                modifier = Modifier.weight(1f).aspectRatio(0.55f),
            )
        }
    }
}

@Composable
private fun BodyPanel(
    view: BodyView,
    blocks: List<MuscleBlock>,
    volumes: Map<Muscle, MuscleVolume>,
    onMuscleTap: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val viewLabel = if (view == BodyView.FRONT) "Front" else "Back"

    BoxWithConstraints(modifier = modifier) {
        val panelW: Dp = maxWidth
        val panelH: Dp = maxHeight

        // Background silhouette outline (decorative).
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics(mergeDescendants = false) { contentDescription = "" },
        ) {
            drawSilhouette(outline, size)
        }

        // Per-muscle tappable boxes, positioned in dp via offset+size.
        blocks.forEach { block ->
            val volume = volumes[block.muscle]
            val color = volumeColor(volume?.total ?: 0)
            block.rects.forEachIndexed { index, nrect ->
                val isPrimaryRect = index == 0
                val description = if (isPrimaryRect && volume != null) muscleSemantic(volume) else null
                MuscleRectBox(
                    nrect = nrect,
                    panelW = panelW,
                    panelH = panelH,
                    color = color,
                    description = description,
                    onClick = { onMuscleTap(block.muscle) },
                )
            }
        }

        // Panel label, kept out of Talkback's flow.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clearAndSetSemantics { },
        ) {
            Text(
                text = viewLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MuscleRectBox(
    nrect: NRect,
    panelW: Dp,
    panelH: Dp,
    color: Color,
    description: String?,
    onClick: () -> Unit,
) {
    val left = panelW * nrect.l
    val top = panelH * nrect.t
    val width = panelW * (nrect.r - nrect.l)
    val height = panelH * (nrect.b - nrect.t)

    val base = Modifier
        .offset(x = left, y = top)
        .size(width = width, height = height)
        .clip(RoundedCornerShape(6.dp))
        .background(color.copy(alpha = 0.85f))
        .clickable(onClick = onClick)

    val withSemantics = if (description != null) {
        base.semantics(mergeDescendants = true) {
            contentDescription = description
            role = Role.Button
        }
    } else {
        // Mirror rect — keep tap target alive but hide from Talkback.
        base.clearAndSetSemantics { }
    }

    Box(modifier = withSemantics)
}

private fun muscleSemantic(volume: MuscleVolume): String {
    val name = volume.muscle.displayName
    val total = volume.total
    val sets = if (total == 1) "1 set" else "$total sets"
    val zone = when {
        total <= 0 -> "no work this week"
        total <= 2 -> "below the 3 to 10 weekly range"
        total <= Muscle.WEEKLY_MAX -> "in the 3 to 10 weekly range"
        else -> "above the 3 to 10 weekly range"
    }
    return "$name, $sets, $zone. Tap for details."
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSilhouette(
    outline: Color,
    panelSize: Size,
) {
    val w = panelSize.width
    val h = panelSize.height
    val stroke = Stroke(width = 3f)

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
}
