package com.felix.greengriffin.data.repository

import com.felix.greengriffin.data.local.DictionaryDao
import com.felix.greengriffin.data.local.DictionaryWord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DictionaryRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao
) {
    fun getRandomWord(
        language: String,
        length: Int,
        firstLetter: String
    ): Flow<DictionaryWord?> {
        return dictionaryDao.getRandomWord(language, length, firstLetter)
    }
}
