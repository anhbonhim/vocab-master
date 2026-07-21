package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import javax.inject.Inject

data class AnswerResult(
    val isCorrect: Boolean,
    val xpEarned: Int,
    val rating: Rating?
)

class EvaluateAnswerUseCase @Inject constructor() {
    operator fun invoke(
        type: QuizType,
        optionIndex: Int? = null,
        textAnswer: String? = null,
        selectedWordsForScrambled: List<String>? = null,
        fsrsRating: Rating? = null
    ): Result<AnswerResult> = runCatching {
        when (type) {
            is QuizType.Introduction -> {
                Result.success(AnswerResult(isCorrect = true, xpEarned = 1, rating = null))
            }
            is QuizType.MultipleChoice -> {
                if (optionIndex == null) {
                    Result.failure(IllegalArgumentException("optionIndex must be provided for MultipleChoice"))
                } else {
                    val isCorrect = (optionIndex == type.correctIndex)
                    Result.success(AnswerResult(isCorrect = isCorrect, xpEarned = if (isCorrect) 10 else 2, rating = null))
                }
            }
            is QuizType.Listening -> {
                if (optionIndex == null) {
                    Result.failure(IllegalArgumentException("optionIndex must be provided for Listening"))
                } else {
                    val isCorrect = (optionIndex == type.correctIndex)
                    Result.success(AnswerResult(isCorrect = isCorrect, xpEarned = if (isCorrect) 10 else 2, rating = null))
                }
            }
            is QuizType.ScrambledSentence -> {
                if (selectedWordsForScrambled == null) {
                    Result.failure(IllegalArgumentException("selectedWordsForScrambled must be provided for ScrambledSentence"))
                } else {
                    val userSentence = selectedWordsForScrambled.joinToString(" ")
                    val isCorrect = (userSentence == type.correctSentence)
                    Result.success(AnswerResult(isCorrect = isCorrect, xpEarned = if (isCorrect) 10 else 2, rating = null))
                }
            }
            is QuizType.Typing -> {
                if (textAnswer == null) {
                    Result.failure(IllegalArgumentException("textAnswer must be provided for Typing"))
                } else {
                    val cleanedUser = textAnswer.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                    val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                    val isCorrect = (cleanedUser == cleanedCorrect)
                    Result.success(AnswerResult(isCorrect = isCorrect, xpEarned = if (isCorrect) 10 else 2, rating = null))
                }
            }
            is QuizType.Matching -> {
                Result.success(AnswerResult(isCorrect = true, xpEarned = 15, rating = null))
            }
            is QuizType.FSRSTailFlashcard -> {
                val rating = fsrsRating ?: Rating.Good
                val xpEarned = when (rating) {
                    Rating.Easy -> 15
                    Rating.Good -> 10
                    Rating.Hard -> 5
                    Rating.Again -> 2
                }
                val isCorrect = (rating != Rating.Again)
                Result.success(AnswerResult(isCorrect = isCorrect, xpEarned = xpEarned, rating = rating))
            }
        }
    }.getOrElse { Result.failure(it) }
}
