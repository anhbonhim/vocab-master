package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.ReviewLog
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    suspend fun insertReviewLog(cardId: Long, log: ReviewLog)
    fun getReviewLogs(cardId: Long): Flow<List<ReviewLog>>
    fun getAllReviewLogs(): Flow<List<ReviewLog>>
    fun getStats(): Flow<ReviewStats>
}
