package com.felix.greengriffin.board.domain

data class ValidWord(
    val word: String,
    val language: String
)

interface WordRepository{

    suspend fun getValidWords(wordsToCheck: List<String>): List<ValidWord>
}