package dev.francescolofranco.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleOrderTest {

    @Test
    fun `anatomical order runs from upper body to calves with unique ranks`() {
        val expected = listOf(
            Muscle.UPPER_BACK_TRAPS,
            Muscle.FRONT_DELTS,
            Muscle.SIDE_DELTS,
            Muscle.REAR_DELTS,
            Muscle.CHEST,
            Muscle.LATS,
            Muscle.BICEPS,
            Muscle.TRICEPS,
            Muscle.FOREARMS,
            Muscle.CORE,
            Muscle.LOWER_BACK,
            Muscle.GLUTES,
            Muscle.ADDUCTORS,
            Muscle.QUADS,
            Muscle.HAMSTRINGS,
            Muscle.CALVES,
        )

        assertEquals(expected, Muscle.topToBottom)
        assertEquals(Muscle.entries.size, Muscle.entries.map { it.anatomicalRank }.distinct().size)
    }

    @Test
    fun `topmost ignores collection insertion and enum order`() {
        val muscles = linkedSetOf(Muscle.CALVES, Muscle.CHEST, Muscle.FRONT_DELTS)

        assertEquals(Muscle.FRONT_DELTS, muscles.topmost())
        assertEquals(
            listOf(Muscle.FRONT_DELTS, Muscle.CHEST, Muscle.CALVES),
            muscles.sortedTopToBottom(),
        )
    }
}
