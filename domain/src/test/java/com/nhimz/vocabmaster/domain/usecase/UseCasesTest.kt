package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import org.junit.Assert.assertEquals
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
}
