package com.felix.greengriffin.data.repository

import com.felix.greengriffin.data.local.DictionaryDao
import com.felix.greengriffin.data.local.DictionaryWord
import com.felix.greengriffin.validation.ValidWord
import com.felix.greengriffin.validation.WordLookUp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DictionaryRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao
): WordLookUp {
    fun getRandomWord(
        language: String,
        length: Int,
        firstLetter: String
    ): Flow<DictionaryWord?> {
        return dictionaryDao.getRandomWord(language, length, firstLetter)
    }

    override suspend fun getValidWords(wordToCheck: String): List<ValidWord> {
        return dictionaryDao.getWord(word = wordToCheck).map { // todo maybe move mapper to own function
            ValidWord(
                word = it.word,
                language = it.language
            )
        }
    }
}
