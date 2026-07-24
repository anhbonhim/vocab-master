package com.nhimz.vocabmaster.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Per-user data DAO.
 *
 * Every query here touches only user-data tables (fsrs_cards, review_logs, node_progress,
 * session_progress, flagged_items) that live in [UserDataDatabase]. The cross-DB JOINs that the
 * old [androidx.room.Dao] performed against curriculum tables have been stripped: each such query
 * is now a single-table query on the user-data side, and
 * [com.nhimz.vocabmaster.data.repository.VocabularyRepositoryImpl] assembles the final result in
 * memory after fetching the curriculum side from [CurriculumDao].
 *
 * The cross-DB helper queries ([getCardsByQuestionIds], [getDueAndNewCardsByQuestionIds],
 * [getCompletedNodeProgressByNodeIds], [countDueCardsByQuestionIds]) filter the user-data tables
 * by the ID lists supplied by [CurriculumDao] (e.g. node IDs for a unit), so the repository can
 * perform the join in memory:
 *   - `getDueAndNewCards`      -> `getDueAndNewCardEntities()` + `CurriculumDao.getQuestionsByIds`
 *   - `getDueAndNewCardsByUnit`/`Section` -> `CurriculumDao.getQuestionIdsByUnit/Section` +
 *       `getDueAndNewCardsByQuestionIds`
 *   - `getMistakes`            -> `getMistakeCards()` + `CurriculumDao.getQuestionsByIds`
 *   - `getCompletedNodesByUnit`/`Section` -> `CurriculumDao.getNodeIdsByUnit/Section` +
 *       `getCompletedNodeProgressByNodeIds`
 *   - `getDueCardCountByUnit`  -> `CurriculumDao.getQuestionIdsByUnit` + `countDueCardsByQuestionIds`
 *
 * [StateCount] is currently defined at the bottom of `VocabDao.kt` in this same package; it will be
 * moved to its own file in T06 when `VocabDao` is deleted.
 */
@Dao
interface UserDataDao {

    // --- FSRS Cards (single-table on fsrs_cards) ---

    @Query("SELECT * FROM fsrs_cards WHERE questionId = :questionId")
    suspend fun getCardByQuestionId(questionId: String): FsrsCardEntity?

    @Query("SELECT * FROM fsrs_cards")
    suspend fun getAllCards(): List<FsrsCardEntity>

