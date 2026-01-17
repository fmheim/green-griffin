package com.felix.greengriffin.board.data.repository

import com.felix.greengriffin.board.data.local.GameStateDao
import com.felix.greengriffin.board.data.mapper.toEntity
import com.felix.greengriffin.board.data.mapper.toSavedGame
import com.felix.greengriffin.board.presentation.SavedGame
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameStateRepository @Inject constructor(
    private val gameStateDao: GameStateDao,
) {

    suspend fun saveGameState(savedGameState: SavedGame) {
        gameStateDao.insertGameState(savedGameState.toEntity())
    }

    suspend fun loadGameState(gameModeId: Int, level: Int): SavedGame? {
        val gameStateEntity = gameStateDao.getGameState(gameModeId = gameModeId, level = level) ?: return null
        return gameStateEntity.toSavedGame()
    }

    suspend fun clearGameState(gameModeId: Int, level: Int?) {
        gameStateDao.clearGameState(gameModeId = gameModeId, level  = level)
    }
}

