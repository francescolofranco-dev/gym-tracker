package dev.francescolofranco.gymtracker.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.francescolofranco.gymtracker.ui.components.AppLogo
import dev.francescolofranco.gymtracker.ui.components.LogoVariant
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.nav.ExerciseRoutes
import dev.francescolofranco.gymtracker.ui.nav.SessionRoutes
import dev.francescolofranco.gymtracker.ui.nav.TemplateRoutes
import dev.francescolofranco.gymtracker.ui.nav.TopDestination
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExerciseDetailScreen
import dev.francescolofranco.gymtracker.ui.screens.exercises.ExercisesScreen
import dev.francescolofranco.gymtracker.ui.screens.restore.DriveRestorePrompt
import dev.francescolofranco.gymtracker.ui.screens.sessions.ActiveSessionScreen
import dev.francescolofranco.gymtracker.ui.screens.sessions.SessionCompletedScreen
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
    val fadeThroughEnter = fadeIn(
        animationSpec = tween(
            durationMillis = 100,
            delayMillis = 80,
            easing = GymMotion.EmphasizedEasing,
        ),
    )
    val fadeThroughExit = fadeOut(
        animationSpec = tween(
            durationMillis = 80,
            easing = GymMotion.ExitEasing,
        ),
    )

    val navigateToTop: (TopDestination) -> Unit = { destination ->
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
            NavHost(
                navController = nav,
                startDestination = TopDestination.Sessions.route,
                modifier = Modifier.fillMaxSize(),
                // Sequential fade-through: the outgoing destination reaches alpha 0 before the
                // incoming one starts. The two layouts are never visible at the same time.
                enterTransition = { fadeThroughEnter },
                exitTransition = { fadeThroughExit },
                popEnterTransition = { fadeThroughEnter },
                popExitTransition = { fadeThroughExit },
            ) {
                composable(TopDestination.Sessions.route) {
                    TopLevelScaffold(TopDestination.Sessions, navigateToTop) {
                        SessionsScreen(
                            onOpenActive = { id -> nav.navigate(SessionRoutes.active(id)) },
                            onOpenCompleted = { id -> nav.navigate(SessionRoutes.complete(id)) },
                            onOpenDetail = { id -> nav.navigate(SessionRoutes.detail(id)) },
                            onOpenTemplates = { nav.navigate(TemplateRoutes.LIST) },
                        )
                    }
                }
                composable(TopDestination.Exercises.route) {
                    TopLevelScaffold(TopDestination.Exercises, navigateToTop) {
                        ExercisesScreen(onOpenDetail = { id -> nav.navigate(ExerciseRoutes.detail(id)) })
                    }
                }
                composable(TopDestination.Stats.route) {
                    TopLevelScaffold(TopDestination.Stats, navigateToTop) {
                        StatsScreen()
                    }
                }
                composable(TopDestination.Settings.route) {
                    TopLevelScaffold(TopDestination.Settings, navigateToTop) {
                        SettingsScreen(onOpenTemplates = { nav.navigate(TemplateRoutes.LIST) })
                    }
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
                        onCompleted = { sessionId ->
                            nav.navigate(SessionRoutes.complete(sessionId)) {
                                popUpTo(SessionRoutes.ACTIVE) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenExerciseStats = { exerciseId -> nav.navigate(ExerciseRoutes.detail(exerciseId)) },
                    )
                }
                composable(
                    route = SessionRoutes.COMPLETE,
                    arguments = listOf(navArgument(SessionRoutes.COMPLETE_ARG) { type = NavType.LongType }),
                ) {
                    SessionCompletedScreen(onDone = { nav.popBackStack() })
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

    DriveRestorePrompt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopLevelScaffold(
    selected: TopDestination,
    onNavigate: (TopDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = SurfaceBase,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceBase)
                    .statusBarsPadding(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    AppLogo(
                        size = 36.dp,
                        variant = LogoVariant.FullLockup,
                        wordmark = stringResource(dev.francescolofranco.gymtracker.R.string.app_name),
                        showContainer = true,
                    )
                }
                HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDeep)
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(color = EditorialBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    TopDestination.entries.forEach { destination ->
                        val isSelected = destination == selected
                        val itemColor = if (isSelected) BrandLime else MutedOlive
                        val label = stringResource(destination.labelRes)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics { this.selected = isSelected }
                                .clickable(
                                    role = Role.Tab,
                                    onClick = { onNavigate(destination) },
                                ),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = label,
                                    tint = itemColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.55.sp,
                                    ),
                                    color = itemColor,
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) BrandLime else androidx.compose.ui.graphics.Color.Transparent),
                                )
                            }
                        }
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
            content()
        }
    }
}
