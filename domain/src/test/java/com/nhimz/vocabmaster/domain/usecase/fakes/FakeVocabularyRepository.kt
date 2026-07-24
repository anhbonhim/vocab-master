package com.nhimz.vocabmaster.domain.usecase.fakes

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.Node
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.Section
import com.nhimz.vocabmaster.domain.model.Session
import com.nhimz.vocabmaster.domain.model.Unit
import com.nhimz.vocabmaster.domain.model.UnitGuidebook
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

open class FakeVocabularyRepository : VocabularyRepository {
    var getSessionsByNodeResult: Result<List<Session>> = Result.success(emptyList())
    var getQuestionsBySessionResult: Result<List<Question>> = Result.success(emptyList())
    var getCardByQuestionIdResult: Card? = null
    var getDueCardsScopedResult: List<QuestionWithCard> = emptyList()
    var getDueCardsResult: Flow<List<QuestionWithCard>> = flowOf(emptyList())
    var getMistakesResult: List<QuestionWithCard> = emptyList()
    var getNodesByUnitResult: Flow<List<Node>> = flowOf(emptyList())
    var getNodesByUnitFailure: Throwable? = null
    var getUnitsBySectionResult: Flow<List<Unit>> = flowOf(emptyList())
    var getUnitsBySectionFailure: Throwable? = null
    var failure: Throwable? = null

    var markNodeCompletedCalls: Int = 0
    var lastMarkNodeCompletedArgs: Triple<String, Float, Int>? = null

    override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> =
        failure?.let { Result.failure(it) } ?: getSessionsByNodeResult

    override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> =
        failure?.let { Result.failure(it) } ?: getQuestionsBySessionResult

    override suspend fun getCardByQuestionId(questionId: String): Card? = getCardByQuestionIdResult

    override suspend fun getDueCardsScoped(
        unitId: String,
        sectionId: String,
        currentTimestamp: Long,
        limit: Int
    ): List<QuestionWithCard> = getDueCardsScopedResult

    override fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> =
        getDueCardsResult

    override suspend fun getMistakes(limit: Int): List<QuestionWithCard> = getMistakesResult

    override fun getNodesByUnit(unitId: String): Flow<List<Node>> =
        getNodesByUnitFailure?.let { throw it } ?: getNodesByUnitResult

    override suspend fun markNodeCompleted(nodeId: String, accuracy: Float, bestScore: Int) {
        markNodeCompletedCalls++
        lastMarkNodeCompletedArgs = Triple(nodeId, accuracy, bestScore)
    }

    override fun getDueCardsByTopic(topic: String, currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
    override fun getCardsByLevel(level: DifficultyLevel): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
    override fun getCardsByTopic(topic: String): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
    override suspend fun getCardById(id: String): QuestionWithCard? = TODO("not needed for these tests")
    override suspend fun updateCard(card: Card) = TODO("not needed for these tests")
    override suspend fun insertAll(items: List<QuestionWithCard>) = TODO("not needed for these tests")
    override suspend fun getCount(): Int = TODO("not needed for these tests")
    override fun getNewCardsByTopicAndLevels(topic: String, levels: List<String>): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
    override fun getNewCardsByLevels(levels: List<String>): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
    override suspend fun getDueCount(now: Long): Int = TODO("not needed for these tests")
    override suspend fun getMistakeCount(): Int = TODO("not needed for these tests")
    override suspend fun getLearnedCountByTopic(topic: String): Int = TODO("not needed for these tests")
    override fun getCompletedLessons(stage: String, unitTopic: String): Flow<List<Int>> =
        TODO("not needed for these tests")
    override suspend fun markLessonCompleted(stage: String, unitTopic: String, lessonIndex: Int) =
        TODO("not needed for these tests")
    override suspend fun getWordCountByTopicAndLevel(topic: String, level: String): Int =
        TODO("not needed for these tests")
    override suspend fun checkAndPrepopulateCurriculum() = TODO("not needed for these tests")
    override fun getSections(): Flow<List<Section>> = TODO("not needed for these tests")
    override fun getUnitsBySection(sectionId: String): Flow<List<Unit>> =
        getUnitsBySectionFailure?.let { throw it } ?: getUnitsBySectionResult
    override suspend fun getGuidebook(unitId: String): Result<UnitGuidebook?> = TODO("not needed for these tests")
    override suspend fun getNodeProgress(nodeId: String): Boolean = TODO("not needed for these tests")
    override suspend fun getCompletedNodesByUnit(unitId: String): List<String> = TODO("not needed for these tests")
    override suspend fun getCompletedNodesBySection(sectionId: String): List<String> = TODO("not needed for these tests")
    override suspend fun getDueCardCountByUnit(unitId: String, currentTimestamp: Long): Int =
        TODO("not needed for these tests")
    override suspend fun getQuestionWithCard(questionId: String): QuestionWithCard? =
        TODO("not needed for these tests")
}
