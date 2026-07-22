package com.nhimz.vocabmaster.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionAndFsrsCard
import com.nhimz.vocabmaster.data.remote.VocabularyCardDto
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Dao
interface VocabDao {

    // --- FSRS Cards & Questions ---

    @Query("""
        SELECT * FROM questions 
        INNER JOIN fsrs_cards ON questions.id = fsrs_cards.questionId
        WHERE fsrs_cards.state = :state OR fsrs_cards.due <= :now 
        ORDER BY fsrs_cards.state ASC, fsrs_cards.due ASC 
        LIMIT :limit
    """)
    fun getDueAndNewCards(state: Int, now: Long, limit: Int): Flow<List<QuestionAndFsrsCard>>

    // Note: We don't have level or topic directly on fsrs_cards or questions currently.
    // If questions belong to units, and units have topics, this gets complicated.
    // I will keep the queries but they might need to join further if topic/level filtering is strict.
    // Wait, questions table doesn't have topic or difficultyLevel! 
    // They are implied by the section/unit they belong to.
    
    // I will remove getCardsByLevel, getCardsByTopic, getDueAndNewCardsByTopic, getNewCardsByTopicAndLevels, getNewCardsByLevels
    // and let the Repository throw NotImplemented or return empty if needed. 
    // Let's implement the basic ones first.

