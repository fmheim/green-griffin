package com.felix.greengriffin.board.presentation


import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.RouteToWordPlacementScreen
import com.felix.greengriffin.board.data.repository.GameStateRepository
import com.felix.greengriffin.board.domain.usecase.AreWordsValidUseCase
import com.felix.greengriffin.board.domain.usecase.IsPlacementValidUseCase
import com.felix.greengriffin.board.domain.usecase.PlacementValidation
import com.felix.greengriffin.board.domain.usecase.WordValidation.Valid
import com.felix.greengriffin.board.presentation.GameState.Alignment.Horizontal
import com.felix.greengriffin.board.presentation.GameState.Alignment.Single
import com.felix.greengriffin.board.presentation.GameState.Alignment.Unaligned
import com.felix.greengriffin.board.presentation.GameState.Alignment.Vertical
import com.felix.greengriffin.board.presentation.GameState.JokerCoordinates
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.board.presentation.components.Word
import com.felix.greengriffin.board.presentation.components.asWord
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

const val DEFAULT_BOARD_SIZE = 10

data class Field(
    val row: Int,
    val column: Int,
)

data class TrailLevel(
    val index: Int,
    val boardSize: Int,
    val startFields: Set<Field>,
    val goalFields: Set<Field>,
    val blockedField: Set<Field>,
)

val trailLevels = setOf(
    TrailLevel(
        index = 1,
        boardSize = 10,
        startFields = List(10) { Field(row = it, column = 0) }.toSet(),
        goalFields = List(10) { Field(row = it, column = 9) }.toSet(),
        blockedField = setOf()
    )
)

sealed interface GameMode {
    val id: Int

    data object FreePlay : GameMode {
        override val id: Int
            get() = FREE_PLAY_ID
    }

    data class Trails(val level: TrailLevel) : GameMode {

        override val id: Int
            get() = TRAILS_ID

        fun isStartField(row: Int, column: Int) =
            level.startFields.contains(row = row, column = column)

        fun isGoalField(row: Int, column: Int) =
            level.goalFields.contains(row = row, column = column)


    }

    companion object {
        const val FREE_PLAY_ID = 1
        const val TRAILS_ID = 2
        fun fromId(
            id: Int,
            trailLevel: TrailLevel? = null,
        ): GameMode = when (id) {
            FREE_PLAY_ID -> FreePlay
            TRAILS_ID  -> {
                if (trailLevel != null) {
                    Trails(trailLevel)
                } else {
                    Trails(trailLevels.first())
                }
            }

            else -> error("Unknown GameMode id: $id, level: $trailLevel")
        }
    }
}

private fun Set<Field>.contains(row: Int, column: Int) =
    Field(row = row, column = column) in this

@Immutable
data class GameState(
    val gameMode: GameMode = GameMode.FreePlay,
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
) {
    enum class Alignment {
        Horizontal, Vertical, Single, Unaligned
    }

    data class JokerCoordinates(val row: Int, val column: Int)

    val isJokerSelectorVisible get() = jokerCoordinates != null

    val boardSize
        get() = if (gameMode is GameMode.Trails) {
            gameMode.level.boardSize
        } else {
            DEFAULT_BOARD_SIZE
        }

    val firstEmptyCoordinates: JokerCoordinates
        get() {
            val lastColumn =
                stonesOnBoard.maxOfOrNull { it.columnIndex } ?: return JokerCoordinates(0, 0)
            val lastRow = stonesOnBoard.maxOfOrNull { it.rowIndex } ?: return JokerCoordinates(0, 0)
            val occupied = stonesOnBoard
                .map { it.columnIndex to it.rowIndex }
                .toSet()

            for (col in 0..lastColumn) {
                for (row in 0..lastRow) {
                    if (Pair(col, row) !in occupied) {
                        return JokerCoordinates(row, col)
                    }
                }
            }
            return JokerCoordinates(0, 0)
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

    val isAbleToSubmit: Boolean
        get() = unlockedStonesOnBoard.isNotEmpty()
                && isValidPlacement
                && isCurrentWordValid == true

    val isAbleToDrawStones: Boolean
        get() = stonesInBag.isNotEmpty() && stonesInHand.size < 7 && unlockedStonesOnBoard.isEmpty()

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
            stonesOnBoard = if (stone is StoneOnBoard) stonesOnBoard + movedStone - stone else stonesOnBoard + movedStone
        )
    }

    fun clearEnteredField(): GameState = copy(enteredField = null)

    fun asSavedGame(): SavedGame = SavedGame(
        gameModeId = gameMode.id,
        levelIndex = if (gameMode is GameMode.Trails) gameMode.level.index else -1,
        totalPoints = totalPoints,
        stonesInHand = stonesInHand,
        stonesOnBoard = stonesOnBoard,
        stonesInBag = stonesInBag,
    )
}

