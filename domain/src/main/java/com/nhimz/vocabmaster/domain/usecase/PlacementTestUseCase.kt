package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.LocalPlacementItem
import com.nhimz.vocabmaster.domain.model.PlacementResponse
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.placement.IrtEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Offline placement test engine.
 *
 * Replaces the old backend-driven placement flow: questions are sourced
 * locally from the on-device vocabulary bank (one MCQ per word, with 3
 * distractor translations), and the learner's ability is estimated with a
 * 2PL IRT model ([IrtEngine]) — no network call is ever made.
 *
 * The test is adaptive: after each answer we re-estimate theta and pick the
 * next unasked item whose difficulty (b) is closest to the current theta.
 * It finishes after 15 items or once the standard error drops below 0.3.
 */
class PlacementTestUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {

    suspend fun startSession(): PlacementTestSession {
        val bank = buildBank()
        return if (bank.isEmpty()) {
            PlacementTestSession(isFinished = true, resultLevel = "A2")
        } else {
            val firstIndex = selectNextQuestion(bank, emptyList(), 0.0)
            PlacementTestSession(
                questionBank = bank,
                currentQuestionIndex = firstIndex,
                estimatedLevel = IrtEngine.mapThetaToCefr(0.0)
            )
        }
    }

    fun submitAnswer(
        session: PlacementTestSession,
        selectedIndex: Int
    ): PlacementTestSession {
        val item = session.questionBank.getOrNull(session.currentQuestionIndex) ?: return session
        val isCorrect = selectedIndex == item.correctOptionId

        val responses = session.responses +
            PlacementResponse(isCorrect = isCorrect, a = item.a, b = item.b)
        val askedIndices = session.askedIndices + session.currentQuestionIndex

        val estimate = IrtEngine.estimateTheta(responses, session.theta)
        val total = session.totalQuestionsAsked + 1
        val finished = total >= MAX_QUESTIONS || estimate.standardError < TARGET_SE

        val nextIndex = if (finished) {
            -1
        } else {
            selectNextQuestion(session.questionBank, askedIndices, estimate.theta)
        }

        return session.copy(
            theta = estimate.theta,
            standardError = estimate.standardError,
            totalQuestionsAsked = total,
            responses = responses,
            askedIndices = askedIndices,
            estimatedLevel = IrtEngine.mapThetaToCefr(estimate.theta),
            isFinished = finished,
            resultLevel = if (finished) IrtEngine.mapThetaToCefr(estimate.theta) else null,
            currentQuestionIndex = nextIndex
        )
    }

    private fun selectNextQuestion(
        bank: List<LocalPlacementItem>,
        askedIndices: List<Int>,
        theta: Double
    ): Int {
        var bestIndex = -1
        var bestDistance = Double.MAX_VALUE
        bank.forEachIndexed { index, item ->
            if (index in askedIndices) return@forEachIndexed
            val distance = kotlin.math.abs(item.b - theta)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private suspend fun buildBank(): List<LocalPlacementItem> {
        val levels = DifficultyLevel.values()
        val perLevel = 8
        val allCards = mutableListOf<QuestionWithCard>()
        val byLevel = mutableListOf<List<QuestionWithCard>>()

        for (level in levels) {
            val cards = vocabularyRepository.getCardsByLevel(level).first()
                .takeIf { it.isNotEmpty() } ?: continue
            byLevel.add(cards)
            allCards.addAll(cards)
        }

        if (allCards.isEmpty()) return emptyList()

        val pool = allCards.mapNotNull { it.question.translation }
            .filter { it.isNotBlank() }
            .distinct()

        val items = mutableListOf<LocalPlacementItem>()
        for ((levelIndex, cards) in byLevel.withIndex()) {
            val b = IrtEngine.cefrToTheta(levels[levelIndex].name)
            for (card in cards.take(perLevel)) {
                val correct = card.question.translation ?: continue
                if (correct.isBlank()) continue
                val distractors = pool.filter { it != correct }
                    .shuffled()
                    .take(3)
                if (distractors.size < 3) continue
                val options = (listOf(correct) + distractors).shuffled()
                val correctOptionId = options.indexOf(correct)
                items.add(
                    LocalPlacementItem(
                        questionId = card.question.id,
                        prompt = card.question.word ?: correct,
                        options = options,
                        correctOptionId = correctOptionId,
                        a = 1.0,
                        b = b
                    )
                )
            }
        }
        return items.shuffled()
    }

    companion object {
        const val MAX_QUESTIONS = 15
        const val TARGET_SE = 0.3
    }
}
