package com.felix.greengriffin.validation

data class ValidWord(
    val word: String,
    val language: String
)

interface WordLookUp{
    suspend fun getValidWords(wordToCheck: String): List<ValidWord>
}