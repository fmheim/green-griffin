package com.felix.greengriffin.board.data.repository

import com.felix.greengriffin.board.data.local.GameStateDao
import com.felix.greengriffin.board.data.mapper.toDomain
import com.felix.greengriffin.board.data.mapper.toEntity
import com.felix.greengriffin.board.presentation.GameState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameStateRepository @Inject constructor(
    private val gameStateDao: GameStateDao,
) {

    suspend fun saveGameState(gameState: GameState) {
        gameStateDao.saveCompleteGameState(
            gameState = gameState.toEntity(),
            stonesInHand = gameState.stonesInHand.map { it.toEntity() },
            stonesOnBoard = gameState.stonesOnBoard.map { it.toEntity() },
            stonesInBag = gameState.stonesInBag.map { it.toEntity() }
        )
    }

    suspend fun loadGameState(): GameState? {
        val gameStateEntity = gameStateDao.getGameState() ?: return null
        val stonesInHand = gameStateDao.getStonesInHand().map { it.toDomain() }
        val stonesOnBoard = gameStateDao.getStonesOnBoard().map { it.toDomain() }
        val stonesInBag = gameStateDao.getStonesInBag().map { it.toDomain() }

        return GameState(
            currentUserId = gameStateEntity.currentUserId,
            totalPoints = gameStateEntity.totalPoints,
            stonesInHand = stonesInHand,
            stonesOnBoard = stonesOnBoard.toSet(),
            stonesInBag = stonesInBag.toSet()
        )
    }

    suspend fun clearGameState() {
        gameStateDao.clearAllGameState()
    }
}
