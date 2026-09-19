package com.felix.greengriffin.board.presentation

import com.felix.greengriffin.board.data.local.GameStateDao
import com.felix.greengriffin.board.data.local.GameStateEntity
import com.felix.greengriffin.board.data.mapper.toSavedGame
import com.felix.greengriffin.board.data.repository.GameStateRepository
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.board.domain.model.StoneInBag
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Records every write in order, so the number of writes and their order can be asserted. */
class RecordingGameStateDao : GameStateDao {
    val writtenGames = mutableListOf<GameStateEntity>()
    var storedGame: GameStateEntity? = null

    override suspend fun getGameState(gameModeId: Int, level: Int): GameStateEntity? = storedGame

    override suspend fun insertGameState(gameState: GameStateEntity) {
        writtenGames += gameState
        storedGame = gameState
    }

    /** The games that were written, read back out of the stored JSON. */
    fun savedGames(): List<SavedGame> = writtenGames.map { checkNotNull(it.toSavedGame()) }
}

fun savedGame(points: Long, letters: String = ""): SavedGame = SavedGame(
    gameModeId = GameMode.FREE_PLAY_ID,
    levelIndex = GameMode.NO_LEVEL,
    totalPoints = points,
    stonesInHand = emptyList(),
    stonesOnBoard = emptySet(),
    stonesInBag = letters.mapIndexed { index, letter ->
        StoneInBag(letter = letter, value = 1, id = "$index")
    }.toSet(),
)

class GameStateAutoSaverTest {

    private val dao = RecordingGameStateDao()

    // The write has to run on the test's own dispatcher, or advancing time would not wait
    // for it and every assertion below would race the database.
    private val testDispatcher = StandardTestDispatcher()
    private val repository = GameStateRepository(dao, testDispatcher)

    @Test
    fun `changes closer together than the debounce window collapse into one write`() = runTest(testDispatcher) {
        val games = MutableStateFlow(savedGame(points = 0))
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = savedGame(points = 0),
            scope = backgroundScope,
        )
        advanceUntilIdle()

        // Four changes in the time a single stone drop used to fire four saves.
        repeat(4) { change ->
            games.value = savedGame(points = change + 1L)
            advanceTimeBy(10)
        }
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(
            "four changes inside one window must cost one write, wrote ${dao.savedGames()}",
            1,
            dao.savedGames().size,
        )
        assertEquals(savedGame(points = 4), dao.savedGames().single())
    }

    @Test
    fun `the restored game is not written straight back`() = runTest(testDispatcher) {
        val restored = savedGame(points = 7, letters = "abc")
        val games = MutableStateFlow(restored)
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = restored,
            scope = backgroundScope,
        )
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS * 4)

        assertEquals("loading a game must not write it again", emptyList<SavedGame>(), dao.savedGames())
    }

    @Test
    fun `a genuine change after a load is written`() = runTest(testDispatcher) {
        val restored = savedGame(points = 7, letters = "abc")
        val games = MutableStateFlow(restored)
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = restored,
            scope = backgroundScope,
        )
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS * 4)

        games.value = savedGame(points = 9, letters = "abc")
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf(savedGame(points = 9, letters = "abc")), dao.savedGames())
    }

    @Test
    fun `a new game with nothing persisted yet is written`() = runTest(testDispatcher) {
        val games = MutableStateFlow(savedGame(points = 0, letters = "abc"))
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = null,
            scope = backgroundScope,
        )
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf(savedGame(points = 0, letters = "abc")), dao.savedGames())
    }

    @Test
    fun `a reset cannot be overwritten by the change it followed`() = runTest(testDispatcher) {
        val played = savedGame(points = 12, letters = "abc")
        val games = MutableStateFlow(played)
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = played,
            scope = backgroundScope,
        )
        advanceUntilIdle()

        // A stone is placed and, before the window elapses, the game is cleared.
        games.value = savedGame(points = 13, letters = "abc")
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS / 2)
        val reset = savedGame(points = 0, letters = "abc")
        games.value = reset
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS * 4)

        assertEquals("the cleared game must be the last word", reset, dao.savedGames().last())
    }

    @Test
    fun `a change from before a reset never lands after it`() = runTest(testDispatcher) {
        val played = savedGame(points = 12, letters = "abc")
        val games = MutableStateFlow(played)
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = played,
            scope = backgroundScope,
        )
        advanceUntilIdle()

        games.value = savedGame(points = 13, letters = "abc")
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        val reset = savedGame(points = 0, letters = "abc")
        games.value = reset
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf(savedGame(points = 13, letters = "abc"), reset), dao.savedGames())
    }

    @Test
    fun `the last change is still written when the scope dies inside the window`() = runTest(testDispatcher) {
        val screenScope = TestScope(testScheduler)
        val games = MutableStateFlow(savedGame(points = 0))
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = savedGame(points = 0),
            scope = screenScope,
        )
        advanceUntilIdle()

        // The player places a stone and immediately leaves the screen.
        games.value = savedGame(points = 5)
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS / 2)
        screenScope.cancel()
        advanceUntilIdle()

        assertEquals("the last stone must not be lost", listOf(savedGame(points = 5)), dao.savedGames())
    }

    @Test
    fun `nothing extra is written on teardown when everything is already saved`() = runTest(testDispatcher) {
        val screenScope = TestScope(testScheduler)
        val games = MutableStateFlow(savedGame(points = 0))
        GameStateAutoSaver(repository).start(
            games = games,
            alreadyPersisted = savedGame(points = 0),
            scope = screenScope,
        )
        advanceUntilIdle()

        games.value = savedGame(points = 5)
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        screenScope.cancel()
        advanceUntilIdle()

        assertEquals(listOf(savedGame(points = 5)), dao.savedGames())
    }
}
