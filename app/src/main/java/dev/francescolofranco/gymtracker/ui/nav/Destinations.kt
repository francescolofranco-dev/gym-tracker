package dev.francescolofranco.gymtracker.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    Sessions("sessions", dev.francescolofranco.gymtracker.R.string.tab_sessions, Icons.Filled.PlayArrow),
    Exercises("exercises", dev.francescolofranco.gymtracker.R.string.tab_exercises, Icons.Filled.FitnessCenter),
    Stats("stats", dev.francescolofranco.gymtracker.R.string.tab_stats, Icons.Filled.BarChart),
    Settings("settings", dev.francescolofranco.gymtracker.R.string.tab_settings, Icons.Filled.Settings)
}

object SessionRoutes {
    const val ACTIVE_ARG = "sessionId"
    const val ACTIVE = "session/active/{$ACTIVE_ARG}"
    const val DETAIL_ARG = "sessionId"
    const val DETAIL = "session/detail/{$DETAIL_ARG}"

    fun active(sessionId: Long) = "session/active/$sessionId"
    fun detail(sessionId: Long) = "session/detail/$sessionId"
}
