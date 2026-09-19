package com.felix.greengriffin.board.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Saved games and Trails progress. A few kilobytes, so it is backed up by Auto Backup.
 *
 * Unlike [DictionaryDatabase] this database must never be destructively migrated — every schema
 * change needs a real `Migration`. The schema is exported to `app/schemas/` (Room Gradle plugin)
 * and committed so migrations can be tested with `MigrationTestHelper`.
 */
@Database(
    entities = [
        GameStateEntity::class,
        CompletedLevelEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
    abstract fun completedLevelsDao(): CompletedLevelsDao

    companion object {
        const val DATABASE_NAME = "user_data.db"
    }
}
