package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>>
    fun getDueCardsByTopic(topic: String, currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>>
    fun getCardsByLevel(level: DifficultyLevel): Flow<List<QuestionWithCard>>
    fun getCardsByTopic(topic: String): Flow<List<QuestionWithCard>>
    suspend fun getCardById(id: String): QuestionWithCard?
    suspend fun updateCard(card: Card)

    suspend fun insertAll(items: List<QuestionWithCard>)
    suspend fun getCount(): Int

    fun getNewCardsByTopicAndLevels(topic: String, levels: List<String>): Flow<List<QuestionWithCard>>
    fun getNewCardsByLevels(levels: List<String>): Flow<List<QuestionWithCard>>

    suspend fun getDueCount(now: Long): Int
    suspend fun getMistakeCount(): Int
    suspend fun getMistakes(limit: Int): List<QuestionWithCard>
    suspend fun getLearnedCountByTopic(topic: String): Int

    fun getCompletedLessons(stage: String, unitTopic: String): Flow<List<Int>>
    suspend fun markLessonCompleted(stage: String, unitTopic: String, lessonIndex: Int)
    suspend fun getWordCountByTopicAndLevel(topic: String, level: String): Int

    suspend fun checkAndPrepopulateCurriculum()
    fun getSections(): Flow<List<Section>>
    fun getUnitsBySection(sectionId: String): Flow<List<Unit>>
    suspend fun getGuidebook(unitId: String): UnitGuidebook?
    fun getNodesByUnit(unitId: String): Flow<List<Node>>
    suspend fun getSessionsByNode(nodeId: String): List<Session>
    suspend fun getQuestionsBySession(sessionId: String): List<Question>
    
    suspend fun getNodeProgress(nodeId: String): Boolean
    suspend fun getCompletedNodesByUnit(unitId: String): List<String>
    suspend fun getCompletedNodesBySection(sectionId: String): List<String>
    suspend fun markNodeCompleted(nodeId: String, accuracy: Float, bestScore: Int)

    suspend fun getCardByQuestionId(questionId: String): Card?
    suspend fun getQuestionWithCard(questionId: String): QuestionWithCard?

    // Scoped due cards for REVIEW nodes on the Duolingo-style learning path.
    // Returns cards due or new, scoped to unit first, then section, then global
    // (3-tier fallback) so the user always gets a full review session when cards exist.
    suspend fun getDueCardsScoped(
        unitId: String,
        sectionId: String,
        currentTimestamp: Long,
        limit: Int
    ): List<QuestionWithCard>

    suspend fun getDueCardCountByUnit(unitId: String, currentTimestamp: Long): Int
}
