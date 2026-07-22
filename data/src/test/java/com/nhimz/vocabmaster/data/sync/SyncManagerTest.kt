package com.nhimz.vocabmaster.data.sync

import androidx.test.core.app.ApplicationProvider
import com.nhimz.vocabmaster.data.auth.AuthManager
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionAndFsrsCard
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity
import com.nhimz.vocabmaster.data.remote.ApiClient
import com.nhimz.vocabmaster.data.remote.AuthInterceptor
import com.nhimz.vocabmaster.data.remote.SyncApiService
import com.nhimz.vocabmaster.data.remote.SyncPayload
import com.nhimz.vocabmaster.data.remote.UserSettingsDto
import com.nhimz.vocabmaster.data.remote.VocabularyCardDto
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Unit tests for [SyncManager].
 *
 * Task 1 covers SYNC-01: `sync()` must catch network failures
 * ([java.io.IOException] / HTTP 5xx) and return `false` cleanly instead of
 * propagating the exception to the caller (the [SettingsViewModel]).
 *
 * Task 2 (separate commits) adds SYNC-02 time-based merging + log
 * preservation tests on top of this scaffolding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
class SyncManagerTest {

    // ---------------------------------------------------------------------
    // Task 1 — SYNC-01: network resilience
    // ---------------------------------------------------------------------

    @Test
    fun testSyncNetworkFailure_pushThrowsIoException_returnsFalse() = runTest {
        val throwingApi = throwingApiService(pushError = IOException("connection reset by peer"))
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = FakeVocabDao(),
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(throwingApi)
        )

        val result = syncManager.sync()

