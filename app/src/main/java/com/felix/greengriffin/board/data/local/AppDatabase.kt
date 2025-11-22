package com.felix.greengriffin.board.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DictionaryWord::class,
        GameStateEntity::class,
        StoneInHandEntity::class,
        StoneOnBoardEntity::class,
        StoneInBagEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun gameStateDao(): GameStateDao
}
