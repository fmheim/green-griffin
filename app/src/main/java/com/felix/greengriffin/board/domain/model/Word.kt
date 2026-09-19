package com.felix.greengriffin.board.domain.model

data class Word(
    val letters: List<StoneData>,
) {
    val points get() = letters.sumOf { it.value }
    val asString get() = letters.map { it.letter }.joinToString("")
}

fun List<StoneData>.asWord() = Word(letters = this)
