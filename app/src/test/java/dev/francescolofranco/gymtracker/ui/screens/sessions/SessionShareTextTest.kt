package dev.francescolofranco.gymtracker.ui.screens.sessions

import dev.francescolofranco.gymtracker.data.db.entities.ExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionEntity
import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import dev.francescolofranco.gymtracker.data.db.entities.SetLogEntity
import dev.francescolofranco.gymtracker.data.db.projections.SessionExerciseDetail
import dev.francescolofranco.gymtracker.domain.ExerciseSide
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
            1 exercise · 2 sets

            • Bench press — 2 sets (10, 8 reps) · 100 kg

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
        assertEquals("1×8 reps", summary.exercises.single().setsLabel)
        assertEquals("BW", summary.exercises.single().firstSetWeightLabel(WeightUnit.KG))
    }

    @Test
    fun `unilateral exercise formats completed sets per side`() {
        val session = SessionEntity(
            startedAt = Instant.parse("2026-08-23T09:55:00Z"),
            acceptedAt = Instant.parse("2026-08-23T10:00:00Z"),
            endedAt = Instant.parse("2026-08-23T10:25:00Z"),
        )
        val details = listOf(
            detail(
                id = 1,
                name = "Dumbbell curl",
                sets = listOf(
                    set(id = 1, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                    set(id = 2, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.RIGHT, reps = 10, kg = 14.0, completed = true),
                    set(id = 3, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.LEFT, reps = 9, kg = 14.0, completed = true),
                    set(id = 4, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.RIGHT, reps = 9, kg = 14.0, completed = true),
                    set(id = 5, sessionExerciseId = 1, setNumber = 3, side = ExerciseSide.LEFT, reps = 8, kg = 14.0, completed = true),
                    set(id = 6, sessionExerciseId = 1, setNumber = 3, side = ExerciseSide.RIGHT, reps = 8, kg = 14.0, completed = true),
                ),
            ),
            detail(
                id = 2,
                name = "Single-arm row",
                sets = listOf(
                    set(id = 7, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.LEFT, reps = 12, kg = 24.0, completed = true),
                    set(id = 8, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.RIGHT, reps = 12, kg = 24.0, completed = true),
                ),
            ),
        )

        val summary = summarizeFinishedSession(details)
        assertEquals(2, summary.exerciseCount)
        assertEquals(8, summary.setCount)
        assertEquals("3 sets/side (10, 9, 8 reps)", summary.exercises[0].setsLabel)
        assertEquals("1×12 reps/side", summary.exercises[1].setsLabel)

        val text = buildSessionShareText(
            session = session,
            details = details,
            unit = WeightUnit.KG,
            zoneId = ZoneOffset.UTC,
            locale = Locale.ENGLISH,
        )

        assertTrue(text.contains("• Dumbbell curl — 3 sets/side (10, 9, 8 reps) · 14 kg"))
        assertTrue(text.contains("• Single-arm row — 1×12 reps/side · 24 kg"))
    }

    @Test
    fun `unilateral exercise with identical reps across all sets formats as sets times reps per side`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Dumbbell curl",
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 2, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.RIGHT, reps = 10, kg = 14.0, completed = true),
                        set(id = 3, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 4, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.RIGHT, reps = 10, kg = 14.0, completed = true),
                        set(id = 5, sessionExerciseId = 1, setNumber = 3, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 6, sessionExerciseId = 1, setNumber = 3, side = ExerciseSide.RIGHT, reps = 10, kg = 14.0, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("3×10 reps/side", summary.exercises.single().setsLabel)
    }

    @Test
    fun `bilateral exercise with identical reps across all sets formats as sets times reps`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Bench press",
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, setNumber = 1, reps = 10, kg = 80.0, completed = true),
                        set(id = 2, sessionExerciseId = 1, setNumber = 2, reps = 10, kg = 80.0, completed = true),
                        set(id = 3, sessionExerciseId = 1, setNumber = 3, reps = 10, kg = 80.0, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("3×10 reps", summary.exercises.single().setsLabel)
    }

    @Test
    fun `bilateral exercise with per-side weight doubles load in summary and share text`() {
        val session = SessionEntity(
            startedAt = Instant.parse("2026-08-23T09:55:00Z"),
            acceptedAt = Instant.parse("2026-08-23T10:00:00Z"),
            endedAt = Instant.parse("2026-08-23T10:40:00Z"),
        )
        val details = listOf(
            detail(
                id = 1,
                name = "Bench press machine",
                weightPerSide = true,
                sets = listOf(
                    set(id = 1, sessionExerciseId = 1, setNumber = 1, reps = 10, kg = 22.5, completed = true),
                    set(id = 2, sessionExerciseId = 1, setNumber = 2, reps = 8, kg = 25.0, completed = true),
                ),
            ),
        )

        val summary = summarizeFinishedSession(details)
        assertEquals(45.0, summary.exercises.single().firstSetKg!!, 0.001)
        assertEquals("45 kg", summary.exercises.single().firstSetWeightLabel(WeightUnit.KG))

        val text = buildSessionShareText(
            session = session,
            details = details,
            unit = WeightUnit.KG,
            zoneId = ZoneOffset.UTC,
            locale = Locale.ENGLISH,
        )

        assertTrue(text.contains("• Bench press machine — 2 sets (10, 8 reps) · 45 kg"))
    }

    @Test
    fun `unilateral equal sets with different reps across sides formats sides separately`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Dumbbell curl",
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 2, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.RIGHT, reps = 8, kg = 14.0, completed = true),
                        set(id = 3, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 4, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.RIGHT, reps = 8, kg = 14.0, completed = true),
                    ),
                ),
                detail(
                    id = 2,
                    name = "Hammer curl",
                    sets = listOf(
                        set(id = 5, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.LEFT, reps = 10, kg = 12.0, completed = true),
                        set(id = 6, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.RIGHT, reps = 9, kg = 12.0, completed = true),
                        set(id = 7, sessionExerciseId = 2, setNumber = 2, side = ExerciseSide.LEFT, reps = 9, kg = 12.0, completed = true),
                        set(id = 8, sessionExerciseId = 2, setNumber = 2, side = ExerciseSide.RIGHT, reps = 8, kg = 12.0, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("2 sets/side (L 10, R 8 reps)", summary.exercises[0].setsLabel)
        assertEquals("2 sets/side (L 10, 9, R 9, 8 reps)", summary.exercises[1].setsLabel)
    }

    @Test
    fun `singular rep formatting uses rep instead of reps`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Heavy single",
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, setNumber = 1, reps = 1, kg = 150.0, completed = true),
                    ),
                ),
                detail(
                    id = 2,
                    name = "Pistol squat",
                    sets = listOf(
                        set(id = 2, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.LEFT, reps = 1, kg = null, completed = true),
                        set(id = 3, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.RIGHT, reps = 1, kg = null, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("1×1 rep", summary.exercises[0].setsLabel)
        assertEquals("1×1 rep/side", summary.exercises[1].setsLabel)
    }

    @Test
    fun `unequal or mixed unilateral sets do not claim equal sets per side`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Dumbbell curl",
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.LEFT, reps = 10, kg = 14.0, completed = true),
                        set(id = 2, sessionExerciseId = 1, setNumber = 1, side = ExerciseSide.RIGHT, reps = 10, kg = 14.0, completed = true),
                        set(id = 3, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.LEFT, reps = 8, kg = 14.0, completed = true),
                        set(id = 4, sessionExerciseId = 1, setNumber = 2, side = ExerciseSide.RIGHT, reps = 8, kg = 14.0, completed = false),
                    ),
                ),
                detail(
                    id = 2,
                    name = "Lateral raise",
                    sets = listOf(
                        set(id = 5, sessionExerciseId = 2, setNumber = 1, side = ExerciseSide.LEFT, reps = 15, kg = 8.0, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("2 L + 1 R sets (L 10, 8, R 10 reps)", summary.exercises[0].setsLabel)
        assertEquals("1 L × 15 reps", summary.exercises[1].setsLabel)
    }

    @Test
    fun `share text converts the first completed set weight to the preferred unit`() {
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
                    set(id = 2, sessionExerciseId = 1, setNumber = 2, reps = 8, kg = 110.0, completed = true),
                    set(id = 1, sessionExerciseId = 1, setNumber = 1, reps = 10, kg = 100.0, completed = true),
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

        assertTrue(text.contains("• Deadlift — 2 sets (10, 8 reps) · 220.462 lbs"))
        assertFalse(text.contains("volume"))
        assertFalse(text.contains("kg"))
    }

    @Test
    fun `bodyweight exercise reports added load from its first set`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Pull-up",
                    bodyweight = true,
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, reps = 6, kg = 10.0, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("BW +10 kg", summary.exercises.single().firstSetWeightLabel(WeightUnit.KG))
    }

    @Test
    fun `assisted bodyweight exercise formats negative weight without plus-minus`() {
        val summary = summarizeFinishedSession(
            listOf(
                detail(
                    id = 1,
                    name = "Assisted pull-up",
                    bodyweight = true,
                    sets = listOf(
                        set(id = 1, sessionExerciseId = 1, reps = 8, kg = -25.0, completed = true),
                    ),
                ),
                detail(
                    id = 2,
                    name = "Assisted dip",
                    bodyweight = false,
                    sets = listOf(
                        set(id = 2, sessionExerciseId = 2, reps = 10, kg = -15.5, completed = true),
                    ),
                ),
            ),
        )

        assertEquals("BW -25 kg", summary.exercises[0].firstSetWeightLabel(WeightUnit.KG))
        assertEquals("-15.5 kg", summary.exercises[1].firstSetWeightLabel(WeightUnit.KG))
    }

    @Test
    fun `negative weight chip and percent delta format cleanly`() {
        assertEquals("0", formatWeightNumber(-0.0))
        assertEquals("0", formatWeightNumber(-0.0001))
        assertEquals("-25", formatWeightChip(-25.0, isBodyweight = true))
        assertEquals("+25", formatWeightChip(25.0, isBodyweight = true))
        assertEquals("-25", formatWeightChip(-25.0, isBodyweight = false))

        assertEquals(PercentDelta("+20%", PercentTone.Up), percentDeltaOrDash(-20.0, -25.0))
        assertEquals(PercentDelta("-20%", PercentTone.Down), percentDeltaOrDash(-30.0, -25.0))
        assertEquals(PercentDelta("0%", PercentTone.Same), percentDeltaOrDash(-25.0, -25.0))
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
        unilateral: Boolean = false,
        weightPerSide: Boolean = false,
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
            isUnilateral = unilateral,
            isWeightPerSide = weightPerSide,
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
        setNumber: Int = id.toInt(),
        side: ExerciseSide = ExerciseSide.BOTH,
    ) = SetLogEntity(
        id = id,
        sessionExerciseId = sessionExerciseId,
        setNumber = setNumber,
        side = side,
        reps = reps,
        kg = kg,
        isSkipped = skipped,
        loggedAt = if (completed) Instant.EPOCH else null,
    )
}
