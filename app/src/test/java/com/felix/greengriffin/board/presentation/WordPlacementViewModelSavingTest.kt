package com.felix.greengriffin.board.presentation

import com.felix.greengriffin.RouteToWordPlacementScreen
import com.felix.greengriffin.board.data.local.CompletedLevelEntity
import com.felix.greengriffin.board.data.local.CompletedLevelsDao
import com.felix.greengriffin.board.data.mapper.toEntity
import com.felix.greengriffin.board.data.repository.CompletedLevelsRepository
import com.felix.greengriffin.board.data.repository.GameStateRepository
import com.felix.greengriffin.board.domain.ValidWord
import com.felix.greengriffin.board.domain.WordRepository
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.SavedGame
import com.felix.greengriffin.board.domain.model.StoneInHand
import com.felix.greengriffin.board.domain.usecase.AreWordsValidUseCase
import com.felix.greengriffin.board.domain.usecase.FindNewlyCreatedWordsUseCase
import com.felix.greengriffin.board.domain.usecase.IsPlacementValidUseCase
import com.felix.greengriffin.trails.domain.usecase.CompleteTrailLevelIfGoalReachedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class NoWordsRepository : WordRepository {
    override suspend fun getValidWords(wordsToCheck: List<String>): List<ValidWord> = emptyList()
}

private class FakeCompletedLevelsDao : CompletedLevelsDao {
    override fun observeCompletedLevelsFor(gameModeId: Int): Flow<List<Int>> = flowOf(emptyList())
    override suspend fun insert(completedLevel: CompletedLevelEntity) = Unit
    override fun observeIfLevelIsCompleted(gameModeId: Int, levelIndex: Int): Flow<Boolean> =
        flowOf(false)
}

/**
 * The saving behaviour of the ViewModel, driven through [GameEvent] exactly as the screen
 * drives it. Word lookups still run on [Dispatchers.IO], but they only touch state that is
 * not persisted, so they cannot influence what is written here.
 */
class WordPlacementViewModelSavingTest {

    private val mainDispatcher = StandardTestDispatcher()
    private val dao = RecordingGameStateDao()
    private val completedLevelsDao = FakeCompletedLevelsDao()

    @Before
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): WordPlacementViewModel {
        val completedLevelsRepository = CompletedLevelsRepository(completedLevelsDao)
        return WordPlacementViewModel(
            areWordsValid = AreWordsValidUseCase(NoWordsRepository()),
            isPlacementValid = IsPlacementValidUseCase(),
            findNewlyCreatedWords = FindNewlyCreatedWordsUseCase(),
            completeTrailLevelIfGoalReached = CompleteTrailLevelIfGoalReachedUseCase(
                completedLevelsRepository
            ),
            gameStateRepository = GameStateRepository(dao, mainDispatcher),
            completedLevelsRepository = completedLevelsRepository,
            navKey = RouteToWordPlacementScreen(gameMode = GameMode.FreePlay),
        )
    }

    @Test
    fun `a restored game is not written back on load`() = runTest(mainDispatcher) {
        val restored = savedGame(points = 42, letters = "abcdefg")
        dao.storedGame = restored.toEntity()
        dao.writtenGames.clear()

        val viewModel = viewModel()
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS * 4)

        assertEquals(
            "restoring a game must not immediately write it again",
            emptyList<SavedGame>(),
            dao.savedGames(),
        )
        assertEquals(42L, viewModel.state.value.totalPoints)
    }

    @Test
    fun `a new game is written once, after the debounce window`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        // Long enough for the load to finish, short enough for the window to still be open.
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS)

        assertEquals("nothing is written before the window elapses", 0, dao.savedGames().size)

        advanceTimeBy(2)

        assertEquals(1, dao.savedGames().size)
        assertEquals(viewModel.state.value.asSavedGame(), dao.savedGames().single())
    }

    @Test
    fun `a stone drop costs a single write`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        val writesAfterDealing = dao.savedGames().size

        viewModel.onEvent(
            GameEvent.StoneDroppedOnBoard(
                stoneData = viewModel.state.value.stonesInHand.first(),
                rowIndex = 4,
                columnIndex = 4,
            )
        )
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(
            "one drop used to fire four saves",
            writesAfterDealing + 1,
            dao.savedGames().size,
        )
        assertEquals(viewModel.state.value.asSavedGame(), dao.savedGames().last())
    }

    @Test
    fun `hovering over fields is not written at all`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        val writesAfterDealing = dao.savedGames().size

        repeat(20) { field ->
            viewModel.onEvent(GameEvent.FieldEntered(index = field))
            advanceTimeBy(10)
        }
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(
            "a hover highlight is not part of the saved game",
            writesAfterDealing,
            dao.savedGames().size,
        )
    }

    @Test
    fun `clearing is not undone by the save the drop before it started`() =
        runTest(mainDispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

            // Drop a stone and clear the game before the drop could be written.
            viewModel.onEvent(
                GameEvent.StoneDroppedOnBoard(
                    stoneData = viewModel.state.value.stonesInHand.first(),
                    rowIndex = 4,
                    columnIndex = 4,
                )
            )
            advanceTimeBy(SAVE_DEBOUNCE_MILLIS / 2)
            viewModel.onEvent(GameEvent.ClearGameStateClick)
            advanceUntilIdle()
            advanceTimeBy(SAVE_DEBOUNCE_MILLIS * 4)

            val persisted = dao.savedGames().last()
            assertTrue(
                "the cleared board must stay cleared, was ${persisted.stonesOnBoard}",
                persisted.stonesOnBoard.isEmpty(),
            )
            assertEquals(
                "the row must end up holding exactly what the player sees",
                viewModel.state.value.asSavedGame(),
                persisted,
            )
        }

    @Test
    fun `a cleared game reloads as a cleared game`() = runTest(mainDispatcher) {
        val first = viewModel()
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        first.onEvent(
            GameEvent.StoneDroppedOnBoard(
                stoneData = first.state.value.stonesInHand.first(),
                rowIndex = 4,
                columnIndex = 4,
            )
        )
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)
        first.onEvent(GameEvent.ClearGameStateClick)
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        val reopened = viewModel()
        advanceUntilIdle()
        advanceTimeBy(SAVE_DEBOUNCE_MILLIS + 1)

        assertTrue(
            "a cleared game must not come back with stones on the board",
            reopened.state.value.stonesOnBoard.isEmpty(),
        )
        assertEquals(0L, reopened.state.value.totalPoints)
        assertEquals(
            "a hand is still dealt after a clear",
            HAND_SIZE,
            reopened.state.value.stonesInHand.count { stone: StoneInHand -> stone.userId == 1 },
        )
    }
}
