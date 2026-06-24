package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import java.time.Instant

/**
 * Estimated one-rep max via the Epley formula: 1RM = w · (1 + reps / 30). It rises whether you add
 * load OR reps, so it captures progressive overload even when you trade reps for weight (a heavier
 * bar for one or two fewer reps). A true single already *is* a 1RM, so reps ≤ 1 returns the weight
 * unchanged rather than the formula's slight overestimate. It overestimates at very high rep counts
 * but stays monotonic, which is all the trend needs.
 */
fun epley1Rm(kg: Double, reps: Int): Double = if (reps <= 1) kg else kg * (1 + reps / 30.0)

/** Per-session rollup of one exercise's logged sets — backs the progress chart, verdict, and history. */
data class SessionProgressPoint(
    val sessionId: Long,
    val startedAt: Instant,
    val setsLogged: Int,
    val volumeKg: Double,
    /** Best estimated 1RM across the session's weighted sets; null when nothing was loaded (bodyweight). */
    val bestE1rmKg: Double?,
    /** Weight and reps of the set that produced [bestE1rmKg]; null together with it. */
    val topSetKg: Double?,
    val topSetReps: Int?,
    /** Most reps in any single set this session — the bodyweight headline. */
    val bestReps: Int,
    val totalReps: Int,
)

/**
 * Collapse the ordered set stream into one point per session, preserving ascending date order
 * (rows are expected oldest-first, as the DAO returns them). Sets with a null or zero weight still
 * count toward volume and reps but are ignored for estimated 1RM and top set.
 */
fun aggregateSessions(rows: List<ExerciseSetRow>): List<SessionProgressPoint> {
    if (rows.isEmpty()) return emptyList()

    // LinkedHashMap preserves first-seen (oldest) session order.
    val bySession = LinkedHashMap<Long, MutableList<ExerciseSetRow>>()
    rows.forEach { bySession.getOrPut(it.sessionId) { mutableListOf() }.add(it) }

    return bySession.values.map { sessionRows ->
        val first = sessionRows.first()
        var volumeKg = 0.0
        var totalReps = 0
        var bestReps = 0
        var bestE1rm: Double? = null
        var topSetKg: Double? = null
        var topSetReps: Int? = null

        sessionRows.forEach { r ->
            val kg = r.kg
            volumeKg += r.reps * (kg ?: 0.0)
            totalReps += r.reps
            if (r.reps > bestReps) bestReps = r.reps
            if (kg != null && kg > 0.0) {
                val e1rm = epley1Rm(kg, r.reps)
                val current = bestE1rm
                if (current == null || e1rm > current) {
                    bestE1rm = e1rm
                    topSetKg = kg
                    topSetReps = r.reps
                }
            }
        }

        SessionProgressPoint(
            sessionId = first.sessionId,
            startedAt = first.sessionStartedAt,
            setsLogged = sessionRows.size,
            volumeKg = volumeKg,
            bestE1rmKg = bestE1rm,
            topSetKg = topSetKg,
            topSetReps = topSetReps,
            bestReps = bestReps,
            totalReps = totalReps,
        )
    }
}

/** A metric the progress chart can plot. Weight metrics are in kilograms and convert for display. */
enum class ProgressMetric(val label: String, val isWeight: Boolean) {
    Est1Rm("Est. 1RM", isWeight = true),
    TopSet("Top set", isWeight = true),
    Volume("Volume", isWeight = true),
    BestReps("Best reps", isWeight = false),
    TotalReps("Total reps", isWeight = false),
}

/** Weighted lifts get load-based metrics; bodyweight exercises get reps-based ones (load is ~0). */
fun metricsFor(isBodyweight: Boolean): List<ProgressMetric> =
    if (isBodyweight) listOf(ProgressMetric.BestReps, ProgressMetric.TotalReps)
    else listOf(ProgressMetric.Est1Rm, ProgressMetric.TopSet, ProgressMetric.Volume)

/** The metric the overload verdict is judged on: estimated 1RM for weighted lifts, best reps otherwise. */
fun headlineMetric(isBodyweight: Boolean): ProgressMetric =
    if (isBodyweight) ProgressMetric.BestReps else ProgressMetric.Est1Rm

/**
 * Raw value of a metric for one session, or null when the session has no data for it (e.g. an
 * estimated 1RM when nothing was loaded). Weight metrics return kilograms; rep metrics return counts.
 */
fun SessionProgressPoint.valueFor(metric: ProgressMetric): Double? = when (metric) {
    ProgressMetric.Est1Rm -> bestE1rmKg
    ProgressMetric.TopSet -> topSetKg
    ProgressMetric.Volume -> volumeKg
    ProgressMetric.BestReps -> bestReps.toDouble()
    ProgressMetric.TotalReps -> totalReps.toDouble()
}

enum class OverloadTrend { Progressing, Holding, Regressing, NotEnoughData }

data class TrendResult(val trend: OverloadTrend, val percentChange: Double?, val sessions: Int)

/** ±0.5% per session counts as "holding steady" rather than a real trend. */
private const val TREND_DEADBAND = 0.005

/**
 * Classify a progress series (oldest first). Fits a least-squares line over the session index and
 * compares its slope — expressed as a fraction of the series mean per session — to a small deadband.
 * Needs at least three points to call a trend. [TrendResult.percentChange] is the first→last change
 * in percent (null when the first value is zero).
 */
fun overloadTrend(values: List<Double>): TrendResult {
    val n = values.size
    if (n < 3) return TrendResult(OverloadTrend.NotEnoughData, null, n)

    val percentChange =
        if (values.first() != 0.0) (values.last() - values.first()) / values.first() * 100 else null
    val mean = values.average()
    if (mean == 0.0) return TrendResult(OverloadTrend.Holding, percentChange, n)

    val xMean = (n - 1) / 2.0
    var num = 0.0
    var den = 0.0
    values.forEachIndexed { i, v ->
        val dx = i - xMean
        num += dx * (v - mean)
        den += dx * dx
    }
    val slope = if (den == 0.0) 0.0 else num / den
    val relSlope = slope / mean
    val trend = when {
        relSlope > TREND_DEADBAND -> OverloadTrend.Progressing
        relSlope < -TREND_DEADBAND -> OverloadTrend.Regressing
        else -> OverloadTrend.Holding
    }
    return TrendResult(trend, percentChange, n)
}
