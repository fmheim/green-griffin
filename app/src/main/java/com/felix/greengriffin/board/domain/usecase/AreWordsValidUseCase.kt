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
    private val wordRepository: WordRepository,
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

        val wordsInDictionary = wordRepository.getValidWords(words)
        val entriesInAllowedLanguages = wordsInDictionary.filter { it.language in allowedLanguages }
        // Every checked word needs *an* entry in an allowed language. Asking whether
        // *all* returned entries are allowed would let a word that also exists in some
        // other language veto itself.
        val everyWordIsAllowed = words.all { word ->
            entriesInAllowedLanguages.any { it.word.equals(word, ignoreCase = true) }
        }
        return when {
            everyWordIsAllowed -> WordValidation.Valid(entriesInAllowedLanguages)
            else -> WordValidation.Invalid(
                checkedWords = words,
                allowedLanguages = allowedLanguages,
                found = wordsInDictionary
            )
        }
    }
}

