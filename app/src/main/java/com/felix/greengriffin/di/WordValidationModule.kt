package com.felix.greengriffin.di

import com.felix.greengriffin.data.repository.DictionaryRepository
import com.felix.greengriffin.validation.WordLookUp
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WordValidationModule {

    @Binds
    abstract fun bindWordLookUp(dictionaryRepository: DictionaryRepository): WordLookUp

}