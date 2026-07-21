package com.nhimz.vocabmaster.domain.usecase.fakes

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.ReviewLog
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.ReviewStats
import kotlinx.coroutines.flow.Flow

class FakeReviewRepository : ReviewRepository {
    var recordReviewCalls: Int = 0
    var lastRecordedCard: Card? = null
    var lastRecordedLog: ReviewLog? = null
    var recordReviewFailure: Throwable? = null

    override suspend fun recordReview(card: Card, log: ReviewLog) {
        recordReviewFailure?.let { throw it }
        recordReviewCalls++
        lastRecordedCard = card
        lastRecordedLog = log
    }

    override suspend fun insertReviewLog(cardId: String, log: ReviewLog) =
        TODO("not needed for these tests")
    override fun getReviewLogs(cardId: String): Flow<List<ReviewLog>> =
        TODO("not needed for these tests")
    override fun getAllReviewLogs(): Flow<List<ReviewLog>> =
        TODO("not needed for these tests")
    override fun getStats(): Flow<ReviewStats> = TODO("not needed for these tests")
}
