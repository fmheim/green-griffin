package com.felix.greengriffin.board.domain.usecase

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
}

class IsPlacementValidUseCase @Inject constructor() {

    operator fun invoke(
        stonesOnBoard: Set<StoneOnBoard>,
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

        // Check connection to existing locked stones
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

        // Either connected to a locked stone or first move (no locked stones on board)
        val isConnectedOrFirstMove =
            lockedNeighbourStones.isNotEmpty() || stonesOnBoard.none { it.isLocked }

        return if (isConnectedOrFirstMove) {
            PlacementValidation.Valid
        } else {
            PlacementValidation.NotConnected
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
