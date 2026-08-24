package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.Muscle
import dev.francescolofranco.gymtracker.domain.WeightUnit
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionShareTextTest {

    @Test
    fun `share text includes a stable completed workout summary`() {
        val session = SessionEntity(
            id = 1,
            startedAt = Instant.parse("2026-08-23T09:55:00Z"),
            acceptedAt = Instant.parse("2026-08-23T10:00:00Z"),
            endedAt = Instant.parse("2026-08-23T11:05:00Z"),
        )
        val details = listOf(
            detail(
                id = 1,
                name = "Bench press",
                sets = listOf(
                    set(id = 1, sessionExerciseId = 1, reps = 10, kg = 100.0, completed = true),
                    set(id = 2, sessionExerciseId = 1, reps = 8, kg = 110.0, completed = true),
                    set(id = 3, sessionExerciseId = 1, reps = 12, kg = 80.0, completed = false),
                    set(id = 4, sessionExerciseId = 1, reps = 5, kg = 200.0, completed = true, skipped = true),
                ),
            ),
            detail(
                id = 2,
                name = "Back squat",
                skipped = true,
                sets = listOf(
                    set(id = 5, sessionExerciseId = 2, reps = 10, kg = 150.0, completed = true),
                ),
            ),
            detail(
                id = 3,
                name = "Cable row",
                sets = listOf(
                    set(id = 6, sessionExerciseId = 3, reps = 12, kg = 50.0, completed = false),
                ),
            ),
        )

        val text = buildSessionShareText(
            session = session,
            details = details,
            unit = WeightUnit.KG,
            zoneId = ZoneOffset.UTC,
            locale = Locale.ENGLISH,
        )

        assertEquals(
            """
            Workout complete! 💪
            23 Aug 2026 · 1h 5m
            1 exercise · 2 sets · 1880 kg volume

            • Bench press — 2 sets · 1880 kg

            Tracked with Gym Tracker
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `summary excludes drafts and skipped work`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Pull-up",
                    bodyweight = true,
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, reps = 8, kg = null, completed = true),
                        set(id = 2, sessionExerciseId = 1, reps = 7, kg = null, completed = false),
                        set(id = 3, sessionExerciseId = 1, reps = 6, kg = null, completed = true, skipped = true),
                    ),
                ),
            ),
        )

        assertEquals(1, summary.exerciseCount)
        assertEquals(1, summary.setCount)
        assertEquals(0.0, summary.volumeKg, 0.0)
    }

    @Test
    fun `share text converts weighted volume to the preferred unit`() {
        val session = SessionEntity(
            startedAt = Instant.EPOCH,
            acceptedAt = Instant.EPOCH,
            endedAt = Instant.EPOCH.plusSeconds(60),
        )
        val details = listOf(
            detail(
                id = 1,
                name = "Deadlift",
                sets = listOf(
                    set(id = 1, sessionExerciseId = 1, reps = 10, kg = 100.0, completed = true),
                ),
            ),
        )

        val text = buildSessionShareText(
            session = session,
            details = details,
            unit = WeightUnit.LBS,
            zoneId = ZoneOffset.UTC,
            locale = Locale.ENGLISH,
        )

        assertTrue(text.contains("2205 lbs volume"))
        assertTrue(text.contains("• Deadlift — 1 set · 2205 lbs"))
        assertFalse(text.contains("kg"))
    }

    @Test
    fun `share duration stays compact at useful boundaries`() {
        assertEquals("<1m", formatShareDuration(Duration.ofSeconds(59)))
        assertEquals("1m", formatShareDuration(Duration.ofMinutes(1)))
        assertEquals("1h", formatShareDuration(Duration.ofHours(1)))
        assertEquals("1h 5m", formatShareDuration(Duration.ofMinutes(65)))
    }

    private fun detail(
        id: Long,
        name: String,
        sets: List<SetLogEntity>,
        skipped: Boolean = false,
        bodyweight: Boolean = false,
    ) = SessionExerciseDetail(
        sessionExercise = SessionExerciseEntity(
            id = id,
            sessionId = 1,
            exerciseId = id,
            orderInSession = id.toInt(),
            isSkipped = skipped,
        ),
        exercise = ExerciseEntity(
            id = id,
            name = name,
            primaryMuscles = setOf(Muscle.CHEST),
            secondaryMuscles = emptySet(),
            targetSets = 3,
            repRangeMin = 6,
            repRangeMax = 12,
            isBodyweight = bodyweight,
            createdAt = Instant.EPOCH,
        ),
        setLogs = sets,
    )

    private fun set(
        id: Long,
        sessionExerciseId: Long,
        reps: Int,
        kg: Double?,
        completed: Boolean,
        skipped: Boolean = false,
    ) = SetLogEntity(
        id = id,
        sessionExerciseId = sessionExerciseId,
        setNumber = id.toInt(),
        reps = reps,
        kg = kg,
        isSkipped = skipped,
        loggedAt = if (completed) Instant.EPOCH else null,
    )
}
