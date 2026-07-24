package com.nhimz.vocabmaster.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity
import kotlinx.coroutines.flow.Flow

/**
 * Curriculum-only DAO.
 *
 * Every query here touches only curriculum tables (sections, units, guidebooks, nodes, sessions,
 * questions) that live in [CurriculumDatabase]. The ID-helper queries ([getQuestionsByIds],
 * [getSessionIdsByUnit], [getSessionIdsBySection], [getQuestionsBySessionIds], [getNodeIdsByUnit],
 * [getNodeIdsBySection], [getQuestionIdsByUnit], [getQuestionIdsBySection]) exist so the repository can fetch the curriculum
 * side of a join and assemble [com.nhimz.vocabmaster.data.database.entity.QuestionAndFsrsCard]
 * objects in memory after querying [com.nhimz.vocabmaster.data.database.UserDataDao] for the
 * per-user FSRS side.
 */
@Dao
interface CurriculumDao {

    // --- Read queries (single-table / within-CurriculumDb) ---

    @Query("SELECT * FROM sections ORDER BY `index` ASC")
    fun getAllSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM units WHERE sectionId = :sectionId ORDER BY `index` ASC")
    fun getUnitsBySection(sectionId: String): Flow<List<UnitEntity>>

    @Query("SELECT * FROM unit_guidebooks WHERE unitId = :unitId LIMIT 1")
    suspend fun getGuidebook(unitId: String): UnitGuidebookEntity?

    @Query("SELECT * FROM nodes WHERE unitId = :unitId ORDER BY `index` ASC")
    fun getNodesByUnit(unitId: String): Flow<List<NodeEntity>>

    @Query("SELECT * FROM sessions WHERE nodeId = :nodeId ORDER BY `index` ASC")
    suspend fun getSessionsByNode(nodeId: String): List<SessionEntity>

    @Query("SELECT * FROM questions WHERE sessionId = :sessionId")
    suspend fun getQuestionsBySession(sessionId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    // --- Helper ID queries for in-memory cross-DB assembly ---

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT id FROM sessions WHERE nodeId IN (SELECT id FROM nodes WHERE unitId = :unitId)")
    suspend fun getSessionIdsByUnit(unitId: String): List<String>

    @Query(
        """
        SELECT id FROM sessions 
        WHERE nodeId IN (
            SELECT id FROM nodes 
            WHERE unitId IN (SELECT id FROM units WHERE sectionId = :sectionId)
        )
        """
    )
    suspend fun getSessionIdsBySection(sectionId: String): List<String>

    @Query("SELECT * FROM questions WHERE sessionId IN (:sessionIds)")
    suspend fun getQuestionsBySessionIds(sessionIds: List<String>): List<QuestionEntity>

    @Query("SELECT id FROM nodes WHERE unitId = :unitId")
    suspend fun getNodeIdsByUnit(unitId: String): List<String>

    @Query("SELECT id FROM nodes WHERE unitId IN (SELECT id FROM units WHERE sectionId = :sectionId)")
    suspend fun getNodeIdsBySection(sectionId: String): List<String>

    @Query(
        """
        SELECT id FROM questions 
        WHERE sessionId IN (
            SELECT id FROM sessions 
            WHERE nodeId IN (SELECT id FROM nodes WHERE unitId = :unitId)
        )
        """
    )
    suspend fun getQuestionIdsByUnit(unitId: String): List<String>

    @Query(
        """
        SELECT id FROM questions 
        WHERE sessionId IN (
            SELECT id FROM sessions 
            WHERE nodeId IN (
                SELECT id FROM nodes 
                WHERE unitId IN (SELECT id FROM units WHERE sectionId = :sectionId)
            )
        )
        """
    )
    suspend fun getQuestionIdsBySection(sectionId: String): List<String>

    // --- Insert (seed) queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSections(sections: List<SectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUnits(units: List<UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGuidebooks(guidebooks: List<UnitGuidebookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNodes(nodes: List<NodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<SessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<QuestionEntity>)
}
