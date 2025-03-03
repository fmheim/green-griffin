package com.felix.greengriffin.board.presentation


import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.BuildConfig
import com.felix.greengriffin.board.presentation.GameState.Alignment.Horizontal
import com.felix.greengriffin.board.presentation.GameState.Alignment.Single
import com.felix.greengriffin.board.presentation.GameState.Alignment.Unaligned
import com.felix.greengriffin.board.presentation.GameState.Alignment.Vertical
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.board.presentation.components.Word
import com.felix.greengriffin.board.presentation.components.asWord
import com.felix.greengriffin.util.extensions.list.isEmptyOrOnlyNulls
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@Immutable
data class GameState(
    val stonesInHand: List<StoneInHand> = emptyList(),
    val stonesOnBoard: Set<StoneOnBoard> = emptySet(),
    val stonesInBag: Set<StoneInBag> = emptySet(),
    val currentUserId: Int = 1,
    val enteredField: Int? = null,
    val isPromptLoading: Boolean = false,
    val totalPoints: Long = 0,
    private val isCurrentWordValid: Boolean? = null
) {
    enum class Alignment {
        Horizontal, Vertical, Single, Unaligned
    }

    val currentUserStonesInHand get() = stonesInHand.filter { it.userId == currentUserId }
    private val unlockedStonesOnBoard get() = stonesOnBoard.filterNot(StoneOnBoard::isLocked)
    private val unlockedStonesAlignment
        get() = when {
            unlockedStonesOnBoard.size == 1 -> Single
            unlockedStonesOnBoard.areHorizontallyAligned -> Horizontal
            unlockedStonesOnBoard.areVerticallyAligned -> Vertical
            else -> Unaligned
        }
    private val sortedUnlockedStonesOnBoard
        get() = when (unlockedStonesAlignment) {
            Horizontal -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::columnIndex)
            Vertical -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::rowIndex)
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
            val missingToFull = 7 - stonesInHand.size
            return when {
                stonesInBag.size >= missingToFull -> missingToFull
                else -> stonesInBag.size
            }
        }

    val isValidWordPlacement: Boolean
        get() {
            if (unlockedStonesOnBoard.isEmpty()) return false // Not valid: Need to place at least one stone

            val firstStone = sortedUnlockedStonesOnBoard.first()
            val lastStone = sortedUnlockedStonesOnBoard.last()

            when (unlockedStonesAlignment) {
                Unaligned -> return false // Not valid: Stones need to be in same row or column
                Horizontal -> {
                    for (columnIndex in firstStone.columnIndex..lastStone.columnIndex) {
                        if (stonesOnBoard.none { it.columnIndex == columnIndex && it.rowIndex == firstStone.rowIndex }) {
                            return false // Gap found, placement is invalid
                        }
                    }
                }

                Vertical -> { // Vertical alignment
                    for (rowIndex in firstStone.rowIndex..lastStone.rowIndex) {
                        if (stonesOnBoard.none { it.rowIndex == rowIndex && it.columnIndex == firstStone.columnIndex }) {
                            return false // Gap found, placement is invalid
                        }
                    }
                }

                Single -> Unit // Continue, single stones can not have gap
            }

            val lockedNeighbourStones = mutableListOf<StoneData>()
            for (unlockedStone in sortedUnlockedStonesOnBoard) {
                val neighbourStones = listOf(
                    stonesOnBoard::find { it.isToLeftOf(unlockedStone) },
                    stonesOnBoard::find { it.isAbove(unlockedStone) },
                    stonesOnBoard::find { it.isToRightOf(unlockedStone) },
                    stonesOnBoard::find { it.isBelow(unlockedStone) }
                )

                if (neighbourStones.isEmptyOrOnlyNulls()) return false // Not valid: all stones need to be connected
                val lockedStones = neighbourStones.filter { it?.isLocked == true }.filterNotNull()
                lockedNeighbourStones.addAll(lockedStones)
            }
            return lockedNeighbourStones.isNotEmpty() || stonesOnBoard.none { it.isLocked } // connected to a locked stone or first move
        }

    val isAbleToSubmit: Boolean
        get() =
            unlockedStonesOnBoard.isNotEmpty()
                    && isValidWordPlacement
                    && isCurrentWordValid == true

    fun lockInWord(): GameState {
        return copy(
            totalPoints = totalPoints + pointsOfCurrentPlacement,
            stonesOnBoard = stonesOnBoard.map { it.copy(isLocked = true) }.toSet()
        )
    }

    val newlyCreatedWords: List<Word>
        get() {
            if (sortedUnlockedStonesOnBoard.isEmpty()) return emptyList()

            return when (unlockedStonesAlignment) {
                Horizontal -> getWordsFromHorizontalPlacement()
                Vertical -> getWordsFromVerticalPlacement()
                Single -> getWordsFromSinglePlacement()
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

    fun moveStoneToBoard(
        stone: StoneData,
        rowIndex: Int,
        columnIndex: Int
    ): GameState {
        val movedStone = when (stone) {
            is StoneInHand -> stone.toStoneOnBoard(rowIndex, columnIndex)
            is StoneOnBoard -> stone.copy(rowIndex = rowIndex, columnIndex = columnIndex)
            is StoneInBag -> return this // todo maybe set error? Invalid operation for stones in the bag
        }
        return copy(
            stonesInHand = if (stone is StoneInHand) stonesInHand - stone else stonesInHand,
            stonesOnBoard = if (stone is StoneOnBoard) stonesOnBoard + movedStone - stone else stonesOnBoard + movedStone
        )
    }

    fun clearEnteredField(): GameState = copy(enteredField = null)

}

sealed interface GameEvent {
    data class StoneDroppedOnBoard(
        val stoneData: StoneData,
        val columnIndex: Int,
        val rowIndex: Int
    ) : GameEvent

    data class StoneDroppedOnHand(
        val stoneData: StoneData
    ) : GameEvent

    data class FieldEntered(val index: Int) : GameEvent
    data object DrawStonesClick : GameEvent
    data object SubmitClick : GameEvent
}


@HiltViewModel
class WordPlacementViewModel @Inject constructor(
    private val generativeModel: GenerativeModel // todo move to repo
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state get() = _state.asStateFlow()

    init {
        _state.update {
            it.copy(stonesInBag = initialStonesInBag)
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.FieldEntered -> _state.update { it.copy(enteredField = event.index) }
            is GameEvent.StoneDroppedOnHand -> moveStoneToHand(event.stoneData)
            GameEvent.DrawStonesClick -> drawStones()
            GameEvent.SubmitClick -> onSubmitClick()
            is GameEvent.StoneDroppedOnBoard -> moveStoneToBoard(
                stoneData = event.stoneData,
                rowIndex = event.rowIndex,
                columnIndex = event.columnIndex
            )
        }
    }

    private fun drawStones() =
        _state.value
            .stonesInBag
            .shuffled()
            .take(_state.value.numberOfStonesToDraw)
            .forEach { stone ->
                _state.update { currentState ->
                    currentState.moveStoneToHand(stone)
                }
            }


    private fun moveStoneToBoard(
        stoneData: StoneData,
        rowIndex: Int,
        columnIndex: Int
    ) {
        println("StoneDroppedOnBoard")

        _state.update { currentState ->
            currentState
                .moveStoneToBoard(
                    stone = stoneData,
                    rowIndex = rowIndex,
                    columnIndex = columnIndex
                )
        }

        onStoneMovedToBoard()
    }

    private fun onStoneMovedToBoard() {
        val isValid = _state.value.isValidWordPlacement
        if (isValid) {
            val words =
                _state.value.newlyCreatedWordsAsStrings // Todo: Also get vertical words (unlocked)
            sendPrompt(
                "Decide if all the given words are valid according to german scrabble rules: $words\n" +
                        "\n" +
                        "All words that are listed as keyword entries in the underlying dictionary are permitted." +
                        "in the dictionary used. This also includes colloquial expressions, foreign words," +
                        "technical terms etc. The German grammatical forms of these words are also permitted." +
                        "inflected forms of these words." +
                        "Abbreviations, names, prefixes and suffixes are not permitted. Also inadmissible are" +
                        "words that are not included in the dictionary used as a basis, that are written with a hyphen" +
                        "or which contain an ellipsis." +
                        "\n" +
                        "Return only:\n" +
                        "true\n" +
                        "or\n" +
                        "false\n" +
                        "\n" +
                        "No other text or explanation."
            )
        }
    }

    private fun moveStoneToHand(stoneData: StoneData) {
        println("StoneDroppedOnHand")
        _state.update { currentState ->
            currentState
                .moveStoneToHand(stoneData)
                .clearEnteredField()
        }
    }


    private fun onSubmitClick() {
        println("SubmitClick")
        val isValid = _state.value.isAbleToSubmit
        println("isValid: $isValid")
        if (!isValid) {
            // show error
            println("Invalid word placement")
            return
        }
        _state.update { it.lockInWord() }
    }

    private fun sendPrompt(
        prompt: String
    ) {
        _state.update { it.copy(isPromptLoading = true, isCurrentWordValid = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    text(prompt)
                })
                println("input: $prompt")
                response.text?.let { outputContent ->
                    println("output: $outputContent")
                }
                val isValid = response.text?.trim()?.lowercase()?.contains("true") == true
                viewModelScope.launch(Dispatchers.Main) {
                    _state.update {
                        if (isValid) {
                            it.copy(isPromptLoading = false, isCurrentWordValid = true)
                        } else {
                            it.copy(isPromptLoading = false, isCurrentWordValid = false)
                        }
                    }
                }

            } catch (e: Exception) {
                println("send prompt failed: $e")
                viewModelScope.launch(Dispatchers.Main) {
                    _state.update { it.copy(isPromptLoading = false, isCurrentWordValid = null) }
                }
            }
        }
    }
}

