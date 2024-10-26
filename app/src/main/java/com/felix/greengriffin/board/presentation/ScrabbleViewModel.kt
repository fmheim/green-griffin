package com.felix.greengriffin.board.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felix.greengriffin.BuildConfig
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.extensions.list.add
import com.felix.greengriffin.extensions.list.replace
import com.felix.greengriffin.extensions.list.replaceByNull
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
    val stonesOnBoard: List<StoneData?> = List(15 * 15) { null },
    val stonesInHand: List<StoneData> = emptyList(),
    val stonesInBag: List<StoneData> = emptyList(),
    val enteredField: Int? = null
) {
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
            val unLockedStoneIndices = stonesOnBoard.mapIndexed { index, stoneData ->
                if (stoneData?.isLocked == false) index
            }
            val isValidPlacement = false
            unLockedStoneIndices.forEach { index ->


            }

            return true
        }


    fun removeStoneFromBoard(id: String): ScrabbleState = copy(
        stonesOnBoard = stonesOnBoard.replaceByNull { stoneOnBoard -> stoneOnBoard?.id == id }
    )

    fun addStoneToBoard(stoneData: StoneData, index: Int): ScrabbleState =
        copy(stonesOnBoard = stonesOnBoard
            .replaceByNull { stoneOnBoard -> stoneOnBoard?.id == stoneData.id }
            .replace(index = index, element = stoneData)
        )

    fun addStoneToHand(stoneData: StoneData): ScrabbleState = copy(
        stonesInHand =
        stonesInHand.add(stoneData, ifNone = { stoneInHand -> stoneInHand.id == stoneData.id })
    )

    fun removeStoneFromHand(id: String): ScrabbleState = copy(
        stonesInHand = stonesInHand.filter { it.id != id }
    )

    fun clearEnteredField(): ScrabbleState = copy(enteredField = null)
}

sealed interface ScrabbleEvent {
    data class StoneDroppedOnBoard(
        val stoneData: StoneData, val index: Int
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
            is ScrabbleEvent.StoneDroppedOnBoard -> {
                moveStoneToBoard(event.stoneData, event.index)
            }

            is ScrabbleEvent.FieldEntered -> _state.update { it.copy(enteredField = event.index) }
            is ScrabbleEvent.StoneDroppedOnHand -> moveStoneToHand(event.stoneData)

            ScrabbleEvent.DrawStonesClick -> {
                val drawnStones =
                    _state.value.stonesInBag.shuffled().take(_state.value.numberOfStonesToDraw)
                _state.update {
                    it.copy(
                        stonesInBag = it.stonesInBag.filter { !drawnStones.contains(it) },
                        stonesInHand = it.stonesInHand + drawnStones
                    )
                }
            }

            ScrabbleEvent.SubmitClick -> onSubmitClick()
        }
    }

    private fun moveStoneToBoard(stoneData: StoneData, index: Int) {
        println("StoneDroppedOnBoard")
        _state.value.stonesOnBoard.run {
            if (getOrNull(index) != null) return
        }

        _state.update { currentState ->
            currentState
                .removeStoneFromHand(stoneData.id)
                .addStoneToBoard(stoneData, index)
        }
    }

    private fun moveStoneToHand(stoneData: StoneData) {
        println("StoneDroppedOnHand")
        _state.update { currentState ->
            currentState
                .removeStoneFromBoard(stoneData.id)
                .addStoneToHand(stoneData)
                .clearEnteredField()
        }
    }


    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", apiKey = BuildConfig.apiKey
    )

    private fun onSubmitClick() {
        val word = _state.value.stonesOnBoard.filter { it?.isLocked == false }.map { it?.letter }
            .joinToString("")

        sendPrompt("Is this a valid german word according to the scrabble rules? Please answer with true or false. No other words. Here the word $word")

    }

    fun sendPrompt(
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

val initialStonesInBag = listOf(
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'A', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Ä', value = 6, id = UUID.randomUUID().toString()),
    StoneData(letter = 'B', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'B', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'C', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'C', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'D', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'D', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'D', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'D', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'E', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'F', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'F', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'G', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'G', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'G', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'H', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'H', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'I', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'J', value = 10, id = UUID.randomUUID().toString()),
    StoneData(letter = 'K', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'K', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'K', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'K', value = 2, id = UUID.randomUUID().toString()),
    StoneData(letter = 'L', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'L', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'L', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'L', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'M', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'M', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'M', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'N', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'O', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'O', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'O', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'O', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Ö', value = 8, id = UUID.randomUUID().toString()),
    StoneData(letter = 'P', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'P', value = 4, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Q', value = 10, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'R', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'S', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'T', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'U', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'U', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'U', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'U', value = 1, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Ü', value = 6, id = UUID.randomUUID().toString()),
    StoneData(letter = 'V', value = 6, id = UUID.randomUUID().toString()),
    StoneData(letter = 'V', value = 6, id = UUID.randomUUID().toString()),
    StoneData(letter = 'W', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'W', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'X', value = 10, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Y', value = 10, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Z', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = 'Z', value = 3, id = UUID.randomUUID().toString()),
    StoneData(letter = ' ', value = 0, id = UUID.randomUUID().toString()),
    StoneData(letter = ' ', value = 0, id = UUID.randomUUID().toString())
)