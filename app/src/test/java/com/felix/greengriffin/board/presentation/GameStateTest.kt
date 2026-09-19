package com.felix.greengriffin.board.presentation

import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    private fun onBoard(row: Int, column: Int, letter: Char = 'A', locked: Boolean = false) =
        StoneOnBoard(letter, 1, "$row-$column-$letter", row, column, locked)

    private fun inHand(letter: Char, value: Int = 1, id: String = letter.toString()) =
        StoneInHand(letter, value, id, userId = 1)

    @Test
    fun `dropping a stone back on its own field keeps it on the board`() {
        val stone = onBoard(3, 3)
        val state = GameState(stonesOnBoard = setOf(stone))

        val moved = state.moveStoneToBoard(stone = stone, rowIndex = 3, columnIndex = 3)

        assertEquals(setOf(stone), moved.stonesOnBoard)
    }

    @Test
    fun `moving a stone to another field leaves exactly one stone`() {
        val stone = onBoard(3, 3)
        val state = GameState(stonesOnBoard = setOf(stone))

        val moved = state.moveStoneToBoard(stone = stone, rowIndex = 4, columnIndex = 7)

        assertEquals(1, moved.stonesOnBoard.size)
        val onlyStone = moved.stonesOnBoard.single()
        assertEquals(4, onlyStone.rowIndex)
        assertEquals(7, onlyStone.columnIndex)
    }

    @Test
    fun `playing a stone from the hand moves it, it is not copied`() {
        val stone = inHand('B')
        val state = GameState(stonesInHand = listOf(stone))

        val moved = state.moveStoneToBoard(stone = stone, rowIndex = 1, columnIndex = 1)

        assertTrue("the stone must leave the hand", moved.stonesInHand.isEmpty())
        assertEquals(1, moved.stonesOnBoard.size)
    }

    @Test
    fun `taking back all unlocked stones leaves the locked ones alone`() {
        val state = GameState(
            stonesOnBoard = setOf(onBoard(0, 0, locked = true), onBoard(0, 1), onBoard(0, 2)),
        )

        val returned = state.moveAllUnlockedStonesToHand()

        assertEquals(setOf(onBoard(0, 0, locked = true)), returned.stonesOnBoard)
        assertEquals(2, returned.stonesInHand.size)
    }

    @Test
    fun `locking in a word scores it and locks every stone`() {
        val state = GameState(
            stonesOnBoard = setOf(
                StoneOnBoard('A', 3, "a", 0, 0, false),
                StoneOnBoard('B', 4, "b", 0, 1, false),
            ),
        )

        val locked = state.lockInWord()

        assertEquals(7L, locked.totalPoints)
        assertTrue(locked.stonesOnBoard.all { it.isLocked })
    }
}
