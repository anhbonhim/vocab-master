package com.nhimz.vocabmaster.ui.viewmodel

import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion

sealed interface QuizUiState {
    object Loading : QuizUiState

    data class Active(
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val correctAnswersCount: Int,
        val xpGained: Int,
        val selectedOption: Int?,
        val isAnswerRevealed: Boolean,
        val startTimeMillis: Long,
        val isFSRSRatingSelected: Boolean = false,
        val incorrectCardIds: List<String> = emptyList(),
        val nodeId: String? = null,
        val sessionId: String? = null,
        val isSectionCheckpoint: Boolean = false,
        val isJumpTest: Boolean = false,
        val isUnitCheckpoint: Boolean = false,
        val nextSectionCefr: String? = null,
        val unitIdForJumpTest: String? = null,
        val unitIdForUnitCheckpoint: String? = null
    ) : QuizUiState

    data class Completed(
        val xpGained: Int,
        val correctCount: Int,
        val totalCount: Int,
        val durationSeconds: Int,
        val averageStability: Double = 0.0,
        val isPassed: Boolean = false,
        val incorrectCardIds: List<String> = emptyList(),
        val isCheckpointOrJumpTest: Boolean = false
    ) : QuizUiState

    data class Error(val message: String) : QuizUiState
}
