package dev.francescolofranco.gymtracker.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    Sessions("sessions", dev.francescolofranco.gymtracker.R.string.tab_sessions, NavIcons.SessionsCalendar),
    Exercises("exercises", dev.francescolofranco.gymtracker.R.string.tab_exercises, NavIcons.ExercisesDumbbell),
    Stats("stats", dev.francescolofranco.gymtracker.R.string.tab_stats, NavIcons.StatsBars),
    Settings("settings", dev.francescolofranco.gymtracker.R.string.tab_settings, Icons.Outlined.Settings)
}

/**
 * Stroked 24×24 vector icons matching the Figma bottom navigation bar (1:187):
 * - Sessions: Outlined calendar with top rings, divider line, and 3 event dots.
 * - Exercises: Outlined horizontal dumbbell.
 * - Stats: Outlined 3-bar ascending chart.
 */
object NavIcons {
    val SessionsCalendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "SessionsCalendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke = SolidColor(Color.White)
            // Calendar outer rounded rect (x=3..21, y=4..21, rx=2)
            path(
                stroke = stroke,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                quadTo(21f, 4f, 21f, 6f)
                lineTo(21f, 19f)
                quadTo(21f, 21f, 19f, 21f)
                lineTo(5f, 21f)
                quadTo(3f, 21f, 3f, 19f)
                lineTo(3f, 6f)
                quadTo(3f, 4f, 5f, 4f)
                close()
                // Top binding rings
                moveTo(16f, 2f)
                lineTo(16f, 6f)
                moveTo(8f, 2f)
                lineTo(8f, 6f)
                // Horizontal header divider
                moveTo(3f, 10f)
                lineTo(21f, 10f)
            }
            // Three dots inside calendar body (y=15, x=8, 12, 16)
            path(
                fill = stroke,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(7f, 14f)
                lineTo(9f, 14f)
                lineTo(9f, 16f)
                lineTo(7f, 16f)
                close()
                moveTo(11f, 14f)
                lineTo(13f, 14f)
                lineTo(13f, 16f)
                lineTo(11f, 16f)
                close()
                moveTo(15f, 14f)
                lineTo(17f, 14f)
                lineTo(17f, 16f)
                lineTo(15f, 16f)
                close()
            }
        }.build()
    }

    val ExercisesDumbbell: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExercisesDumbbell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke = SolidColor(Color.White)
            path(
                stroke = stroke,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Center bar
                moveTo(6.5f, 12f)
                lineTo(17.5f, 12f)
                // Inner plates
                moveTo(6.5f, 6.5f)
                lineTo(6.5f, 17.5f)
                moveTo(17.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                // Outer plates
                moveTo(3.5f, 8.5f)
                lineTo(3.5f, 15.5f)
                moveTo(20.5f, 8.5f)
                lineTo(20.5f, 15.5f)
                // End caps
                moveTo(2f, 12f)
                lineTo(3.5f, 12f)
                moveTo(20.5f, 12f)
                lineTo(22f, 12f)
            }
        }.build()
    }

    val StatsBars: ImageVector by lazy {
        ImageVector.Builder(
            name = "StatsBars",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke = SolidColor(Color.White)
            path(
                stroke = stroke,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Left short bar
                moveTo(6f, 20f)
                lineTo(6f, 14f)
                // Middle medium bar
                moveTo(12f, 20f)
                lineTo(12f, 10f)
                // Right tall bar
                moveTo(18f, 20f)
                lineTo(18f, 4f)
            }
        }.build()
    }
}

object SessionRoutes {
    const val ACTIVE_ARG = "sessionId"
    const val ACTIVE = "session/active/{$ACTIVE_ARG}"
    const val COMPLETE_ARG = "sessionId"
    const val COMPLETE = "session/complete/{$COMPLETE_ARG}"
    const val DETAIL_ARG = "sessionId"
    const val DETAIL = "session/detail/{$DETAIL_ARG}"

    fun active(sessionId: Long) = "session/active/$sessionId"
    fun complete(sessionId: Long) = "session/complete/$sessionId"
    fun detail(sessionId: Long) = "session/detail/$sessionId"
}

object TemplateRoutes {
    const val LIST = "template/list"
    const val EDIT_ARG = "templateId"
    /** Use 0 (or any non-positive) to indicate "create new". */
    const val EDIT = "template/edit/{$EDIT_ARG}"

    fun edit(templateId: Long) = "template/edit/$templateId"
    fun create() = "template/edit/0"
}

object ExerciseRoutes {
    const val DETAIL_ARG = "exerciseId"
    const val DETAIL = "exercise/detail/{$DETAIL_ARG}"

    fun detail(exerciseId: Long) = "exercise/detail/$exerciseId"
}
