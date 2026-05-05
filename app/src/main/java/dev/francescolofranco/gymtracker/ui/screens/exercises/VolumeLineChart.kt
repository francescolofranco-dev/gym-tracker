package dev.francescolofranco.gymtracker.ui.screens.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSessionPoint

/**
 * Lightweight Compose-Canvas line chart for an exercise's per-session volume. Vico would also
 * fit the bill, but a hand-rolled chart matches the project's "no extra deps for fixed-shape
 * UI" choice (same call as the body diagram). Plots are evenly spaced by sequence rather than
 * by date, which keeps the curve readable even after long gaps between sessions.
 */
@Composable
fun VolumeLineChart(
    points: List<ExerciseSessionPoint>,
    convert: (Double) -> Double,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val axis = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val padX = 12f
            val padY = 12f
            val w = size.width
            val h = size.height

            // baseline (x-axis at bottom)
            drawLine(
                color = axis,
                start = Offset(padX, h - padY),
                end = Offset(w - padX, h - padY),
                strokeWidth = 2f,
            )

            if (points.size < 2) return@Canvas

            val values = points.map { convert(it.volumeKg) }
            val maxV = values.max().coerceAtLeast(1.0)
            val n = values.size

            fun x(i: Int): Float = padX + (w - 2 * padX) * (i.toFloat() / (n - 1))
            fun y(v: Double): Float = (h - padY) - ((v / maxV) * (h - 2 * padY)).toFloat()

            // area fill
            val area = Path().apply {
                moveTo(x(0), h - padY)
                values.forEachIndexed { i, v -> lineTo(x(i), y(v)) }
                lineTo(x(n - 1), h - padY)
                close()
            }
            drawPath(area, fill)

            // line
            val curve = Path().apply {
                moveTo(x(0), y(values.first()))
                values.drop(1).forEachIndexed { i, v -> lineTo(x(i + 1), y(v)) }
            }
            drawPath(curve, line, style = Stroke(width = 4f))

            // points (subtle dots)
            values.forEachIndexed { i, v ->
                drawCircle(color = line, radius = 4f, center = Offset(x(i), y(v)))
            }

            // emphasise the last point
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(x(n - 1), y(values.last())),
            )
            drawCircle(
                color = line,
                radius = 5f,
                center = Offset(x(n - 1), y(values.last())),
            )
        }
    }
}
