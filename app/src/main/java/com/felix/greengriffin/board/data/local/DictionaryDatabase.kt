package com.felix.greengriffin.board.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Read-only word list, pre-populated from `assets/dictionary.db`.
 *
 * This is a rebuildable cache: it contains no user data, so it may be dropped and re-copied from
 * the asset at any time. It is therefore excluded from Auto Backup (see `res/xml/backup_rules.xml`
 * and `res/xml/data_extraction_rules.xml`) — at ~29 MB it alone exceeds the 25 MB backup quota.
 */
@Database(
    entities = [DictionaryWord::class],
    version = 1,
    exportSchema = false,
)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        /**
         * Deliberately *not* `dictionary.db`: that name belongs to the old combined database
         * (dictionary + user tables), whose Room identity hash no longer matches this
         * dictionary-only schema. A fresh file makes Room copy the asset again instead of failing
         * to open a file it cannot verify.
         */
        const val DATABASE_NAME = "dictionary_cache.db"
        const val ASSET_NAME = "dictionary.db"
    }
}
