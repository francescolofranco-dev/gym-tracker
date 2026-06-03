package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.domain.Muscle
import kotlin.math.cos
import kotlin.math.sin

/**
 * The 15 muscle groups collapsed into the six regions the radar plots. Aggregating to regions is
 * what makes the chart legible — a 15-spoke radar would be noise, whereas six spokes read as a
 * clear "balanced vs. lopsided" shape at a glance. Per-muscle detail lives in the tappable
 * volume-by-muscle bars below the radar.
 */
private val RADAR_REGIONS: List<Pair<String, List<Muscle>>> = listOf(
    "Chest" to listOf(Muscle.CHEST),
    "Back" to listOf(Muscle.LATS, Muscle.UPPER_BACK_TRAPS),
    "Shoulders" to listOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.REAR_DELTS),
    "Arms" to listOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS),
    "Legs" to listOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.ADDUCTORS, Muscle.GLUTES, Muscle.CALVES),
    "Core" to listOf(Muscle.CORE),
)

// Virtual drawing space (matches the design's SVG viewBox so geometry ports 1:1). The Canvas is
// locked to this aspect ratio, so a single uniform scale maps virtual units to pixels.
private const val VB_W = 300f
private const val VB_H = 250f
private const val CX = 150f
private const val CY = 128f
private const val RADIUS = 88f
private val RINGS = floatArrayOf(0.33f, 0.66f, 1f)

/**
 * Training-balance radar — the Stats hero. Aggregates weekly effective sets into six regions and
 * plots them as a polygon scaled to the busiest region, so under-trained areas show up as dents.
 * Replaces the old front/back body heatmap, which looked anatomical but was poor at ranking
 * "most vs. least trained" at a glance.
 */
@Composable
fun RegionalRadar(
    volumes: Map<Muscle, MuscleVolume>,
    modifier: Modifier = Modifier,
) {
    // Sum rounded effective sets per region so the plotted vertex matches the number on the label.
    val regionSets: List<Int> = remember(volumes) {
        RADAR_REGIONS.map { (_, muscles) ->
            muscles.sumOf { volumes[it]?.total ?: 0 }
        }
    }
    val allZero = regionSets.all { it == 0 }
    val subtitle = if (allZero) {
        "No sets logged yet this week."
    } else {
        val strongest = RADAR_REGIONS[regionSets.indexOf(regionSets.max())].first
        val weakest = RADAR_REGIONS[regionSets.indexOf(regionSets.min())].first
        "Weekly sets per region · $strongest leads, $weakest lags"
    }

    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val nameColor = MaterialTheme.colorScheme.onSurface
    val valueColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Training balance",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VB_W / VB_H),
        ) {
            val s = size.width / VB_W
            val max = (regionSets.max()).coerceAtLeast(1)
            val n = RADAR_REGIONS.size

            fun point(i: Int, frac: Float): Offset {
                val angle = -Math.PI / 2 + (i.toDouble() / n) * 2 * Math.PI
                val vx = CX + cos(angle).toFloat() * RADIUS * frac
                val vy = CY + sin(angle).toFloat() * RADIUS * frac
                return Offset(vx * s, vy * s)
            }

            // Concentric guide hexagons.
            RINGS.forEach { frac ->
                drawPath(
                    path = ringPath(n, frac, ::point),
                    color = ringColor,
                    alpha = 0.5f,
                    style = Stroke(width = 1f * s),
                )
            }
            // Spokes out to each region.
            val center = Offset(CX * s, CY * s)
            for (i in 0 until n) {
                drawLine(
                    color = ringColor,
                    start = center,
                    end = point(i, 1f),
                    strokeWidth = 1f * s,
                    alpha = 0.4f,
                )
            }
            // The data polygon: filled cyan wash + crisp cyan outline.
            val dataPath = Path().apply {
                regionSets.forEachIndexed { i, v ->
                    val p = point(i, v.toFloat() / max)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(dataPath, color = accent, alpha = 0.26f)
            drawPath(dataPath, color = accent, style = Stroke(width = 2f * s, join = StrokeJoin.Round))
            // Vertex dots.
            regionSets.forEachIndexed { i, v ->
                drawCircle(color = accent, radius = 3.5f * s, center = point(i, v.toFloat() / max))
            }
            // Region labels (name + value), centred on the anchor just outside the outer ring.
            regionSets.forEachIndexed { i, v ->
                val anchor = point(i, 1.2f)
                val label = regionLabel(RADAR_REGIONS[i].first, v, nameColor, valueColor)
                val layout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = (12.5f * s).toSp(), fontWeight = FontWeight.SemiBold),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        anchor.x - layout.size.width / 2f,
                        anchor.y - layout.size.height / 2f,
                    ),
                )
            }
        }
    }
}

private fun ringPath(n: Int, frac: Float, point: (Int, Float) -> Offset): Path = Path().apply {
    for (i in 0 until n) {
        val p = point(i, frac)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

private fun regionLabel(name: String, value: Int, nameColor: Color, valueColor: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = nameColor)) { append(name) }
        append(" ")
        withStyle(SpanStyle(color = valueColor)) { append(value.toString()) }
    }
