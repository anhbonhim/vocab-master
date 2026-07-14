package com.nhimz.vocabmaster.data.remote

import com.nhimz.vocabmaster.data.remote.AnswerRequestDto
import com.nhimz.vocabmaster.data.remote.PlacementAnswerResponse
import com.nhimz.vocabmaster.data.remote.PlacementStartResponse
import com.nhimz.vocabmaster.data.remote.SyncPayload
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TopicsResponse(
    val data: List<String>
)

@Serializable
data class VocabularyItemDto(
    val id: Int,
    val word: String,  // Note: we can keep this matching the backend schema
    val definition: String,
    val part_of_speech: String,
    val difficulty_level: String,
    val ipa: String? = null,
    val topic: String,
    val audio_url: String? = null,
    val example: String? = null,
    val scrambled_data: String? = null
)

@Serializable
data class VocabularyCatalogDto(
    val topic: String,
    val level: String,
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<VocabularyItemDto>
)

interface VocabularyApiService {
    @GET("api/v1/vocabulary/topics")
    suspend fun getTopics(): Response<TopicsResponse>

    @GET("api/v1/vocabulary/catalog")
    suspend fun getCatalog(
        @Query("topic") topic: String,
        @Query("level") level: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<VocabularyCatalogDto>
}

interface SyncApiService {
    @POST("api/v1/sync/push")
    suspend fun pushSync(@Body payload: SyncPayload): Response<Unit>

    @GET("api/v1/sync/pull")
    suspend fun pullSync(@Query("since") since: Long): Response<SyncPayload>
}