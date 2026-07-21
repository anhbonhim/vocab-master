package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class QuizCompletionInput(
    val correctCount: Int,
    val totalQuestions: Int,
    val xpGained: Int,
    val nodeId: String? = null,
    val isJumpTest: Boolean = false,
    val isSectionCheckpoint: Boolean = false,
    val isUnitCheckpoint: Boolean = false,
    val nextSectionCefr: String? = null,
    val unitIdForJumpTest: String? = null,
    val unitIdForUnitCheckpoint: String? = null
)

data class QuizCompletionOutcome(
    val accuracy: Float,
    val isPassed: Boolean
)

class CompleteQuizSessionUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val settingsRepository: SettingsRepository,
    private val updateStreakUseCase: UpdateStreakUseCase
) {
    suspend operator fun invoke(input: QuizCompletionInput): Result<QuizCompletionOutcome> = runCatching {
        updateStreakUseCase.execute()

        val accuracy = if (input.totalQuestions > 0) {
            input.correctCount.toFloat() / input.totalQuestions
        } else {
            0f
        }
        var isPassed = false

        if (input.isJumpTest || input.isSectionCheckpoint || input.isUnitCheckpoint) {
            isPassed = accuracy >= 0.8f
            if (isPassed) {
                when {
                    input.isSectionCheckpoint && input.nextSectionCefr != null -> {
                        settingsRepository.setPlacementLevel(input.nextSectionCefr)
                    }
                    input.isJumpTest && input.unitIdForJumpTest != null -> {
                        val nodes = vocabularyRepository.getNodesByUnit(input.unitIdForJumpTest).first()
                        nodes.forEach {
                            vocabularyRepository.markNodeCompleted(it.id, 1.0f, input.xpGained)
                        }
                    }
                    input.isUnitCheckpoint && input.unitIdForUnitCheckpoint != null -> {
                        val nodes = vocabularyRepository.getNodesByUnit(input.unitIdForUnitCheckpoint).first()
                        val unitCheckpointNode = nodes.firstOrNull { it.type == NodeType.UNIT_CHECKPOINT }
                        unitCheckpointNode?.let {
                            vocabularyRepository.markNodeCompleted(it.id, accuracy, input.xpGained)
                        }
                    }
                }
            }
        } else {
            if (input.nodeId != null && accuracy >= 0.7f) {
                vocabularyRepository.markNodeCompleted(input.nodeId, accuracy, input.xpGained)
            }
        }

        Result.success(QuizCompletionOutcome(accuracy = accuracy, isPassed = isPassed))
    }.getOrElse { Result.failure(it) }
}
