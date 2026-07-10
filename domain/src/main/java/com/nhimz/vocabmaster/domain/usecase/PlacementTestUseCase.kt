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

        val nextQuestionsAsked = session.questionsAskedInCurrentLevel + 1
        val nextCorrectAnswers = session.correctAnswersInCurrentLevel + (if (isCorrect) 1 else 0)

        val questionsPerLevel = 8
        if (nextQuestionsAsked < questionsPerLevel) {
            return session.copy(
                questionsAskedInCurrentLevel = nextQuestionsAsked,
                correctAnswersInCurrentLevel = nextCorrectAnswers
            )
        } else {
            // Level completed. Evaluate if correctness >= 70% (which is 5.6, i.e. >= 6 out of 8)
            val correctness = nextCorrectAnswers.toDouble() / questionsPerLevel
            if (correctness >= 0.70) {
                // Passed current level! Move up
                val nextLevel = getNextLevel(session.currentLevel)
                if (nextLevel != null) {
                    return session.copy(
                        currentLevel = nextLevel,
                        questionsAskedInCurrentLevel = 0,
                        correctAnswersInCurrentLevel = 0,
                        completedLevels = session.completedLevels + session.currentLevel
                    )
                } else {
                    // Passed the highest level (C2). Finish test successfully!
                    return session.copy(
                        isFinished = true,
                        resultLevel = session.currentLevel,
                        completedLevels = session.completedLevels + session.currentLevel
                    )
                }
            } else {
                // Failed current level! Stop the test.
                // The placement result is the previous level (the last one passed), or A1 if they failed A2.
                val resultLevel = getPreviousLevel(session.currentLevel) ?: DifficultyLevel.A1
                return session.copy(
                    isFinished = true,
                    resultLevel = resultLevel
                )
            }
        }
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
