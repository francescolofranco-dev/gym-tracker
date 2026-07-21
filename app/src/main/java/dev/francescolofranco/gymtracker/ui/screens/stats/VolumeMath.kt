package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.ui.graphics.Color
import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import dev.francescolofranco.gymtracker.data.db.projections.StatSetRow
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.domain.ExerciseSide
import dev.francescolofranco.gymtracker.ui.screens.exercises.aggregateSessions
import dev.francescolofranco.gymtracker.ui.screens.exercises.detectPersonalRecords
import dev.francescolofranco.gymtracker.ui.screens.exercises.epley1Rm
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import dev.francescolofranco.gymtracker.ui.theme.VolumeGrey
import dev.francescolofranco.gymtracker.ui.theme.VolumeRed
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class DateRange(val startInclusive: Instant, val endExclusive: Instant)

enum class StatsPeriod(val label: String, val days: Long) {
    DAYS_7("7d", 7),
    DAYS_28("28d", 28),
    DAYS_90("90d", 90),
}

fun periodRange(period: StatsPeriod, now: Instant = Instant.now()): DateRange = DateRange(
    startInclusive = now.minusSeconds(period.days * 24 * 3600L),
    endExclusive = now,
)

/**
 * Indirect (secondary-mover) contributions weight at this factor. Industry convention
 * (RP Strength, Renaissance Periodization) treats a secondary set as ~half a working set
 * for hypertrophy purposes — and the same scaling applies to attributed tonnage. So a
 * bench-press set worth 80 kg × 10 contributes 800 kg to chest, 400 kg to triceps, and
 * 400 kg to front delts, rather than 800/800/800 (which over-counted indirect work).
 */
const val INDIRECT_WEIGHT = 0.5

data class MuscleVolume(
    val muscle: Muscle,
    val directSets: Double,
    val indirectSets: Double,
    val directVolumeKg: Double,
    val indirectVolumeKg: Double,
    val contributingExercises: List<ContributingExercise>,
) {
    /**
     * Effective working sets for this muscle = direct + INDIRECT_WEIGHT × indirect. Used
     * by the body-diagram traffic-light colouring and the drill-down "Total" block. Direct
     * and indirect are kept as the raw integers so the drill sheet can show both alongside.
     */
    val effectiveSets: Double get() = directSets + INDIRECT_WEIGHT * indirectSets

    /** Rounded effective sets — for thresholds / displayed totals. */
    val total: Double get() = effectiveSets

    /**
     * Per-muscle attributed tonnage retained for data-level analysis. It is deliberately not a
     * headline UI metric because compound lifts can attribute the same load to several muscles.
     */
    val totalVolumeKg: Double get() = directVolumeKg + INDIRECT_WEIGHT * indirectVolumeKg
}

data class ContributingExercise(
    val exerciseId: Long,
    val name: String,
    val sets: Double,
    val isPrimary: Boolean,
)

fun weekRange(mode: WeekMode, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    return when (mode) {
        WeekMode.ROLLING_7 -> DateRange(
            startInclusive = now.minusSeconds(7 * 24 * 3600L),
            endExclusive = now,
        )
        WeekMode.MON_SUN -> {
            val today = now.atZone(zone).toLocalDate()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val nextMonday = monday.plusDays(7)
            DateRange(
                startInclusive = monday.atStartOfDay(zone).toInstant(),
                endExclusive = nextMonday.atStartOfDay(zone).toInstant(),
            )
        }
    }
}

/**
 * The period immediately preceding [current], same length. For rolling 7d this is the prior
 * 7-day window; for Mon-Sun it's the previous Mon-Sun. Used for week-over-week deltas.
 */
fun previousOf(current: DateRange): DateRange {
    val span = Duration.between(current.startInclusive, current.endExclusive)
    return DateRange(
        startInclusive = current.startInclusive.minus(span),
        endExclusive = current.startInclusive,
    )
}

