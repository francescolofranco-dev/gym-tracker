package dev.francescolofranco.gymtracker.data.repository

import dev.francescolofranco.gymtracker.data.db.entities.SessionExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseNoteLifecycleTest {
    @Test
    fun eitherTypeCanBeAddedWithoutReplacingTheOther() {
        val withPin = occurrence(pinnedNotes = "Safety bars")
        val addedRegular = savedExerciseNote(withPin, "Try 40 kg", withPin.pinnedNotes)
        assertEquals("Safety bars", addedRegular.pinnedText)
        assertEquals("Try 40 kg", addedRegular.text)
        assertTrue(addedRegular.carryForward)

        val withRegular = occurrence(notes = "Try 40 kg", noteCarryForward = true)
        val addedPin = savedExerciseNote(withRegular, withRegular.notes, "Safety bars")
        assertEquals(addedRegular, addedPin)
    }

    @Test
    fun bothCarryButOnlyPinnedNoteSurvivesSecondOccurrence() {
        val first = requireNotNull(noteForNextSession(occurrence(
            notes = "Try 40 kg", pinnedNotes = "Safety bars", noteCarryForward = true,
        )))
        assertEquals("Try 40 kg", first.text)
        assertEquals("Safety bars", first.pinnedText)
        assertFalse(first.carryForward)
        val second = requireNotNull(noteForNextSession(occurrence(
            notes = first.text, pinnedNotes = first.pinnedText, noteCarryForward = first.carryForward,
        )))
        assertNull(second.text)
        assertEquals("Safety bars", second.pinnedText)
    }

    @Test
    fun unpinnedNoteExpiresAfterOneCarry() {
        val first = requireNotNull(noteForNextSession(occurrence(
            notes = "Try 40 kg", noteCarryForward = true,
        )))
        assertNull(noteForNextSession(occurrence(notes = first.text)))
    }

    @Test
    fun clearingEitherNotePreservesTheOther() {
        val both = occurrence("Try 40 kg", "Safety bars", true)
        val clearedPin = savedExerciseNote(both, both.notes, "  ")
        assertNull(clearedPin.pinnedText)
        assertEquals(both.notes, clearedPin.text)
        assertTrue(clearedPin.carryForward)
        val clearedRegular = savedExerciseNote(both, "  ", both.pinnedNotes)
        assertNull(clearedRegular.text)
        assertFalse(clearedRegular.carryForward)
        assertEquals(both.pinnedNotes, clearedRegular.pinnedText)
        assertNull(noteForNextSession(occurrence()))
    }

    @Test
    fun editingPinDoesNotRenewCarriedRegularNote() {
        val saved = savedExerciseNote(occurrence("Try 40 kg", "Safety bars"), "Try 40 kg", "New pin")
        assertFalse(saved.carryForward)
        assertEquals("New pin", saved.pinnedText)
    }

    @Test
    fun editingCarriedRegularNoteRenewsItWithoutChangingPin() {
        val saved = savedExerciseNote(occurrence("Try 40 kg", "Safety bars"), "Try 45 kg", "Safety bars")
        assertTrue(saved.carryForward)
        assertEquals("Safety bars", saved.pinnedText)
    }

    private fun occurrence(
        notes: String? = null,
        pinnedNotes: String? = null,
        noteCarryForward: Boolean = false,
    ) = SessionExerciseEntity(
        sessionId = 1, exerciseId = 2, orderInSession = 0,
        notes = notes, pinnedNotes = pinnedNotes, noteCarryForward = noteCarryForward,
    )
}
