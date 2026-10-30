package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeightUnit
import dev.francescolofranco.gymtracker.ui.components.ErrorPane
import dev.francescolofranco.gymtracker.ui.components.Loadable
import dev.francescolofranco.gymtracker.ui.components.LoadingPane
import dev.francescolofranco.gymtracker.ui.motion.GymMotion
import dev.francescolofranco.gymtracker.ui.screens.sessions.convertFromKg
import dev.francescolofranco.gymtracker.ui.screens.sessions.formatWeightNumber
import dev.francescolofranco.gymtracker.ui.screens.sessions.label
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.DarkOnSurface
import dev.francescolofranco.gymtracker.ui.theme.OliveSubtext
import dev.francescolofranco.gymtracker.ui.theme.OswaldFontFamily
import dev.francescolofranco.gymtracker.ui.theme.SpaceGroteskFontFamily
import dev.francescolofranco.gymtracker.ui.theme.SurfaceCard
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import dev.francescolofranco.gymtracker.ui.theme.SurfaceControl
import dev.francescolofranco.gymtracker.ui.theme.SurfaceDeep
import dev.francescolofranco.gymtracker.ui.theme.SurfaceElevated
import dev.francescolofranco.gymtracker.ui.theme.TagShape
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val loadableState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMuscle by viewModel.selectedMuscle.collectAsStateWithLifecycle()
    (loadableState as? Loadable.Error)?.let {
        ErrorPane(it.message, viewModel::retry)
        return
    }
    val state = (loadableState as? Loadable.Ready)?.value
    if (state == null) {
        LoadingPane()
        return
    }

    var showAllMuscles by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDeep),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "range") {
            HeaderControls(
                period = state.period,
                onPeriodChange = viewModel::setPeriod,
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "kpis") {
            KpiStrip(
                state,
                Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "changes") {
            WhatChangedCard(
                state,
                Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "coverage") {
            MuscleCoverageCard(
                state = state,
                showAll = showAllMuscles,
                onToggleShowAll = { showAllMuscles = !showAllMuscles },
                onMuscleClick = viewModel::selectMuscle,
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
        item(key = "progress") {
            ExerciseProgressCard(
                state,
                Modifier
                    .animateItem(
                        fadeInSpec = GymMotion.ItemFadeIn,
                        placementSpec = GymMotion.ItemPlacement,
                        fadeOutSpec = GymMotion.ItemFadeOut,
                    )
                    .padding(horizontal = 16.dp),
            )
        }
    }

    selectedMuscle?.let { muscle ->
        val volume = state.muscleVolumes[muscle] ?: return@let
        MuscleDrillSheet(
            volume = volume,
            previous = state.previousMuscleVolumes[muscle],
            period = state.period,
            onDismiss = { viewModel.selectMuscle(null) },
        )
    }
}

@Composable
private fun HeaderControls(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    PeriodToggle(period, onPeriodChange, modifier.fillMaxWidth())
}

@Composable
private fun PeriodToggle(
    period: StatsPeriod,
    onChange: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatsPeriod.entries.forEach { option ->
            val isSelected = period == option
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) SurfaceControl else Color.Transparent)
                    .clickable { onChange(option) }
                    .semantics {
                        role = Role.RadioButton
                        selected = isSelected
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(13.dp),
                    )
                }
                Text(
                    text = option.label.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = OswaldFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.65.sp,
                    ),
                    color = if (isSelected) BrandLime else OliveSubtext,
                )
            }
        }
    }
}

@Composable
private fun KpiStrip(state: StatsUiState, modifier: Modifier = Modifier) {
    val current = state.currentPeriod
    val previous = state.previousPeriod
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Kpi(
            label = "WORKOUTS",
            value = current.sessions.toString(),
            delta = signedNumber(current.sessions - previous.sessions),
            modifier = Modifier.weight(1f),
        )
        Kpi(
            label = "LOGGED SETS",
            value = current.loggedSets.toString(),
            delta = signedNumber(current.loggedSets - previous.loggedSets),
            highlighted = true,
            modifier = Modifier.weight(1f),
        )
        Kpi(
            label = "TRAINING TIME",
            value = formatTrainingTime(current.trainingTime.seconds),
            delta = signedDuration(current.trainingTime.seconds - previous.trainingTime.seconds),
            compactValue = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Kpi(
    label: String,
    value: String,
    delta: SignedValue,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    compactValue: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (highlighted) SurfaceElevated.copy(alpha = 0.4f) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) +
                    slideInVertically(tween(GymMotion.Standard, easing = GymMotion.EmphasizedEasing)) { it / 3 })
                    .togetherWith(
                        fadeOut(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) +
                            slideOutVertically(tween(GymMotion.Quick, easing = GymMotion.ExitEasing)) { -it / 3 },
                    )
            },
            label = "stat-$label",
        ) { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = if (compactValue) 24.sp else 32.sp,
                    fontWeight = if (compactValue) FontWeight.SemiBold else FontWeight.Bold,
                    letterSpacing = if (compactValue) 0.22.sp else 0.64.sp,
                ),
                color = DarkOnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            ),
            color = OliveSubtext,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        ChangeLabel(delta.text, delta.direction)
    }
}

