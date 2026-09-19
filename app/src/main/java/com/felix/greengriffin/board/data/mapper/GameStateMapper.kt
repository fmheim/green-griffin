package com.felix.greengriffin.board.data.mapper

import com.felix.greengriffin.board.data.local.GameStateEntity
import com.felix.greengriffin.board.domain.model.SavedGame
import kotlinx.serialization.json.Json

/**
 * Tolerates fields a newer build wrote and an older one does not know, so that adding
 * state to [SavedGame] does not invalidate everybody's saved games.
 */
private val savedGameJson = Json { ignoreUnknownKeys = true }

fun SavedGame.toEntity(): GameStateEntity = GameStateEntity(
    gameModeId = gameModeId,
    level = levelIndex,
    gameStateJson = savedGameJson.encodeToString(this),
)

/** The stored game, or `null` when the row cannot be read back as one. */
fun GameStateEntity.toSavedGame(): SavedGame? = runCatching {
    savedGameJson.decodeFromString<SavedGame>(gameStateJson)
}.getOrNull()
