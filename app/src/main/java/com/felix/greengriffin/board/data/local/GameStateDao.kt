package com.felix.greengriffin.board.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_state WHERE game_mode_id = :gameModeId AND level = :level")
    suspend fun getGameState(gameModeId: Int, level: Int): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(gameState: GameStateEntity)

    @Query("DELETE FROM game_state WHERE game_mode_id = :gameModeId AND level = :level ")
    suspend fun clearGameState(gameModeId: Int, level: Int?)

}
