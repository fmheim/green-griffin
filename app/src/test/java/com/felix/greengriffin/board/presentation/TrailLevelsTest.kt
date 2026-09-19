package com.felix.greengriffin.board.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailLevelsTest {

    @Test
    fun `every level has a unique index`() {
        val indexes = trailLevels.map { it.index }
        assertEquals(indexes.size, indexes.distinct().size)
    }

    @Test
    fun `no level references a field outside its own board`() {
        trailLevels.forEach { level ->
            val allFields = level.startFields + level.goalFields + level.blockedField
            val outOfBounds = allFields.filterNot { field ->
                field.row in 0 until level.boardSize && field.column in 0 until level.boardSize
            }
            assertEquals(
                "level ${level.index} references fields outside a ${level.boardSize}x${level.boardSize} board: $outOfBounds",
                emptyList<Field>(),
                outOfBounds,
            )
        }
    }

    @Test
    fun `every level has at least one start field and one goal field`() {
        trailLevels.forEach { level ->
            assertTrue("level ${level.index} has no start fields", level.startFields.isNotEmpty())
            assertTrue("level ${level.index} has no goal fields", level.goalFields.isNotEmpty())
        }
    }

    @Test
    fun `start fields and goal fields never overlap`() {
        trailLevels.forEach { level ->
            assertEquals(
                "level ${level.index} has fields that are both start and goal",
                emptySet<Field>(),
                level.startFields intersect level.goalFields,
            )
        }
    }
}
