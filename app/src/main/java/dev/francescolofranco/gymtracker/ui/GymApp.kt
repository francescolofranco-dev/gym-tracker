package dev.francescolofranco.gymtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import dev.francescolofranco.gymtracker.ui.nav.TopDestination
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExercisesScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.ActiveSessionScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionDetailScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionsScreen
import dev.francescolofranco.gymtracker.ui.screens.settings.SettingsScreen
import dev.francescolofranco.gymtracker.ui.screens.stats.StatsScreen

@Composable
fun GymApp() {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val current = backstack?.destination
    val onTopLevel = current?.route in TopDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (onTopLevel) {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        val selected = current?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(dest.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopDestination.Sessions.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDestination.Sessions.route) {
                SessionsScreen(
                    onOpenActive = { id -> nav.navigate(SessionRoutes.active(id)) },
                    onOpenDetail = { id -> nav.navigate(SessionRoutes.detail(id)) },
                )
            }
            composable(TopDestination.Exercises.route) { ExercisesScreen() }
            composable(TopDestination.Stats.route) { StatsScreen() }
            composable(TopDestination.Settings.route) { SettingsScreen() }

            composable(
                route = SessionRoutes.ACTIVE,
                arguments = listOf(navArgument(SessionRoutes.ACTIVE_ARG) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(SessionRoutes.ACTIVE_ARG) ?: return@composable
                ActiveSessionScreen(
                    sessionId = id,
                    onExit = { nav.popBackStack() },
                )
            }

            composable(
                route = SessionRoutes.DETAIL,
                arguments = listOf(navArgument(SessionRoutes.DETAIL_ARG) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(SessionRoutes.DETAIL_ARG) ?: return@composable
                SessionDetailScreen(
                    sessionId = id,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