fun computeMuscleVolumes(rows: List<StatSetRow>): Map<Muscle, MuscleVolume> {
    val directSets = HashMap<Muscle, Double>()
    val indirectSets = HashMap<Muscle, Double>()
    val directVol = HashMap<Muscle, Double>()
    val indirectVol = HashMap<Muscle, Double>()
    val byMuscleByExercise = HashMap<Muscle, HashMap<Long, ExerciseAccumulator>>()

    rows.forEach { r ->
        val setVol = (r.kg ?: 0.0) * r.reps
        // Left/right logs are each half of one bilateral-equivalent working set. Their tonnage
        // remains additive: 10 kg × 10 on each arm really is 200 kg of external work.
        // Side is stored on the historical set itself, so later editing the exercise's
        // unilateral toggle cannot retroactively double or halve old statistics.
        val setCredit = if (r.side != ExerciseSide.BOTH) 0.5 else 1.0

        // Each primary mover gets full credit for the set. A Bulgarian split squat with
        // {Quads, Glutes, Hamstrings} as primaries contributes +1 direct set to each of the
        // three muscles for a single working set.
        r.primaryMuscles.forEach { m ->
            directSets.merge(m, setCredit, Double::plus)
            directVol.merge(m, setVol, Double::plus)
            byMuscleByExercise.getOrPut(m) { HashMap() }
                .getOrPut(r.exerciseId) { ExerciseAccumulator(r.exerciseId, r.exerciseName, isPrimary = true) }
                .also { it.sets += setCredit }
        }

        r.secondaryMuscles.forEach { m ->
            if (m in r.primaryMuscles) return@forEach
            indirectSets.merge(m, setCredit, Double::plus)
            indirectVol.merge(m, setVol, Double::plus)
            byMuscleByExercise.getOrPut(m) { HashMap() }
                .getOrPut(r.exerciseId) { ExerciseAccumulator(r.exerciseId, r.exerciseName, isPrimary = false) }
                .also { it.sets += setCredit }
        }
    }

    return Muscle.entries.associateWith { m ->
        val exMap = byMuscleByExercise[m].orEmpty()
        val contrib = exMap.values
            .map { ContributingExercise(it.exerciseId, it.name, it.sets, it.isPrimary) }
            .sortedWith(compareByDescending<ContributingExercise> { it.isPrimary }.thenByDescending { it.sets })
        MuscleVolume(
            muscle = m,
            directSets = directSets[m] ?: 0.0,
            indirectSets = indirectSets[m] ?: 0.0,
            directVolumeKg = directVol[m] ?: 0.0,
            indirectVolumeKg = indirectVol[m] ?: 0.0,
            contributingExercises = contrib,
        )
    }
}

private data class ExerciseAccumulator(val exerciseId: Long, val name: String, val isPrimary: Boolean) {
    var sets: Double = 0.0
}

/** The 3-10 traffic-light: 0 grey, 1-2 blue (under), 3-10 green (in range), 11+ red (over). */
fun volumeColor(total: Double, periodDays: Long = 7): Color {
    val scale = periodDays / 7.0
    val low = 2 * scale
    val high = Muscle.WEEKLY_MAX * scale
    return when {
    total <= 0 -> VolumeGrey
    total <= low -> VolumeBlue
    total <= high -> VolumeGreen
    else -> VolumeRed
    }
}

/** Normalizes any selected range to an average seven-day workload. */
fun weeklyAverage(total: Double, period: StatsPeriod): Double = total * 7.0 / period.days

data class MuscleChange(
    val muscle: Muscle,
    val currentWeeklySets: Double,
    val previousWeeklySets: Double,
) {
    val deltaWeeklySets: Double get() = currentWeeklySets - previousWeeklySets
    val percentChange: Double?
        get() = previousWeeklySets.takeIf { it > 0.0 }
            ?.let { deltaWeeklySets / it * 100.0 }
}

