package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.domain.model.Alignment
import com.felix.greengriffin.board.domain.model.Field
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.board.domain.model.Word
import com.felix.greengriffin.board.domain.model.alignmentOf
import com.felix.greengriffin.board.domain.model.asWord
import com.felix.greengriffin.board.domain.model.sortedForAlignment
import javax.inject.Inject

/**
 * The words the unlocked stones of a placement form, together with the locked stones they
 * touch. Returns an empty list when there is nothing to read as a word — no unlocked
 * stones, stones that do not line up, or a lone stone that touches nothing locked.
 *
 * Alignment is classified by [alignmentOf], the same function placement validation uses,
 * so a placement that validates is read as a word the same way it was judged.
 */
class FindNewlyCreatedWordsUseCase @Inject constructor() {

    operator fun invoke(stonesOnBoard: Set<StoneOnBoard>): List<Word> {
        val unlockedStones = stonesOnBoard.filterNot(StoneOnBoard::isLocked)
        if (unlockedStones.isEmpty()) return emptyList()

        val alignment = alignmentOf(unlockedStones)
        val placedStones = unlockedStones.sortedForAlignment(alignment)
        // Built once per extraction, not once per lookup.
        val board = Board(stonesOnBoard)

        return when (alignment) {
            Alignment.Horizontal -> buildList {
                add(board.horizontalWordThrough(placedStones.first()))
                addAll(placedStones.mapNotNull(board::verticalWordOnLockedStones))
            }

            Alignment.Vertical -> buildList {
                add(board.verticalWordThrough(placedStones.first()))
                addAll(placedStones.mapNotNull(board::horizontalWordOnLockedStones))
            }

            Alignment.Single -> listOfNotNull(
                board.horizontalWordOnLockedStones(placedStones.first()),
                board.verticalWordOnLockedStones(placedStones.first()),
            )

            Alignment.Unaligned -> emptyList()
        }
    }
}

/** The stones on the board, indexed by field so a word can be read off in a straight line. */
private class Board(stones: Set<StoneOnBoard>) {

    private val stonesByField: Map<Field, StoneOnBoard> = stones.associateBy(StoneOnBoard::toField)

    fun horizontalWordThrough(stone: StoneOnBoard): Word {
        val row = stone.rowIndex
        var firstColumn = stone.columnIndex
        while (stoneAt(row = row, column = firstColumn - 1) != null) firstColumn--
        var lastColumn = stone.columnIndex
        while (stoneAt(row = row, column = lastColumn + 1) != null) lastColumn++

        return (firstColumn..lastColumn)
            .mapNotNull { column -> stoneAt(row = row, column = column) }
            .asWord()
    }

    fun verticalWordThrough(stone: StoneOnBoard): Word {
        val column = stone.columnIndex
        var firstRow = stone.rowIndex
        while (stoneAt(row = firstRow - 1, column = column) != null) firstRow--
        var lastRow = stone.rowIndex
        while (stoneAt(row = lastRow + 1, column = column) != null) lastRow++

        return (firstRow..lastRow)
            .mapNotNull { row -> stoneAt(row = row, column = column) }
            .asWord()
    }

    /** The horizontal word through [stone], or `null` when it stands alone between locked stones. */
    fun horizontalWordOnLockedStones(stone: StoneOnBoard): Word? =
        if (hasLockedHorizontalNeighbour(stone)) horizontalWordThrough(stone) else null

    /** The vertical word through [stone], or `null` when it stands alone between locked stones. */
    fun verticalWordOnLockedStones(stone: StoneOnBoard): Word? =
        if (hasLockedVerticalNeighbour(stone)) verticalWordThrough(stone) else null

    private fun hasLockedHorizontalNeighbour(stone: StoneOnBoard): Boolean =
        isLockedAt(row = stone.rowIndex, column = stone.columnIndex - 1) ||
                isLockedAt(row = stone.rowIndex, column = stone.columnIndex + 1)

    private fun hasLockedVerticalNeighbour(stone: StoneOnBoard): Boolean =
        isLockedAt(row = stone.rowIndex - 1, column = stone.columnIndex) ||
                isLockedAt(row = stone.rowIndex + 1, column = stone.columnIndex)

    private fun isLockedAt(row: Int, column: Int): Boolean =
        stoneAt(row = row, column = column)?.isLocked == true

    private fun stoneAt(row: Int, column: Int): StoneOnBoard? =
        stonesByField[Field(row = row, column = column)]
}
