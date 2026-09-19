package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.domain.ValidWord
import com.felix.greengriffin.board.domain.WordRepository
import javax.inject.Inject

sealed interface WordValidation {
    data class Valid(val words: List<ValidWord>) : WordValidation
    data class Invalid(
        val checkedWords: List<String>,
        val allowedLanguages: List<String>,
        val found: List<ValidWord> = emptyList(), // optional diagnostics
    ) : WordValidation

    // todo maybe distinguish between empty result and wrong language??
}

class AreWordsValidUseCase @Inject constructor(
    private val localWordRepository: WordRepository,
) {
    suspend operator fun invoke(
        words: List<String>,
        allowedLanguages: List<String>,
    ): WordValidation {
        // "Nothing was spelled" is never a valid placement, and an empty `IN ()` query
        // would otherwise make every comparison below trivially true.
        if (words.isEmpty()) {
            return WordValidation.Invalid(
                checkedWords = words,
                allowedLanguages = allowedLanguages,
            )
        }

        val wordsInDictionary = localWordRepository.getValidWords(words)
        val allWordsAreInADictionary =
            wordsInDictionary.map { it.word.lowercase() }.toSet() == words.map { it.lowercase() }
                .toSet()
        val isValidForLanguage =
            allWordsAreInADictionary && wordsInDictionary.all { it.language in allowedLanguages }
        return when {
            isValidForLanguage -> WordValidation.Valid(wordsInDictionary)
            else -> WordValidation.Invalid(
                checkedWords = words,
                allowedLanguages = allowedLanguages,
                found = wordsInDictionary
            )
        }
    }
}

