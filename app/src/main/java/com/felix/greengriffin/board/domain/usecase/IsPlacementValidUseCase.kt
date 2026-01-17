package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.domain.usecase.GameModeViolation.FirstWordNotOnCorrectStartPosition
import com.felix.greengriffin.board.presentation.GameMode
import com.felix.greengriffin.board.presentation.TrailLevel
import com.felix.greengriffin.board.presentation.components.StoneData
import com.felix.greengriffin.board.presentation.components.StoneOnBoard
import com.felix.greengriffin.util.extensions.list.isEmptyOrOnlyNulls
import javax.inject.Inject

sealed interface PlacementValidation {
    data object Valid : PlacementValidation
    data object NoStonesPlaced : PlacementValidation
    data object NotAligned : PlacementValidation
    data object HasGaps : PlacementValidation
    data object NotConnected : PlacementValidation
    data class Violation(val gameModeViolation: GameModeViolation) : PlacementValidation
}

enum class GameModeViolation {
    FirstWordNotOnCorrectStartPosition;
}

class IsPlacementValidUseCase @Inject constructor() {

    operator fun invoke(
        stonesOnBoard: Set<StoneOnBoard>,
        gameMode: GameMode,
    ): PlacementValidation {
        val unlockedStonesOnBoard = stonesOnBoard.filterNot(StoneOnBoard::isLocked)

        if (unlockedStonesOnBoard.isEmpty()) {
            return PlacementValidation.NoStonesPlaced
        }

        val alignment = getAlignment(unlockedStonesOnBoard)

        if (alignment == Alignment.Unaligned) {
            return PlacementValidation.NotAligned
        }

        val sortedUnlockedStones = sortStones(unlockedStonesOnBoard, alignment)
        val firstStone = sortedUnlockedStones.first()
        val lastStone = sortedUnlockedStones.last()

        // Check for gaps in placement
        when (alignment) {
            Alignment.Horizontal -> {
                for (columnIndex in firstStone.columnIndex..lastStone.columnIndex) {
                    if (stonesOnBoard.none { it.columnIndex == columnIndex && it.rowIndex == firstStone.rowIndex }) {
                        return PlacementValidation.HasGaps
                    }
                }
            }

            Alignment.Vertical -> {
                for (rowIndex in firstStone.rowIndex..lastStone.rowIndex) {
                    if (stonesOnBoard.none { it.rowIndex == rowIndex && it.columnIndex == firstStone.columnIndex }) {
                        return PlacementValidation.HasGaps
                    }
                }
            }

            Alignment.Single -> Unit // Single stones cannot have gaps
            Alignment.Unaligned -> return PlacementValidation.NotAligned
        }


        val lockedNeighbourStones = mutableListOf<StoneData>()

        for (unlockedStone in sortedUnlockedStones) {
            val neighbourStones = listOf(
                stonesOnBoard.find { it.isToLeftOf(unlockedStone) },
                stonesOnBoard.find { it.isAbove(unlockedStone) },
                stonesOnBoard.find { it.isToRightOf(unlockedStone) },
                stonesOnBoard.find { it.isBelow(unlockedStone) }
            )

            if (neighbourStones.isEmptyOrOnlyNulls()) {
                return PlacementValidation.NotConnected
            }

            val lockedStones = neighbourStones.filter { it?.isLocked == true }.filterNotNull()
            lockedNeighbourStones.addAll(lockedStones)
        }

        val isConnectedOrFirstMove =
            lockedNeighbourStones.isNotEmpty() || stonesOnBoard.none { it.isLocked }

        val gameModeValidation = when (gameMode) {
            is GameMode.FreePlay -> PlacementValidation.Valid
            is GameMode.Trails -> validateTrailGameMode(
                trailLevel = gameMode.level,
                stonesOnBoard = stonesOnBoard
            )
        }

        return when {
            !isConnectedOrFirstMove -> PlacementValidation.NotConnected
            else -> gameModeValidation
        }

    }

    private fun getAlignment(stones: List<StoneOnBoard>): Alignment {
        return when {
            stones.size == 1 -> Alignment.Single
            stones.areHorizontallyAligned -> Alignment.Horizontal
            stones.areVerticallyAligned -> Alignment.Vertical
            else -> Alignment.Unaligned
        }
    }

    private fun sortStones(stones: List<StoneOnBoard>, alignment: Alignment): List<StoneOnBoard> {
        return when (alignment) {
            Alignment.Horizontal -> stones.sortedBy(StoneOnBoard::columnIndex)
            Alignment.Vertical -> stones.sortedBy(StoneOnBoard::rowIndex)
            else -> stones
        }
    }

    private fun validateTrailGameMode(
        trailLevel: TrailLevel,
        stonesOnBoard: Set<StoneOnBoard>,
    ): PlacementValidation {
        val isOutsideStart = isTrailStartingOutsideStartFields(
            trailLevel = trailLevel,
            stonesOnBoard = stonesOnBoard
        )
        return when {
            isOutsideStart -> PlacementValidation.Violation(gameModeViolation = FirstWordNotOnCorrectStartPosition)
            else -> PlacementValidation.Valid
        }
    }

    private fun isTrailStartingOutsideStartFields(
        trailLevel: TrailLevel,
        stonesOnBoard: Set<StoneOnBoard>,
    ): Boolean {
        val startFields = trailLevel.startFields
        return (stonesOnBoard.map(StoneOnBoard::toField).none { it in startFields })
    }

    private enum class Alignment {
        Horizontal, Vertical, Single, Unaligned
    }

    private val List<StoneOnBoard>.areHorizontallyAligned: Boolean
        get() = map { it.rowIndex }.toSet().size == 1

    private val List<StoneOnBoard>.areVerticallyAligned: Boolean
        get() = map { it.columnIndex }.toSet().size == 1
}

// Extension function to find related stones
private fun Set<StoneOnBoard>.find(predicate: (StoneOnBoard) -> Boolean): StoneOnBoard? {
    return firstOrNull(predicate)
}
