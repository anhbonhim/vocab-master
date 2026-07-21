package com.nhimz.vocabmaster.data.repository

import androidx.room.withTransaction
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.ReviewLog
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.ReviewStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val database: VocabDatabase,
    private val vocabDao: VocabDao
) : ReviewRepository {

    override suspend fun recordReview(card: Card, log: ReviewLog) = withContext(Dispatchers.IO) {
        database.withTransaction {
            vocabDao.updateFsrsCard(FsrsCardEntity.fromDomain(card))
            vocabDao.insertReviewLog(ReviewLogEntity.fromDomain(log))
        }
        Unit
    }

    override suspend fun insertReviewLog(cardId: String, log: ReviewLog) = withContext(Dispatchers.IO) {
        val entity = ReviewLogEntity.fromDomain(
            log.copy(cardId = cardId)
        )
        vocabDao.insertReviewLog(entity)
        Unit
    }

    override fun getReviewLogs(cardId: String): Flow<List<ReviewLog>> {
        return vocabDao.getReviewLogsFlow(cardId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllReviewLogs(): Flow<List<ReviewLog>> {
        return vocabDao.getAllReviewLogsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getStats(): Flow<ReviewStats> {
        val learnedCountFlow = vocabDao.getLearnedCount()
        val stateCountsFlow = vocabDao.getStateCounts()

        return combine(learnedCountFlow, stateCountsFlow) { learnedCount, stateCounts ->
            val stateMap = stateCounts.associate { it.state to it.count }
            val fullStateMap = State.entries.associateWith { state -> stateMap[state.value] ?: 0 }

            val fullLevelMap = DifficultyLevel.entries.associateWith { 0 }

            ReviewStats(
                totalLearned = learnedCount,
                countByState = fullStateMap,
                countByLevel = fullLevelMap
            )
        }.flowOn(Dispatchers.IO)
    }
}
