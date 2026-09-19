package com.felix.greengriffin.board.domain.model

/** How the stones of a placement line up on the board. */
enum class Alignment {
    Horizontal, Vertical, Single, Unaligned
}

/**
 * How [stones] line up on the board.
 *
 * This is the single source of truth for both placement validation and word extraction,
 * so the two can never disagree about what a placement is.
 */
fun alignmentOf(stones: List<StoneOnBoard>): Alignment = when {
    stones.size == 1 -> Alignment.Single
    stones.areHorizontallyAligned -> Alignment.Horizontal
    stones.areVerticallyAligned -> Alignment.Vertical
    else -> Alignment.Unaligned
}

/** The stones in reading order along [alignment]; unordered alignments keep their order. */
fun List<StoneOnBoard>.sortedForAlignment(alignment: Alignment): List<StoneOnBoard> =
    when (alignment) {
        Alignment.Horizontal -> sortedBy(StoneOnBoard::columnIndex)
        Alignment.Vertical -> sortedBy(StoneOnBoard::rowIndex)
        Alignment.Single, Alignment.Unaligned -> this
    }
