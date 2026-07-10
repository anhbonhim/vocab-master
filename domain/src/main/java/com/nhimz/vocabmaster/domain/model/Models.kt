package com.nhimz.vocabmaster.domain.model

enum class DifficultyLevel {
    A1, A2, B1, B2, C1, C2
}

data class VocabularyItem(
    val id: String,
    val word: String,
    val definition: String,
    val partOfSpeech: String,
    val difficultyLevel: DifficultyLevel,
    val example: String? = null,
    val ipa: String? = null
)

data class PlacementTestSession(
    val currentLevel: DifficultyLevel = DifficultyLevel.A2,
    val questionsAskedInCurrentLevel: Int = 0,
    val correctAnswersInCurrentLevel: Int = 0,
    val completedLevels: List<DifficultyLevel> = emptyList(),
    val isFinished: Boolean = false,
    val resultLevel: DifficultyLevel? = null
)

data class ReviewStats(
    val totalLearned: Int,
    val countByState: Map<com.nhimz.vocabmaster.domain.fsrs.State, Int>,
    val countByLevel: Map<DifficultyLevel, Int>
)
