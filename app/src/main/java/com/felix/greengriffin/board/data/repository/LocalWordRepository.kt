package com.felix.greengriffin.board.data.repository

import com.felix.greengriffin.board.data.local.DictionaryDao
import com.felix.greengriffin.board.data.local.DictionaryWord
import com.felix.greengriffin.board.domain.ValidWord
import com.felix.greengriffin.board.domain.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalWordRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao,
) : WordRepository {
    fun getRandomWord(
        language: String,
        length: Int,
        firstLetter: String,
    ): Flow<DictionaryWord?> {
        return dictionaryDao.getRandomWord(language, length, firstLetter)
    }

    override suspend fun getValidWords(wordsToCheck: List<String>): List<ValidWord> {
        return dictionaryDao.getWords(
            wordsInLowercase = wordsToCheck.map{it.lowercase()}
        ).map { // todo maybe move mapper to own function
            ValidWord(
                word = it.word,
                language = it.language
            )
        }
    }
}
