package com.felix.greengriffin.board.presentation

import androidx.compose.runtime.Immutable
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.board.domain.model.StoneData
import com.felix.greengriffin.board.domain.model.StoneInBag
import com.felix.greengriffin.board.domain.model.StoneInHand
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.board.domain.model.Word
import com.felix.greengriffin.trails.domain.model.trailLevels

const val DEFAULT_BOARD_SIZE = 10

/** Number of stones a player holds when their hand is full. */
const val HAND_SIZE = 7

@Immutable
data class GameState(
    val gameMode: GameMode = GameMode.FreePlay,
    val isCurrentLevelCompleted: Boolean = false,
    val stonesInHand: List<StoneInHand> = emptyList(),
    val stonesOnBoard: Set<StoneOnBoard> = emptySet(),
    val stonesInBag: Set<StoneInBag> = emptySet(),
    val currentUserId: Int = 1,
    val enteredField: Int? = null,
    val isPromptLoading: Boolean = false,
    val totalPoints: Long = 0,
    val jokerCoordinates: JokerCoordinates? = null,
    private val isCurrentWordValid: Boolean? = null,
    val isValidPlacement: Boolean = false,
    /**
     * The words the current placement forms, computed by the ViewModel whenever the board
     * changes. Derived rather than computed on read, so that composition can read the
     * score without re-running word extraction.
     */
    val newlyCreatedWords: List<Word> = emptyList(),
    val errorText: String? = null,
) {
    data class JokerCoordinates(val row: Int, val column: Int)

    val isJokerSelectorVisible get() = jokerCoordinates != null

    val pointsOfCurrentPlacement: Int = newlyCreatedWords.sumOf(Word::points)

    val boardSize
        get() = if (gameMode is GameMode.Trails) {
            gameMode.level.boardSize
        } else {
            DEFAULT_BOARD_SIZE
        }

    val currentUserStonesInHand get() = stonesInHand.filter { it.userId == currentUserId }
    private val unlockedStonesOnBoard get() = stonesOnBoard.filterNot(StoneOnBoard::isLocked)

    fun isPositionOnBoardAvailable(columnIndex: Int, rowIndex: Int) = stonesOnBoard.none {
        it.columnIndex == columnIndex
                && it.rowIndex == rowIndex
    }

    val numberOfStonesToDraw: Int
        get() {
            // A hand can only ever be over-full through a bug elsewhere, but it must not
            // turn into a negative draw count and throw out of `take()`.
            val missingToFull = (HAND_SIZE - stonesInHand.size).coerceAtLeast(0)
            return minOf(missingToFull, stonesInBag.size)
        }

    val isAbleToSubmit: Boolean
        get() = unlockedStonesOnBoard.isNotEmpty()
                && isValidPlacement
                && isCurrentWordValid == true

    val isAbleToDrawStones: Boolean
        get() = stonesInBag.isNotEmpty() &&
                stonesInHand.size < HAND_SIZE &&
                unlockedStonesOnBoard.isEmpty()

    val hasUnlockedStones: Boolean
        get() = unlockedStonesOnBoard.isNotEmpty()

    fun lockInWord(): GameState {
        return copy(
            totalPoints = totalPoints + pointsOfCurrentPlacement,
            stonesOnBoard = stonesOnBoard.map { it.copy(isLocked = true) }.toSet(),
            // Nothing is newly placed any more, so there is no pending word to score.
            newlyCreatedWords = emptyList(),
        )
    }

    val hasError: Boolean
        get() = errorText != null

    fun moveStoneToHand(stone: StoneData): GameState {
        val movedStone = when (stone) {
            is StoneInBag -> stone.toStoneInHand(currentUserId)
            is StoneOnBoard -> stone.toStoneInHand(currentUserId)
            is StoneInHand -> return this
        }
        return copy(
            stonesInBag = if (stone is StoneInBag) stonesInBag - stone else stonesInBag,
            stonesOnBoard = if (stone is StoneOnBoard) stonesOnBoard - stone else stonesOnBoard,
            stonesInHand = stonesInHand + movedStone
        )
    }

    fun moveAllUnlockedStonesToHand(): GameState {
        val unlocked = unlockedStonesOnBoard.toSet()
        return copy(
            stonesOnBoard = stonesOnBoard - unlocked,
            stonesInHand = stonesInHand + unlocked.map { it.toStoneInHand(currentUserId) }
        )
    }

    fun moveStoneToBoard(
        stone: StoneData,
        rowIndex: Int,
        columnIndex: Int,
    ): GameState {
        val movedStone = when (stone) {
            is StoneInHand -> stone.toStoneOnBoard(rowIndex, columnIndex)
            is StoneOnBoard -> stone.copy(rowIndex = rowIndex, columnIndex = columnIndex)
            is StoneInBag -> return this // todo maybe set error? Invalid operation for stones in the bag
        }
        val updatedStonesInHand = when {
            stone !is StoneInHand -> stonesInHand
            stone in stonesInHand -> stonesInHand - stone
            stone.value == 0 ->
                stonesInHand.find { it.isJoker }?.let { stonesInHand.minus(it) } ?: stonesInHand

            else -> stonesInHand.minus(stone)
        }
        return copy(
            stonesInHand = updatedStonesInHand,
            // Remove first, then add: dropping a stone back on its own field makes
            // `movedStone == stone`, and adding before removing would delete it.
            stonesOnBoard = if (stone is StoneOnBoard) stonesOnBoard - stone + movedStone else stonesOnBoard + movedStone
        )
    }

    fun clearEnteredField(): GameState = copy(enteredField = null)

    fun asSavedGame(): SavedGame = SavedGame(
        gameModeId = gameMode.id,
        levelIndex = gameMode.levelKey,
        totalPoints = totalPoints,
        stonesInHand = stonesInHand,
        stonesOnBoard = stonesOnBoard,
        stonesInBag = stonesInBag,
    )
}

/**
 * The saved game as playable state, or `null` when it names a game mode or level this
 * build no longer has. Restoring it onto some other level would both show the wrong
 * board and overwrite that level's own save.
 */
fun SavedGame.asGameState(): GameState? {
    val gameMode = GameMode.fromId(
        id = gameModeId,
        trailLevel = trailLevels.find { it.index == levelIndex },
    ) ?: return null

    return GameState(
        gameMode = gameMode,
        totalPoints = totalPoints,
        stonesInHand = stonesInHand,
        stonesOnBoard = stonesOnBoard,
        stonesInBag = stonesInBag,
    )
}

sealed interface GameEvent {
    data class StoneDroppedOnBoard(
        val stoneData: StoneData,
        val columnIndex: Int,
        val rowIndex: Int,
    ) : GameEvent

    data class StoneDroppedOnHand(
        val stoneData: StoneData,
    ) : GameEvent

    data class FieldEntered(val index: Int) : GameEvent
    data object SubmitClick : GameEvent
    data object ReturnAllUnlockedStonesClick : GameEvent
    data class JokerSelected(val letter: Char) : GameEvent
    data object JokerSelectorDismissRequested : GameEvent
    data object ClearGameStateClick : GameEvent
}
