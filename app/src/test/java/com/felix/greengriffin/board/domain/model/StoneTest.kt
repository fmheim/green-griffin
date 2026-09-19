package com.felix.greengriffin.board.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoneTest {

    private fun onBoard(row: Int, column: Int) =
        StoneOnBoard(letter = 'A', value = 1, id = "$row-$column", rowIndex = row, columnIndex = column, isLocked = false)

    @Test
    fun `a stone knows its neighbours`() {
        val stone = onBoard(row = 2, column = 2)

        assertTrue(onBoard(row = 2, column = 1).isToLeftOf(stone))
        assertTrue(onBoard(row = 2, column = 3).isToRightOf(stone))
        assertTrue(onBoard(row = 1, column = 2).isAbove(stone))
        assertTrue(onBoard(row = 3, column = 2).isBelow(stone))
        assertFalse(onBoard(row = 3, column = 3).isToLeftOf(stone))
    }

    @Test
    fun `a blank stone is a joker`() {
        assertTrue(StoneInBag(letter = ' ', value = 0, id = "blank").isJoker)
        assertFalse(StoneInBag(letter = 'A', value = 1, id = "a").isJoker)
    }

    @Test
    fun `a word scores the value of its letters`() {
        val word = listOf(onBoard(row = 0, column = 0), onBoard(row = 0, column = 1)).asWord()

        assertEquals(2, word.points)
        assertEquals("AA", word.asString)
    }

    @Test
    fun `stones in one row are horizontally aligned`() {
        val stones = listOf(onBoard(row = 1, column = 0), onBoard(row = 1, column = 1))

        assertTrue(stones.areHorizontallyAligned)
        assertFalse(stones.areVerticallyAligned)
    }
}
