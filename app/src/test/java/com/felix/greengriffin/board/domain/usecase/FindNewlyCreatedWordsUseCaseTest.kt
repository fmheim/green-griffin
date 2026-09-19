package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.board.domain.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun boardStone(
    row: Int,
    column: Int,
    letter: Char = 'A',
    value: Int = 1,
    locked: Boolean = false,
) = StoneOnBoard(
    letter = letter,
    value = value,
    id = "$row-$column-$letter",
    rowIndex = row,
    columnIndex = column,
    isLocked = locked,
)

/** The stones of `word`, e.g. "HAUS", laid out from (row, column) to the right. */
private fun horizontalStones(
    row: Int,
    column: Int,
    word: String,
    value: Int = 1,
    locked: Boolean = false,
) = word.mapIndexed { offset, letter ->
    boardStone(row = row, column = column + offset, letter = letter, value = value, locked = locked)
}

private fun verticalStones(
    row: Int,
    column: Int,
    word: String,
    value: Int = 1,
    locked: Boolean = false,
) = word.mapIndexed { offset, letter ->
    boardStone(row = row + offset, column = column, letter = letter, value = value, locked = locked)
}

class FindNewlyCreatedWordsUseCaseTest {

    private val findNewlyCreatedWords = FindNewlyCreatedWordsUseCase()

    private fun wordsOf(vararg stones: StoneOnBoard) =
        findNewlyCreatedWords(stonesOnBoard = stones.toSet())

    private fun wordStrings(vararg stones: StoneOnBoard) =
        wordsOf(*stones).map(Word::asString)

    @Test
    fun `an empty board forms no word`() {
        assertEquals(emptyList<Word>(), wordsOf())
    }

    @Test
    fun `a board of only locked stones forms no new word`() {
        val locked = horizontalStones(0, 0, "HAUS", locked = true).toTypedArray()

        assertEquals(emptyList<Word>(), wordsOf(*locked))
    }

    @Test
    fun `a horizontal placement forms the word it spells`() {
        val placed = horizontalStones(3, 2, "HAUS").toTypedArray()

        assertEquals(listOf("HAUS"), wordStrings(*placed))
    }

    @Test
    fun `a vertical placement forms the word it spells`() {
        val placed = verticalStones(1, 4, "HAUS").toTypedArray()

        assertEquals(listOf("HAUS"), wordStrings(*placed))
    }

    @Test
    fun `unaligned stones form no word`() {
        assertEquals(emptyList<Word>(), wordsOf(boardStone(0, 0, 'A'), boardStone(1, 1, 'B')))
    }

    @Test
    fun `a gap between placed stones stops the word at the gap`() {
        // (3,2) and (3,4) with nothing in between: only the run through the first stone
        // is a word. The placement is rejected as `HasGaps` before this ever matters.
        val words = wordStrings(boardStone(3, 2, 'A'), boardStone(3, 4, 'B'))

        assertEquals(listOf("A"), words)
    }

    @Test
    fun `a placement reads through the locked stones it grows from`() {
        val locked = horizontalStones(3, 0, "HA", locked = true).toTypedArray()
        val placed = horizontalStones(3, 2, "US").toTypedArray()

        assertEquals(listOf("HAUS"), wordStrings(*locked, *placed))
    }

    @Test
    fun `a placement between two locked runs joins them into one word`() {
        val lockedLeft = boardStone(3, 0, 'H', locked = true)
        val lockedRight = horizontalStones(3, 2, "US", locked = true).toTypedArray()
        val placed = boardStone(3, 1, 'A')

        assertEquals(listOf("HAUS"), wordStrings(lockedLeft, placed, *lockedRight))
    }

    @Test
    fun `a lone stone touching nothing locked forms no word`() {
        assertEquals(emptyList<Word>(), wordsOf(boardStone(3, 3, 'A')))
    }

    @Test
    fun `a lone stone extending a locked word forms that word`() {
        val locked = horizontalStones(3, 0, "HAU", locked = true).toTypedArray()

        assertEquals(listOf("HAUS"), wordStrings(*locked, boardStone(3, 3, 'S')))
    }

    @Test
    fun `a lone stone crossing two locked words forms both`() {
        // Locked "HA" to the left of (3,2) and locked "T" above it: the placed 'S'
        // completes a horizontal and a vertical word at once.
        val lockedHorizontal = horizontalStones(3, 0, "HA", locked = true).toTypedArray()
        val lockedVertical = boardStone(2, 2, 'T', locked = true)

        assertEquals(
            listOf("HAS", "TS"),
            wordStrings(*lockedHorizontal, lockedVertical, boardStone(3, 2, 'S')),
        )
    }

