package dev.francescolofranco.gymtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
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
    var lastTop by remember { mutableStateOf(TopDestination.Sessions) }
    LaunchedEffect(selectedTop) {
        if (selectedTop != null) lastTop = selectedTop
    }

    val navigateToTop: (TopDestination) -> Unit = { destination ->
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val chromeEnter = fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
        expandVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing))
    val chromeExit = fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
        shrinkVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing))

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = selectedTop != null,
                enter = chromeEnter,
                exit = chromeExit,
            ) {
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
            AnimatedVisibility(
                visible = selectedTop != null,
                enter = fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                    expandVertically(
                        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                        expandFrom = Alignment.Bottom,
                    ),
                exit = fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                    shrinkVertically(
                        animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                        shrinkTowards = Alignment.Bottom,
                    ),
            ) {
                NavigationBar {
                    TopDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == lastTop,
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
                enterTransition = {
                    if (initialState.destination.route.isTopLevel() && targetState.destination.route.isTopLevel()) {
                        val direction = topLevelDirection(initialState.destination.route, targetState.destination.route)
                        fadeIn(
                            tween(GymMotion.Standard, delayMillis = 35, easing = GymMotion.EmphasizedEasing),
                        ) + slideInHorizontally(
                            tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        ) { width -> direction * width / 14 } + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        )
                    } else {
                        fadeIn(
                            tween(GymMotion.Standard, delayMillis = 45, easing = GymMotion.EmphasizedEasing),
                        ) + slideInHorizontally(
                            tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        ) { it / 7 } + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        )
                    }
                },
                exitTransition = {
                    if (initialState.destination.route.isTopLevel() && targetState.destination.route.isTopLevel()) {
                        val direction = topLevelDirection(initialState.destination.route, targetState.destination.route)
                        fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                            slideOutHorizontally(
                                tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                            ) { width -> -direction * width / 20 }
                    } else {
                        fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                            slideOutHorizontally(
                                tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                            ) { -it / 12 } + scaleOut(
                                targetScale = 0.99f,
                                animationSpec = tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing),
                            )
                    }
                },
                popEnterTransition = {
                    fadeIn(tween(GymMotion.Standard, delayMillis = 35, easing = GymMotion.EmphasizedEasing)) +
                        slideInHorizontally(
                            tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        ) { -it / 9 } + scaleIn(
                            initialScale = 0.99f,
                            animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        )
                },
                popExitTransition = {
                    fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                        slideOutHorizontally(
                            tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        ) { it / 7 } + scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
                        )
                },
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

private fun String?.isTopLevel(): Boolean = TopDestination.entries.any { it.route == this }

private fun topLevelDirection(initial: String?, target: String?): Int {
    val from = TopDestination.entries.indexOfFirst { it.route == initial }
    val to = TopDestination.entries.indexOfFirst { it.route == target }
    return if (from >= 0 && to >= 0 && to < from) -1 else 1
}
