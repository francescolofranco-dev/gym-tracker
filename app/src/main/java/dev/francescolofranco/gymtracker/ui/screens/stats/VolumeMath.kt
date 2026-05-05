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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class DateRange(val startInclusive: Instant, val endExclusive: Instant)

data class MuscleVolume(
    val muscle: Muscle,
    val directSets: Int,
    val indirectSets: Int,
    val contributingExercises: List<ContributingExercise>,
) {
    val total: Int get() = directSets + indirectSets
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

fun computeMuscleVolumes(rows: List<StatSetRow>): Map<Muscle, MuscleVolume> {
    val direct = HashMap<Muscle, Int>()
    val indirect = HashMap<Muscle, Int>()
    val byMuscleByExercise = HashMap<Muscle, HashMap<Long, ExerciseAccumulator>>()

    rows.forEach { r ->
        // Direct contribution (primary)
        direct.merge(r.primaryMuscle, 1, Int::plus)
        byMuscleByExercise.getOrPut(r.primaryMuscle) { HashMap() }
            .getOrPut(r.exerciseId) { ExerciseAccumulator(r.exerciseId, r.exerciseName, isPrimary = true) }
            .also { it.sets += 1 }

        // Indirect contributions (secondaries — exclude duplicate of primary)
        r.secondaryMuscles.forEach { m ->
            if (m == r.primaryMuscle) return@forEach
            indirect.merge(m, 1, Int::plus)
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
            directSets = direct[m] ?: 0,
            indirectSets = indirect[m] ?: 0,
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

fun volumeColorFor(volumes: Map<Muscle, MuscleVolume>): Map<Muscle, Color> =
    volumes.mapValues { (_, v) -> volumeColor(v.total) }

fun setVolumeKg(rows: List<StatSetRow>): Double = rows.sumOf { (it.kg ?: 0.0) * it.reps }
