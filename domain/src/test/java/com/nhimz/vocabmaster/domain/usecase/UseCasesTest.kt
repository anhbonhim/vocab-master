package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
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
    fun testPlacementTestUseCase_ImmediateFailure() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        assertEquals(DifficultyLevel.A2, session.currentLevel)
        assertFalse(session.isFinished)

        // Drop to A1 (2 questions wrong)
        session = useCase.answerQuestion(session, isCorrect = false)
        session = useCase.answerQuestion(session, isCorrect = false)
        
        assertEquals(DifficultyLevel.A1, session.currentLevel)

        // Then get two wrong again
        session = useCase.answerQuestion(session, isCorrect = false)
        session = useCase.answerQuestion(session, isCorrect = false)
        
        // Then get two wrong again
        session = useCase.answerQuestion(session, isCorrect = false)
        session = useCase.answerQuestion(session, isCorrect = false)
        
        // 7
        session = useCase.answerQuestion(session, isCorrect = false)
        
        // 8 -> finishes
        session = useCase.answerQuestion(session, isCorrect = false)

        assertTrue(session.isFinished)
        assertEquals(DifficultyLevel.A1, session.resultLevel)
    }

    @Test
    fun testPlacementTestUseCase_SuccessAndThenFailure() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        // 1st correct -> A2 -> B1
        session = useCase.answerQuestion(session, isCorrect = true)
        assertEquals(DifficultyLevel.B1, session.currentLevel)

        // 2nd incorrect, 3rd incorrect -> B1 -> drops to A2
        session = useCase.answerQuestion(session, isCorrect = false)
        session = useCase.answerQuestion(session, isCorrect = false)
        assertEquals(DifficultyLevel.A2, session.currentLevel)
        
        // Let's answer enough questions correctly to trigger 15 limit without failing
        for (i in 1..12) {
            session = useCase.answerQuestion(session, isCorrect = true)
        }
        
        assertTrue(session.isFinished)
    }

    @Test
    fun testPlacementTestUseCase_FullCompletion() {
        val useCase = PlacementTestUseCase()
        var session = PlacementTestSession()

        // Just answering correct consecutively 8 times should bump level up to C2 and then finish
        for (i in 1..8) {
            session = useCase.answerQuestion(session, isCorrect = true)
        }

        assertTrue(session.isFinished)
        assertEquals(DifficultyLevel.C2, session.resultLevel)
    }
}