data class LetterProperties(
    val value: Int,
    val frequency: Int
)

val letterPropertiesMap = mapOf(
    'A' to LetterProperties(frequency = 10, value = 1),
    'Ä' to LetterProperties(frequency = 1, value = 6),
    'B' to LetterProperties(frequency = 2, value = 3),
    'C' to LetterProperties(frequency = 2, value = 4),
    'D' to LetterProperties(frequency = 4, value = 1),
    'E' to LetterProperties(frequency = 15, value = 1),
    'F' to LetterProperties(frequency = 2, value = 4),
    'G' to LetterProperties(frequency = 3, value = 2),
    'H' to LetterProperties(frequency = 2, value = 4),
    'I' to LetterProperties(frequency = 6, value = 1),
    'J' to LetterProperties(frequency = 1, value = 10),
    'K' to LetterProperties(frequency = 4, value = 2),
    'L' to LetterProperties(frequency = 4, value = 1),
    'M' to LetterProperties(frequency = 3, value = 3),
    'N' to LetterProperties(frequency = 6, value = 1),
    'O' to LetterProperties(frequency = 4, value = 1),
    'Ö' to LetterProperties(frequency = 1, value = 8),
    'P' to LetterProperties(frequency = 2, value = 4),
    'Q' to LetterProperties(frequency = 1, value = 10),
    'R' to LetterProperties(frequency = 6, value = 1),
    'S' to LetterProperties(frequency = 6, value = 1),
    'T' to LetterProperties(frequency = 6, value = 1),
    'U' to LetterProperties(frequency = 4, value = 1),
    'Ü' to LetterProperties(frequency = 1, value = 6),
    'V' to LetterProperties(frequency = 2, value = 6),
    'W' to LetterProperties(frequency = 2, value = 3),
    'X' to LetterProperties(frequency = 1, value = 10),
    'Y' to LetterProperties(frequency = 1, value = 10),
    'Z' to LetterProperties(frequency = 2, value = 3),
    ' ' to LetterProperties(frequency = 2, value = 0)
)

val initialStonesInBag: Set<StoneInBag> =
    letterPropertiesMap.flatMap { (letter, properties) ->
        List(properties.frequency) {
            StoneInBag(
                letter = letter,
                value = properties.value,
                id = UUID.randomUUID().toString()
            )
        }
    }.toSet()

val alphabet = letterPropertiesMap.keys.filter { !it.isWhitespace() }

// StoneListExtensions

val List<StoneOnBoard>.areHorizontallyAligned: Boolean get() = map { it.rowIndex }.toSet().size == 1
val List<StoneOnBoard>.areVerticallyAligned: Boolean get() = map { it.columnIndex }.toSet().size == 1