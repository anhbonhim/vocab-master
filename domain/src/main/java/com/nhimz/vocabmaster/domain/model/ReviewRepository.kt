package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.ReviewLog
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    suspend fun recordReview(card: Card, log: ReviewLog)
    suspend fun insertReviewLog(cardId: String, log: ReviewLog)
    fun getReviewLogs(cardId: String): Flow<List<ReviewLog>>
    fun getAllReviewLogs(): Flow<List<ReviewLog>>
    fun getStats(): Flow<ReviewStats>
}
