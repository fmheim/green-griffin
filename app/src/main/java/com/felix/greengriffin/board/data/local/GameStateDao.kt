package com.felix.greengriffin.board.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_state WHERE id = 1")
    suspend fun getGameState(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(gameState: GameStateEntity)

    @Query("SELECT * FROM stones_in_hand")
    suspend fun getStonesInHand(): List<StoneInHandEntity>

    @Query("SELECT * FROM stones_on_board")
    suspend fun getStonesOnBoard(): List<StoneOnBoardEntity>

    @Query("SELECT * FROM stones_in_bag")
    suspend fun getStonesInBag(): List<StoneInBagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStonesInHand(stones: List<StoneInHandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStonesOnBoard(stones: List<StoneOnBoardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStonesInBag(stones: List<StoneInBagEntity>)

    @Query("DELETE FROM stones_in_hand")
    suspend fun clearStonesInHand()

    @Query("DELETE FROM stones_on_board")
    suspend fun clearStonesOnBoard()

    @Query("DELETE FROM stones_in_bag")
    suspend fun clearStonesInBag()

    @Transaction
    suspend fun saveCompleteGameState(
        gameState: GameStateEntity,
        stonesInHand: List<StoneInHandEntity>,
        stonesOnBoard: List<StoneOnBoardEntity>,
        stonesInBag: List<StoneInBagEntity>,
    ) {
        insertGameState(gameState)
        clearStonesInHand()
        clearStonesOnBoard()
        clearStonesInBag()
        insertStonesInHand(stonesInHand)
        insertStonesOnBoard(stonesOnBoard)
        insertStonesInBag(stonesInBag)
    }

    @Transaction
    suspend fun clearAllGameState() {
        clearStonesInHand()
        clearStonesOnBoard()
        clearStonesInBag()
    }
}