    @Query("SELECT COUNT(*) FROM fsrs_cards")
    suspend fun getCardCount(): Int

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE state = :state")
    suspend fun getCardCountByState(state: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FsrsCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllFsrsCards(cards: List<FsrsCardEntity>)

    @Update
    suspend fun updateFsrsCard(card: FsrsCardEntity)

    @Query("DELETE FROM fsrs_cards")
    suspend fun deleteAllCards()

    // --- FSRS cards for in-memory cross-DB assembly (strips the questions JOIN) ---

    /**
     * User-data half of the old `getDueAndNewCards` join: returns the FSRS cards alone
     * (state = :state OR due <= :now). The repository maps the resulting question IDs to
     * [CurriculumDao.getQuestionsByIds] and assembles [QuestionAndFsrsCard] in memory.
     */
    @Query(
        """
        SELECT * FROM fsrs_cards 
        WHERE state = :state OR due <= :now 
        ORDER BY state ASC, due ASC 
        LIMIT :limit
        """
    )
    suspend fun getDueAndNewCardEntities(state: Int, now: Long, limit: Int): List<FsrsCardEntity>

    /** Cards whose questionId is in the supplied list. Used to join fsrs data with curriculum. */
    @Query("SELECT * FROM fsrs_cards WHERE questionId IN (:questionIds)")
    suspend fun getCardsByQuestionIds(questionIds: List<String>): List<FsrsCardEntity>

    /**
     * User-data half of the old `getDueAndNewCardsByUnit`/`BySection` joins, scoped to a set of
     * question IDs supplied by [CurriculumDao] (e.g. `getQuestionIdsByUnit`).
     */
    @Query(
        """
        SELECT * FROM fsrs_cards 
        WHERE questionId IN (:questionIds) 
          AND (state = :state OR due <= :now)
        ORDER BY state ASC, due ASC 
        LIMIT :limit
        """
    )
    suspend fun getDueAndNewCardsByQuestionIds(
        questionIds: List<String>,
        state: Int,
        now: Long,
        limit: Int
    ): List<FsrsCardEntity>

    /** User-data half of the old `getMistakes` join (lapses > 0). */
    @Query(
        """
        SELECT * FROM fsrs_cards 
        WHERE lapses > 0 
        ORDER BY lastReview DESC 
        LIMIT :limit
        """
    )
    suspend fun getMistakeCards(limit: Int): List<FsrsCardEntity>

    // --- Stats ---

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE due <= :now AND state != 0")
    suspend fun getDueCount(now: Long): Int

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE lapses > 0")
    suspend fun getMistakeCount(): Int

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE state != 0") // 0 = State.New.value
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT state, COUNT(*) as count FROM fsrs_cards GROUP BY state")
    fun getStateCounts(): Flow<List<StateCount>>

    // --- Review Logs ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReviewLogs(logs: List<ReviewLogEntity>)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewDatetime DESC")
    suspend fun getReviewLogs(cardId: String): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewDatetime DESC")
    fun getReviewLogsFlow(cardId: String): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY reviewDatetime ASC")
    fun getAllReviewLogsFlow(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY reviewDatetime ASC")
    suspend fun getAllReviewLogsList(): List<ReviewLogEntity>

    @Query("DELETE FROM review_logs")
    suspend fun deleteAllReviewLogs()

    // --- Flagged Items ---

    @Query("SELECT * FROM flagged_items ORDER BY timestamp DESC")
    suspend fun getAllFlaggedItems(): List<FlaggedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlaggedItem(item: FlaggedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFlaggedItems(items: List<FlaggedItemEntity>)

    @Query("DELETE FROM flagged_items WHERE questionId = :questionId")
    suspend fun deleteFlaggedItem(questionId: String)

    @Query("DELETE FROM flagged_items")
    suspend fun deleteAllFlaggedItems()

    // --- Session Progress ---

    @Query("SELECT * FROM session_progress")
    suspend fun getAllSessionProgress(): List<SessionProgressEntity>

    @Query("DELETE FROM session_progress")
    suspend fun deleteAllSessionProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessionProgress(progressList: List<SessionProgressEntity>)

    // --- Node Progress ---

    @Query("SELECT * FROM node_progress WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNodeProgress(nodeId: String): NodeProgressEntity?

    @Query("SELECT * FROM node_progress")
    suspend fun getAllNodeProgress(): List<NodeProgressEntity>

    @Query("DELETE FROM node_progress")
    suspend fun deleteAllNodeProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNodeProgress(progressList: List<NodeProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodeProgress(progress: NodeProgressEntity)

    /**
     * User-data half of the old `getCompletedNodesByUnit`/`BySection` joins. Returns the completed
     * node_progress rows whose nodeId is in the supplied list (the repository provides the node IDs
     * from [CurriculumDao.getNodeIdsByUnit]/[CurriculumDao.getNodeIdsBySection]).
     */
    @Query("SELECT * FROM node_progress WHERE nodeId IN (:nodeIds) AND isCompleted = 1")
    suspend fun getCompletedNodeProgressByNodeIds(nodeIds: List<String>): List<NodeProgressEntity>

    /** User-data half of the old `getDueCardCountByUnit` join, scoped to a set of question IDs. */
    @Query(
        """
        SELECT COUNT(*) FROM fsrs_cards 
        WHERE questionId IN (:questionIds) 
          AND state != :newState 
          AND due <= :now
        """
    )
    suspend fun countDueCardsByQuestionIds(questionIds: List<String>, newState: Int, now: Long): Int
}
