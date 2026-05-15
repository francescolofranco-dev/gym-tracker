package dev.francescolofranco.gymtracker.ui.screens.stats

import androidx.compose.ui.graphics.Color
import dev.francescolofranco.gymtracker.data.db.projections.StatSetRow
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeekMode
import dev.francescolofranco.gymtracker.ui.theme.VolumeBlue
import dev.francescolofranco.gymtracker.ui.theme.VolumeGreen
import dev.francescolofranco.gymtracker.ui.theme.VolumeGrey
import dev.francescolofranco.gymtracker.ui.theme.VolumeRed
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class DateRange(val startInclusive: Instant, val endExclusive: Instant)

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
    val directSets: Int,
    val indirectSets: Int,
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
    val total: Int get() = kotlin.math.round(effectiveSets).toInt()

    /**
     * Per-muscle kg tonnage attributed to this muscle this period, weighting indirect at
     * [INDIRECT_WEIGHT]. The "Volume by muscle" card and the drill-down sheet's volume
     * row both read this.
     */
    val totalVolumeKg: Double get() = directVolumeKg + INDIRECT_WEIGHT * indirectVolumeKg
}

data class ContributingExercise(
    val exerciseId: Long,
    val name: String,
    val sets: Int,
    val isPrimary: Boolean,
)

fun weekRange(mode: WeekMode, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    return when (mode) {
        WeekMode.ROLLING_7 -> DateRange(
            startInclusive = now.minusSeconds(7 * 24 * 3600L),
            endExclusive = now,
        )
        WeekMode.MON_SUN -> {
            val today = LocalDate.now(zone)
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val nextMonday = monday.plusDays(7)
            DateRange(
                startInclusive = monday.atStartOfDay(zone).toInstant(),
                endExclusive = nextMonday.atStartOfDay(zone).toInstant(),
            )
        }
    }
}

fun monthRange(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    val today = LocalDate.now(zone)
    val ym = YearMonth.from(today)
    val first = ym.atDay(1).atStartOfDay(zone).toInstant()
    val nextFirst = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
    return DateRange(first, nextFirst)
}

fun yearRange(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    val today = LocalDate.now(zone)
    val first = LocalDate.of(today.year, 1, 1).atStartOfDay(zone).toInstant()
    val nextFirst = LocalDate.of(today.year + 1, 1, 1).atStartOfDay(zone).toInstant()
    return DateRange(first, nextFirst)
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

fun previousMonthRange(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    val today = LocalDate.now(zone)
    val ym = YearMonth.from(today).minusMonths(1)
    val first = ym.atDay(1).atStartOfDay(zone).toInstant()
    val nextFirst = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
    return DateRange(first, nextFirst)
}

fun previousYearRange(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DateRange {
    val today = LocalDate.now(zone)
    val first = LocalDate.of(today.year - 1, 1, 1).atStartOfDay(zone).toInstant()
    val nextFirst = LocalDate.of(today.year, 1, 1).atStartOfDay(zone).toInstant()
    return DateRange(first, nextFirst)
}

fun computeMuscleVolumes(rows: List<StatSetRow>): Map<Muscle, MuscleVolume> {
    val directSets = HashMap<Muscle, Int>()
    val indirectSets = HashMap<Muscle, Int>()
    val directVol = HashMap<Muscle, Double>()
    val indirectVol = HashMap<Muscle, Double>()
    val byMuscleByExercise = HashMap<Muscle, HashMap<Long, ExerciseAccumulator>>()

    rows.forEach { r ->
        val setVol = (r.kg ?: 0.0) * r.reps

        // Each primary mover gets full credit for the set. A Bulgarian split squat with
        // {Quads, Glutes, Hamstrings} as primaries contributes +1 direct set to each of the
        // three muscles for a single working set.
        r.primaryMuscles.forEach { m ->
            directSets.merge(m, 1, Int::plus)
            directVol.merge(m, setVol, Double::plus)
            byMuscleByExercise.getOrPut(m) { HashMap() }
                .getOrPut(r.exerciseId) { ExerciseAccumulator(r.exerciseId, r.exerciseName, isPrimary = true) }
                .also { it.sets += 1 }
        }

        r.secondaryMuscles.forEach { m ->
            if (m in r.primaryMuscles) return@forEach
            indirectSets.merge(m, 1, Int::plus)
            indirectVol.merge(m, setVol, Double::plus)
            byMuscleByExercise.getOrPut(m) { HashMap() }
                .getOrPut(r.exerciseId) { ExerciseAccumulator(r.exerciseId, r.exerciseName, isPrimary = false) }
                .also { it.sets += 1 }
        }
    }

    return Muscle.entries.associateWith { m ->
        val exMap = byMuscleByExercise[m].orEmpty()
        val contrib = exMap.values
            .map { ContributingExercise(it.exerciseId, it.name, it.sets, it.isPrimary) }
            .sortedWith(compareByDescending<ContributingExercise> { it.isPrimary }.thenByDescending { it.sets })
        MuscleVolume(
            muscle = m,
            directSets = directSets[m] ?: 0,
            indirectSets = indirectSets[m] ?: 0,
            directVolumeKg = directVol[m] ?: 0.0,
            indirectVolumeKg = indirectVol[m] ?: 0.0,
            contributingExercises = contrib,
        )
    }
}

private data class ExerciseAccumulator(val exerciseId: Long, val name: String, val isPrimary: Boolean) {
    var sets: Int = 0
}

/** The 3-10 traffic-light: 0 grey, 1-2 blue (under), 3-10 green (in range), 11+ red (over). */
fun volumeColor(total: Int): Color = when {
    total <= 0 -> VolumeGrey
    total <= 2 -> VolumeBlue
    total <= Muscle.WEEKLY_MAX -> VolumeGreen
    else -> VolumeRed
}

fun setVolumeKg(rows: List<StatSetRow>): Double = rows.sumOf { (it.kg ?: 0.0) * it.reps }
