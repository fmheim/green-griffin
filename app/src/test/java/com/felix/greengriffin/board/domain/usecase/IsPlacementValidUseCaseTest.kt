package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.presentation.Field
import com.felix.greengriffin.board.presentation.GameMode
import com.felix.greengriffin.board.presentation.TrailLevel
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.board.presentation.trailLevels
import org.junit.Assert.assertEquals
import org.junit.Test

private fun stone(row: Int, column: Int, letter: Char = 'A', locked: Boolean = false) =
    StoneOnBoard(
        letter = letter,
        value = 1,
        id = "$row-$column-$letter",
        rowIndex = row,
        columnIndex = column,
        isLocked = locked,
    )

class IsPlacementValidUseCaseTest {

    private val isPlacementValid = IsPlacementValidUseCase()

    private fun validate(vararg stones: StoneOnBoard, gameMode: GameMode = GameMode.FreePlay) =
        isPlacementValid(stonesOnBoard = stones.toSet(), gameMode = gameMode)

    @Test
    fun `an empty board has nothing to validate`() {
        assertEquals(PlacementValidation.NoStonesPlaced, validate())
    }

    @Test
    fun `a board with only locked stones has nothing new to validate`() {
        assertEquals(
            PlacementValidation.NoStonesPlaced,
            validate(stone(0, 0, locked = true), stone(0, 1, locked = true)),
        )
    }

    @Test
    fun `stones that share neither a row nor a column are not aligned`() {
        assertEquals(PlacementValidation.NotAligned, validate(stone(0, 0), stone(1, 1)))
    }

    @Test
    fun `a horizontal placement with an empty cell in between has gaps`() {
        assertEquals(PlacementValidation.HasGaps, validate(stone(5, 0), stone(5, 2)))
    }

    @Test
    fun `a vertical placement with an empty cell in between has gaps`() {
        assertEquals(PlacementValidation.HasGaps, validate(stone(0, 3), stone(2, 3)))
    }

    @Test
    fun `a gap closed by an already locked stone is not a gap`() {
        assertEquals(
            PlacementValidation.Valid,
            validate(stone(5, 0), stone(5, 1, locked = true), stone(5, 2)),
        )
    }

    @Test
    fun `the first word may stand on its own`() {
        assertEquals(PlacementValidation.Valid, validate(stone(5, 5), stone(5, 6)))
    }

    @Test
    fun `a single stone is not a placement on its own`() {
        // Connectivity is decided by neighbours, so a one-letter opening move is rejected.
        assertEquals(PlacementValidation.NotConnected, validate(stone(4, 4)))
    }

    @Test
    fun `a later word must touch what is already on the board`() {
        assertEquals(
            PlacementValidation.NotConnected,
            validate(stone(0, 0, locked = true), stone(5, 5), stone(5, 6)),
        )
    }

    @Test
    fun `a later word touching a locked stone is valid`() {
        assertEquals(
            PlacementValidation.Valid,
            validate(stone(5, 4, locked = true), stone(5, 5), stone(5, 6)),
        )
    }

    @Test
    fun `a trail must start on a start field`() {
        val level = trailLevels.first { it.index == 1 } // start fields: column 0
        assertEquals(
            PlacementValidation.Violation(GameModeViolation.FirstWordNotOnCorrectStartPosition),
            validate(stone(0, 4), stone(0, 5), gameMode = GameMode.Trails(level)),
        )
    }

    @Test
    fun `a trail starting on a start field is valid`() {
        val level = trailLevels.first { it.index == 1 }
        assertEquals(
            PlacementValidation.Valid,
            validate(stone(0, 0), stone(0, 1), gameMode = GameMode.Trails(level)),
        )
    }

    @Test
    fun `a trail may be continued away from the start fields`() {
        val level = trailLevels.first { it.index == 1 }
        assertEquals(
            PlacementValidation.Valid,
            validate(
                stone(0, 0, locked = true),
                stone(0, 1, locked = true),
                stone(0, 2),
                stone(0, 3),
                gameMode = GameMode.Trails(level),
            ),
        )
    }

    @Test
    fun `a level without start fields rejects every placement`() {
        // Why MainActivity must not invent a level for an unknown index: with no start
        // fields there is no legal first move, so the level cannot be played at all.
        val unplayable = TrailLevel(
            index = 0,
            boardSize = 10,
            startFields = emptySet(),
            goalFields = emptySet(),
            blockedField = emptySet(),
        )
        assertEquals(
            PlacementValidation.Violation(GameModeViolation.FirstWordNotOnCorrectStartPosition),
            validate(stone(0, 0), stone(0, 1), gameMode = GameMode.Trails(unplayable)),
        )
    }

    @Test
    fun `start fields are matched by row and column, not by a flat index`() {
        val level = trailLevels.first { it.index == 2 } // start fields: row 0
        assertEquals(
            PlacementValidation.Valid,
            validate(stone(0, 7), stone(1, 7), gameMode = GameMode.Trails(level)),
        )
        assertEquals(
            PlacementValidation.Violation(GameModeViolation.FirstWordNotOnCorrectStartPosition),
            validate(stone(7, 0), stone(7, 1), gameMode = GameMode.Trails(level)),
        )
    }

    @Test
    fun `fields are compared by value`() {
        assertEquals(Field(row = 3, column = 4), stone(3, 4).toField())
    }
}