        assertFalse("sync() must return false on push IOException", result)
    }

    @Test
    fun testSyncPushHttpError_returnsFalse() = runTest {
        val http500 = Response.error<Unit>(
            500,
            "{\"error\":\"internal\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        val api = stubApiService(
            pushResult = { http500 },
            pullResult = { Response.success(emptyPayload()) }
        )
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = FakeVocabDao(),
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(api)
        )

        val result = syncManager.sync()

        assertFalse("sync() must return false on HTTP 500 push", result)
    }

    @Test
    fun testSyncPullHttpError_returnsFalse() = runTest {
        val okPush = Response.success<Unit>(Unit)
        val http500 = Response.error<SyncPayload>(
            500,
            "{\"error\":\"internal\"}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        val api = stubApiService(
            pushResult = { okPush },
            pullResult = { http500 }
        )
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = FakeVocabDao(),
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(api)
        )

        val result = syncManager.sync()

        assertFalse("sync() must return false on HTTP 500 pull", result)
    }

    // ---------------------------------------------------------------------
    // Task 2 — SYNC-02: time-based merging & log preservation
    //
    // D-03 (Server-wins with Time-Based Merging): the pull must skip an
    // update when the local card has a newer lastReview than the pulled
    // lastModified; this prevents an older server payload from
    // downgrading the FSRS state.
    //
    // D-04 (Review Log Preservation): pushSync must not delete local
    // review logs on failure.
    // ---------------------------------------------------------------------

    @Test
    fun testTimeBasedMerging_olderPullDoesNotOverwriteLocalState() = runTest {
        // Local card has lastReview = 200; pulled card has lastModified = 100.
        // The merge must SKIP the update so FSRS state is not downgraded.
        val now = System.currentTimeMillis()
        val localCard = FsrsCardEntity(
            questionId = "q1",
            due = now,
            stability = 5.0,
            difficulty = 4.0,
            step = null,
            state = State.Review.value,
            lastReview = 200L,
            reps = 1,
            lapses = 0
        )
        val pulledCard = VocabularyCardDto(
            questionId = "q1",
            due = now.toString(),
            stability = 99.0,
            difficulty = 99.0,
            interval = 0,
            reps = 999,
            lapses = 999,
            state = State.New.value,
            lastReview = 100L.toString(),
            lastModified = 100L
        )
        val fakeDao = FakeVocabDao(initialCards = listOf(localCard))
        val api = stubApiService(
            pushResult = { Response.success(Unit) },
            pullResult = {
                Response.success(emptyPayload().copy(vocabularyCards = listOf(pulledCard)))
            }
        )
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = fakeDao,
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(api)
        )

        val result = syncManager.sync()

        assertTrue("sync() should succeed when only pull timestamps are stale", result)
        assertEquals(
            "updateFsrsCard must NOT be called when the pulled timestamp is older",
            0,
            fakeDao.updateFsrsCardCallCount
        )
        val stored = fakeDao.getCardByQuestionId("q1")
        assertEquals(
            "Local stability must be preserved",
            5.0,
            stored?.stability ?: 0.0,
            0.0001
        )
        assertEquals(
            "Local lastReview must be preserved (200)",
            200L,
            stored?.lastReview
        )
    }

    @Test
    fun testTimeBasedMerging_serverWinsWhenNewer() = runTest {
        val now = System.currentTimeMillis()
        val localCard = FsrsCardEntity(
            questionId = "q1",
            due = now,
            stability = 5.0,
            difficulty = 4.0,
            step = null,
            state = State.Review.value,
            lastReview = 100L,
            reps = 1,
            lapses = 0
        )
        val pulledCard = VocabularyCardDto(
            questionId = "q1",
            due = now.toString(),
            stability = 7.5,
            difficulty = 3.5,
            interval = 0,
            reps = 3,
            lapses = 0,
            state = State.Review.value,
            lastReview = 300L.toString(),
            lastModified = 300L
        )
        val fakeDao = FakeVocabDao(initialCards = listOf(localCard))
        val api = stubApiService(
            pushResult = { Response.success(Unit) },
            pullResult = {
                Response.success(emptyPayload().copy(vocabularyCards = listOf(pulledCard)))
            }
        )
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = fakeDao,
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(api)
        )

        val result = syncManager.sync()

        assertTrue("sync() should succeed when pull is newer", result)
        assertEquals(
            "updateFsrsCard must be called exactly once for the server-wins path",
            1,
            fakeDao.updateFsrsCardCallCount
        )
        val stored = fakeDao.getCardByQuestionId("q1")
        assertEquals(
            "Pulled stability must overwrite",
            7.5,
            stored?.stability ?: 0.0,
            0.0001
        )
        assertEquals(
            "Pulled lastReview must overwrite (300)",
            300L,
            stored?.lastReview
        )
    }

    @Test
    fun testReviewLogsPreservedOnFailure() = runTest {
        // Push fails with IOException — local review logs must NOT be deleted.
        // Per D-04 / SYNC-02.
        val fakeDao = FakeVocabDao(
            initialReviewLogs = listOf(
                ReviewLogEntity(cardId = "q1", rating = 3, reviewDatetime = 1000L, reviewDuration = null),
                ReviewLogEntity(cardId = "q1", rating = 4, reviewDatetime = 2000L, reviewDuration = 500L)
            )
        )
        val api = throwingApiService(pushError = IOException("push down"))
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val syncManager = SyncManager(
            context = ctx,
            vocabDao = fakeDao,
            settingsRepository = FakeSettingsRepository(),
            apiClient = FakeApiClient(api)
        )

        val result = syncManager.sync()

        assertFalse("sync() must return false on push failure", result)
        assertEquals(
            "deleteAllReviewLogs must NEVER be called when push fails",
            0,
            fakeDao.deleteAllReviewLogsCallCount
        )
        assertEquals(
            "Local review log count must remain 2 when push fails",
            2,
            fakeDao.getAllReviewLogsList().size
        )
    }

    // ---------------------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------------------

    private fun emptyPayload(): SyncPayload = SyncPayload(
        userSettings = UserSettingsDto(
            dailyGoalXp = 50,
            currentStreak = 0,
            longestStreak = 0,
            availableFreezes = 1,
            lastStudyDate = 0L,
            xpTotal = 0,
            desiredRetention = 0.9,
            theme = "SYSTEM",
            language = "VI",
            placementLevel = null,
            selectedTopic = "general"
        ),
        vocabularyCards = emptyList(),
        reviewLogs = emptyList(),
        lastSyncTimestamp = 0L
    )

    private fun stubApiService(
        pushResult: () -> Response<Unit>,
        pullResult: () -> Response<SyncPayload>
    ): SyncApiService = object : SyncApiService {
        override suspend fun pushSync(payload: SyncPayload): Response<Unit> = pushResult()
        override suspend fun pullSync(since: Long): Response<SyncPayload> = pullResult()
    }

    private fun throwingApiService(
        pushError: Throwable? = null,
        pullError: Throwable? = null
    ): SyncApiService = object : SyncApiService {
        override suspend fun pushSync(payload: SyncPayload): Response<Unit> {
            pushError?.let { throw it }
            return Response.success(Unit)
        }
        override suspend fun pullSync(since: Long): Response<SyncPayload> {
            pullError?.let { throw it }
            return Response.success(emptyPayload())
        }
    }

    /**
     * Subclass of [ApiClient] that replaces [ApiClient.syncApi] with the
     * supplied fake — lets us drive network behaviour without a real server.
     */
    private class FakeApiClient(fakeSyncApi: SyncApiService) : ApiClient(
        authInterceptor = AuthInterceptor(
            AuthManager(ApplicationProvider.getApplicationContext())
        )
    ) {
        override val syncApi: SyncApiService = fakeSyncApi
    }

    // ---------------------------------------------------------------------
    // In-memory VocabDao fake — used by network-resilience tests in Task 1
    // and by the time-based merging tests added in Task 2. Tracks the
    // calls that the production SyncManager is allowed/not allowed to make
    // so the tests can assert on them.
    // ---------------------------------------------------------------------

    private class FakeVocabDao(
        initialCards: List<FsrsCardEntity> = emptyList(),
        initialReviewLogs: List<ReviewLogEntity> = emptyList()
    ) : VocabDao {
        private val cards: MutableList<FsrsCardEntity> = initialCards.toMutableList()
        private val reviewLogs: MutableList<ReviewLogEntity> = initialReviewLogs.toMutableList()

        var updateFsrsCardCallCount: Int = 0
        var deleteAllReviewLogsCallCount: Int = 0

        override suspend fun getCardByQuestionId(questionId: String): FsrsCardEntity? =
            cards.firstOrNull { it.questionId == questionId }

        override suspend fun getAllCards(): List<FsrsCardEntity> = cards.toList()

        override suspend fun updateFsrsCard(card: FsrsCardEntity) {
            updateFsrsCardCallCount++
            val idx = cards.indexOfFirst { it.questionId == card.questionId }
            if (idx >= 0) cards[idx] = card
        }

        override suspend fun insertCard(card: FsrsCardEntity): Long {
            cards.add(card)
            return card.questionId.hashCode().toLong()
        }

        override suspend fun getReviewLogs(cardId: String): List<ReviewLogEntity> =
            reviewLogs.filter { it.cardId == cardId }

        override suspend fun getAllReviewLogsList(): List<ReviewLogEntity> =
            reviewLogs.toList()

        override suspend fun mergePulledCards(
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
                    if (existing.lastReview != null && c.lastModified < existing.lastReview) {
                        continue
                    }
                    updateFsrsCard(
                        existing.copy(
                            due = dueMillis,
                            stability = c.stability,
                            difficulty = c.difficulty,
                            step = existing.step,
                            reps = c.reps,
                            lapses = c.lapses,
                            state = stateEnum.value,
                            lastReview = lastReviewMillis
                        )
                    )
                } else {
                    insertCard(
                        FsrsCardEntity(
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
                    )
                }
            }
        }

        override suspend fun deleteAllReviewLogs() {
            deleteAllReviewLogsCallCount++
            // Intentionally do NOT mutate the list — that would prove the
            // violation, but the production code must not call this on push
            // failure. We only count calls.
        }

        override fun getDueAndNewCards(state: Int, now: Long, limit: Int):
            Flow<List<QuestionAndFsrsCard>> = MutableStateFlow(emptyList())

        override suspend fun getQuestionById(id: String) = null
        override suspend fun getCardCount(): Int = cards.size
        override suspend fun getCardCountByState(state: Int): Int =
            cards.count { it.state == state }
        override suspend fun insertAllFsrsCards(cards: List<FsrsCardEntity>) {
            this.cards.addAll(cards)
        }
        override suspend fun deleteAllCards() { cards.clear() }
        override suspend fun getDueCount(now: Long): Int = 0
        override suspend fun getMistakeCount(): Int = 0
        override suspend fun getMistakes(limit: Int) = emptyList<QuestionAndFsrsCard>()
        override fun getLearnedCount(): Flow<Int> = MutableStateFlow(0)
        override fun getStateCounts(): Flow<List<com.nhimz.vocabmaster.data.database.StateCount>> =
            MutableStateFlow(emptyList())
        override suspend fun insertReviewLog(log: ReviewLogEntity) { reviewLogs.add(log) }
        override suspend fun insertAllReviewLogs(logs: List<ReviewLogEntity>) { reviewLogs.addAll(logs) }
        override fun getReviewLogsFlow(cardId: String): Flow<List<ReviewLogEntity>> =
            MutableStateFlow(reviewLogs.filter { it.cardId == cardId })
        override fun getAllReviewLogsFlow(): Flow<List<ReviewLogEntity>> =
            MutableStateFlow(reviewLogs.toList())
        override suspend fun getAllFlaggedItems() = emptyList<FlaggedItemEntity>()
        override suspend fun insertFlaggedItem(item: FlaggedItemEntity) {}
        override suspend fun insertAllFlaggedItems(items: List<FlaggedItemEntity>) {}
        override suspend fun deleteFlaggedItem(questionId: String) {}
        override suspend fun deleteAllFlaggedItems() {}
        override suspend fun getAllSessionProgress() = emptyList<SessionProgressEntity>()
        override suspend fun deleteAllSessionProgress() {}
        override suspend fun insertAllSessionProgress(progressList: List<SessionProgressEntity>) {}
        override fun getAllSections(): Flow<List<SectionEntity>> = MutableStateFlow(emptyList())
        override fun getUnitsBySection(sectionId: String): Flow<List<UnitEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun getGuidebook(unitId: String) = null
        override fun getNodesByUnit(unitId: String): Flow<List<NodeEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun getSessionsByNode(nodeId: String) = emptyList<SessionEntity>()
        override suspend fun getQuestionsBySession(sessionId: String) = emptyList<QuestionEntity>()
        override suspend fun getAllQuestions() = emptyList<QuestionEntity>()
        override suspend fun getQuestionCount(): Int = 0
        override suspend fun insertAllSections(sections: List<SectionEntity>) {}
        override suspend fun insertAllUnits(units: List<UnitEntity>) {}
        override suspend fun insertAllGuidebooks(guidebooks: List<UnitGuidebookEntity>) {}
        override suspend fun insertAllNodes(nodes: List<NodeEntity>) {}
        override suspend fun insertAllSessions(sessions: List<SessionEntity>) {}
        override suspend fun insertAllQuestions(questions: List<QuestionEntity>) {}
        override suspend fun getNodeProgress(nodeId: String) = null
        override suspend fun getAllNodeProgress() = emptyList<NodeProgressEntity>()
        override suspend fun deleteAllNodeProgress() {}
        override suspend fun insertAllNodeProgress(progressList: List<NodeProgressEntity>) {}
        override suspend fun getCompletedNodesByUnit(unitId: String) = emptyList<String>()
        override suspend fun getCompletedNodesBySection(sectionId: String) = emptyList<String>()
        override suspend fun insertNodeProgress(progress: NodeProgressEntity) {}
        override fun getDueAndNewCardsByTopicFallback(limit: Int):
            Flow<List<QuestionAndFsrsCard>> = MutableStateFlow(emptyList())
        override fun getDueAndNewCardsByUnit(unitId: String, state: Int, now: Long, limit: Int):
            Flow<List<QuestionAndFsrsCard>> = MutableStateFlow(emptyList())
        override fun getDueAndNewCardsBySection(sectionId: String, state: Int, now: Long, limit: Int):
            Flow<List<QuestionAndFsrsCard>> = MutableStateFlow(emptyList())
        override suspend fun getDueCardCountByUnit(unitId: String, newState: Int, now: Long): Int = 0
    }

    // ---------------------------------------------------------------------
    // In-memory SettingsRepository fake — enough to satisfy SyncManager.sync().
    // ---------------------------------------------------------------------

    private class FakeSettingsRepository : SettingsRepository {
        private val dailyGoalXpFlow = MutableStateFlow(50)
        private val currentStreakFlow = MutableStateFlow(0)
        private val longestStreakFlow = MutableStateFlow(0)
        private val availableFreezesFlow = MutableStateFlow(1)
        private val lastStudyDateFlow = MutableStateFlow(0L)
        private val todayStudySecondsFlow = MutableStateFlow(0)
        private val xpTotalFlow = MutableStateFlow(0)
        private val badgeStatusFlow = MutableStateFlow(emptyList<String>())
        private val desiredRetentionFlow = MutableStateFlow(0.9)
        private val themeFlow = MutableStateFlow("SYSTEM")
        private val languageFlow = MutableStateFlow("VI")
        private val placementLevelFlow = MutableStateFlow<String?>(null)
        private val selectedTopicFlow = MutableStateFlow("general")
        private val useLocalDevServerFlow = MutableStateFlow(false)

        override val dailyGoalXp: Flow<Int> = dailyGoalXpFlow
        override suspend fun updateDailyGoal(xp: Int) { dailyGoalXpFlow.value = xp }
        override val currentStreak: Flow<Int> = currentStreakFlow
        override suspend fun setCurrentStreak(streak: Int) { currentStreakFlow.value = streak }
        override val longestStreak: Flow<Int> = longestStreakFlow
        override suspend fun setLongestStreak(streak: Int) { longestStreakFlow.value = streak }
        override val availableFreezes: Flow<Int> = availableFreezesFlow
        override suspend fun setAvailableFreezes(freezes: Int) { availableFreezesFlow.value = freezes }
        override val lastStudyDate: Flow<Long> = lastStudyDateFlow
        override suspend fun setLastStudyDate(timestamp: Long) { lastStudyDateFlow.value = timestamp }
        override val todayStudySeconds: Flow<Int> = todayStudySecondsFlow
        override suspend fun addStudySeconds(seconds: Int) { todayStudySecondsFlow.value += seconds }
        override val xpTotal: Flow<Int> = xpTotalFlow
        override suspend fun addXp(xp: Int) { xpTotalFlow.value += xp }
        override suspend fun setXpTotal(xp: Int) { xpTotalFlow.value = xp }
        override val badgeStatus: Flow<List<String>> = badgeStatusFlow
        override suspend fun addBadge(badge: String) { badgeStatusFlow.value = badgeStatusFlow.value + badge }
        override suspend fun setBadgeStatus(badges: List<String>) { badgeStatusFlow.value = badges }
        override val desiredRetention: Flow<Double> = desiredRetentionFlow
        override suspend fun setDesiredRetention(retention: Double) { desiredRetentionFlow.value = retention }
        override val theme: Flow<String> = themeFlow
        override suspend fun setTheme(theme: String) { themeFlow.value = theme }
        override val language: Flow<String> = languageFlow
        override suspend fun setLanguage(language: String) { languageFlow.value = language }
        override val placementLevel: Flow<String?> = placementLevelFlow
        override suspend fun setPlacementLevel(level: String) { placementLevelFlow.value = level }
        override val selectedTopic: Flow<String> = selectedTopicFlow
        override suspend fun setSelectedTopic(topic: String) { selectedTopicFlow.value = topic }
        override val useLocalDevServer: Flow<Boolean> = useLocalDevServerFlow
        override suspend fun setUseLocalDevServer(enabled: Boolean) { useLocalDevServerFlow.value = enabled }
    }
}
