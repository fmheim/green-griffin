package com.felix.greengriffin.board.data.repository

import com.felix.greengriffin.board.data.local.GameStateDao
import com.felix.greengriffin.board.data.mapper.toEntity
import com.felix.greengriffin.board.data.mapper.toSavedGame
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameStateRepository @Inject constructor(
    private val gameStateDao: GameStateDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    // Room dispatches the SQL itself, but `toEntity` serialises the whole board to JSON on
    // the caller's thread, and the caller is now the auto saver running on the main one.
    // Injected rather than hardcoded so a test can schedule this write like any other.
    suspend fun saveGameState(savedGameState: SavedGame) = withContext(ioDispatcher) {
        gameStateDao.insertGameState(savedGameState.toEntity())
    }

    suspend fun loadGameState(gameModeId: Int, level: Int): SavedGame? {
        val gameStateEntity = gameStateDao.getGameState(gameModeId = gameModeId, level = level) ?: return null
        return gameStateEntity.toSavedGame()
    }
}

