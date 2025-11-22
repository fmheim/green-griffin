package com.felix.greengriffin.board.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for single game state

    @ColumnInfo(name = "current_user_id")
    val currentUserId: Int,

    @ColumnInfo(name = "total_points")
    val totalPoints: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "stones_in_hand")
data class StoneInHandEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "letter")
    val letter: Char,

    @ColumnInfo(name = "value")
    val value: Int,

    @ColumnInfo(name = "user_id")
    val userId: Int,
)

@Entity(tableName = "stones_on_board")
data class StoneOnBoardEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "letter")
    val letter: Char,

    @ColumnInfo(name = "value")
    val value: Int,

    @ColumnInfo(name = "row_index")
    val rowIndex: Int,

    @ColumnInfo(name = "column_index")
    val columnIndex: Int,

    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean,
)

@Entity(tableName = "stones_in_bag")
data class StoneInBagEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "letter")
    val letter: Char,

    @ColumnInfo(name = "value")
    val value: Int,
)
