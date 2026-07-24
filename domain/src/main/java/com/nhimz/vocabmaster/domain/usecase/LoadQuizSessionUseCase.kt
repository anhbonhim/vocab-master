package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed class QuizSessionRequest {
    data class NodeSession(val nodeId: String, val sessionIndex: Int) : QuizSessionRequest()
    data class ReviewNode(val nodeId: String, val unitId: String?, val sectionId: String?) : QuizSessionRequest()
    data class UnitCheckpoint(val unitId: String) : QuizSessionRequest()
    data class JumpTest(val unitId: String) : QuizSessionRequest()
    data class SectionCheckpoint(val sectionId: String, val nextSectionCefr: String?) : QuizSessionRequest()
    data class MistakeReview(val cardIds: List<String>?) : QuizSessionRequest()
}

data class QuizSessionData(
    val questions: List<QuizQuestion>,
    val nodeId: String? = null,
    val sessionId: String? = null,
    val isSectionCheckpoint: Boolean = false,
    val isJumpTest: Boolean = false,
    val isUnitCheckpoint: Boolean = false,
    val nextSectionCefr: String? = null,
    val unitIdForJumpTest: String? = null,
    val unitIdForUnitCheckpoint: String? = null
)

class LoadQuizSessionUseCase @Inject constructor(
    private val vocabularyRepository: VocabularyRepository
) {
    suspend operator fun invoke(request: QuizSessionRequest): Result<QuizSessionData> = runCatching {
        when (request) {
            is QuizSessionRequest.NodeSession -> loadNodeSession(request)
            is QuizSessionRequest.ReviewNode -> loadReviewNode(request)
            is QuizSessionRequest.UnitCheckpoint -> loadUnitCheckpoint(request)
            is QuizSessionRequest.JumpTest -> loadJumpTest(request)
            is QuizSessionRequest.SectionCheckpoint -> loadSectionCheckpoint(request)
            is QuizSessionRequest.MistakeReview -> loadMistakeReview(request)
        }
    }.getOrElse { Result.failure(it) }

    private suspend fun loadNodeSession(request: QuizSessionRequest.NodeSession): Result<QuizSessionData> {
        val sessions = vocabularyRepository.getSessionsByNode(request.nodeId).getOrElse { error ->
            return Result.failure(error)
        }
        val sessionToRun = sessions.getOrNull(request.sessionIndex)
            ?: return Result.success(QuizSessionData(emptyList()))

        val rawQuestions = vocabularyRepository.getQuestionsBySession(sessionToRun.id).getOrElse { error ->
            return Result.failure(error)
        }

        val questionsList = rawQuestions.map { q ->
            val itemWithCard = vocabularyRepository.getCardByQuestionId(q.id)?.let { QuestionWithCard(q, it) }
            mapToQuizQuestion(q, itemWithCard)
        }

        return if (questionsList.isEmpty()) {
            Result.success(QuizSessionData(emptyList()))
        } else {
            Result.success(
                QuizSessionData(
                    questions = questionsList,
                    nodeId = request.nodeId,
                    sessionId = sessionToRun.id
                )
            )
        }
    }

    private suspend fun loadReviewNode(request: QuizSessionRequest.ReviewNode): Result<QuizSessionData> {
        val now = System.currentTimeMillis()
        val dueCards = if (request.unitId != null && request.sectionId != null) {
            vocabularyRepository.getDueCardsScoped(request.unitId, request.sectionId, now, 15)
        } else {
            kotlin.runCatching {
                vocabularyRepository.getDueCards(now, 15).let { flow ->
                    // Flow.first() can throw; wrap it
                    flow.let { it }
                }
            }.getOrElse { return Result.failure(it) }
                .let { flow ->
                    kotlin.runCatching { flow.first() }.getOrElse { return Result.failure(it) }
                }
        }

        return if (dueCards.isEmpty()) {
            Result.success(QuizSessionData(emptyList()))
        } else {
            Result.success(
                QuizSessionData(
                    questions = dueCards.map { QuizQuestion(QuizType.FSRSTailFlashcard(it)) },
                    nodeId = request.nodeId
                )
            )
        }
    }

    private suspend fun loadUnitCheckpoint(request: QuizSessionRequest.UnitCheckpoint): Result<QuizSessionData> {
        val nodes = kotlin.runCatching {
            vocabularyRepository.getNodesByUnit(request.unitId).first()
        }.getOrElse { return Result.failure(it) }

        val quizNodeTypes = setOf(NodeType.LESSON, NodeType.REVIEW)
        val quizNodes = nodes.filter { it.type in quizNodeTypes }

        val questionsList = mutableListOf<QuizQuestion>()
        for (node in quizNodes) {
            val sessions = vocabularyRepository.getSessionsByNode(node.id).getOrElse { error ->
                return Result.failure(error)
            }
            for (session in sessions) {
                val rawQuestions = vocabularyRepository.getQuestionsBySession(session.id).getOrElse { error ->
                    return Result.failure(error)
                }
                for (q in rawQuestions) {
                    val itemWithCard = vocabularyRepository.getCardByQuestionId(q.id)?.let { QuestionWithCard(q, it) }
                    questionsList.add(mapToQuizQuestion(q, itemWithCard))
                }
            }
        }

        val capped = questionsList.shuffled().take(16)
        return if (capped.isEmpty()) {
            Result.success(QuizSessionData(emptyList()))
        } else {
            Result.success(
                QuizSessionData(
                    questions = capped,
                    isUnitCheckpoint = true,
                    unitIdForUnitCheckpoint = request.unitId
                )
            )
        }
    }

    private fun loadJumpTest(request: QuizSessionRequest.JumpTest): Result<QuizSessionData> {
        return Result.success(
            QuizSessionData(
                questions = emptyList(),
                isJumpTest = true,
                unitIdForJumpTest = request.unitId
            )
        )
    }

    private fun loadSectionCheckpoint(request: QuizSessionRequest.SectionCheckpoint): Result<QuizSessionData> {
        return Result.success(
            QuizSessionData(
                questions = emptyList(),
                isSectionCheckpoint = true,
                nextSectionCefr = request.nextSectionCefr
            )
        )
    }

    private suspend fun loadMistakeReview(request: QuizSessionRequest.MistakeReview): Result<QuizSessionData> {
        val mistakeCards = kotlin.runCatching {
            vocabularyRepository.getMistakes(20)
        }.getOrElse { return Result.failure(it) }

        if (mistakeCards.isEmpty()) {
            return Result.success(QuizSessionData(emptyList()))
        }

        val questionsList = mutableListOf<QuizQuestion>()
        for (cardItem in mistakeCards) {
            val q = cardItem.question
            val title = q.word ?: q.prompt
            val definition = q.translation ?: ""

            questionsList.add(
                QuizQuestion(
                    QuizType.Introduction(
                        itemWithCard = cardItem,
                        prompt = "$title: $definition",
                        audioUrl = q.audioUrl
                    )
                )
            )
            questionsList.add(
                QuizQuestion(
                    QuizType.Typing(
                        itemWithCard = cardItem,
                        prompt = "Type the word: $definition",
                        correctSentence = q.word ?: title,
                        audioUrl = q.audioUrl,
                        audioUrlSlow = null
                    )
                )
            )
        }

        return Result.success(QuizSessionData(questions = questionsList))
    }

    private fun mapToQuizQuestion(q: Question, itemWithCard: QuestionWithCard?): QuizQuestion {
        val quizType = when (q.type) {
            QuestionType.INTRODUCTION -> {
                QuizType.Introduction(itemWithCard, q.prompt, q.audioUrl)
            }
            QuestionType.FILL_IN_BLANK -> {
                QuizType.FillInBlank(
                    itemWithCard = itemWithCard,
                    prompt = q.prompt,
                    options = q.options ?: emptyList(),
                    correctIndex = q.correctIndex ?: 0,
                    word = q.word
                )
            }
            QuestionType.MULTIPLE_CHOICE -> {
                QuizType.MultipleChoice(
                    itemWithCard = itemWithCard,
                    direction = QuestionDirection.EN_TO_VI,
                    prompt = q.prompt,
                    options = q.options ?: emptyList(),
                    correctIndex = q.correctIndex ?: 0
                )
            }
            QuestionType.SCRAMBLED -> {
                QuizType.ScrambledSentence(
                    itemWithCard = itemWithCard,
                    scrambledWords = q.scrambledWords ?: emptyList(),
                    correctSentence = q.correctSentence ?: ""
                )
            }
            QuestionType.LISTENING -> {
                QuizType.Listening(
                    itemWithCard = itemWithCard,
                    prompt = q.prompt,
                    audioUrl = q.audioUrl,
                    audioUrlSlow = q.audioUrlSlow,
                    options = q.options,
                    correctIndex = q.correctIndex
                )
            }
            QuestionType.MATCHING -> {
                QuizType.Matching(
                    itemWithCard = itemWithCard,
                    prompt = q.prompt,
                    pairs = q.matchingPairs ?: emptyList()
                )
            }
            QuestionType.TYPING -> {
                QuizType.Typing(
                    itemWithCard = itemWithCard,
                    prompt = q.prompt,
                    correctSentence = q.correctSentence ?: "",
                    audioUrl = q.audioUrl,
                    audioUrlSlow = q.audioUrlSlow
                )
            }
        }
        return QuizQuestion(quizType)
    }
}
