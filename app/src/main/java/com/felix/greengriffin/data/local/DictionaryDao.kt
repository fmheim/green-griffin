package com.felix.greengriffin.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE language = :language AND length = :length AND first_letter = :firstLetter ORDER BY RANDOM() LIMIT 1")
    fun getRandomWord(language: String, length: Int, firstLetter: String): Flow<DictionaryWord?>

    @Query("SELECT * FROM dictionary WHERE word = :wordInLowercase") // Note: All words are stored in lowercase
    suspend fun getWord(wordInLowercase: String): List<DictionaryWord>
}
