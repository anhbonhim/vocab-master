package com.nhimz.vocabmaster.domain.model.quiz

import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.domain.model.QuestionWithCard

enum class QuestionDirection {
    EN_TO_VI, VI_TO_EN
}

sealed class QuizType {
    data class Introduction(
        val itemWithCard: QuestionWithCard?,
        val prompt: String,
        val audioUrl: String?
    ) : QuizType()

    data class MultipleChoice(
        val itemWithCard: QuestionWithCard?, // Can be null if testing generic phrase
        val direction: QuestionDirection,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int
    ) : QuizType()

    data class ScrambledSentence(
        val itemWithCard: QuestionWithCard?,
        val scrambledWords: List<String>,
        val correctSentence: String
    ) : QuizType()

    data class Listening(
        val itemWithCard: QuestionWithCard?,
        val prompt: String,
        val audioUrl: String?,
        val audioUrlSlow: String?,
        val options: List<String>?,
        val correctIndex: Int?
    ) : QuizType()

    data class Matching(
        val itemWithCard: QuestionWithCard?,
        val prompt: String,
        val pairs: List<MatchPair>
    ) : QuizType()

    data class Typing(
        val itemWithCard: QuestionWithCard?,
        val prompt: String,
        val correctSentence: String,
        val audioUrl: String?,
        val audioUrlSlow: String?
    ) : QuizType()

    data class FSRSTailFlashcard(
        val itemWithCard: QuestionWithCard
    ) : QuizType()
}

data class QuizQuestion(
    val type: QuizType
) {
    val itemWithCard: QuestionWithCard?
        get() = when (type) {
            is QuizType.Introduction -> type.itemWithCard
            is QuizType.MultipleChoice -> type.itemWithCard
            is QuizType.ScrambledSentence -> type.itemWithCard
            is QuizType.Listening -> type.itemWithCard
            is QuizType.Matching -> type.itemWithCard
            is QuizType.Typing -> type.itemWithCard
            is QuizType.FSRSTailFlashcard -> type.itemWithCard
        }
}
