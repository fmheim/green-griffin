package com.felix.greengriffin.board.domain.model

import com.felix.greengriffin.trails.domain.model.TrailLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serial names are pinned to the class names this hierarchy had before it moved out of
 * `board.presentation`. The navigation back stack persists a serialized [GameMode], so
 * changing them would make a back stack written by an older build unreadable.
 */
@Serializable
@SerialName("com.felix.greengriffin.board.presentation.GameMode")
sealed interface GameMode {
    val id: Int

    /** Identifies the saved game of this mode. Modes without levels use [NO_LEVEL]. */
    val levelKey: Int

    @Serializable
    @SerialName("com.felix.greengriffin.board.presentation.GameMode.FreePlay")
    data object FreePlay : GameMode {
        override val id: Int
            get() = FREE_PLAY_ID

        override val levelKey: Int
            get() = NO_LEVEL
    }

    @Serializable
    @SerialName("com.felix.greengriffin.board.presentation.GameMode.Trails")
    data class Trails(val level: TrailLevel) : GameMode {

        override val id: Int
            get() = TRAILS_ID

        override val levelKey: Int
            get() = level.index

        fun isStartField(row: Int, column: Int) =
            level.startFields.contains(row = row, column = column)

        fun isGoalField(row: Int, column: Int) =
            level.goalFields.contains(row = row, column = column)


    }

    companion object {
        const val FREE_PLAY_ID = 1
        const val TRAILS_ID = 2

        /** Level key of a game mode that has no levels. Persisted, so it must not change. */
        const val NO_LEVEL = -1
        /**
         * The mode a persisted id refers to, or `null` when it refers to nothing this
         * build knows about. Callers discard such data instead of guessing a mode.
         */
        fun fromId(
            id: Int,
            trailLevel: TrailLevel? = null,
        ): GameMode? = when (id) {
            FREE_PLAY_ID -> FreePlay
            TRAILS_ID -> trailLevel?.let(::Trails)
            else -> null
        }
    }
}

private fun Set<Field>.contains(row: Int, column: Int) =
    Field(row = row, column = column) in this
