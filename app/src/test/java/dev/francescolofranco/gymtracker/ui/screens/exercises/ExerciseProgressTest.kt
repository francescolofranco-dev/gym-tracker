package dev.francescolofranco.gymtracker.ui.screens.exercises

import dev.francescolofranco.gymtracker.data.db.projections.ExerciseSetRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExerciseProgressTest {

    @Test
    fun epley1Rm_singleRepIsTheWeight() {
        assertEquals(100.0, epley1Rm(100.0, 1), 1e-9)
    }

    @Test
    fun epley1Rm_higherRepsRaiseTheEstimate() {
        // 100 * (1 + 10/30) = 133.333...
        assertEquals(133.3333, epley1Rm(100.0, 10), 1e-3)
        assertTrue(epley1Rm(100.0, 10) > epley1Rm(100.0, 5))
    }

    @Test
    fun aggregateSessions_picksBestSetForEstimated1Rm() {
        // One session, three sets; the 100x5 set has the highest Epley 1RM.
        val rows = listOf(
            setRow(sessionId = 1, day = 0, setNumber = 1, reps = 8, kg = 80.0),
            setRow(sessionId = 1, day = 0, setNumber = 2, reps = 5, kg = 100.0),
            setRow(sessionId = 1, day = 0, setNumber = 3, reps = 12, kg = 60.0),
        )

        val points = aggregateSessions(rows)

        assertEquals(1, points.size)
        val p = points.single()
        assertEquals(100.0, p.topSetKg!!, 1e-9)
        assertEquals(5, p.topSetReps)
        assertEquals(epley1Rm(100.0, 5), p.bestE1rmKg!!, 1e-9)
        assertEquals(8 * 80.0 + 5 * 100.0 + 12 * 60.0, p.volumeKg, 1e-9)
        assertEquals(12, p.bestReps)
        assertEquals(25, p.totalReps)
        assertEquals(3, p.setsLogged)
    }

    @Test
    fun aggregateSessions_moreWeightFewerRepsStillCountsAsOverload() {
        // Session 1: 80kg x 8 (e1RM ~101.3). Session 2: 85kg x 7 (e1RM ~104.8) — heavier bar,
        // one fewer rep, yet the estimated 1RM rises. This is the case the user called out.
        val rows = listOf(
            setRow(sessionId = 1, day = 0, setNumber = 1, reps = 8, kg = 80.0),
            setRow(sessionId = 2, day = 7, setNumber = 1, reps = 7, kg = 85.0),
        )

        val points = aggregateSessions(rows)

        assertEquals(2, points.size)
        assertTrue(points[1].bestE1rmKg!! > points[0].bestE1rmKg!!)
    }

    @Test
    fun aggregateSessions_bodyweightHasNoEstimated1RmButTracksReps() {
        val rows = listOf(
            setRow(sessionId = 1, day = 0, setNumber = 1, reps = 10, kg = null),
            setRow(sessionId = 1, day = 0, setNumber = 2, reps = 8, kg = 0.0),
        )

        val p = aggregateSessions(rows).single()

        assertNull(p.bestE1rmKg)
        assertNull(p.topSetKg)
        assertEquals(10, p.bestReps)
        assertEquals(18, p.totalReps)
        assertEquals(0.0, p.volumeKg, 1e-9)
    }

    @Test
    fun aggregateSessions_keepsSessionsOldestFirst() {
        val rows = listOf(
            setRow(sessionId = 5, day = 0, setNumber = 1, reps = 5, kg = 50.0),
            setRow(sessionId = 6, day = 3, setNumber = 1, reps = 5, kg = 55.0),
            setRow(sessionId = 7, day = 9, setNumber = 1, reps = 5, kg = 60.0),
        )

        val ids = aggregateSessions(rows).map { it.sessionId }

        assertEquals(listOf(5L, 6L, 7L), ids)
    }

    @Test
    fun overloadTrend_risingSeriesIsProgressing() {
        val result = overloadTrend(listOf(100.0, 103.0, 106.0, 110.0))
        assertEquals(OverloadTrend.Progressing, result.trend)
        assertEquals(10.0, result.percentChange!!, 1e-6)
        assertEquals(4, result.sessions)
    }

    @Test
    fun overloadTrend_fallingSeriesIsRegressing() {
        val result = overloadTrend(listOf(110.0, 106.0, 102.0, 98.0))
        assertEquals(OverloadTrend.Regressing, result.trend)
        assertTrue(result.percentChange!! < 0)
    }

    @Test
    fun overloadTrend_flatSeriesIsHolding() {
        val result = overloadTrend(listOf(100.0, 100.5, 99.8, 100.2))
        assertEquals(OverloadTrend.Holding, result.trend)
    }

    @Test
    fun overloadTrend_underThreePointsIsNotEnoughData() {
        assertEquals(OverloadTrend.NotEnoughData, overloadTrend(listOf(100.0, 110.0)).trend)
        assertEquals(OverloadTrend.NotEnoughData, overloadTrend(emptyList()).trend)
    }

    @Test
    fun overloadTrend_zeroFirstValueHasNullPercentChangeAndNoCrash() {
        val result = overloadTrend(listOf(0.0, 5.0, 10.0))
        assertNull(result.percentChange)
        assertEquals(OverloadTrend.Progressing, result.trend)
    }

    @Test
    fun overloadTrend_allZeroSeriesHoldsWithoutDividingByZero() {
        val result = overloadTrend(listOf(0.0, 0.0, 0.0))
        assertEquals(OverloadTrend.Holding, result.trend)
        assertNull(result.percentChange)
    }

    private fun setRow(sessionId: Long, day: Long, setNumber: Int, reps: Int, kg: Double?) =
        ExerciseSetRow(
            sessionId = sessionId,
            sessionStartedAt = Instant.EPOCH.plusSeconds(day * 86_400),
            reps = reps,
            kg = kg,
            setNumber = setNumber,
        )
}
