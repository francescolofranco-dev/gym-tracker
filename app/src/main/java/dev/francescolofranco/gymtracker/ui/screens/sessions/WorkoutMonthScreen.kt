package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.R
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.domain.workoutDuration
import dev.francescolofranco.gymtracker.domain.workoutStartedAt
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.GymIcons
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import java.text.NumberFormat
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutMonthScreen(
    month: YearMonth,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: SessionsViewModel,
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }

    Scaffold(
        containerColor = SurfaceDeep,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(monthLabel) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(GymIcons.ArrowBack, stringResource(R.string.workout_history_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBase),
                )
                HorizontalDivider(color = EditorialBorder)
            }
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when (val current = content) {
            Loadable.Loading -> LoadingPane(body)
            is Loadable.Error -> ErrorPane(current.message, viewModel::retry, body)
            is Loadable.Ready -> {
                val zone = ZoneId.systemDefault()
                val past = current.value.past
                val workouts = remember(past, month, zone) { completedWorkoutsByMonth(past, zone)[month].orEmpty() }
                if (workouts.isEmpty()) {
                    Box(body.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.workout_history_empty_month),
                            style = MaterialTheme.typography.bodyLarge,
                            color = SlateSubtext,
                        )
                    }
                } else {
                    LazyColumn(modifier = body, contentPadding = PaddingValues(vertical = 16.dp)) {
                        item(key = "workout-count") {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.workout_history_workouts,
                                    workouts.size,
                                    workouts.size,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = SlateSubtext,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            )
                        }
                        itemsIndexed(workouts, key = { _, workout -> workout.session.id }) { index, workout ->
                            PastSessionRow(
                                summary = workout,
                                unit = current.value.unit,
                                isFirst = index == 0,
                                isLast = index == workouts.lastIndex,
                                onClick = { onOpenDetail(workout.session.id) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = GymMotion.ItemFadeIn,
                                    placementSpec = GymMotion.ItemPlacement,
                                    fadeOutSpec = GymMotion.ItemFadeOut,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastSessionRow(
    summary: SessionSummary,
    unit: WeightUnit,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = summary.session.workoutDuration()
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 4.dp else 0.dp,
        topEnd = if (isFirst) 4.dp else 0.dp,
        bottomStart = if (isLast) 4.dp else 0.dp,
        bottomEnd = if (isLast) 4.dp else 0.dp,
    )
    val convertedVolume = convertFromKg(summary.totalVolume, unit).roundToInt()
    val formattedVolumeNumber = remember(convertedVolume) {
        NumberFormat.getIntegerInstance(Locale.US).format(convertedVolume)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(SurfaceBase)
            .border(1.dp, EditorialBorder, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatSessionDate(summary.session.workoutStartedAt()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildList {
                        add("${summary.exerciseCount} exercise${if (summary.exerciseCount == 1) "" else "s"}")
                        add("${summary.setCount} set${if (summary.setCount == 1) "" else "s"}")
                        add(formatDuration(duration))
                    }.joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SlateSubtext,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = formattedVolumeNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = unit.label().uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MutedOlive,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = "TOTAL VOLUME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 0.72.sp,
                        ),
                        color = MutedOlive,
                    )
                }
                Icon(
                    imageVector = GymIcons.ChevronForward,
                    contentDescription = null,
                    tint = MutedOlive,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
