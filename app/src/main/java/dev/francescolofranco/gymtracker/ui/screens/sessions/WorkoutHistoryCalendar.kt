package dev.francescolofranco.gymtracker.ui.screens.sessions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.time.format.TextStyle
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
    val firstYear = history.keys.minOfOrNull { it.year }?.coerceAtMost(today.year) ?: today.year
    val lastYear = history.keys.maxOfOrNull { it.year }?.coerceAtLeast(today.year) ?: today.year
    var selectedYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val year = selectedYear?.coerceIn(firstYear, lastYear) ?: today.year

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "weekly-progress") { weeklyProgress() }
        item(key = "workout-history-heading") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.workout_history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = { onOpenMonth(YearMonth.from(today)) }) {
                    Text(stringResource(R.string.workout_history_this_month))
                }
            }
        }
        item(key = "workout-history-year") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { selectedYear = year - 1 },
                    enabled = year > firstYear,
                ) {
                    Icon(GymIcons.ArrowBack, stringResource(R.string.workout_history_previous_year))
                }
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                IconButton(
                    onClick = { selectedYear = if (year + 1 == today.year) null else year + 1 },
                    enabled = year < lastYear,
                ) {
                    Icon(GymIcons.ArrowForward, stringResource(R.string.workout_history_next_year))
                }
            }
        }
        items(count = 6, key = { "workout-months-$it" }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(2) { column ->
                    val month = YearMonth.of(year, row * 2 + column + 1)
                    WorkoutMonthCard(
                        month = month,
                        workouts = history[month].orEmpty(),
                        today = today,
                        zone = zone,
                        onClick = { onOpenMonth(month) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
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
    val monthName = month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
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
                text = monthName.replaceFirstChar { it.titlecase(locale) },
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
                    day.workoutCount > 1 -> BrandLime
                    day.workoutCount == 1 -> BrandLime.copy(alpha = 0.64f)
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
