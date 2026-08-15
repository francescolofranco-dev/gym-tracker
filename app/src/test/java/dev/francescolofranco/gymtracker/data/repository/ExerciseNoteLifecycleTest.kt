package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseNoteLifecycleTest {

    @Test
    fun unpinnedNoteCarriesToExactlyOneSubsequentOccurrence() {
        val original = occurrence(
            notes = "Keep elbows tucked",
            noteCarryForward = true,
        )

        val firstCarry = requireNotNull(noteForNextSession(original))
        assertEquals("Keep elbows tucked", firstCarry.text)
        assertFalse(firstCarry.isPinned)

        val carriedOccurrence = occurrence(
            notes = firstCarry.text,
            isNotePinned = firstCarry.isPinned,
            noteCarryForward = firstCarry.carryForward,
        )
        assertNull(noteForNextSession(carriedOccurrence))
    }

    @Test
    fun pinnedNoteKeepsCarrying() {
        val original = occurrence(
            notes = "Use the safety bars",
            isNotePinned = true,
            noteCarryForward = true,
        )

        val firstCarry = requireNotNull(noteForNextSession(original))
        assertTrue(firstCarry.isPinned)

        val nextOccurrence = occurrence(
            notes = firstCarry.text,
            isNotePinned = firstCarry.isPinned,
            noteCarryForward = firstCarry.carryForward,
        )
        val secondCarry = requireNotNull(noteForNextSession(nextOccurrence))
        assertEquals("Use the safety bars", secondCarry.text)
        assertTrue(secondCarry.isPinned)
    }

    @Test
    fun unpinnedPersistentNoteGetsOneFinalCarryThenExpires() {
        val currentPinned = occurrence(
            notes = "Use the safety bars",
            isNotePinned = true,
            noteCarryForward = true,
        )
        val unpinned = savedExerciseNote(
            current = currentPinned,
            notes = "Use the safety bars",
            isPinned = false,
        )
        val savedOccurrence = occurrence(
            notes = unpinned.text,
            isNotePinned = unpinned.isPinned,
            noteCarryForward = unpinned.carryForward,
        )

        val finalCarry = requireNotNull(noteForNextSession(savedOccurrence))
        assertFalse(finalCarry.isPinned)
        assertNull(
            noteForNextSession(
                occurrence(
                    notes = finalCarry.text,
                    isNotePinned = finalCarry.isPinned,
                    noteCarryForward = finalCarry.carryForward,
                ),
            ),
        )
    }

    @Test
    fun clearingNoteAlsoClearsPinnedState() {
        val cleared = savedExerciseNote(
            current = occurrence(
                notes = "Use the safety bars",
                isNotePinned = true,
                noteCarryForward = true,
            ),
            notes = "   ",
            isPinned = true,
        )

        assertNull(cleared.text)
        assertFalse(cleared.isPinned)
        assertFalse(cleared.carryForward)
    }

    @Test
    fun savingUnchangedCarriedNoteDoesNotRenewIt() {
        val carried = occurrence(
            notes = "Keep elbows tucked",
            noteCarryForward = false,
        )

        val saved = savedExerciseNote(
            current = carried,
            notes = "Keep elbows tucked",
            isPinned = false,
        )

        assertFalse(saved.carryForward)
    }

    @Test
    fun editingCarriedNoteRenewsItForOneOccurrence() {
        val carried = occurrence(
            notes = "Keep elbows tucked",
            noteCarryForward = false,
        )

        val saved = savedExerciseNote(
            current = carried,
            notes = "Keep elbows tucked on the way down",
            isPinned = false,
        )

        assertTrue(saved.carryForward)
    }

    private fun occurrence(
        notes: String?,
        isNotePinned: Boolean = false,
        noteCarryForward: Boolean = false,
    ) = SessionExerciseEntity(
        sessionId = 1,
        exerciseId = 2,
        orderInSession = 0,
        notes = notes,
        isNotePinned = isNotePinned,
        noteCarryForward = noteCarryForward,
    )
}
