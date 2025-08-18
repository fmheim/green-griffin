package com.felix.greengriffin.di

import com.felix.greengriffin.board.data.repository.LocalWordRepository
import com.felix.greengriffin.board.domain.WordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WordValidationModule {

    @Binds
    abstract fun bindWordLookUp(localWordRepository: LocalWordRepository): WordRepository

}