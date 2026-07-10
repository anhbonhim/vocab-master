package com.nhimz.vocabmaster.data.repository

import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.domain.fsrs.ReviewLog
import com.nhimz.vocabmaster.domain.fsrs.State
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
    private val vocabDao: VocabDao
) : ReviewRepository {

    override suspend fun insertReviewLog(cardId: Long, log: ReviewLog) = withContext(Dispatchers.IO) {
        val entity = ReviewLogEntity.fromDomain(cardId, log)
        vocabDao.insertReviewLog(entity)
        Unit
    }

    override fun getReviewLogs(cardId: Long): Flow<List<ReviewLog>> {
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
        val levelCountsFlow = vocabDao.getLevelCounts()

        return combine(learnedCountFlow, stateCountsFlow, levelCountsFlow) { learnedCount, stateCounts, levelCounts ->
            val stateMap = stateCounts.associate { it.state to it.count }
            val fullStateMap = State.entries.associateWith { state -> stateMap[state.value] ?: 0 }

            val levelMap = levelCounts.associate { 
                try {
                    DifficultyLevel.valueOf(it.difficultyLevel) to it.count
                } catch (e: Exception) {
                    DifficultyLevel.A1 to 0
                }
            }
            val fullLevelMap = DifficultyLevel.values().associateWith { level -> levelMap[level] ?: 0 }

            ReviewStats(
                totalLearned = learnedCount,
                countByState = fullStateMap,
                countByLevel = fullLevelMap
            )
        }.flowOn(Dispatchers.IO)
    }
}
