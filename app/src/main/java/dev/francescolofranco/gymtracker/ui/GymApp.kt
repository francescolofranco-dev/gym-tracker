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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.francescolofranco.gymtracker.ui.nav.TopDestination
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExercisesScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionsScreen
import dev.francescolofranco.gymtracker.ui.screens.settings.SettingsScreen
import dev.francescolofranco.gymtracker.ui.screens.stats.StatsScreen

@Composable
fun GymApp() {
    val nav = rememberNavController()
    val backstack by nav.currentBackStackEntryAsState()
    val current = backstack?.destination

    Scaffold(
        bottomBar = {
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
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopDestination.Sessions.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDestination.Sessions.route) { SessionsScreen() }
            composable(TopDestination.Exercises.route) { ExercisesScreen() }
            composable(TopDestination.Stats.route) { StatsScreen() }
            composable(TopDestination.Settings.route) { SettingsScreen() }
        }
    }
}
