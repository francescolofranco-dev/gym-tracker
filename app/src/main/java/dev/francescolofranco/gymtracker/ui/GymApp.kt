package dev.francescolofranco.gymtracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymApp() {
    val nav = rememberNavController()
    val currentEntry by nav.currentBackStackEntryAsState()
    val selectedTop = TopDestination.entries.firstOrNull { it.route == currentEntry?.destination?.route }

    val navigateToTop: (TopDestination) -> Unit = { destination ->
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(
        topBar = {
            if (selectedTop != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(dev.francescolofranco.gymtracker.R.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        },
        bottomBar = {
            if (selectedTop != null) {
                NavigationBar {
                    TopDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == selectedTop,
                            onClick = { navigateToTop(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            NavHost(
                navController = nav,
                startDestination = TopDestination.Sessions.route,
                modifier = Modifier.fillMaxSize(),
                // Never render two complete destinations in the same frame. The session and
                // stats screens are intentionally dense; overlapping them during a transition
                // is more expensive than an instant Material-style destination change.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
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
                        onExit = { nav.popBackStack() },
                        onOpenExerciseStats = { exerciseId -> nav.navigate(ExerciseRoutes.detail(exerciseId)) },
                    )
                }
                composable(
                    route = SessionRoutes.DETAIL,
                    arguments = listOf(navArgument(SessionRoutes.DETAIL_ARG) { type = NavType.LongType }),
                ) {
                    SessionDetailScreen(
                        onBack = { nav.popBackStack() },
                        onOpenExerciseStats = { exerciseId -> nav.navigate(ExerciseRoutes.detail(exerciseId)) },
                    )
                }
            }
        }
    }

    DriveRestorePrompt()
}
