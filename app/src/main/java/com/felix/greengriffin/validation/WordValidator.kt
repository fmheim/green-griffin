package com.felix.greengriffin.validation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton


interface OpenThesaurusApiService {
    @GET("synonyme/search?format=application/json")
    suspend fun checkWord(@Query("q") word: String): ThesaurusResponse
}

@Singleton
class WordValidator @Inject constructor(
    private val apiService: OpenThesaurusApiService
) {
    suspend fun isValid(word: String): Boolean {
        return try {
            val response = apiService.checkWord(word)
            response.synsets.isNotEmpty()
        } catch (e: Exception) {
            // TODO: Add logging
            false
        }
    }
}

@Serializable
data class ThesaurusResponse(
    @SerialName("synsets")
    val synsets: List<Synset> = emptyList()
)

@Serializable
data class Synset(
    @SerialName("terms")
    val terms: List<Term>
)

@Serializable
data class Term(
    @SerialName("term")
    val term: String
)
