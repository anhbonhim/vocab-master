package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.Rating
import javax.inject.Inject

class MapRatingUseCase @Inject constructor() {
    /**
     * Maps the user's response time and correctness to an FSRS Rating:
     * - Incorrect -> Again (FSRS Rating: Again)
     * - Correct, responseTime < 3s -> Good or Easy (FSRS Rating: Easy)
     * - Correct, 3s <= responseTime <= 10s -> Good (FSRS Rating: Good)
     * - Correct, responseTime > 10s -> Hard (FSRS Rating: Hard)
     */
    fun execute(isCorrect: Boolean, responseTimeMs: Long): Rating {
        if (!isCorrect) {
            return Rating.Again
        }
        val responseTimeSeconds = responseTimeMs / 1000.0
        return when {
            responseTimeSeconds < 3.0 -> Rating.Easy
            responseTimeSeconds <= 10.0 -> Rating.Good
            else -> Rating.Hard
        }
    }
}
