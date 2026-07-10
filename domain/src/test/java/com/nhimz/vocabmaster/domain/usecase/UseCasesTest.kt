package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {

    @Test
    fun testMapRatingUseCase() {
        val useCase = MapRatingUseCase()

        // Incorrect answer -> Again
        assertEquals(Rating.Again, useCase.execute(isCorrect = false, responseTimeMs = 1500))
        assertEquals(Rating.Again, useCase.execute(isCorrect = false, responseTimeMs = 8000))

        // Correct, < 3s -> Easy
        assertEquals(Rating.Easy, useCase.execute(isCorrect = true, responseTimeMs = 2999))
        assertEquals(Rating.Easy, useCase.execute(isCorrect = true, responseTimeMs = 1500))

        // Correct, 3s <= responseTime <= 10s -> Good
        assertEquals(Rating.Good, useCase.execute(isCorrect = true, responseTimeMs = 3000))
        assertEquals(Rating.Good, useCase.execute(isCorrect = true, responseTimeMs = 5000))
        assertEquals(Rating.Good, useCase.execute(isCorrect = true, responseTimeMs = 10000))

        // Correct, > 10s -> Hard
        assertEquals(Rating.Hard, useCase.execute(isCorrect = true, responseTimeMs = 10001))
        assertEquals(Rating.Hard, useCase.execute(isCorrect = true, responseTimeMs = 15000))
    }

    @Test
    fun testGenerateDistractorsUseCase() {
        val useCase = GenerateDistractorsUseCase()

        val item1 = VocabularyItem("1", "apple", "A round red fruit", "Noun", DifficultyLevel.A1)
        val item2 = VocabularyItem("2", "banana", "A long yellow fruit", "Noun", DifficultyLevel.A1)
        val item3 = VocabularyItem("3", "orange", "A round orange fruit", "Noun", DifficultyLevel.A1)
        val item4 = VocabularyItem("4", "grape", "A small purple fruit", "Noun", DifficultyLevel.A1)
        val item5 = VocabularyItem("5", "run", "To move quickly", "Verb", DifficultyLevel.A2)
        val item6 = VocabularyItem("6", "walk", "To move at a regular pace", "Verb", DifficultyLevel.A1)

        val database = listOf(item1, item2, item3, item4, item5, item6)

        // Generate distractors for apple (Noun, A1)
        val distractors = useCase.execute(item1, database, count = 3)

        // Must return exactly 3 distractors
        assertEquals(3, distractors.size)

        // Must not contain target item
        assertFalse(distractors.any { it.id == "1" })

        // Rule checking: banana (Noun, A1), orange (Noun, A1), grape (Noun, A1) all match POS AND level.
        // They should be selected first.
        assertTrue(distractors.contains(item2))
        assertTrue(distractors.contains(item3))
        assertTrue(distractors.contains(item4))
    }

    @Test
    fun testPlacementTestUseCase_ImmediateFailure() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        assertEquals(DifficultyLevel.A2, session.currentLevel)
        assertFalse(session.isFinished)

        // Fail A2 level immediately (e.g. 5 correct, 3 incorrect out of 8 -> 62.5% < 70%)
        for (i in 1..5) {
            session = useCase.answerQuestion(session, isCorrect = true)
        }
        for (i in 1..3) {
            session = useCase.answerQuestion(session, isCorrect = false)
        }

        assertTrue(session.isFinished)
        assertEquals(DifficultyLevel.A1, session.resultLevel)
    }

    @Test
    fun testPlacementTestUseCase_SuccessAndThenFailure() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        // Pass A2 level (8 correct -> 100% >= 70%)
        for (i in 1..8) {
            session = useCase.answerQuestion(session, isCorrect = true)
        }

        // Assert progressed to B1
        assertEquals(DifficultyLevel.B1, session.currentLevel)
        assertEquals(0, session.questionsAskedInCurrentLevel)
        assertEquals(listOf(DifficultyLevel.A2), session.completedLevels)
        assertFalse(session.isFinished)

        // Fail B1 level (5 correct, 3 incorrect out of 8 -> 62.5% < 70%)
        for (i in 1..5) {
            session = useCase.answerQuestion(session, isCorrect = true)
        }
        for (i in 1..3) {
            session = useCase.answerQuestion(session, isCorrect = false)
        }

        // Assert test ended and placed at A2
        assertTrue(session.isFinished)
        assertEquals(DifficultyLevel.A2, session.resultLevel)
    }

    @Test
    fun testPlacementTestUseCase_FullCompletion() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        // levels: A2, B1, B2, C1, C2 (5 levels total to pass to complete all)
        val levelsToTest = listOf(DifficultyLevel.A2, DifficultyLevel.B1, DifficultyLevel.B2, DifficultyLevel.C1, DifficultyLevel.C2)

        for (level in levelsToTest) {
            assertEquals(level, session.currentLevel)
            assertFalse(session.isFinished)
            // Answer all 8 questions correctly for each level
            for (i in 1..8) {
                session = useCase.answerQuestion(session, isCorrect = true)
            }
        }

        // After passing C2, test should be finished with level C2
        assertTrue(session.isFinished)
        assertEquals(DifficultyLevel.C2, session.resultLevel)
        assertEquals(levelsToTest, session.completedLevels)
    }
}
