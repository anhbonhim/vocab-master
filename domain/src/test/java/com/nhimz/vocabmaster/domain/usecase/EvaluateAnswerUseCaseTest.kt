package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateAnswerUseCaseTest {
    private val useCase = EvaluateAnswerUseCase()

    private fun qWithCard(type: QuizType): QuestionWithCard? =
        if (type is QuizType.FSRSTailFlashcard) type.itemWithCard else null

    @Test
    fun `Introduction always correct with 1 XP`() {
        val result = useCase(QuizType.Introduction(null, "prompt", null))
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 1, rating = null), result.getOrThrow())
    }

    @Test
    fun `MultipleChoice correct index returns 10 XP`() {
        val result = useCase(
            QuizType.MultipleChoice(null, com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection.EN_TO_VI, "prompt", listOf("a", "b"), 1),
            optionIndex = 1
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 10, rating = null), result.getOrThrow())
    }

    @Test
    fun `MultipleChoice wrong index returns 2 XP`() {
        val result = useCase(
            QuizType.MultipleChoice(null, com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection.EN_TO_VI, "prompt", listOf("a", "b"), 1),
            optionIndex = 0
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = false, xpEarned = 2, rating = null), result.getOrThrow())
    }

    @Test
    fun `Listening correct index returns 10 XP`() {
        val result = useCase(
            QuizType.Listening(null, "prompt", null, null, listOf("a", "b"), 0),
            optionIndex = 0
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 10, rating = null), result.getOrThrow())
    }

    @Test
    fun `Listening wrong index returns 2 XP`() {
        val result = useCase(
            QuizType.Listening(null, "prompt", null, null, listOf("a", "b"), 0),
            optionIndex = 1
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = false, xpEarned = 2, rating = null), result.getOrThrow())
    }

    @Test
    fun `ScrambledSentence joined correctly returns 10 XP`() {
        val result = useCase(
            QuizType.ScrambledSentence(null, listOf("hello", "world"), "hello world"),
            selectedWordsForScrambled = listOf("hello", "world")
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 10, rating = null), result.getOrThrow())
    }

    @Test
    fun `ScrambledSentence wrong order returns 2 XP`() {
        val result = useCase(
            QuizType.ScrambledSentence(null, listOf("hello", "world"), "hello world"),
            selectedWordsForScrambled = listOf("world", "hello")
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = false, xpEarned = 2, rating = null), result.getOrThrow())
    }

    @Test
    fun `Typing ignores case punctuation and spaces`() {
        val result = useCase(
            QuizType.Typing(null, "prompt", "Hello, World!", null, null),
            textAnswer = "hello world"
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 10, rating = null), result.getOrThrow())
    }

    @Test
    fun `Typing wrong answer returns 2 XP`() {
        val result = useCase(
            QuizType.Typing(null, "prompt", "Hello, World!", null, null),
            textAnswer = "goodbye world"
        )
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = false, xpEarned = 2, rating = null), result.getOrThrow())
    }

    @Test
    fun `Matching always correct with 15 XP`() {
        val result = useCase(QuizType.Matching(null, "prompt", listOf(MatchPair("a", "b"))))
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 15, rating = null), result.getOrThrow())
    }

    @Test
    fun `FSRSTailFlashcard Easy returns 15 XP and correct`() {
        val item = QuestionWithCard(
            Question(id = "q1", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", options = null, correctIndex = null, correctSentence = "c", scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q1")
        )
        val result = useCase(QuizType.FSRSTailFlashcard(item), fsrsRating = Rating.Easy)
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 15, rating = Rating.Easy), result.getOrThrow())
    }

    @Test
    fun `FSRSTailFlashcard Good returns 10 XP and correct`() {
        val item = QuestionWithCard(
            Question(id = "q2", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", options = null, correctIndex = null, correctSentence = "c", scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q2")
        )
        val result = useCase(QuizType.FSRSTailFlashcard(item), fsrsRating = Rating.Good)
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 10, rating = Rating.Good), result.getOrThrow())
    }

    @Test
    fun `FSRSTailFlashcard Hard returns 5 XP and correct`() {
        val item = QuestionWithCard(
            Question(id = "q3", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", options = null, correctIndex = null, correctSentence = "c", scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q3")
        )
        val result = useCase(QuizType.FSRSTailFlashcard(item), fsrsRating = Rating.Hard)
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 5, rating = Rating.Hard), result.getOrThrow())
    }

    @Test
    fun `FSRSTailFlashcard Again returns 2 XP and incorrect`() {
        val item = QuestionWithCard(
            Question(id = "q4", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", options = null, correctIndex = null, correctSentence = "c", scrambledWords = null, translation = "t", audioUrl = null, audioUrlSlow = null, matchingPairs = null, imagePath = null),
            Card(cardId = "q4")
        )
        val result = useCase(QuizType.FSRSTailFlashcard(item), fsrsRating = Rating.Again)
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = false, xpEarned = 2, rating = Rating.Again), result.getOrThrow())
    }

    @Test
    fun `MultipleChoice missing optionIndex returns failure`() {
        val result = useCase(QuizType.MultipleChoice(null, com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection.EN_TO_VI, "prompt", listOf("a", "b"), 0))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `Listening missing optionIndex returns failure`() {
        val result = useCase(QuizType.Listening(null, "prompt", null, null, listOf("a", "b"), 0))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `ScrambledSentence missing selectedWords returns failure`() {
        val result = useCase(QuizType.ScrambledSentence(null, listOf("hello"), "hello"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `Typing missing textAnswer returns failure`() {
        val result = useCase(QuizType.Typing(null, "prompt", "hello", null, null))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
