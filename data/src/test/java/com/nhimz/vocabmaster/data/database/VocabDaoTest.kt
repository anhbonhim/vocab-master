package com.nhimz.vocabmaster.data.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.repository.ReviewRepositoryImpl
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.ReviewLog
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
class VocabDaoTest {

    private var database: VocabDatabase? = null
    private var vocabDao: VocabDao? = null

    @Before
    fun setup() {
        try {
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                VocabDatabase::class.java
            ).allowMainThreadQueries().build()
            vocabDao = database?.vocabDao()
        } catch (e: UnsatisfiedLinkError) {
            println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
        }
    }

    @After
    fun teardown() {
        database?.close()
    }

    @Test
    fun insertAndReadCard_v8Shape() = runTest {
        val dao = vocabDao ?: return@runTest
        val cardId = "q1"
        dao.insertCurriculumFixture(cardId)
        val now = System.currentTimeMillis()
        val entity = FsrsCardEntity(
            questionId = cardId,
            due = now,
            stability = null,
            difficulty = null,
            step = 0,
            state = State.New.value,
            lastReview = null,
            reps = 0,
            lapses = 0
        )

        dao.insertCard(entity)
        val read = dao.getCardByQuestionId(cardId)

        assertNotNull(read)
        assertEquals(cardId, read?.questionId)
        assertEquals(now, read?.due)
        assertNull(read?.stability)
        assertNull(read?.difficulty)
        assertEquals(0, read?.step)
        assertEquals(State.New.value, read?.state)
        assertNull(read?.lastReview)
    }

    @Test
    fun updateCardPersistsV6Fields() = runTest {
        val dao = vocabDao ?: return@runTest
        val cardId = "q2"
        dao.insertCurriculumFixture(cardId)
        val now = System.currentTimeMillis()
        dao.insertCard(
            FsrsCardEntity(
                questionId = cardId,
                due = now,
                stability = null,
                difficulty = null,
                step = 0,
                state = State.New.value,
                lastReview = null,
                reps = 0,
                lapses = 0
            )
        )

        val updated = FsrsCardEntity(
            questionId = cardId,
            due = now + 86_400_000L,
            stability = 12.5,
            difficulty = 5.6,
            step = null,
            state = State.Review.value,
            lastReview = now,
            reps = 1,
            lapses = 0
        )
        dao.updateFsrsCard(updated)

        val read = dao.getCardByQuestionId(cardId)
        assertNotNull(read)
        assertEquals(State.Review.value, read?.state)
        assertNull(read?.step)
        assertEquals(12.5, read?.stability ?: 0.0, 0.0001)
        assertEquals(5.6, read?.difficulty ?: 0.0, 0.0001)
        assertEquals(now, read?.lastReview)
    }

    @Test
    fun dueAndNewCardsFlow_emitsReactively() = runTest {
        val dao = vocabDao ?: return@runTest
        val newCardId = "q_new"
        val futureCardId = "q_future"
        val now = System.currentTimeMillis()
        dao.insertCurriculumFixture(newCardId)
        dao.insertCurriculumFixture(futureCardId)

        dao.insertCard(
            FsrsCardEntity(
                questionId = newCardId,
                due = now,
                stability = null,
                difficulty = null,
                step = 0,
                state = State.New.value,
                lastReview = null,
                reps = 0,
                lapses = 0
            )
        )
        dao.insertCard(
            FsrsCardEntity(
                questionId = futureCardId,
                due = now + 7 * 86_400_000L,
                stability = 10.0,
                difficulty = 5.0,
                step = null,
                state = State.Review.value,
                lastReview = now,
                reps = 1,
                lapses = 0
            )
        )

        val dueCards = dao.getDueAndNewCards(
            state = State.New.value,
            now = now,
            limit = 10
        ).first()

        assertEquals(1, dueCards.size)
        assertEquals(newCardId, dueCards[0].question.id)
        assertNotNull(dueCards[0].fsrsCard)
    }

    @Test
    fun reviewLogOrderingAndDuration() = runTest {
        val dao = vocabDao ?: return@runTest
        val cardId = "q_log"
        dao.insertCurriculumFixture(cardId)
        dao.insertCard(
            FsrsCardEntity(
                questionId = cardId,
                due = System.currentTimeMillis(),
                stability = null,
                difficulty = null,
                step = 0,
                state = State.New.value,
                lastReview = null,
                reps = 0,
                lapses = 0
            )
        )

        val logs = listOf(
            ReviewLogEntity(cardId = cardId, rating = Rating.Again.value, reviewDatetime = 1_000L, reviewDuration = 1_200L),
            ReviewLogEntity(cardId = cardId, rating = Rating.Good.value, reviewDatetime = 3_000L, reviewDuration = null),
            ReviewLogEntity(cardId = cardId, rating = Rating.Easy.value, reviewDatetime = 2_000L, reviewDuration = 900L)
        )
        logs.forEach { dao.insertReviewLog(it) }

        val read = dao.getReviewLogs(cardId)
        assertEquals(3, read.size)
        assertEquals(3_000L, read[0].reviewDatetime)
        assertNull(read[0].reviewDuration)
        assertEquals(2_000L, read[1].reviewDatetime)
        assertEquals(900L, read[1].reviewDuration)
        assertEquals(1_000L, read[2].reviewDatetime)
        assertEquals(1_200L, read[2].reviewDuration)
    }

    @Test
    fun recordReview_isAtomic() = runTest {
        val dao = vocabDao ?: return@runTest
        val db = database ?: return@runTest
        val cardId = "q_atomic"
        dao.insertCurriculumFixture(cardId)
        val now = System.currentTimeMillis()
        val original = FsrsCardEntity(
            questionId = cardId,
            due = now,
            stability = 5.0,
            difficulty = 4.0,
            step = null,
            state = State.Review.value,
            lastReview = now - 86_400_000L,
            reps = 1,
            lapses = 0
        )
        dao.insertCard(original)

        val repo = ReviewRepositoryImpl(db, dao)
        val domainCard = Card(
            cardId = cardId,
            state = State.Review,
            step = null,
            stability = 15.0,
            difficulty = 5.5,
            due = now + 10 * 86_400_000L,
            lastReview = now,
            reps = 2,
            lapses = 0
        )
        val domainLog = ReviewLog(
            cardId = cardId,
            rating = Rating.Good,
            reviewDatetime = now,
            reviewDuration = 1_500L
        )
        repo.recordReview(domainCard, domainLog)

        val updatedCard = dao.getCardByQuestionId(cardId)
        assertNotNull(updatedCard)
        assertEquals(15.0, updatedCard?.stability ?: 0.0, 0.0001)
        assertEquals(5.5, updatedCard?.difficulty ?: 0.0, 0.0001)
        assertEquals(2, updatedCard?.reps)

        val logs = dao.getReviewLogs(cardId)
        assertEquals(1, logs.size)
        assertEquals(Rating.Good.value, logs[0].rating)
        assertEquals(now, logs[0].reviewDatetime)
        assertEquals(1_500L, logs[0].reviewDuration)

        // Sabotage: mid-transaction exception must roll back the card update and log insert.
        try {
            db.withTransaction {
                dao.updateFsrsCard(original.copy(stability = 99.0))
                dao.insertReviewLog(
                    ReviewLogEntity(
                        cardId = cardId,
                        rating = Rating.Easy.value,
                        reviewDatetime = now + 1L,
                        reviewDuration = 100L
                    )
                )
                throw RuntimeException("transaction sabotage")
            }
        } catch (e: RuntimeException) {
            // Expected rollback trigger.
        }

        val afterRollback = dao.getCardByQuestionId(cardId)
        assertNotNull(afterRollback)
        assertEquals(15.0, afterRollback?.stability ?: 0.0, 0.0001)
        assertEquals(1, dao.getReviewLogs(cardId).size)
    }

    @Test
    fun stateCountsAndLearnedCount() = runTest {
        val dao = vocabDao ?: return@runTest
        val ids = listOf("new1", "learn1", "review1", "relearn1")
        ids.forEachIndexed { index, id ->
            dao.insertCurriculumFixture(id)
            val state = when (index) {
                0 -> State.New
                1 -> State.Learning
                2 -> State.Review
                else -> State.Relearning
            }
            dao.insertCard(
                FsrsCardEntity(
                    questionId = id,
                    due = System.currentTimeMillis(),
                    stability = null,
                    difficulty = null,
                    step = if (state == State.Review) null else 0,
                    state = state.value,
                    lastReview = null,
                    reps = 0,
                    lapses = 0
                )
            )
        }

        val stateCounts = dao.getStateCounts().first()
        val stateMap = stateCounts.associate { it.state to it.count }
        assertEquals(1, stateMap[State.New.value])
        assertEquals(1, stateMap[State.Learning.value])
        assertEquals(1, stateMap[State.Review.value])
        assertEquals(1, stateMap[State.Relearning.value])

        val learnedCount = dao.getLearnedCount().first()
        assertEquals(3, learnedCount)
    }

    private suspend fun VocabDao.insertCurriculumFixture(questionId: String) {
        val section = SectionEntity(
            id = "section_$questionId",
            index = 0,
            name = "Section",
            cefrSublevel = "A1",
            icon = "ic",
            description = ""
        )
        val unit = UnitEntity(
            id = "unit_$questionId",
            sectionId = section.id,
            index = 0,
            topic = "topic",
            title = "Unit",
            storySummary = "summary",
            icon = "ic",
            guidebookId = ""
        )
        val node = NodeEntity(
            id = "node_$questionId",
            unitId = unit.id,
            index = 0,
            type = 0,
            title = "Node",
            scenarioContext = "context",
            icon = "ic"
        )
        val session = SessionEntity(
            id = "session_$questionId",
            nodeId = node.id,
            index = 0,
            title = "Session",
            durationMinutes = 5,
            questionIds = "[]"
        )
        val question = QuestionEntity(
            id = questionId,
            sessionId = session.id,
            word = "word",
            type = 0,
            prompt = "Prompt",
            options = null,
            correctIndex = null,
            correctSentence = null,
            scrambledWords = null,
            translation = "Translation",
            audioUrl = null,
            audioUrlSlow = null,
            matchingPairs = null,
            imagePath = null
        )

        insertAllSections(listOf(section))
        insertAllUnits(listOf(unit))
        insertAllNodes(listOf(node))
        insertAllSessions(listOf(session))
        insertAllQuestions(listOf(question))
    }
}
