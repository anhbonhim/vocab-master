package com.nhimz.vocabmaster.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabDataException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
@Suppress("LabeledExpression")
class VocabularyRepositoryImplTest {

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
    fun malformedOptionsJsonFailsLoudly() = runTest {
        val dao = vocabDao ?: return@runTest
        seedOneQuestion(dao, options = "{not valid json")
        val repo = VocabularyRepositoryImpl(dao, ApplicationProvider.getApplicationContext())

        val result = repo.getQuestionsBySession("session_1")

        assertTrue("Malformed options must produce a failure", result.isFailure)
        assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
    }

    @Test
    fun malformedMatchingPairsJsonFailsLoudly() = runTest {
        val dao = vocabDao ?: return@runTest
        seedOneQuestion(dao, matchingPairs = "{not valid json")
        val repo = VocabularyRepositoryImpl(dao, ApplicationProvider.getApplicationContext())

        val result = repo.getQuestionsBySession("session_1")

        assertTrue("Malformed matchingPairs must produce a failure", result.isFailure)
        assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
    }

    @Test
    fun malformedGuidebookJsonReturnsFailure() = runTest {
        val dao = vocabDao ?: return@runTest
        seedOneQuestion(dao)
        val guidebook = UnitGuidebookEntity(
            id = "guidebook_1",
            unitId = "unit_1",
            grammarTips = "{not valid json",
            keyPhrases = "[]",
            storyIntro = "Intro",
            illustrationSvg = null
        )
        dao.insertAllGuidebooks(listOf(guidebook))
        val repo = VocabularyRepositoryImpl(dao, ApplicationProvider.getApplicationContext())

        val result = repo.getGuidebook("unit_1")

        assertTrue("Malformed grammarTips must produce a failure", result.isFailure)
        assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
    }

    @Test
    fun malformedSessionQuestionIdsReturnsFailure() = runTest {
        val dao = vocabDao ?: return@runTest
        seedMinimalCurriculum(dao)
        val session = SessionEntity(
            id = "session_bad",
            nodeId = "node_1",
            index = 1,
            title = "Bad session",
            durationMinutes = 5,
            questionIds = "{not valid json"
        )
        dao.insertAllSessions(listOf(session))
        val repo = VocabularyRepositoryImpl(dao, ApplicationProvider.getApplicationContext())

        val result = repo.getSessionsByNode("node_1")

        assertTrue("Malformed questionIds must produce a failure", result.isFailure)
        assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
    }

    @Test
    fun validRowsStillDecode() = runTest {
        val dao = vocabDao ?: return@runTest
        seedOneQuestion(dao)
        val repo = VocabularyRepositoryImpl(dao, ApplicationProvider.getApplicationContext())

        val sessionResult = repo.getSessionsByNode("node_1")
        assertTrue("Valid session must decode successfully", sessionResult.isSuccess)
        assertEquals(listOf("q1"), sessionResult.getOrThrow().first().questionIds)

        val questionResult = repo.getQuestionsBySession("session_1")
        assertTrue("Valid question must decode successfully", questionResult.isSuccess)
        val question = questionResult.getOrThrow().first()
        assertEquals("q1", question.id)
        assertEquals(4, question.options?.size)
        assertEquals(1, question.matchingPairs?.size)
        assertEquals(3, question.scrambledWords?.size)
    }

    @Test
    fun importBackupMalformedJsonReturnsFailure() = runTest {
        val dao = vocabDao ?: return@runTest
        val fakeSettings = FakeSettingsRepository()
        val repo = BackupRepositoryImpl(database!!, dao, fakeSettings)

        val result = repo.importBackup("{ definitely not json")

        assertTrue("Malformed backup JSON must produce a failure", result.isFailure)
        assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
    }

    @Test
    fun importBackupVersion2ReturnsSuccessFalse() = runTest {
        val dao = vocabDao ?: return@runTest
        val fakeSettings = FakeSettingsRepository()
        val repo = BackupRepositoryImpl(database!!, dao, fakeSettings)

        val v2Payload = """
            {
                "version": 2,
                "timestamp": 0,
                "settings": {
                    "currentStreak": 0,
                    "longestStreak": 0,
                    "availableFreezes": 1,
                    "lastStudyDate": 0,
                    "xpTotal": 0,
                    "badgeStatus": [],
                    "dailyGoalXp": 50,
                    "desiredRetention": 0.9,
                    "theme": "SYSTEM",
                    "language": "VI"
                },
                "cards": [],
                "reviewLogs": [],
                "flaggedItems": []
            }
        """.trimIndent()

        val result = repo.importBackup(v2Payload)

        assertTrue("Version-2 backup must succeed as a rejection", result.isSuccess)
        assertFalse("Version-2 backup must be rejected (false)", result.getOrThrow())
    }

    private suspend fun seedMinimalCurriculum(dao: VocabDao) {
        val section = SectionEntity(
            id = "section_1",
            index = 0,
            name = "Test Section",
            cefrSublevel = "A1",
            icon = "",
            description = ""
        )
        val unit = UnitEntity(
            id = "unit_1",
            sectionId = "section_1",
            index = 0,
            topic = "test",
            title = "Test Unit",
            storySummary = "",
            icon = "",
            guidebookId = "guidebook_1"
        )
        val node = NodeEntity(
            id = "node_1",
            unitId = "unit_1",
            index = 0,
            type = 0,
            title = "Test Node",
            scenarioContext = "",
            icon = ""
        )
        dao.insertAllSections(listOf(section))
        dao.insertAllUnits(listOf(unit))
        dao.insertAllNodes(listOf(node))
    }

    private suspend fun seedOneQuestion(
        dao: VocabDao,
        options: String = "[\"a\",\"b\",\"c\",\"d\"]",
        scrambledWords: String = "[\"word1\",\"word2\",\"word3\"]",
        matchingPairs: String = "[{\"left\":\"a\",\"right\":\"b\"}]"
    ) {
        seedMinimalCurriculum(dao)
        val session = SessionEntity(
            id = "session_1",
            nodeId = "node_1",
            index = 0,
            title = "Test Session",
            durationMinutes = 5,
            questionIds = "[\"q1\"]"
        )
        val question = QuestionEntity(
            id = "q1",
            sessionId = "session_1",
            word = "word",
            type = 0,
            prompt = "prompt",
            options = options,
            correctIndex = 0,
            correctSentence = null,
            scrambledWords = scrambledWords,
            translation = "translation",
            audioUrl = null,
            audioUrlSlow = null,
            matchingPairs = matchingPairs,
            imagePath = null
        )
        val card = FsrsCardEntity(
            questionId = "q1",
            due = 0L,
            stability = null,
            difficulty = null,
            step = 0,
            state = State.New.value,
            lastReview = null,
            reps = 0,
            lapses = 0
        )
        dao.insertAllSessions(listOf(session))
        dao.insertAllQuestions(listOf(question))
        dao.insertAllFsrsCards(listOf(card))
    }

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
        override suspend fun addBadge(badge: String) {
            badgeStatusFlow.value = badgeStatusFlow.value + badge
        }
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