@Composable
private fun WhatChangedCard(state: StatsUiState, modifier: Modifier = Modifier) {
    val insights = remember(state) { buildInsights(state) }
    StatsCard(modifier) {
        Text(
            text = "WHAT CHANGED",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = OswaldFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.45.sp,
            ),
            color = DarkOnSurface,
        )
        Column(
            modifier = Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            insights.forEach { insight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = insight.icon,
                        contentDescription = null,
                        tint = if (insight.emphasized) BrandLime else OliveSubtext,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = insight.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (insight.emphasized) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = if (insight.emphasized) DarkOnSurface else OliveSubtext,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun buildInsights(state: StatsUiState): List<Insight> {
    if (state.currentPeriod.sessions == 0) {
        return listOf(
            Insight(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                text = "No workouts logged in this period.",
                emphasized = false,
            ),
        )
    }
    val result = mutableListOf<Insight>()
    if (state.personalRecords.count > 0) {
        val suffix = if (state.personalRecords.count == 1) "record" else "records"
        result += Insight(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            text = "${state.personalRecords.count} personal $suffix",
            emphasized = true,
        )
    }
    state.muscleChanges.filter { it.deltaWeeklySets < 0 }.minByOrNull { it.deltaWeeklySets }?.let {
        result += Insight(
            icon = Icons.Filled.ArrowDownward,
            text = "${it.muscle.displayName} received ${abs(it.percentChange ?: 0.0).roundToInt()}% less work",
            emphasized = false,
        )
    }
    state.muscleChanges.filter { it.deltaWeeklySets > 0 }.maxByOrNull { it.deltaWeeklySets }?.let {
        result += Insight(
            icon = Icons.Filled.ArrowUpward,
            text = "${it.muscle.displayName} received ${abs(it.percentChange ?: 0.0).roundToInt()}% more work",
            emphasized = true,
        )
    }
    if (result.isEmpty()) {
        result += Insight(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            text = "Training stayed close to the previous ${state.period.days} days.",
            emphasized = false,
        )
    }
    return result.take(3)
}

@Composable
private fun MuscleCoverageCard(
    state: StatsUiState,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onMuscleClick: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(state.muscleVolumes, state.previousMuscleVolumes, state.period) {
        Muscle.entries.mapNotNull { muscle ->
            val current = state.muscleVolumes[muscle] ?: return@mapNotNull null
            val previous = state.previousMuscleVolumes[muscle]
            val currentWeekly = weeklyAverage(current.effectiveSets, state.period)
            val previousWeekly = weeklyAverage(previous?.effectiveSets ?: 0.0, state.period)
            CoverageRow(current, currentWeekly, previousWeekly)
                .takeIf { currentWeekly > 0.0 || previousWeekly > 0.0 }
        }.sortedByDescending { it.currentWeekly }
    }
    val visibleRows = if (showAll) rows else rows.take(6)

    StatsCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "MUSCLE COVERAGE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.45.sp,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = "AVERAGE EFFECTIVE SETS PER WEEK · TAP FOR DETAILS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.66.sp,
                ),
                color = OliveSubtext,
            )
        }
        if (rows.isEmpty()) {
            Text(
                text = "No completed sets in this period.",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGroteskFontFamily),
                color = OliveSubtext,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visibleRows.forEach { row ->
                    WeeklyMuscleRow(row, onClick = { onMuscleClick(row.volume.muscle) })
                }
            }
            if (rows.size > 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .clickable(onClick = onToggleShowAll)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (showAll) "SHOW LESS" else "SHOW ALL ${rows.size}",
                        color = BrandLime,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = OswaldFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.7.sp,
                        ),
                    )
                    Icon(
                        imageVector = if (showAll) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyMuscleRow(row: CoverageRow, onClick: () -> Unit) {
    val delta = row.currentWeekly - row.previousWeekly
    val fraction = (row.currentWeekly / Muscle.WEEKLY_MAX).coerceIn(0.0, 1.0).toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(GymMotion.Emphasized, easing = GymMotion.EmphasizedEasing),
        label = "muscle coverage",
    )
    val color = volumeColor(row.currentWeekly)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${row.volume.muscle.displayName}, ${formatSetCount(row.currentWeekly)} effective sets per week"
            },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = row.volume.muscle.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.15).sp,
                ),
                color = DarkOnSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${formatSetCount(row.currentWeekly)}/wk",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = OswaldFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.15).sp,
                    ),
                    color = DarkOnSurface,
                )
                if (abs(delta) >= 0.05) {
                    ChangeLabel(formatSignedSets(delta), direction(delta))
                } else {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.66.sp,
                        ),
                        color = OliveSubtext,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceChip),
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun ExerciseProgressCard(state: StatsUiState, modifier: Modifier = Modifier) {
    StatsCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EXERCISE PROGRESS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = OswaldFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.45.sp,
                    ),
                    color = DarkOnSurface,
                    modifier = Modifier.weight(1f),
                )
                if (state.personalRecords.count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(TagShape)
                            .background(BrandLime.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${state.personalRecords.count} PR${if (state.personalRecords.count == 1) "" else "S"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = BrandLime,
                        )
                    }
                }
            }
            Text(
                text = "BEST PERFORMANCE VS PREVIOUS ${state.period.days} DAYS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.66.sp,
                ),
                color = OliveSubtext,
            )
        }
        if (state.exerciseProgress.isEmpty()) {
            Text(
                text = "No comparable exercise performances yet.",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGroteskFontFamily),
                color = OliveSubtext,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.exerciseProgress.take(5).forEach { signal ->
                    ProgressRow(
                        signal = signal,
                        unit = state.unit,
                        hasPr = signal.exerciseId in state.personalRecords.exerciseIds,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(signal: ExerciseProgressSignal, unit: WeightUnit, hasPr: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = signal.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = DarkOnSurface,
            )
            Text(
                text = if (signal.isBodyweight) "BEST SET" else "ESTIMATED 1RM",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.66.sp,
                ),
                color = OliveSubtext,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = if (signal.isBodyweight) {
                    "${signal.currentValue.roundToInt()} reps"
                } else {
                    "${formatWeightNumber(convertFromKg(signal.currentValue, unit))} ${unit.label()}"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = OswaldFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.36.sp,
                ),
                color = BrandLime,
            )
            when {
                hasPr -> {
                    Text(
                        text = if (signal.percentChange != null) "${formatPercent(signal.percentChange)} (PR)" else "PR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.66.sp,
                        ),
                        color = OliveSubtext,
                    )
                }
                signal.percentChange != null -> {
                    ChangeLabel(
                        text = formatPercent(signal.percentChange),
                        direction = direction(signal.percentChange),
                    )
                }
                else -> {
                    Text(
                        text = "NEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.66.sp,
                        ),
                        color = OliveSubtext,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun ChangeLabel(text: String, direction: Direction, modifier: Modifier = Modifier) {
    val color = when (direction) {
        Direction.Up -> BrandLime
        Direction.Down -> OliveSubtext
        Direction.Flat -> OliveSubtext
    }
    val arrow = when (direction) {
        Direction.Up -> "▲"
        Direction.Down -> "▼"
        Direction.Flat -> "→"
    }
    Text(
        text = "$arrow $text",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.66.sp,
        ),
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

private data class Insight(val icon: ImageVector, val text: String, val emphasized: Boolean)
private data class CoverageRow(val volume: MuscleVolume, val currentWeekly: Double, val previousWeekly: Double)
private data class SignedValue(val text: String, val direction: Direction)
private enum class Direction { Up, Down, Flat }

private fun direction(value: Int): Direction = direction(value.toDouble())
private fun direction(value: Long): Direction = direction(value.toDouble())
private fun direction(value: Double): Direction = when {
    value > 0 -> Direction.Up
    value < 0 -> Direction.Down
    else -> Direction.Flat
}

private fun signedNumber(value: Int): SignedValue = SignedValue(
    text = when {
        value > 0 -> "+$value"
        value < 0 -> value.toString()
        else -> "0"
    },
    direction = direction(value),
)

private fun signedDuration(seconds: Long): SignedValue = SignedValue(
    text = when {
        seconds > 0 -> "+${formatTrainingTime(seconds)}"
        seconds < 0 -> "−${formatTrainingTime(abs(seconds))}"
        else -> "0m"
    },
    direction = direction(seconds),
)

private fun formatTrainingTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun formatSignedSets(value: Double): String =
    if (value > 0) "+${formatSetCount(value)}" else "−${formatSetCount(abs(value))}"

private fun formatPercent(value: Double): String {
    val rounded = abs(value).roundToInt()
    return if (value > 0) "+$rounded%" else if (value < 0) "−$rounded%" else "0%"
}

internal fun formatSetCount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