    @Test
    fun `a horizontal placement also forms the vertical words it crosses`() {
        val lockedAbove = boardStone(2, 1, 'T', locked = true)
        val lockedBelow = boardStone(4, 2, 'X', locked = true)
        val placed = horizontalStones(3, 0, "ABC").toTypedArray()

        assertEquals(listOf("ABC", "TB", "CX"), wordStrings(lockedAbove, lockedBelow, *placed))
    }

    @Test
    fun `a vertical placement also forms the horizontal words it crosses`() {
        val lockedLeft = boardStone(1, 0, 'T', locked = true)
        val placed = verticalStones(0, 1, "AB").toTypedArray()

        assertEquals(listOf("AB", "TB"), wordStrings(lockedLeft, *placed))
    }

    @Test
    fun `a word scores the value of every stone in it, placed or already locked`() {
        val locked = horizontalStones(3, 0, "HA", value = 4, locked = true).toTypedArray()
        val placed = horizontalStones(3, 2, "US", value = 3).toTypedArray()

        assertEquals(4 + 4 + 3 + 3, wordsOf(*locked, *placed).single().points)
    }

    @Test
    fun `a joker scores nothing but still spells its letter`() {
        // A played joker keeps value 0 and carries the letter the player chose.
        val joker = boardStone(3, 2, 'U', value = 0)
        val locked = horizontalStones(3, 0, "HA", value = 2, locked = true).toTypedArray()
        val placed = boardStone(3, 3, 'S', value = 5)

        val word = wordsOf(*locked, joker, placed).single()

        assertEquals("HAUS", word.asString)
        assertEquals(2 + 2 + 0 + 5, word.points)
    }

    @Test
    fun `a crossing stone is scored once in each word it belongs to`() {
        val lockedAbove = boardStone(2, 1, 'T', value = 7, locked = true)
        val placed = horizontalStones(3, 0, "AB", value = 1).toTypedArray()

        assertEquals(listOf(2, 8), wordsOf(lockedAbove, *placed).map(Word::points))
    }

    @Test
    fun `every valid placement forms at least one word`() {
        val isPlacementValid = IsPlacementValidUseCase()
        val locked = setOf(
            boardStone(3, 3, 'H', locked = true),
            boardStone(3, 4, 'A', locked = true),
            boardStone(4, 4, 'B', locked = true),
        )

        // Every placement of one or two stones on the fields around the locked ones.
        val candidateFields = (1..6).flatMap { row -> (1..6).map { column -> row to column } }
            .filter { (row, column) -> locked.none { it.rowIndex == row && it.columnIndex == column } }
        val singlePlacements = candidateFields.map { (row, column) ->
            listOf(boardStone(row, column, 'X'))
        }
        val pairPlacements = candidateFields.flatMap { first ->
            candidateFields.filterNot { it == first }.map { second ->
                listOf(
                    boardStone(first.first, first.second, 'X'),
                    boardStone(second.first, second.second, 'Y'),
                )
            }
        }
        val placements = singlePlacements + pairPlacements

        var validPlacements = 0
        for (placement in placements) {
            val board = locked + placement
            val isValid = isPlacementValid(
                stonesOnBoard = board,
                gameMode = GameMode.FreePlay,
            ) == PlacementValidation.Valid
            if (!isValid) continue
            validPlacements++

            assertTrue(
                "a valid placement must form a word: $placement",
                findNewlyCreatedWords(stonesOnBoard = board).isNotEmpty(),
            )
        }

        // Without this the loop would pass vacuously if nothing validated any more.
        assertTrue(
            "no placement was valid, so the invariant was never exercised",
            validPlacements > 0,
        )
    }

    @Test
    fun `a valid first move on an empty board also forms a word`() {
        val isPlacementValid = IsPlacementValidUseCase()
        val placed = horizontalStones(3, 3, "AB").toSet()

        assertEquals(
            PlacementValidation.Valid,
            isPlacementValid(stonesOnBoard = placed, gameMode = GameMode.FreePlay),
        )
        assertEquals(listOf("AB"), findNewlyCreatedWords(placed).map(Word::asString))
    }
}
