package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.francescolofranco.gymtracker.R
import dev.francescolofranco.gymtracker.data.db.projections.SessionSummary
import dev.francescolofranco.gymtracker.ui.components.GymIcons
import dev.francescolofranco.gymtracker.ui.theme.BrandLime
import dev.francescolofranco.gymtracker.ui.theme.EditorialBorder
import dev.francescolofranco.gymtracker.ui.theme.MutedOlive
import dev.francescolofranco.gymtracker.ui.theme.SlateSubtext
import dev.francescolofranco.gymtracker.ui.theme.SurfaceBase
import dev.francescolofranco.gymtracker.ui.theme.SurfaceChip
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Composable
internal fun WorkoutHistoryCalendar(
    summaries: List<SessionSummary>,
    onOpenMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    weeklyProgress: @Composable () -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LifecycleResumeEffect(Unit) {
        now = Instant.now()
        onPauseOrDispose { }
    }
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()
    LaunchedEffect(now, zone) {
        val nextDay = today.plusDays(1).atStartOfDay(zone).toInstant()
        delay(Duration.between(Instant.now(), nextDay).toMillis().coerceAtLeast(1))
        now = Instant.now()
    }

    val history = remember(summaries, zone) { completedWorkoutsByMonth(summaries, zone) }
    val currentMonth = YearMonth.from(today)
    val monthRows = remember(currentMonth, history) {
        workoutHistoryMonths(currentMonth, history.keys).chunked(2)
    }
    val futureMonthRows = remember(currentMonth, history) {
        history.keys.filter { it > currentMonth }.sortedDescending().chunked(2)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "weekly-progress") { weeklyProgress() }
        item(key = "workout-history-heading") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.workout_history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                WorkoutSetLegend()
            }
        }
        workoutMonthRows(monthRows, history, today, zone, onOpenMonth)
        if (futureMonthRows.isNotEmpty()) {
            item(key = "future-workouts-heading") {
                Text(
                    text = stringResource(R.string.workout_history_future),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics { heading() },
                )
            }
            workoutMonthRows(futureMonthRows, history, today, zone, onOpenMonth)
        }
    }
}

private fun LazyListScope.workoutMonthRows(
    rows: List<List<YearMonth>>,
    history: Map<YearMonth, List<SessionSummary>>,
    today: LocalDate,
    zone: ZoneId,
    onOpenMonth: (YearMonth) -> Unit,
) {
    items(rows, key = { it.first().toString() }) { months ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            months.forEach { month ->
                WorkoutMonthCard(
                    month = month,
                    workouts = history[month].orEmpty(),
                    today = today,
                    zone = zone,
                    onClick = { onOpenMonth(month) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (months.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkoutSetLegend() {
    val description = stringResource(R.string.workout_history_set_brightness, BRIGHTEST_DAY_SETS)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.workout_history_logged_sets),
            color = SlateSubtext,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 4.dp),
        )
        Text("0", color = MutedOlive, style = MaterialTheme.typography.labelSmall)
        for (sets in 0..BRIGHTEST_DAY_SETS step 5) {
            Box(
                Modifier.size(9.dp).background(
                    BrandLime.copy(alpha = loggedSetsBrightness(sets.toLong())),
                    RoundedCornerShape(2.dp),
                ),
            )
        }
        Text(
            text = stringResource(R.string.workout_history_sets_or_more, BRIGHTEST_DAY_SETS),
            color = SlateSubtext,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun WorkoutMonthCard(
    month: YearMonth,
    workouts: List<SessionSummary>,
    today: LocalDate,
    zone: ZoneId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val days = remember(month, workouts, zone, firstDayOfWeek) {
        workoutCalendarDays(month, workouts, zone, firstDayOfWeek)
    }
    val shortMonthLabel = month.format(DateTimeFormatter.ofPattern("LLL yyyy", locale))
    val monthLabel = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    val currentMonth = month == YearMonth.from(today)
    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(SurfaceBase)
            .border(1.dp, if (currentMonth) MutedOlive else EditorialBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shortMonthLabel.replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleMedium,
                color = if (currentMonth) BrandLime else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = monthLabel },
            )
            Icon(GymIcons.ChevronForward, null, Modifier.size(16.dp), tint = MutedOlive)
        }
        Canvas(
            Modifier
                .widthIn(max = 112.dp)
                .fillMaxWidth()
                .aspectRatio(7f / 6f),
        ) {
            val gap = 3.dp.toPx()
            val cell = ((size.width - 6 * gap) / 7).coerceAtLeast(0f)
            val corner = CornerRadius(2.dp.toPx())
            days.forEachIndexed { index, day ->
                if (day == null) return@forEachIndexed
                val position = Offset(index % 7 * (cell + gap), index / 7 * (cell + gap))
                val color = when {
                    day.loggedSets != null -> BrandLime.copy(alpha = loggedSetsBrightness(day.loggedSets))
                    day.date.isAfter(today) -> SurfaceChip.copy(alpha = 0.4f)
                    else -> SurfaceChip
                }
                drawRoundRect(color, position, Size(cell, cell), corner)
                if (day.date == today) {
                    drawRoundRect(
                        color = SlateSubtext,
                        topLeft = position,
                        size = Size(cell, cell),
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }
        Text(
            text = pluralStringResource(R.plurals.workout_history_workouts, workouts.size, workouts.size),
            color = if (workouts.isEmpty()) MutedOlive else SlateSubtext,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
