package dev.francescolofranco.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleCategoryTest {

    @Test
    fun `app uses push pull legs core in that order`() {
        assertEquals(listOf("Push", "Pull", "Legs", "Core"), MuscleCategory.entries.map { it.displayName })
    }

    @Test
    fun `every muscle belongs to exactly one category`() {
        val assignments = MuscleCategory.entries
            .flatMap { category -> category.muscles.map { muscle -> muscle to category } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        assertEquals(Muscle.entries.toSet(), assignments.keys)
        assertTrue(assignments.values.all { it.size == 1 })
        Muscle.entries.forEach { muscle ->
            assertEquals(assignments.getValue(muscle).single(), MuscleCategory.containing(muscle))
        }
    }
}
