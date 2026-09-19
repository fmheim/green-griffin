package com.felix.greengriffin.board.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Field(
    val row: Int,
    val column: Int,
)
