package com.felix.greengriffin.board.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "completed_levels",
    primaryKeys = ["game_mode_id", "level_index"]
)
data class CompletedLevelEntity(
    @ColumnInfo(name = "game_mode_id")
    val gameModeId: Int,

    @ColumnInfo(name = "level_index")
    val levelIndex: Int,

    )