/** Workload changes large enough to be useful rather than normal set-to-set noise. */
fun meaningfulMuscleChanges(
    current: Map<Muscle, MuscleVolume>,
    previous: Map<Muscle, MuscleVolume>,
    period: StatsPeriod,
): List<MuscleChange> = Muscle.entries.mapNotNull { muscle ->
    val currentWeekly = weeklyAverage(current[muscle]?.effectiveSets ?: 0.0, period)
    val previousWeekly = weeklyAverage(previous[muscle]?.effectiveSets ?: 0.0, period)
    val change = MuscleChange(muscle, currentWeekly, previousWeekly)
    val percent = change.percentChange
    change.takeIf {
        previousWeekly >= 1.0 &&
            abs(change.deltaWeeklySets) >= 0.5 &&
            percent != null && abs(percent) >= 15.0
    }
}

data class ExerciseProgressSignal(
    val exerciseId: Long,
    val name: String,
    val isBodyweight: Boolean,
    val currentValue: Double,
    val percentChange: Double?,
)

/** Best estimated 1RM (or bodyweight reps) compared with the preceding equal-length window. */
fun computeExerciseProgress(
    currentRows: List<StatSetRow>,
    previousRows: List<StatSetRow>,
): List<ExerciseProgressSignal> {
    val previousByExercise = previousRows.groupBy { it.exerciseId }
    return currentRows.groupBy { it.exerciseId }.mapNotNull { (exerciseId, rows) ->
        val first = rows.firstOrNull() ?: return@mapNotNull null
        val currentValue = headlineValue(rows, first.isBodyweight) ?: return@mapNotNull null
        val previousValue = headlineValue(previousByExercise[exerciseId].orEmpty(), first.isBodyweight)
        ExerciseProgressSignal(
            exerciseId = exerciseId,
            name = first.exerciseName,
            isBodyweight = first.isBodyweight,
            currentValue = currentValue,
            percentChange = previousValue?.takeIf { it > 0.0 }
                ?.let { (currentValue - it) / it * 100.0 },
        )
    }.sortedWith(
        compareByDescending<ExerciseProgressSignal> { it.percentChange != null }
            .thenByDescending { abs(it.percentChange ?: 0.0) }
            .thenBy { it.name.lowercase() },
    )
}

private fun headlineValue(rows: List<StatSetRow>, isBodyweight: Boolean): Double? =
    if (isBodyweight) {
        rows.maxOfOrNull { it.reps.toDouble() }
    } else {
        rows.mapNotNull { row -> row.kg?.takeIf { it > 0.0 }?.let { epley1Rm(it, row.reps) } }.maxOrNull()
    }

data class PersonalRecordActivity(val count: Int, val exerciseIds: Set<Long>)

/** All-time PR events whose session falls inside the selected current period. */
fun personalRecordActivity(allRows: List<StatSetRow>, current: DateRange): PersonalRecordActivity {
    var count = 0
    val exerciseIds = linkedSetOf<Long>()
    allRows.groupBy { it.exerciseId }.forEach { (exerciseId, rows) ->
        val points = aggregateSessions(rows.map(StatSetRow::toExerciseSetRow))
        val records = detectPersonalRecords(points, rows.first().isBodyweight)
        points.forEach { point ->
            if (!point.startedAt.isBefore(current.startInclusive) && point.startedAt.isBefore(current.endExclusive)) {
                val earned = records[point.sessionId].orEmpty()
                count += earned.size
                if (earned.isNotEmpty()) exerciseIds += exerciseId
            }
        }
    }
    return PersonalRecordActivity(count, exerciseIds)
}

private fun StatSetRow.toExerciseSetRow() = ExerciseSetRow(
    sessionId = sessionId,
    sessionStartedAt = sessionStartedAt,
    reps = reps,
    kg = kg,
    setNumber = setNumber,
    side = side,
)
