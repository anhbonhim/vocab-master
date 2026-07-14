package com.nhimz.vocabmaster.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class PlacementOptionDto(
    val id: Int,
    val text: String
)

@Serializable
data class PlacementQuestionDto(
    val vocab_id: Int,
    val word: String,
    val options: List<PlacementOptionDto>
)

@Serializable
data class PlacementStartResponse(
    val session_id: String,
    val current_theta: Double,
    val next_question: PlacementQuestionDto
)

@Serializable
data class AnswerRequestDto(
    val vocab_id: Int,
    val is_correct: Boolean,
    val response_time_ms: Int
)

@Serializable
data class PlacementResultDto(
    val final_level: String,
    val theta: Double,
    val confidence: Double,
    val questions_asked: Int
)

@Serializable
data class PlacementAnswerResponse(
    val status: String, // "continue" or "finished"
    val current_theta: Double,
    val standard_error: Double,
    val estimated_level: String,
    val next_question: PlacementQuestionDto? = null,
    val result: PlacementResultDto? = null
)

interface PlacementApiService {
    @POST("api/v1/placement/start")
    suspend fun startSession(): Response<PlacementStartResponse>

    @POST("api/v1/placement/{session_id}/answer")
    suspend fun submitAnswer(
        @Path("session_id") sessionId: String,
        @Body request: AnswerRequestDto
    ): Response<PlacementAnswerResponse>
}
