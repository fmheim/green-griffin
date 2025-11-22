package com.felix.greengriffin.board.data.mapper

import com.felix.greengriffin.board.data.local.GameStateEntity
import com.felix.greengriffin.board.data.local.StoneInBagEntity
import com.felix.greengriffin.board.data.local.StoneInHandEntity
import com.felix.greengriffin.board.data.local.StoneOnBoardEntity
import com.felix.greengriffin.board.presentation.GameState
import com.felix.greengriffin.board.presentation.components.StoneInBag
import com.felix.greengriffin.board.presentation.components.StoneInHand
import com.felix.greengriffin.board.presentation.components.StoneOnBoard

// Convert domain models to entities
fun GameState.toEntity(): GameStateEntity {
    return GameStateEntity(
        id = 1,
        currentUserId = currentUserId,
        totalPoints = totalPoints
    )
}

fun StoneInHand.toEntity(): StoneInHandEntity {
    return StoneInHandEntity(
        id = id,
        letter = letter,
        value = value,
        userId = userId
    )
}

fun StoneOnBoard.toEntity(): StoneOnBoardEntity {
    return StoneOnBoardEntity(
        id = id,
        letter = letter,
        value = value,
        rowIndex = rowIndex,
        columnIndex = columnIndex,
        isLocked = isLocked
    )
}

fun StoneInBag.toEntity(): StoneInBagEntity {
    return StoneInBagEntity(
        id = id,
        letter = letter,
        value = value
    )
}

// Convert entities to domain models
fun StoneInHandEntity.toDomain(): StoneInHand {
    return StoneInHand(
        id = id,
        letter = letter,
        value = value,
        userId = userId
    )
}

fun StoneOnBoardEntity.toDomain(): StoneOnBoard {
    return StoneOnBoard(
        id = id,
        letter = letter,
        value = value,
        rowIndex = rowIndex,
        columnIndex = columnIndex,
        isLocked = isLocked
    )
}

fun StoneInBagEntity.toDomain(): StoneInBag {
    return StoneInBag(
        id = id,
        letter = letter,
        value = value
    )
}
