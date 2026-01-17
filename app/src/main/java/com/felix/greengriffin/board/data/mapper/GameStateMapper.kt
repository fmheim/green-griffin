package com.felix.greengriffin.board.data.mapper

import com.felix.greengriffin.board.data.local.GameStateEntity
import com.felix.greengriffin.board.presentation.SavedGame
import kotlinx.serialization.json.Json


fun SavedGame.toEntity() : GameStateEntity = GameStateEntity(
    gameModeId = gameModeId,
    level = levelIndex ?: -1,
    gameStateJson = Json.encodeToString(this),
)

fun GameStateEntity.toSavedGame() : SavedGame = Json.decodeFromString(gameStateJson)