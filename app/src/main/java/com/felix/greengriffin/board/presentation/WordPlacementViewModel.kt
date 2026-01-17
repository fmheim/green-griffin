package com.felix.greengriffin.board.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.RouteToWordPlacementScreen
import com.felix.greengriffin.board.data.repository.GameStateRepository
import com.felix.greengriffin.board.domain.usecase.AreWordsValidUseCase
import com.felix.greengriffin.board.domain.usecase.IsPlacementValidUseCase
import com.felix.greengriffin.board.domain.usecase.PlacementValidation
import com.felix.greengriffin.board.domain.usecase.WordValidation.Valid
import com.felix.greengriffin.board.presentation.GameState.JokerCoordinates
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneInHand
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
import java.util.UUID


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

// todo: combine with swedish alphabet?

// StoneListExtensions

