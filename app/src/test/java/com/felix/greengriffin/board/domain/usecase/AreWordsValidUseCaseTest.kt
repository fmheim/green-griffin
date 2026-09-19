package com.felix.greengriffin.board.domain.usecase

import com.felix.greengriffin.board.domain.ValidWord
import com.felix.greengriffin.board.domain.WordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeWordRepository(private val entries: List<ValidWord>) : WordRepository {
    var lastQuery: List<String>? = null

    override suspend fun getValidWords(wordsToCheck: List<String>): List<ValidWord> {
        lastQuery = wordsToCheck
        val wanted = wordsToCheck.map(String::lowercase)
        return entries.filter { it.word.lowercase() in wanted }
    }
}

class AreWordsValidUseCaseTest {

    private val allowed = listOf("de", "sv")

    @Test
    fun `an empty word list is not valid`() = runBlocking {
        val useCase = AreWordsValidUseCase(FakeWordRepository(emptyList()))

        val result = useCase(words = emptyList(), allowedLanguages = allowed)

        assertTrue("an empty placement must never be submittable, was $result", result is WordValidation.Invalid)
    }

    @Test
    fun `a word in an allowed language is valid`() = runBlocking {
        val useCase = AreWordsValidUseCase(FakeWordRepository(listOf(ValidWord("haus", "de"))))

        val result = useCase(words = listOf("HAUS"), allowedLanguages = allowed)

        assertTrue("HAUS is a German word, was $result", result is WordValidation.Valid)
    }

    @Test
    fun `a word that is not in the dictionary is invalid`() = runBlocking {
        val useCase = AreWordsValidUseCase(FakeWordRepository(listOf(ValidWord("haus", "de"))))

        val result = useCase(words = listOf("HAUS", "XQZ"), allowedLanguages = allowed)

        assertEquals(
            WordValidation.Invalid(
                checkedWords = listOf("HAUS", "XQZ"),
                allowedLanguages = allowed,
                found = listOf(ValidWord("haus", "de")),
            ),
            result,
        )
    }

    @Test
    fun `a word only in a disallowed language is invalid`() = runBlocking {
        val useCase = AreWordsValidUseCase(FakeWordRepository(listOf(ValidWord("house", "en"))))

        val result = useCase(words = listOf("HOUSE"), allowedLanguages = allowed)

        assertTrue("English is not an allowed language, was $result", result is WordValidation.Invalid)
    }

    @Test
    fun `words are passed to the repository as they are and matched case-insensitively`() = runBlocking {
        // Lowercasing for the SQL lookup is LocalWordRepository's job, not the use case's.
        val repository = FakeWordRepository(listOf(ValidWord("haus", "de")))

        val result = AreWordsValidUseCase(repository)(
            words = listOf("HAUS"),
            allowedLanguages = allowed,
        )

        assertEquals(listOf("HAUS"), repository.lastQuery)
        assertTrue("case must not decide validity, was $result", result is WordValidation.Valid)
    }

    @Test
    fun `a word that also exists in a disallowed language is still valid`() = runBlocking {
        // The dictionary lookup is not filtered by language, so a spelling that exists in
        // several languages comes back as several entries.
        val useCase = AreWordsValidUseCase(
            FakeWordRepository(listOf(ValidWord("haus", "de"), ValidWord("haus", "en"))),
        )

        val result = useCase(words = listOf("HAUS"), allowedLanguages = listOf("de", "sv"))

        assertTrue("HAUS is German; an English entry must not veto it, was $result", result is WordValidation.Valid)
    }

    @Test
    fun `a duplicated word only needs to be valid once`() = runBlocking {
        // Two crossing words can be the same string.
        val useCase = AreWordsValidUseCase(FakeWordRepository(listOf(ValidWord("otto", "de"))))

        val result = useCase(words = listOf("OTTO", "OTTO"), allowedLanguages = listOf("de", "sv"))

        assertTrue(result is WordValidation.Valid)
    }
}
