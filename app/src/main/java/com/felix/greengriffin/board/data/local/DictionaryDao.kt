package com.felix.greengriffin.board.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE word IN (:wordsInLowercase)") // Note: All words are stored in lowercase
    suspend fun getWords(wordsInLowercase: List<String>): List<DictionaryWord>
}
