package com.felix.greengriffin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DictionaryWord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
}
