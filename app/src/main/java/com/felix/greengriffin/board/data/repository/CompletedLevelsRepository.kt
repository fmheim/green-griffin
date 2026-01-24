package com.felix.greengriffin.board.data.repository

import com.felix.greengriffin.board.data.local.CompletedLevelEntity
import com.felix.greengriffin.board.data.local.CompletedLevelsDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletedLevelsRepository @Inject constructor(
    private val completedLevelsDao: CompletedLevelsDao,
) {

    suspend fun completeLevel(gameModeId: Int, levelIndex: Int) {
        completedLevelsDao.insert(
            CompletedLevelEntity(
                gameModeId = gameModeId,
                levelIndex = levelIndex
            )
        )
    }

    fun observeCompletedLevelsFor(gameModeId: Int) =
        completedLevelsDao.observeCompletedLevelsFor(gameModeId)

    fun observeIfLevelIsCompleted(gameModeId: Int, levelIndex: Int) =
        completedLevelsDao.observeIfLevelIsCompleted(
            gameModeId = gameModeId,
            levelIndex = levelIndex
        )

}