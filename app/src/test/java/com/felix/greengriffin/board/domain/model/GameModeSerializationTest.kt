package com.felix.greengriffin.board.domain.model

import com.felix.greengriffin.trails.domain.model.TrailLevel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The navigation back stack and dragged stones are persisted as the serialized form of
 * these hierarchies, so their type discriminators must survive the move out of
 * `board.presentation`. These are the names the classes had there.
 */
class GameModeSerializationTest {

    @Test
    fun `free play keeps its serial name`() {
        assertEquals(
            """{"type":"com.felix.greengriffin.board.presentation.GameMode.FreePlay"}""",
            Json.encodeToString<GameMode>(GameMode.FreePlay),
        )
    }

    @Test
    fun `a trails mode keeps its serial name`() {
        val level = TrailLevel(
            index = 7,
            boardSize = 3,
            startFields = setOf(Field(row = 0, column = 0)),
            goalFields = setOf(Field(row = 2, column = 2)),
        )

        assertEquals(
            """{"type":"com.felix.greengriffin.board.presentation.GameMode.Trails",""" +
                    """"level":{"index":7,"boardSize":3,"startFields":[{"row":0,"column":0}],""" +
                    """"goalFields":[{"row":2,"column":2}]}}""",
            Json.encodeToString<GameMode>(GameMode.Trails(level = level)),
        )
    }

    @Test
    fun `a dragged stone keeps its serial name`() {
        assertEquals(
            """{"type":"com.felix.greengriffin.board.presentation.components.StoneInHand",""" +
                    """"letter":"A","value":1,"id":"id","userId":1}""",
            Json.encodeToString<StoneData>(
                StoneInHand(letter = 'A', value = 1, id = "id", userId = 1)
            ),
        )
    }

    @Test
    fun `a saved game round trips through json`() {
        val savedGame = SavedGame(
            gameModeId = GameMode.TRAILS_ID,
            levelIndex = 2,
            totalPoints = 12,
            stonesInHand = listOf(StoneInHand('A', 1, "hand", userId = 1)),
            stonesOnBoard = setOf(StoneOnBoard('B', 3, "board", 1, 2, isLocked = true)),
            stonesInBag = setOf(StoneInBag('C', 4, "bag")),
        )

        assertEquals(savedGame, Json.decodeFromString<SavedGame>(Json.encodeToString(savedGame)))
    }
}
