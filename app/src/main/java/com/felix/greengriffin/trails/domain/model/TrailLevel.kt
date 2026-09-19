package com.felix.greengriffin.trails.domain.model

import com.felix.greengriffin.board.domain.model.Field
import kotlinx.serialization.Serializable

@Serializable
data class TrailLevel(
    val index: Int,
    val boardSize: Int,
    val startFields: Set<Field>,
    val goalFields: Set<Field>,
    val blockedFields: Set<Field> = emptySet(),
)

val trailLevels = setOf(
    TrailLevel(
        index = 1,
        boardSize = 10,
        startFields = List(10) { Field(row = it, column = 0) }.toSet(),
        goalFields = List(10) { Field(row = it, column = 9) }.toSet(),
    ),
    TrailLevel(
        index = 2,
        boardSize = 10,
        startFields = List(10) { Field(row = 0, column = it) }.toSet(),
        goalFields = List(10) { Field(row = 9, column = it) }.toSet(),
    ),
    TrailLevel(
        index = 3,
        boardSize = 10,
        startFields = List(6) { Field(row = it, column = 0) }.toSet(),
        goalFields = List(6) { Field(row = it + 4, column = 9) }.toSet(),
    ),

)
