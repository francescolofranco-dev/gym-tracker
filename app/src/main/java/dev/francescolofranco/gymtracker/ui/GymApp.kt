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
import dev.francescolofranco.gymtracker.ui.nav.ExerciseRoutes
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import dev.francescolofranco.gymtracker.ui.nav.TemplateRoutes
import dev.francescolofranco.gymtracker.ui.nav.TopDestination
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExerciseDetailScreen
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExercisesScreen
import dev.francescolofranco.gymtracker.ui.screens.restore.DriveRestorePrompt
import dev.francescolofranco.gymtracker.ui.screens.sessions.ActiveSessionScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionDetailScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionsScreen
import dev.francescolofranco.gymtracker.ui.screens.settings.SettingsScreen
import dev.francescolofranco.gymtracker.ui.screens.stats.StatsScreen
import dev.francescolofranco.gymtracker.ui.screens.templates.TemplateEditScreen
import dev.francescolofranco.gymtracker.ui.screens.templates.TemplatesListScreen

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
            composable(TopDestination.Exercises.route) {
                ExercisesScreen(onOpenDetail = { id -> nav.navigate(ExerciseRoutes.detail(id)) })
            }
            composable(TopDestination.Stats.route) { StatsScreen() }
            composable(TopDestination.Settings.route) {
                SettingsScreen(onOpenTemplates = { nav.navigate(TemplateRoutes.LIST) })
            }

            composable(TemplateRoutes.LIST) {
                TemplatesListScreen(
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate(TemplateRoutes.create()) },
                    onEdit = { id -> nav.navigate(TemplateRoutes.edit(id)) },
                )
            }
            composable(
                route = TemplateRoutes.EDIT,
                arguments = listOf(navArgument(TemplateRoutes.EDIT_ARG) { type = NavType.LongType }),
            ) {
                TemplateEditScreen(onBack = { nav.popBackStack() })
            }

            composable(
                route = ExerciseRoutes.DETAIL,
                arguments = listOf(navArgument(ExerciseRoutes.DETAIL_ARG) { type = NavType.LongType }),
            ) {
                ExerciseDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { id -> nav.navigate(SessionRoutes.detail(id)) },
                )
            }

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

    // One-shot fresh-install restore offer; checks DB emptiness + Drive sign-in once and
    // marks the offer consumed afterwards.
    DriveRestorePrompt()
}
