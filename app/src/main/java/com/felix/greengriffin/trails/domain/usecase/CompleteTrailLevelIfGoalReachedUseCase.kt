package com.felix.greengriffin.trails.domain.usecase

import com.felix.greengriffin.board.data.repository.CompletedLevelsRepository
import com.felix.greengriffin.board.presentation.Field
import com.felix.greengriffin.board.presentation.GameMode
import com.felix.greengriffin.board.presentation.TrailLevel
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import javax.inject.Inject


class CompleteTrailLevelIfGoalReachedUseCase @Inject constructor(
    private val completedLevelsRepository: CompletedLevelsRepository,
) {
    suspend operator fun invoke(
        stonesOnBoard: Set<StoneOnBoard>,
        level: TrailLevel,
    ): Boolean {
        val isCompleted = isLevelCompleted(stonesOnBoard = stonesOnBoard, goalFields = level.goalFields)
        if (isCompleted) {
            completedLevelsRepository.completeLevel(
                gameModeId = GameMode.TRAILS_ID,
                levelIndex = level.index
            )
        }
        return isCompleted
    }

    private fun isLevelCompleted(stonesOnBoard: Set<StoneOnBoard>, goalFields: Set<Field>): Boolean {
        val lockedInStonesOnBoard =
            stonesOnBoard
                .filter(StoneOnBoard::isLocked)
                .map(StoneOnBoard::toField)

        return lockedInStonesOnBoard.any { it in goalFields }
    }
}




