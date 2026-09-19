package com.felix.greengriffin.trails.domain.usecase

import com.felix.greengriffin.board.data.local.CompletedLevelEntity
import com.felix.greengriffin.board.data.local.CompletedLevelsDao
import com.felix.greengriffin.board.data.repository.CompletedLevelsRepository
import com.felix.greengriffin.board.domain.model.Field
import com.felix.greengriffin.board.domain.model.GameMode
import com.felix.greengriffin.board.domain.model.StoneOnBoard
import com.felix.greengriffin.trails.domain.model.TrailLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCompletedLevelsDao : CompletedLevelsDao {
    val inserted = mutableListOf<CompletedLevelEntity>()

    override suspend fun insert(completedLevel: CompletedLevelEntity) {
        inserted += completedLevel
    }

    override fun observeCompletedLevelsFor(gameModeId: Int): Flow<List<Int>> =
        flowOf(inserted.filter { it.gameModeId == gameModeId }.map { it.levelIndex })

    override fun observeIfLevelIsCompleted(gameModeId: Int, levelIndex: Int): Flow<Boolean> =
        flowOf(CompletedLevelEntity(gameModeId, levelIndex) in inserted)
}

class CompleteTrailLevelIfGoalReachedUseCaseTest {

    private val dao = FakeCompletedLevelsDao()
    private val completeLevel =
        CompleteTrailLevelIfGoalReachedUseCase(CompletedLevelsRepository(dao))

    private val level = TrailLevel(
        index = 7,
        boardSize = 10,
        startFields = setOf(Field(row = 0, column = 0)),
        goalFields = setOf(Field(row = 0, column = 9)),
    )

    private fun stone(row: Int, column: Int, locked: Boolean) =
        StoneOnBoard('A', 1, "$row-$column", row, column, locked)

    @Test
    fun `a board that never reaches a goal field does not complete the level`() = runBlocking {
        val reached = completeLevel(
            stonesOnBoard = setOf(stone(0, 0, locked = true), stone(0, 1, locked = true)),
            level = level,
        )

        assertFalse(reached)
        assertTrue("nothing should be written", dao.inserted.isEmpty())
    }

    @Test
    fun `a locked stone on a goal field completes the level`() = runBlocking {
        val reached = completeLevel(stonesOnBoard = setOf(stone(0, 9, locked = true)), level = level)

        assertTrue(reached)
        assertEquals(
            listOf(CompletedLevelEntity(gameModeId = GameMode.TRAILS_ID, levelIndex = 7)),
            dao.inserted,
        )
    }

    @Test
    fun `an unlocked stone on a goal field does not complete the level`() = runBlocking {
        // The word still has to be submitted before it counts.
        val reached = completeLevel(stonesOnBoard = setOf(stone(0, 9, locked = false)), level = level)

        assertFalse(reached)
        assertTrue(dao.inserted.isEmpty())
    }

    @Test
    fun `a level without goal fields can never be completed`() = runBlocking {
        val reached = completeLevel(
            stonesOnBoard = setOf(stone(0, 9, locked = true)),
            level = level.copy(goalFields = emptySet()),
        )

        assertFalse(reached)
    }

    @Test
    fun `completion is recorded against the trails game mode`() = runBlocking {
        completeLevel(stonesOnBoard = setOf(stone(0, 9, locked = true)), level = level)

        assertEquals(GameMode.TRAILS_ID, dao.inserted.single().gameModeId)
    }

    @Test
    fun `reaching a goal field is the only requirement, not a path from the start`() = runBlocking {
        // Records today's rule deliberately: there is no start-to-goal path check, so a
        // single locked stone touching a goal field is enough. See the PR description.
        val reached = completeLevel(stonesOnBoard = setOf(stone(0, 9, locked = true)), level = level)

        assertTrue(reached)
    }
}
