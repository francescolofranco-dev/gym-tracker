package dev.francescolofranco.gymtracker.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector
import dev.francescolofranco.gymtracker.ui.components.GymIcons
import java.time.YearMonth

enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    Sessions("sessions", dev.francescolofranco.gymtracker.R.string.tab_sessions, GymIcons.Calendar),
    Exercises("exercises", dev.francescolofranco.gymtracker.R.string.tab_exercises, GymIcons.Dumbbell),
    Stats("stats", dev.francescolofranco.gymtracker.R.string.tab_stats, GymIcons.BarChart),
    Settings("settings", dev.francescolofranco.gymtracker.R.string.tab_settings, GymIcons.Settings)
}

object SessionRoutes {
    const val ACTIVE_ARG = "sessionId"
    const val ACTIVE = "session/active/{$ACTIVE_ARG}"
    const val COMPLETE_ARG = "sessionId"
    const val COMPLETE = "session/complete/{$COMPLETE_ARG}"
    const val DETAIL_ARG = "sessionId"
    const val DETAIL = "session/detail/{$DETAIL_ARG}"
    const val HISTORY_MONTH_ARG = "month"
    const val HISTORY_MONTH = "session/history/{$HISTORY_MONTH_ARG}"

    fun active(sessionId: Long) = "session/active/$sessionId"
    fun complete(sessionId: Long) = "session/complete/$sessionId"
    fun detail(sessionId: Long) = "session/detail/$sessionId"
    fun historyMonth(month: YearMonth) = "session/history/$month"
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
