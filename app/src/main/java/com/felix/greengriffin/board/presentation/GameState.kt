package com.felix.greengriffin.board.presentation

import androidx.compose.runtime.Immutable
import com.felix.greengriffin.board.domain.model.Alignment
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.board.domain.model.StoneData
import com.felix.greengriffin.board.domain.model.StoneInBag
import com.felix.greengriffin.board.domain.model.StoneInHand
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.board.domain.model.Word
import com.felix.greengriffin.board.domain.model.areHorizontallyAligned
import com.felix.greengriffin.board.domain.model.areVerticallyAligned
import com.felix.greengriffin.board.domain.model.asWord
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
    val errorText: String? = null,
) {
    data class JokerCoordinates(val row: Int, val column: Int)

    val isJokerSelectorVisible get() = jokerCoordinates != null

    val boardSize
        get() = if (gameMode is GameMode.Trails) {
            gameMode.level.boardSize
        } else {
            DEFAULT_BOARD_SIZE
        }

    val currentUserStonesInHand get() = stonesInHand.filter { it.userId == currentUserId }
    private val unlockedStonesOnBoard get() = stonesOnBoard.filterNot(StoneOnBoard::isLocked)
    private val unlockedStonesAlignment
        get() = when {
            unlockedStonesOnBoard.size == 1 -> Alignment.Single
            unlockedStonesOnBoard.areHorizontallyAligned -> Alignment.Horizontal
            unlockedStonesOnBoard.areVerticallyAligned -> Alignment.Vertical
            else -> Alignment.Unaligned
        }
    private val sortedUnlockedStonesOnBoard
        get() = when (unlockedStonesAlignment) {
            Alignment.Horizontal -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::columnIndex)
            Alignment.Vertical -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::rowIndex)
            else -> unlockedStonesOnBoard
        }

    private fun hasVerticalConnection(stone: StoneOnBoard): Boolean =
        stonesOnBoard.any { lockedStone ->
            lockedStone.isLocked &&
                    lockedStone.columnIndex == stone.columnIndex &&
                    (lockedStone.isAbove(stone) || lockedStone.isBelow(stone))
        }

    private fun hasHorizontalConnection(stone: StoneOnBoard): Boolean =
        stonesOnBoard.any { lockedStone ->
            lockedStone.isLocked &&
                    lockedStone.rowIndex == stone.rowIndex &&
                    (lockedStone.isToLeftOf(stone) || lockedStone.isToRightOf(stone))
        }

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
            stonesOnBoard = stonesOnBoard.map { it.copy(isLocked = true) }.toSet()
        )
    }

    private val newlyCreatedWords: List<Word>
        get() {
            if (sortedUnlockedStonesOnBoard.isEmpty()) return emptyList()

            return when (unlockedStonesAlignment) {
                Alignment.Horizontal -> getWordsFromHorizontalPlacement()
                Alignment.Vertical -> getWordsFromVerticalPlacement()
                Alignment.Single -> getWordsFromSinglePlacement()
                else -> emptyList()
            }
        }

    val newlyCreatedWordsAsStrings: List<String> get() = newlyCreatedWords.map { it.asString }

    val pointsOfCurrentPlacement: Int get() = newlyCreatedWords.sumOf { it.points }

    private fun getWordsFromSinglePlacement(): List<Word> {
        val createdWords = mutableListOf<Word>()
        val singleStone = sortedUnlockedStonesOnBoard.first()

        // Check for horizontal word
        if (hasHorizontalConnection(singleStone)) {
            val horizontalWord = createHorizontalWordAt(singleStone)
            createdWords.add(horizontalWord)
        }

        // Check for vertical word
        if (hasVerticalConnection(singleStone)) {
            val verticalWord = createVerticalWordAt(singleStone)
            createdWords.add(verticalWord)
        }

        return createdWords
    }

    val hasError: Boolean
        get() = errorText != null

    private fun getWordsFromHorizontalPlacement(): List<Word> {
        val createdWords = mutableListOf<Word>()
        val mainWord = getMainHorizontalWord()
        createdWords.add(mainWord)
        createdWords.addAll(getPerpendicularWordsFromHorizontalPlacement())
        return createdWords
    }

    private fun getWordsFromVerticalPlacement(): List<Word> {
        val createdWords = mutableListOf<Word>()
        val mainWord = getMainVerticalWord()
        createdWords.add(mainWord)
        createdWords.addAll(getPerpendicularWordsFromVerticalPlacement())
        return createdWords
    }

    private fun getPerpendicularWordsFromHorizontalPlacement(): List<Word> {
        val perpendicularWords = mutableListOf<Word>()

        unlockedStonesOnBoard.forEach { unlockedStone ->
            if (hasVerticalConnection(unlockedStone)) {
                val verticalWord = createVerticalWordAt(unlockedStone)
                perpendicularWords.add(verticalWord)
            }
        }

        return perpendicularWords
    }

    private fun getPerpendicularWordsFromVerticalPlacement(): List<Word> {
        val perpendicularWords = mutableListOf<Word>()

        unlockedStonesOnBoard.forEach { unlockedStone ->
            if (hasHorizontalConnection(unlockedStone)) {
                val horizontalWord = createHorizontalWordAt(unlockedStone)
                perpendicularWords.add(horizontalWord)
            }
        }

        return perpendicularWords
    }

    private fun getMainHorizontalWord(): Word {
        val firstUnlockedStone = sortedUnlockedStonesOnBoard.first()
        val firstStoneColumnIndex = findIndexOfFirstLetterOfHorizontalWord(
            firstUnlockedStone.columnIndex,
            firstUnlockedStone.rowIndex
        )
        val lastStoneColumnIndex = findIndexOfLastLetterOfHorizontalWord(
            firstUnlockedStone.columnIndex,
            firstUnlockedStone.rowIndex
        )

        return (firstStoneColumnIndex..lastStoneColumnIndex)
            .mapNotNull { columnIndex ->
                stonesOnBoard.find { it.columnIndex == columnIndex && it.rowIndex == firstUnlockedStone.rowIndex }
            }.asWord()
    }

    private fun getMainVerticalWord(): Word {
        val firstUnlockedStone = sortedUnlockedStonesOnBoard.first()
        val firstStoneRowIndex = findIndexOfFirstLetterOfVerticalWord(
            firstUnlockedStone.columnIndex,
            firstUnlockedStone.rowIndex
        )
        val lastStoneRowIndex = findIndexOfLastLetterOfVerticalWord(
            firstUnlockedStone.columnIndex,
            firstUnlockedStone.rowIndex
        )

        return (firstStoneRowIndex..lastStoneRowIndex)
            .mapNotNull { rowIndex ->
                stonesOnBoard.find { it.columnIndex == firstUnlockedStone.columnIndex && it.rowIndex == rowIndex }
            }
            .asWord()
    }

    private fun createVerticalWordAt(stone: StoneOnBoard): Word {
        val firstVerticalIndex =
            findIndexOfFirstLetterOfVerticalWord(stone.columnIndex, stone.rowIndex)
        val lastVerticalIndex =
            findIndexOfLastLetterOfVerticalWord(stone.columnIndex, stone.rowIndex)

        return (firstVerticalIndex..lastVerticalIndex)
            .mapNotNull { rowIndex ->
                stonesOnBoard.find { it.columnIndex == stone.columnIndex && it.rowIndex == rowIndex }
            }
            .asWord()
    }

    private fun createHorizontalWordAt(stone: StoneOnBoard): Word {
        val firstHorizontalIndex =
            findIndexOfFirstLetterOfHorizontalWord(stone.columnIndex, stone.rowIndex)
        val lastHorizontalIndex =
            findIndexOfLastLetterOfHorizontalWord(stone.columnIndex, stone.rowIndex)

        return (firstHorizontalIndex..lastHorizontalIndex)
            .mapNotNull { columnIndex ->
                stonesOnBoard.find { it.columnIndex == columnIndex && it.rowIndex == stone.rowIndex }
            }
            .asWord()
    }

    private fun findIndexOfFirstLetterOfHorizontalWord(startColumn: Int, rowIndex: Int): Int {
        val columnIndexes = stonesOnBoard.associateBy { it.columnIndex to it.rowIndex }

        return generateSequence(startColumn) { it - 1 }
            .takeWhile { (it to rowIndex) in columnIndexes }
            .last()
    }

    private fun findIndexOfLastLetterOfHorizontalWord(startColumn: Int, rowIndex: Int): Int {
        val columnIndexes = stonesOnBoard.associateBy { it.columnIndex to it.rowIndex }

        return generateSequence(startColumn) { it + 1 }
            .takeWhile { (it to rowIndex) in columnIndexes }
            .last()
    }

    private fun findIndexOfFirstLetterOfVerticalWord(columnIndex: Int, startRow: Int): Int {
        val rowIndexes = stonesOnBoard.associateBy { it.columnIndex to it.rowIndex }

        return generateSequence(startRow) { it - 1 }
            .takeWhile { (columnIndex to it) in rowIndexes }
            .last()
    }

    private fun findIndexOfLastLetterOfVerticalWord(columnIndex: Int, startRow: Int): Int {
        val rowIndexes = stonesOnBoard.associateBy { it.columnIndex to it.rowIndex }

        return generateSequence(startRow) { it + 1 }
            .takeWhile { (columnIndex to it) in rowIndexes }
            .last()
    }

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
