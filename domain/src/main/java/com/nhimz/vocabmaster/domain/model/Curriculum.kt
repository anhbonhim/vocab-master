package com.nhimz.vocabmaster.domain.model

enum class DifficultyLevel {
    A1, A2, B1, B2, C1, C2
}

data class PlacementTestSession(
    val currentLevel: DifficultyLevel = DifficultyLevel.A2,
    val totalQuestionsAsked: Int = 0,
    val consecutiveWrongAnswers: Int = 0,
    val isFinished: Boolean = false,
    val resultLevel: DifficultyLevel? = null
)

data class ReviewStats(
    val totalLearned: Int,
    val countByState: Map<com.nhimz.vocabmaster.domain.fsrs.v6.State, Int>,
    val countByLevel: Map<DifficultyLevel, Int>
)

data class Section(
    val id: String,
    val index: Int,
    val name: String,
    val cefrSublevel: String,
    val icon: String,
    val description: String
)

data class Unit(
    val id: String,
    val sectionId: String,
    val index: Int,
    val topic: String,
    val title: String,
    val storySummary: String,
    val icon: String,
    val guidebookId: String
)

data class UnitGuidebook(
    val id: String,
    val unitId: String,
    val grammarTips: List<String>,
    val keyPhrases: List<KeyPhrase>,
    val storyIntro: String,
    val illustrationSvg: String?
)

data class KeyPhrase(
    val phrase: String,
    val translation: String,
    val note: String?
)

data class Node(
    val id: String,
    val unitId: String,
    val index: Int,
    val type: NodeType,
    val title: String,
    val scenarioContext: String,
    val icon: String
)

enum class NodeType {
    LESSON,
    REVIEW,
    UNIT_CHECKPOINT,
    SECTION_CHECKPOINT,
    JUMP_TEST,
    GUIDEBOOK
}

data class Session(
    val id: String,
    val nodeId: String,
    val index: Int,
    val title: String,
    val durationMinutes: Int,
    val questionIds: List<String>
)

data class Question(
    val id: String,
    val sessionId: String,
    val word: String?,
    val type: QuestionType,
    val prompt: String,
    val options: List<String>?,
    val correctIndex: Int?,
    val correctSentence: String?,
    val scrambledWords: List<String>?,
    val translation: String?,
    val audioUrl: String?,
    val audioUrlSlow: String?,
    val matchingPairs: List<MatchPair>?,
    val imagePath: String?
)

data class MatchPair(
    val left: String,
    val right: String
)

enum class QuestionType {
    INTRODUCTION,
    FILL_IN_BLANK,
    MULTIPLE_CHOICE,
    SCRAMBLED,
    LISTENING,
    MATCHING,
    TYPING
}
