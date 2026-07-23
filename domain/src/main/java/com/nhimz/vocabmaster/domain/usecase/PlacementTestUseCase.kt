package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
import javax.inject.Inject

class PlacementTestUseCase @Inject constructor() {
    /**
     * Adaptive placement test logic:
     * - Starts at A2 level (initialized in PlacementTestSession).
     * - Asks 5-8 questions per level (we use exactly 8 questions for precision).
     * - Moves up to the next level if correctness >= 70% (>= 6 out of 8 correct).
     * - Stops on failure (moves to the last successfully completed level, or A1 if A2 was failed) or completion of all levels.
     */
    fun answerQuestion(session: PlacementTestSession, isCorrect: Boolean): PlacementTestSession {
        if (session.isFinished) return session

        val totalQuestionsAsked = session.totalQuestionsAsked + 1
        var consecutiveWrongAnswers = session.consecutiveWrongAnswers
        var currentLevel = session.currentLevel

        if (isCorrect) {
            consecutiveWrongAnswers = 0
            currentLevel = getNextLevel(currentLevel) ?: currentLevel
        } else {
            consecutiveWrongAnswers += 1
            if (consecutiveWrongAnswers >= 2) {
                currentLevel = getPreviousLevel(currentLevel) ?: currentLevel
                consecutiveWrongAnswers = 0
            }
        }

        val isFinished = totalQuestionsAsked >= 15 || 
                (totalQuestionsAsked >= 8 && (
                        (currentLevel == DifficultyLevel.C2 && isCorrect) || 
                        (currentLevel == DifficultyLevel.A1 && !isCorrect)
                ))

        return session.copy(
            currentLevel = currentLevel,
            totalQuestionsAsked = totalQuestionsAsked,
            consecutiveWrongAnswers = consecutiveWrongAnswers,
            isFinished = isFinished,
            resultLevel = if (isFinished) currentLevel else null
        )
    }

    private fun getNextLevel(level: DifficultyLevel): DifficultyLevel? {
        val levels = DifficultyLevel.values()
        val index = level.ordinal
        return if (index < levels.lastIndex) levels[index + 1] else null
    }

    private fun getPreviousLevel(level: DifficultyLevel): DifficultyLevel? {
        val levels = DifficultyLevel.values()
        val index = level.ordinal
        return if (index > 0) levels[index - 1] else null
    }
}
