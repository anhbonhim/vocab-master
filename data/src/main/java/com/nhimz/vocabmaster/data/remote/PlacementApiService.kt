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
    val question_id: String,
    val prompt: String,
    val correct_option_id: Int,
    val options: List<PlacementOptionDto>,
    val type: Int = 2 // MULTIPLE_CHOICE
)

@Serializable
data class PlacementStartResponse(
    val session_id: String,
    val current_theta: Double,
    val next_question: PlacementQuestionDto
)

@Serializable
data class AnswerItemDto(
    val question_id: String,
    val is_correct: Boolean,
    val response_time_ms: Int
)

@Serializable
data class AnswerRequestDto(
    val responses: List<AnswerItemDto>?,
    val latest_question_id: String,
    val latest_is_correct: Boolean,
    val latest_response_time_ms: Int
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
