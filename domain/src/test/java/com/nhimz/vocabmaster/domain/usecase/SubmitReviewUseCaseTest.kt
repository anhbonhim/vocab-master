package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.domain.usecase.fakes.FakeReviewRepository
import com.nhimz.vocabmaster.domain.usecase.fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitReviewUseCaseTest {
    private val reviewRepository = FakeReviewRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val mapRatingUseCase = MapRatingUseCase()
    private val useCase = SubmitReviewUseCase(reviewRepository, settingsRepository, mapRatingUseCase)

    private fun flashcardQuestion(rating: Rating? = null): QuizType.FSRSTailFlashcard {
        val item = QuestionWithCard(
            Question(id = "q1", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", options = null, correctIndex = null, correctSentence = "c", scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q1", state = State.New)
        )
        return QuizType.FSRSTailFlashcard(item)
    }

    @Test
    fun `flashcard happy path schedules card and awards XP`() = runTest {
        val question = QuizQuestion(flashcardQuestion(Rating.Good))

        val result = useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10, explicitRating = Rating.Good)

        assertTrue(result.isSuccess)
        assertEquals(1, reviewRepository.recordReviewCalls)
        assertEquals(10, settingsRepository.lastAddedXp)
        assertEquals(Rating.Good, reviewRepository.lastRecordedLog?.rating)
        assertEquals("q1", reviewRepository.lastRecordedCard?.cardId)
    }

    @Test
    fun `explicit FSRS rating wins over MapRatingUseCase`() = runTest {
        val question = QuizQuestion(flashcardQuestion())

        useCase(question, isCorrect = false, responseTimeMs = 2000, xpEarned = 5, explicitRating = Rating.Easy)

        assertEquals(Rating.Easy, reviewRepository.lastRecordedLog?.rating)
        assertEquals(5, settingsRepository.lastAddedXp)
    }

    @Test
    fun `non-flashcard type resolves rating via MapRatingUseCase`() = runTest {
        val item = QuestionWithCard(
            Question(id = "q2", sessionId = "s1", word = "w", type = QuestionType.MULTIPLE_CHOICE, prompt = "p", options = listOf("a", "b"), correctIndex = 0, correctSentence = null, scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q2", state = State.New)
        )
        val question = QuizQuestion(QuizType.MultipleChoice(item, com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection.EN_TO_VI, "prompt", listOf("a", "b"), 0))

        useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10)

        // Correct with <3s -> Easy per MapRatingUseCase
        assertEquals(Rating.Easy, reviewRepository.lastRecordedLog?.rating)
        assertEquals(1, reviewRepository.recordReviewCalls)
        assertEquals(10, settingsRepository.lastAddedXp)
    }

    @Test
    fun `question without card skips scheduling but still awards XP`() = runTest {
        val question = QuizQuestion(QuizType.Introduction(null, "prompt", null))

        val result = useCase(question, isCorrect = true, responseTimeMs = 1000, xpEarned = 1)

        assertTrue(result.isSuccess)
        assertEquals(0, reviewRepository.recordReviewCalls)
        assertEquals(1, settingsRepository.lastAddedXp)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `recordReview failure returns failure and does not award XP`() = runTest {
        val question = QuizQuestion(flashcardQuestion(Rating.Good))
        reviewRepository.recordReviewFailure = IllegalStateException("db error")

        val result = useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(0, settingsRepository.addXpCalls)
    }
}