@Serializable
data class SavedGame(
    val gameModeId: Int,
    val levelIndex: Int,
    val totalPoints: Long,
    val stonesInHand: List<StoneInHand>,
    val stonesOnBoard: Set<StoneOnBoard>,
    val stonesInBag: Set<StoneInBag>,
) {
    fun asGameState(): GameState = GameState(
        gameMode = GameMode.fromId(gameModeId, trailLevels.find { it.index == levelIndex }),
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


@HiltViewModel(assistedFactory = WordPlacementViewModel.Factory::class)
class WordPlacementViewModel @AssistedInject constructor(
    private val areWordsValidUseCase: AreWordsValidUseCase,
    private val isPlacementValidUseCase: IsPlacementValidUseCase,
    private val gameStateRepository: GameStateRepository,
    @Assisted val navKey: RouteToWordPlacementScreen
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navKey: RouteToWordPlacementScreen): WordPlacementViewModel
    }


    private val _state = MutableStateFlow(value = GameState(gameMode = navKey.gameMode))
    val state: StateFlow<GameState> = _state.asStateFlow()


    init {
        loadInitialGameState()
    }

    private fun loadInitialGameState() {
        viewModelScope.launch {
            val savedState = loadSavedState()
            when {
                savedState != null -> _state.update { savedState }
                else -> initializeNewGame()
            }
        }
    }

    private suspend fun loadSavedState(): GameState? {
        val initialState = _state.value
        val savedState = gameStateRepository.loadGameState(
            gameModeId = initialState.gameMode.id,
            level = when (initialState.gameMode) {
                is GameMode.Trails -> initialState.gameMode.level.index
                else -> -1
            }
        )?.asGameState()
        return savedState
    }

    private fun initializeNewGame() {
        _state.update {
            it.copy(stonesInBag = initialStonesInBag)
        }
        if (_state.value.isAbleToDrawStones) {
            drawStones()
        }
    }

    private fun saveGameState() {
        viewModelScope.launch(Dispatchers.IO) {
            gameStateRepository.saveGameState(_state.value.asSavedGame())
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.FieldEntered -> _state.update { it.copy(enteredField = event.index) }
            is GameEvent.StoneDroppedOnHand -> moveStoneToHand(event.stoneData)
            GameEvent.SubmitClick -> onSubmitClick()
            is GameEvent.StoneDroppedOnBoard -> moveStoneToBoard(
                stoneData = event.stoneData,
                rowIndex = event.rowIndex,
                columnIndex = event.columnIndex
            )

            is GameEvent.JokerSelected -> onJokerSelected(letter = event.letter)
            GameEvent.JokerSelectorDismissRequested -> dismissJokerSelector()
            GameEvent.ReturnAllUnlockedStonesClick -> returnAllUnlockedStones()
            GameEvent.ClearGameStateClick -> clearGameState()
        }
    }

    private fun onJokerSelected(letter: Char) {
        moveStoneToBoard(
            stoneData = StoneInHand(
                letter = letter,
                value = 0,
                id = UUID.randomUUID().toString(),
                userId = 1
            ),
            rowIndex = _state.value.jokerCoordinates?.row
                ?: _state.value.firstEmptyCoordinates.row,
            columnIndex = _state.value.jokerCoordinates?.column
                ?: _state.value.firstEmptyCoordinates.column
        )
        dismissJokerSelector()
    }

    private fun dismissJokerSelector() {
        _state.update { it.copy(jokerCoordinates = null) }
        saveGameState()
    }

    private fun returnAllUnlockedStones() {
        _state.update { currentState ->
            currentState
                .moveAllUnlockedStonesToHand()
                .clearEnteredField()
                .copy(isValidPlacement = false, isCurrentWordValid = null)
        }
        saveGameState()
    }

    private fun drawStones() {
        _state.value
            .stonesInBag
            .shuffled()
            .take(_state.value.numberOfStonesToDraw)
            .forEach { stone ->
                _state.update { currentState ->
                    currentState.moveStoneToHand(stone)
                }
            }
        saveGameState()
    }


    private fun moveStoneToBoard(
        stoneData: StoneData,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        println("StoneDroppedOnBoard")

        if (stoneData.isJoker) {
            _state.update {
                it.copy(
                    jokerCoordinates = JokerCoordinates(
                        row = rowIndex,
                        column = columnIndex
                    )
                )
            }
            saveGameState()
            return
        }

        _state.update { currentState ->
            currentState
                .moveStoneToBoard(
                    stone = stoneData,
                    rowIndex = rowIndex,
                    columnIndex = columnIndex
                )
        }

        onStoneMovedToBoard()
        saveGameState()
    }

    private fun onStoneMovedToBoard() {
        val placementValidation = isPlacementValidUseCase(_state.value.stonesOnBoard)
        val isValidPlacement = placementValidation is PlacementValidation.Valid

        _state.update { it.copy(isValidPlacement = isValidPlacement) }
        saveGameState()

        if (isValidPlacement) {
            val words = _state.value.newlyCreatedWordsAsStrings
            validateWords(words)
        }
    }

    private fun moveStoneToHand(stoneData: StoneData) {
        println("StoneDroppedOnHand")
        _state.update { currentState ->
            currentState
                .moveStoneToHand(stoneData)
                .clearEnteredField()
                .copy(isValidPlacement = false, isCurrentWordValid = null)
        }
        saveGameState()
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
        saveGameState()
        drawStones()
    }

    private fun validateWords(words: List<String>) {
        _state.update { it.copy(isPromptLoading = true, isCurrentWordValid = null) }
        saveGameState()

        viewModelScope.launch(Dispatchers.IO) {
            val wordValidation = areWordsValidUseCase(
                words = words,
                allowedLanguages = listOf("sv", "de") // todo: specify languages from settings
            )

            println("WordValidation: $wordValidation")

            _state.update {
                it.copy(
                    isPromptLoading = false,
                    isCurrentWordValid = wordValidation is Valid
                )
            }
            saveGameState()
        }
    }

    private fun clearGameState() {
        viewModelScope.launch(Dispatchers.IO) {
            gameStateRepository.clearGameState(gameModeId = 1, level = null)
            _state.update {
                GameState(stonesInBag = initialStonesInBag)
            }
            if (_state.value.isAbleToDrawStones) {
                drawStones()
            }
        }
    }
}

data class LetterProperties(
    val value: Int,
    val frequency: Int,
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

val germanAlphabet = letterPropertiesMap.keys.filter { !it.isWhitespace() }
// todo: combine with swedish alphabet?

// StoneListExtensions

val List<StoneOnBoard>.areHorizontallyAligned: Boolean get() = map { it.rowIndex }.toSet().size == 1
val List<StoneOnBoard>.areVerticallyAligned: Boolean get() = map { it.columnIndex }.toSet().size == 1
