package com.felix.greengriffin.board.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameModeTest {

    @Test
    fun `free play uses the no-level key`() {
        assertEquals(GameMode.NO_LEVEL, GameMode.FreePlay.levelKey)
    }

    @Test
    fun `a trails game is keyed by its level index`() {
        val level = trailLevels.first { it.index == 2 }
        assertEquals(2, GameMode.Trails(level).levelKey)
    }

    @Test
    fun `a saved game is stored under the key its own game mode reports`() {
        val modes = listOf(GameMode.FreePlay) + trailLevels.map { GameMode.Trails(it) }
        modes.forEach { mode ->
            assertEquals(
                "saving $mode must use the key the loader and the cleaner look up",
                mode.levelKey,
                GameState(gameMode = mode).asSavedGame().levelIndex,
            )
        }
    }

    @Test
    fun `a saved game round trips back into the same game mode`() {
        val modes = listOf(GameMode.FreePlay) + trailLevels.map { GameMode.Trails(it) }
        modes.forEach { mode ->
            val restored = GameState(gameMode = mode).asSavedGame().asGameState()
            assertEquals(mode, restored?.gameMode)
        }
    }

    @Test
    fun `a saved game naming an unknown level is discarded`() {
        val saved = SavedGame(
            gameModeId = GameMode.TRAILS_ID,
            levelIndex = 999,
            totalPoints = 42,
            stonesInHand = emptyList(),
            stonesOnBoard = emptySet(),
            stonesInBag = emptySet(),
        )

        assertNull(
            "a board from a level this build does not have must not be loaded onto another level",
            saved.asGameState(),
        )
    }

    @Test
    fun `a saved game naming an unknown game mode is discarded`() {
        val saved = SavedGame(
            gameModeId = 99,
            levelIndex = GameMode.NO_LEVEL,
            totalPoints = 0,
            stonesInHand = emptyList(),
            stonesOnBoard = emptySet(),
            stonesInBag = emptySet(),
        )

        assertNull(saved.asGameState())
    }

    @Test
    fun `an unknown id does not resolve to a game mode`() {
        assertNull(GameMode.fromId(id = 99))
        assertNull(GameMode.fromId(id = GameMode.TRAILS_ID, trailLevel = null))
    }
}
