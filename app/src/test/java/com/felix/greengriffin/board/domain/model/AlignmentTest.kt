package com.felix.greengriffin.board.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

private fun boardStone(row: Int, column: Int, letter: Char = 'A') = StoneOnBoard(
    letter = letter,
    value = 1,
    id = "$row-$column-$letter",
    rowIndex = row,
    columnIndex = column,
    isLocked = false,
)

/** The stones of [word] laid out from (row, column) to the right. */
private fun horizontalStones(row: Int, column: Int, word: String) =
    word.mapIndexed { offset, letter -> boardStone(row = row, column = column + offset, letter = letter) }

private fun verticalStones(row: Int, column: Int, word: String) =
    word.mapIndexed { offset, letter -> boardStone(row = row + offset, column = column, letter = letter) }

class AlignmentTest {

    @Test
    fun `a lone stone is a single placement`() {
        assertEquals(Alignment.Single, alignmentOf(listOf(boardStone(2, 2))))
    }

    @Test
    fun `stones sharing a row are horizontal`() {
        assertEquals(Alignment.Horizontal, alignmentOf(horizontalStones(2, 1, "AB")))
    }

    @Test
    fun `stones sharing a column are vertical`() {
        assertEquals(Alignment.Vertical, alignmentOf(verticalStones(1, 2, "AB")))
    }

    @Test
    fun `stones sharing neither a row nor a column are unaligned`() {
        assertEquals(
            Alignment.Unaligned,
            alignmentOf(listOf(boardStone(0, 0), boardStone(1, 1))),
        )
    }

    @Test
    fun `a horizontal placement sorts left to right`() {
        val stones = listOf(boardStone(2, 5, 'C'), boardStone(2, 3, 'A'), boardStone(2, 4, 'B'))

        val sorted = stones.sortedForAlignment(Alignment.Horizontal)

        assertEquals(listOf('A', 'B', 'C'), sorted.map { it.letter })
    }

    @Test
    fun `a vertical placement sorts top to bottom`() {
        val stones = listOf(boardStone(5, 2, 'C'), boardStone(3, 2, 'A'), boardStone(4, 2, 'B'))

        val sorted = stones.sortedForAlignment(Alignment.Vertical)

        assertEquals(listOf('A', 'B', 'C'), sorted.map { it.letter })
    }
}
