package com.felix.greengriffin.di

import android.content.Context
import androidx.room.Room
import com.felix.greengriffin.data.local.AppDatabase
import com.felix.greengriffin.data.local.DictionaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dictionary.db"
        )
            .createFromAsset("dictionary.db")
            .build()
    }

    @Provides
    fun provideDictionaryDao(appDatabase: AppDatabase): DictionaryDao {
        return appDatabase.dictionaryDao()
    }
}
