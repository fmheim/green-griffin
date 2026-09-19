package com.felix.greengriffin.board.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedGame(
    val gameModeId: Int,
    val levelIndex: Int,
    val totalPoints: Long,
    val stonesInHand: List<StoneInHand>,
    val stonesOnBoard: Set<StoneOnBoard>,
    val stonesInBag: Set<StoneInBag>,
)
