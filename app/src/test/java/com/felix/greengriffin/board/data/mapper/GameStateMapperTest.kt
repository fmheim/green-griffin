package com.felix.greengriffin.board.data.mapper

import com.felix.greengriffin.board.data.local.GameStateEntity
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.board.domain.model.StoneInBag
import com.felix.greengriffin.board.domain.model.StoneInHand
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStateMapperTest {

    private val savedGame = SavedGame(
        gameModeId = GameMode.TRAILS_ID,
        levelIndex = 2,
        totalPoints = 123,
        stonesInHand = listOf(StoneInHand('A', 1, "hand-a", userId = 1)),
        stonesOnBoard = setOf(StoneOnBoard('B', 3, "board-b", 4, 5, isLocked = true)),
        stonesInBag = setOf(StoneInBag('Ä', 6, "bag-ae")),
    )

    @Test
    fun `a saved game survives a round trip through the database row`() {
        assertEquals(savedGame, savedGame.toEntity().toSavedGame())
    }

    @Test
    fun `the row is keyed by game mode and level`() {
        val entity = savedGame.toEntity()

        assertEquals(GameMode.TRAILS_ID, entity.gameModeId)
        assertEquals(2, entity.level)
    }

    @Test
    fun `non-ascii letters survive the round trip`() {
        val restored = savedGame.toEntity().toSavedGame()

        assertEquals(setOf('Ä'), restored?.stonesInBag?.map { it.letter }?.toSet())
    }

    @Test
    fun `an unreadable row is discarded instead of throwing`() {
        val corrupt = GameStateEntity(
            gameModeId = GameMode.FREE_PLAY_ID,
            level = GameMode.NO_LEVEL,
            gameStateJson = "{ this is not the json we wrote",
        )

        assertNull(corrupt.toSavedGame())
    }

    @Test
    fun `a row written by a newer build is still readable`() {
        val withExtraField = GameStateEntity(
            gameModeId = GameMode.FREE_PLAY_ID,
            level = GameMode.NO_LEVEL,
            gameStateJson = """
                {"gameModeId":1,"levelIndex":-1,"totalPoints":7,"stonesInHand":[],
                 "stonesOnBoard":[],"stonesInBag":[],"somethingAddedLater":true}
            """.trimIndent(),
        )

        assertEquals(7L, withExtraField.toSavedGame()?.totalPoints)
    }
}
