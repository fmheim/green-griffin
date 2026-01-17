package com.felix.greengriffin.board.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "game_state",
    primaryKeys = ["game_mode_id", "level"])
data class GameStateEntity(
    @ColumnInfo(name = "game_mode_id")
    val gameModeId: Int,

    @ColumnInfo
    val level: Int,

    @ColumnInfo(name = "game_state_json")
    val gameStateJson: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)