package com.felix.greengriffin.board.presentation


import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.BuildConfig
import com.felix.greengriffin.board.presentation.ScrabbleState.Alignment.Horizontal
import com.felix.greengriffin.board.presentation.ScrabbleState.Alignment.Single
import com.felix.greengriffin.board.presentation.ScrabbleState.Alignment.Unaligned
import com.felix.greengriffin.board.presentation.ScrabbleState.Alignment.Vertical
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


@Immutable
data class ScrabbleState(
    val stonesInHand: List<StoneInHand> = emptyList(),
    val stonesOnBoard: Set<StoneOnBoard> = emptySet(),
    val stonesInBag: Set<StoneInBag> = emptySet(),
    val currentUserId: Int = 1,
    val enteredField: Int? = null
) {
    val currentUserStonesInHand get() = stonesInHand.filter { it.userId == currentUserId }
    val unlockedStonesOnBoard get() = stonesOnBoard.filterNot(StoneOnBoard::isLocked)
    val unlockedStonesAlignment
        get() = when {
            unlockedStonesOnBoard.size == 1 -> Single
            unlockedStonesOnBoard.areHorizontallyAligned -> Horizontal
            unlockedStonesOnBoard.areVerticallyAligned -> Vertical
            else -> Unaligned
        }
    val sortedUnlockedStonesOnBoard
        get() = when (unlockedStonesAlignment) {
            Horizontal -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::columnIndex)
            Vertical -> unlockedStonesOnBoard.sortedBy(StoneOnBoard::rowIndex)
            else -> unlockedStonesOnBoard
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

    enum class Alignment {
        Horizontal, Vertical, Unaligned, Single
    }

    val isValidWordPlacement: Boolean
        get() {
            if (unlockedStonesOnBoard.isEmpty()) return false // Not valid: Need to place at least one stone
            if (unlockedStonesAlignment == Unaligned) return false // Not valid: Stones need to be in same row or column
            var isConnectedToLocked = false
            val lockedConnectedStones = mutableListOf<StoneData>()
            for (unlockedStone in sortedUnlockedStonesOnBoard) {
                val stoneToTheLeft = stonesOnBoard.find { it.isToLeftOf(unlockedStone) }
                if (stoneToTheLeft?.isLocked == true) {
                    isConnectedToLocked = true
                    lockedConnectedStones.add(stoneToTheLeft)
                    continue
                }

                val stoneAbove = stonesOnBoard.find { it.isAbove(unlockedStone) }
                if (stoneAbove?.isLocked == true) {
                    isConnectedToLocked = true
                    lockedConnectedStones.add(stoneAbove)
                    continue
                }

                val stoneToTheRight = stonesOnBoard.find { it.isToRightOf(unlockedStone) }
                if (stoneToTheRight?.isLocked == true) {
                    isConnectedToLocked = true
                    lockedConnectedStones.add(stoneToTheRight)
                    continue
                }

                val stoneBelow = stonesOnBoard.find { it.isBelow(unlockedStone) }
                if (stoneBelow?.isLocked == true) {
                    isConnectedToLocked = true
                    lockedConnectedStones.add(stoneBelow)
                    continue
                }

                // Todo continue with logic from sheet


            }
            return true
        }
// todo do same for other moving operations, think about index of stone in hand, necessary?

    fun moveStoneToHand(stone: StoneData): ScrabbleState {
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
    ): ScrabbleState {
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

    fun clearEnteredField(): ScrabbleState = copy(enteredField = null)

}

sealed interface ScrabbleEvent {
    data class StoneDroppedOnBoard(
        val stoneData: StoneData,
        val columnIndex: Int,
        val rowIndex: Int
    ) : ScrabbleEvent

    data class StoneDroppedOnHand(
        val stoneData: StoneData
    ) : ScrabbleEvent

    data class FieldEntered(val index: Int) : ScrabbleEvent
    data object DrawStonesClick : ScrabbleEvent
    data object SubmitClick : ScrabbleEvent
}


class ScrabbleViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScrabbleState())
    val state get() = _state.asStateFlow()

    init {
        _state.update {
            it.copy(stonesInBag = initialStonesInBag)
        }
    }

    fun onEvent(event: ScrabbleEvent) {
        when (event) {
            is ScrabbleEvent.FieldEntered -> _state.update { it.copy(enteredField = event.index) }
            is ScrabbleEvent.StoneDroppedOnHand -> moveStoneToHand(event.stoneData)
            ScrabbleEvent.DrawStonesClick -> drawStones()
            ScrabbleEvent.SubmitClick -> onSubmitClick()
            is ScrabbleEvent.StoneDroppedOnBoard -> moveStoneToBoard(
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
    }

    private fun moveStoneToHand(stoneData: StoneData) {
        println("StoneDroppedOnHand")
        _state.update { currentState ->
            currentState
                .moveStoneToHand(stoneData)
                .clearEnteredField()
        }
    }


    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

    private fun onSubmitClick() {
        val word = _state.value.stonesOnBoard.filter { !it.isLocked }.map { it.letter }
            .joinToString("")

        sendPrompt("Is this a valid german word according to the scrabble rules? Please answer with true or false. No other words. Here the word $word")

    }

    private fun sendPrompt(
        prompt: String
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    text(prompt)
                    println("input: $prompt")
                })
                response.text?.let { outputContent ->
                    println("output: $outputContent")
                }
            } catch (e: Exception) {
                println("send prompt failed: $e")
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


// StoneListExtensions

val List<StoneOnBoard>.areHorizontallyAligned: Boolean get() = map { it.rowIndex }.toSet().size == 1
val List<StoneOnBoard>.areVerticallyAligned: Boolean get() = map { it.columnIndex }.toSet().size == 1