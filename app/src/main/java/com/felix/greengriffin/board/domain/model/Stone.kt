package com.felix.greengriffin.board.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serial names are pinned to the class names this hierarchy had before it moved out of
 * `board.presentation.components`, so that stones written by an older build still
 * deserialize.
 */
@Serializable
@SerialName("com.felix.greengriffin.board.presentation.components.StoneData")
sealed class StoneData {
    abstract val letter: Char
    abstract val value: Int
    abstract val id: String
    val isJoker get() = letter == ' ' && value == 0
}

@Serializable
@SerialName("com.felix.greengriffin.board.presentation.components.StoneInBag")
data class StoneInBag(
    override val letter: Char,
    override val value: Int,
    override val id: String,
) : StoneData() {

    fun toStoneInHand(userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId,
    )
}

@Serializable
@SerialName("com.felix.greengriffin.board.presentation.components.StoneInHand")
data class StoneInHand(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val userId: Int,
) : StoneData() {

    fun toStoneOnBoard(rowIndex: Int, columnIndex: Int) = StoneOnBoard(
        letter = letter,
        value = value,
        id = id,
        rowIndex = rowIndex,
        columnIndex = columnIndex,
        isLocked = false
    )
}

@Serializable
@SerialName("com.felix.greengriffin.board.presentation.components.StoneOnBoard")
data class StoneOnBoard(
    override val letter: Char,
    override val value: Int,
    override val id: String,
    val rowIndex: Int,
    val columnIndex: Int,
    val isLocked: Boolean,
) : StoneData() {

    fun toStoneInHand(userId: Int) = StoneInHand(
        letter = letter,
        value = value,
        id = id,
        userId = userId,
    )

    fun toField() = Field(
        row = rowIndex,
        column = columnIndex,
    )

    fun isToLeftOf(other: StoneOnBoard) =
        columnIndex == other.columnIndex - 1 && rowIndex == other.rowIndex

    fun isAbove(other: StoneOnBoard) =
        columnIndex == other.columnIndex && rowIndex == other.rowIndex - 1

    fun isToRightOf(other: StoneOnBoard) =
        columnIndex == other.columnIndex + 1 && rowIndex == other.rowIndex

    fun isBelow(other: StoneOnBoard) =
        columnIndex == other.columnIndex && rowIndex == other.rowIndex + 1
}

val List<StoneOnBoard>.areHorizontallyAligned: Boolean get() = map { it.rowIndex }.toSet().size == 1
val List<StoneOnBoard>.areVerticallyAligned: Boolean get() = map { it.columnIndex }.toSet().size == 1
