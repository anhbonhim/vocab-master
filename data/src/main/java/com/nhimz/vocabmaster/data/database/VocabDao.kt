package com.nhimz.vocabmaster.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {

    // --- Vocabulary Cards ---

    @Query("""
        SELECT * FROM vocabulary_cards 
        WHERE state = :state OR due <= :now 
        ORDER BY state ASC, due ASC 
        LIMIT :limit
    """)
    fun getDueAndNewCards(state: com.nhimz.vocabmaster.domain.fsrs.State, now: java.time.LocalDateTime, limit: Int): Flow<List<VocabularyCardEntity>>

    @Query("SELECT * FROM vocabulary_cards WHERE difficultyLevel = :level")
    fun getCardsByLevel(level: String): Flow<List<VocabularyCardEntity>>

    @Query("SELECT * FROM vocabulary_cards WHERE id = :id")
    suspend fun getCardById(id: Long): VocabularyCardEntity?

    @Query("SELECT * FROM vocabulary_cards")
    suspend fun getAllCards(): List<VocabularyCardEntity>

    @Query("SELECT COUNT(*) FROM vocabulary_cards")
    suspend fun getCardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: VocabularyCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCards(cards: List<VocabularyCardEntity>)

    @Update
    suspend fun updateCard(card: VocabularyCardEntity)

    @Query("DELETE FROM vocabulary_cards")
    suspend fun deleteAllCards()

    // --- Stats Queries ---

    @Query("SELECT COUNT(*) FROM vocabulary_cards WHERE state != 0") // 0 = State.New.value
    fun getLearnedCount(): Flow<Int>

    // Assuming State values: 0=New, 1=Learning, 2=Review, 3=Relearning
    @Query("SELECT state, COUNT(*) as count FROM vocabulary_cards GROUP BY state")
    fun getStateCounts(): Flow<List<StateCount>>

    @Query("SELECT difficultyLevel, COUNT(*) as count FROM vocabulary_cards GROUP BY difficultyLevel")
    fun getLevelCounts(): Flow<List<LevelCount>>


    // --- Review Logs ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReviewLogs(logs: List<ReviewLogEntity>)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY timestamp DESC")
    suspend fun getReviewLogs(cardId: Long): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getReviewLogsFlow(cardId: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY timestamp ASC")
    fun getAllReviewLogsFlow(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY timestamp ASC")
    suspend fun getAllReviewLogsList(): List<ReviewLogEntity>

    @Query("DELETE FROM review_logs")
    suspend fun deleteAllReviewLogs()
}

data class StateCount(val state: Int, val count: Int)
data class LevelCount(val difficultyLevel: String, val count: Int)
