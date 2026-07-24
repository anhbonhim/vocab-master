package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.Scheduler
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SubmitReviewUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val mapRatingUseCase: MapRatingUseCase
) {
    suspend operator fun invoke(
        question: QuizQuestion,
        isCorrect: Boolean,
        responseTimeMs: Long,
        xpEarned: Int,
        explicitRating: Rating? = null
    ): Result<Card?> = runCatching {
        val cardItem = question.itemWithCard
        if (cardItem == null || question.type is QuizType.Introduction) {
            settingsRepository.addXp(xpEarned)
            return Result.success(null)
        }

        val rating = if (question.type is QuizType.FSRSTailFlashcard) {
            explicitRating ?: Rating.Good
        } else {
            mapRatingUseCase.execute(isCorrect, responseTimeMs)
        }

        val desiredRetention = settingsRepository.desiredRetention.first()
        val scheduler = Scheduler(desiredRetention = desiredRetention)
        val now = System.currentTimeMillis()
        val (updatedCard, log) = scheduler.reviewCard(
            cardItem.card,
            rating,
            now,
            reviewDurationMillis = responseTimeMs
        )

        reviewRepository.recordReview(updatedCard, log)
        settingsRepository.addXp(xpEarned)
        Result.success(updatedCard)
    }.getOrElse { Result.failure(it) }
}
