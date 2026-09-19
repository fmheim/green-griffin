package com.felix.greengriffin.di

import android.content.Context
import androidx.room.Room
import com.felix.greengriffin.board.data.local.CompletedLevelsDao
import com.felix.greengriffin.board.data.local.DictionaryDao
import com.felix.greengriffin.board.data.local.DictionaryDatabase
import com.felix.greengriffin.board.data.local.GameStateDao
import com.felix.greengriffin.board.data.local.UserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val LEGACY_COMBINED_DATABASE_NAME = "dictionary.db"

    @Provides
    @Singleton
    fun provideDictionaryDatabase(@ApplicationContext context: Context): DictionaryDatabase {
        // The pre-split database held the dictionary and the user data in one file. Its rows are
        // gone either way, and it is dead weight once the two databases below exist, so reclaim
        // its ~29 MB. deleteDatabase is a no-op when the file is absent and also removes the
        // -wal/-shm siblings, so this is safe to run on every start and can go once nobody is
        // running a pre-split build any more.
        context.deleteDatabase(LEGACY_COMBINED_DATABASE_NAME)

        return Room.databaseBuilder(
            context,
            DictionaryDatabase::class.java,
            DictionaryDatabase.DATABASE_NAME
        )
            .createFromAsset(DictionaryDatabase.ASSET_NAME)
            // The dictionary holds no user data, so re-copying it from the asset is always safe.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase {
        return Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            UserDatabase.DATABASE_NAME
        )
            // Deliberately no destructive fallback: saved games and Trails progress must survive
            // schema changes, so every version bump needs a real Migration.
            .build()
    }

    @Provides
    fun provideDictionaryDao(dictionaryDatabase: DictionaryDatabase): DictionaryDao {
        return dictionaryDatabase.dictionaryDao()
    }

    @Provides
    fun provideGameStateDao(userDatabase: UserDatabase): GameStateDao {
        return userDatabase.gameStateDao()
    }

    @Provides
    fun provideCompletedLevelsDao(userDatabase: UserDatabase): CompletedLevelsDao {
        return userDatabase.completedLevelsDao()
    }
}