    @Query("SELECT * FROM fsrs_cards WHERE questionId = :questionId")
    suspend fun getCardByQuestionId(questionId: String): FsrsCardEntity?

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): com.nhimz.vocabmaster.data.database.entity.QuestionEntity?

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

    // --- Stats Queries ---

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE due <= :now AND state != 0")
    suspend fun getDueCount(now: Long): Int

    @Query("SELECT COUNT(*) FROM fsrs_cards WHERE lapses > 0")
    suspend fun getMistakeCount(): Int

    @Query("""
        SELECT * FROM questions 
        INNER JOIN fsrs_cards ON questions.id = fsrs_cards.questionId
        WHERE fsrs_cards.lapses > 0 
        ORDER BY fsrs_cards.lastReview DESC 
        LIMIT :limit
    """)
    suspend fun getMistakes(limit: Int): List<QuestionAndFsrsCard>

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
    suspend fun getAllFlaggedItems(): List<com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlaggedItem(item: com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFlaggedItems(items: List<com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity>)

    @Query("DELETE FROM flagged_items WHERE questionId = :questionId")
    suspend fun deleteFlaggedItem(questionId: String)

    @Query("DELETE FROM flagged_items")
    suspend fun deleteAllFlaggedItems()

    // --- Session Progress ---

    @Query("SELECT * FROM session_progress")
    suspend fun getAllSessionProgress(): List<com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity>

    @Query("DELETE FROM session_progress")
    suspend fun deleteAllSessionProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessionProgress(progressList: List<com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity>)

    // --- Static Curriculum ---
    
    @Query("SELECT * FROM sections ORDER BY `index` ASC")
    fun getAllSections(): Flow<List<com.nhimz.vocabmaster.data.database.entity.SectionEntity>>

    @Query("SELECT * FROM units WHERE sectionId = :sectionId ORDER BY `index` ASC")
    fun getUnitsBySection(sectionId: String): Flow<List<com.nhimz.vocabmaster.data.database.entity.UnitEntity>>

    @Query("SELECT * FROM unit_guidebooks WHERE unitId = :unitId LIMIT 1")
    suspend fun getGuidebook(unitId: String): com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity?

    @Query("SELECT * FROM nodes WHERE unitId = :unitId ORDER BY `index` ASC")
    fun getNodesByUnit(unitId: String): Flow<List<com.nhimz.vocabmaster.data.database.entity.NodeEntity>>

    @Query("SELECT * FROM sessions WHERE nodeId = :nodeId ORDER BY `index` ASC")
    suspend fun getSessionsByNode(nodeId: String): List<com.nhimz.vocabmaster.data.database.entity.SessionEntity>

    @Query("SELECT * FROM questions WHERE sessionId = :sessionId")
    suspend fun getQuestionsBySession(sessionId: String): List<com.nhimz.vocabmaster.data.database.entity.QuestionEntity>

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<com.nhimz.vocabmaster.data.database.entity.QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSections(sections: List<com.nhimz.vocabmaster.data.database.entity.SectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUnits(units: List<com.nhimz.vocabmaster.data.database.entity.UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGuidebooks(guidebooks: List<com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNodes(nodes: List<com.nhimz.vocabmaster.data.database.entity.NodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<com.nhimz.vocabmaster.data.database.entity.SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<com.nhimz.vocabmaster.data.database.entity.QuestionEntity>)

    // --- Node Progress ---
    
    @Query("SELECT * FROM node_progress WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNodeProgress(nodeId: String): com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity?

    @Query("SELECT * FROM node_progress")
    suspend fun getAllNodeProgress(): List<com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity>

    @Query("DELETE FROM node_progress")
    suspend fun deleteAllNodeProgress()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNodeProgress(progressList: List<com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity>)

    @Query("""
        SELECT nodeId FROM node_progress 
        INNER JOIN nodes ON node_progress.nodeId = nodes.id 
        WHERE nodes.unitId = :unitId AND isCompleted = 1
    """)
    suspend fun getCompletedNodesByUnit(unitId: String): List<String>
    
    @Query("""
        SELECT nodeId FROM node_progress 
        INNER JOIN nodes ON node_progress.nodeId = nodes.id 
        INNER JOIN units ON nodes.unitId = units.id
        WHERE units.sectionId = :sectionId AND isCompleted = 1
    """)
    suspend fun getCompletedNodesBySection(sectionId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodeProgress(progress: com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity)

    // Legacy Topic and Level queries - Fallbacks
    @Query("SELECT * FROM questions INNER JOIN fsrs_cards ON questions.id = fsrs_cards.questionId LIMIT :limit")
    fun getDueAndNewCardsByTopicFallback(limit: Int): Flow<List<QuestionAndFsrsCard>>

    // --- Scoped Due Cards (for REVIEW node on Duolingo-style path) ---

    @Query("""
        SELECT questions.* FROM questions
        INNER JOIN fsrs_cards ON questions.id = fsrs_cards.questionId
        INNER JOIN sessions ON questions.sessionId = sessions.id
        INNER JOIN nodes ON sessions.nodeId = nodes.id
        WHERE nodes.unitId = :unitId
          AND (fsrs_cards.state = :state OR fsrs_cards.due <= :now)
        ORDER BY fsrs_cards.state ASC, fsrs_cards.due ASC
        LIMIT :limit
    """)
    fun getDueAndNewCardsByUnit(
        unitId: String,
        state: Int,
        now: Long,
        limit: Int
    ): Flow<List<QuestionAndFsrsCard>>

    @Query("""
        SELECT questions.* FROM questions
        INNER JOIN fsrs_cards ON questions.id = fsrs_cards.questionId
        INNER JOIN sessions ON questions.sessionId = sessions.id
        INNER JOIN nodes ON sessions.nodeId = nodes.id
        INNER JOIN units ON nodes.unitId = units.id
        WHERE units.sectionId = :sectionId
          AND (fsrs_cards.state = :state OR fsrs_cards.due <= :now)
        ORDER BY fsrs_cards.state ASC, fsrs_cards.due ASC
        LIMIT :limit
    """)
    fun getDueAndNewCardsBySection(
        sectionId: String,
        state: Int,
        now: Long,
        limit: Int
    ): Flow<List<QuestionAndFsrsCard>>

    @Query("""
        SELECT COUNT(*) FROM fsrs_cards
        INNER JOIN questions ON fsrs_cards.questionId = questions.id
        INNER JOIN sessions ON questions.sessionId = sessions.id
        INNER JOIN nodes ON sessions.nodeId = nodes.id
        WHERE nodes.unitId = :unitId
          AND fsrs_cards.state != :newState
          AND fsrs_cards.due <= :now
    """)
    suspend fun getDueCardCountByUnit(unitId: String, newState: Int, now: Long): Int

    // --- Sync merge (D-03 / SYNC-02) -----------------------------------
    //
    // Server-wins with time-based merging. We only overwrite the local
    // FSRS card when the pulled payload is strictly newer than what we
    // have locally. This prevents an older server snapshot (e.g. a device
    // that has been offline for a while) from downgrading the FSRS state
    // the user accumulated on this device.
    //
    // Rule (D-03): if `existing.lastReview != null` and the pulled
    // `lastModified < existing.lastReview`, the update is SKIPPED.
    //
    // @Transaction guarantees the whole merge runs atomically — either
    // every card in the batch is updated/inserted, or nothing changes.

    @Transaction
    suspend fun mergePulledCards(
        pulledCards: List<VocabularyCardDto>,
        formatter: DateTimeFormatter
    ) {
        for (c in pulledCards) {
            val existing = getCardByQuestionId(c.questionId)
            val dueMillis = LocalDateTime.parse(c.due, formatter)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            val lastReviewMillis = c.lastReview?.let {
                LocalDateTime.parse(it, formatter).toInstant(ZoneOffset.UTC).toEpochMilli()
            }
            val stateEnum = State.entries.firstOrNull { it.value == c.state } ?: State.New

            if (existing != null) {
                // Skip stale payload — the local FSRS state is newer than the
                // server snapshot. This is the core D-03 invariant.
                if (existing.lastReview != null && c.lastModified < existing.lastReview) {
                    continue
                }
                val updated = existing.copy(
                    due = dueMillis,
                    stability = c.stability,
                    difficulty = c.difficulty,
                    step = existing.step,
                    reps = c.reps,
                    lapses = c.lapses,
                    state = stateEnum.value,
                    lastReview = lastReviewMillis
                )
                updateFsrsCard(updated)
            } else {
                val newCard = FsrsCardEntity(
                    questionId = c.questionId,
                    due = dueMillis,
                    stability = c.stability,
                    difficulty = c.difficulty,
                    step = 0,
                    reps = c.reps,
                    lapses = c.lapses,
                    state = stateEnum.value,
                    lastReview = lastReviewMillis
                )
                insertCard(newCard)
            }
        }
    }

}

data class StateCount(val state: Int, val count: Int)
