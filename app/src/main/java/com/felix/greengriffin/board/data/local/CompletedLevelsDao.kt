package com.felix.greengriffin.board.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedLevelsDao {

    @Query("SELECT level_index FROM completed_levels WHERE game_mode_id = :gameModeId")
    fun observeCompletedLevelsFor(gameModeId: Int): Flow<List<Int>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completedLevel: CompletedLevelEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM completed_levels WHERE game_mode_id = :gameModeId AND level_index = :levelIndex LIMIT 1)")
    fun observeIfLevelIsCompleted(gameModeId: Int, levelIndex: Int): Flow<Boolean>

}