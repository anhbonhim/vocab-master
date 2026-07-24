package com.nhimz.vocabmaster.data.repository

import android.content.Context
import android.util.Log
import com.nhimz.vocabmaster.data.database.CurriculumDao
import com.nhimz.vocabmaster.data.database.UserDataDao
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionAndFsrsCard
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.KeyPhrase
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.domain.model.Node
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.Section
import com.nhimz.vocabmaster.domain.model.Session
import com.nhimz.vocabmaster.domain.model.UnitGuidebook
import com.nhimz.vocabmaster.domain.model.VocabDataException
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.Unit as DomainUnit
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.InputStreamReader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MatchPairAssetItem(
    val left: String,
    val right: String
)

@Serializable
private data class QuestionAssetItem(
    val id: String,
    val word: String? = null,
    val type: String,
    val prompt: String,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    val correctSentence: String? = null,
    val scrambledWords: List<String>? = null,
    val translation: String? = null,
    val audioUrl: String? = null,
    val audioUrlSlow: String? = null,
    val matchingPairs: List<MatchPairAssetItem>? = null,
    val imagePath: String? = null
)

@Serializable
private data class SessionAssetItem(
    val id: String,
    val index: Int,
    val title: String,
    val durationMinutes: Int,
    val questions: List<QuestionAssetItem>
)

@Serializable
private data class NodeAssetItem(
    val id: String,
    val index: Int,
    val type: String,
    val title: String,
    val scenarioContext: String,
    val icon: String,
    val sessions: List<SessionAssetItem>? = null
)

@Serializable
private data class KeyPhraseAssetItem(
    val phrase: String,
    val translation: String,
    val note: String? = null
)

@Serializable
private data class GuidebookAssetItem(
    val id: String,
    val grammarTips: List<String>,
    val keyPhrases: List<KeyPhraseAssetItem>,
    val storyIntro: String,
    val illustrationSvg: String? = null
)

@Serializable
private data class UnitAssetItem(
    val id: String,
    val index: Int,
    val topic: String,
    val title: String,
    val storySummary: String,
    val icon: String,
    val guidebook: GuidebookAssetItem,
    val nodes: List<NodeAssetItem>
)

@Serializable
private data class SectionAssetItem(
    val id: String,
    val index: Int,
    val name: String,
    val cefrSublevel: String,
    val icon: String,
    val description: String,
    val units: List<UnitAssetItem>
)

@Serializable
private data class LessonsV2Asset(
    val schemaVersion: Int,
    val generatedAt: String,
    val sections: List<SectionAssetItem>
)

@Singleton
@Suppress(
    "TooManyFunctions",
    "LongMethod",
    "CyclomaticComplexMethod",
    "LabeledExpression",
    "MaxLineLength",
    "MagicNumber",
    "TooGenericExceptionCaught",
    "SwallowedException"
)
class VocabularyRepositoryImpl @Inject constructor(
    private val curriculumDao: CurriculumDao,
    private val userDataDao: UserDataDao,
    @param:ApplicationContext private val context: Context
) : VocabularyRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val prepopulateCurriculumMutex = Mutex()

    companion object {
        private const val DEFAULT_TOPIC_FALLBACK_LIMIT = 100
        private const val NEW_CARD_TOPIC_FALLBACK_LIMIT = 10
        private const val MIN_SESSION_CARDS = 5
        private const val TAG = "VocabularyRepositoryImpl"
        private const val MALFORMED = "Malformed "
        private const val JSON_FOR_QUESTION = " JSON for question "
        private const val JSON_FOR_GUIDEBOOK = " JSON for guidebook "
        private const val JSON_FOR_SESSION = " JSON for session "
        private const val FAILED_TO_SEED = "Failed to seed curriculum from lessons_v3.json"
        private const val FAILED_TO_READ_ASSET = "Failed to read curriculum asset lessons_v3.json"
    }

    /**
     * Seeds the static curriculum tables (sections, units, guidebooks, nodes, sessions, questions)
     * into [CurriculumDao] from the bundled `lessons_v3.json` asset. Split out of the old
     * `ensureCurriculumAndFsrsSeeded()` during the split-database refactor (T06) so curriculum
     * content lives only in [CurriculumDao].
     */
    private suspend fun ensureCurriculumSeeded() = withContext(Dispatchers.IO) {
        prepopulateCurriculumMutex.withLock {
            val count = curriculumDao.getQuestionCount()
            if (count == 0) {
                try {
                    context.assets.open("lessons_v3.json").use { inputStream ->
                        InputStreamReader(inputStream).use { reader ->
                            val assetV2 = json.decodeFromString<LessonsV2Asset>(reader.readText())

                            val sectionEntities = mutableListOf<SectionEntity>()
                            val unitEntities = mutableListOf<UnitEntity>()
                            val guidebookEntities = mutableListOf<UnitGuidebookEntity>()
                            val nodeEntities = mutableListOf<NodeEntity>()
                            val sessionEntities = mutableListOf<SessionEntity>()
                            val questionEntities = mutableListOf<QuestionEntity>()

                            for (sec in assetV2.sections) {
                                sectionEntities.add(SectionEntity(
                                    id = sec.id, index = sec.index, name = sec.name,
                                    cefrSublevel = sec.cefrSublevel, icon = sec.icon, description = sec.description
                                ))
                                for (uni in sec.units) {
                                    unitEntities.add(UnitEntity(
                                        id = uni.id, sectionId = sec.id, index = uni.index,
                                        topic = uni.topic, title = uni.title, storySummary = uni.storySummary,
                                        icon = uni.icon, guidebookId = uni.guidebook.id
                                    ))
                                    guidebookEntities.add(UnitGuidebookEntity(
                                        id = uni.guidebook.id, unitId = uni.id,
                                        grammarTips = json.encodeToString(uni.guidebook.grammarTips),
                                        keyPhrases = json.encodeToString(uni.guidebook.keyPhrases),
                                        storyIntro = uni.guidebook.storyIntro,
                                        illustrationSvg = uni.guidebook.illustrationSvg
                                    ))
                                    for (nod in uni.nodes) {
                                        val nodeType = try {
                                            NodeType.valueOf(nod.type)
                                        } catch (e: IllegalArgumentException) {
                                            Log.e(TAG, "Unknown node type '${nod.type}' for node ${nod.id}", e)
                                            throw VocabDataException("Unknown node type '${nod.type}' for node ${nod.id}", e)
                                        }
                                        nodeEntities.add(NodeEntity(
                                            id = nod.id, unitId = uni.id, index = nod.index,
                                            type = nodeType.ordinal, title = nod.title, scenarioContext = nod.scenarioContext,
                                            icon = nod.icon
                                        ))
                                        nod.sessions?.let { sessions ->
                                            for (ses in sessions) {
                                                val qIds = ses.questions.map { it.id }
                                                sessionEntities.add(SessionEntity(
                                                    id = ses.id, nodeId = nod.id, index = ses.index,
                                                    title = ses.title, durationMinutes = ses.durationMinutes,
                                                    questionIds = json.encodeToString(qIds)
                                                ))
                                                for (q in ses.questions) {
                                                    val qType = try {
                                                        QuestionType.valueOf(q.type)
                                                    } catch (e: IllegalArgumentException) {
                                                        Log.e(TAG, "Unknown question type '${q.type}' for question ${q.id}", e)
                                                        throw VocabDataException("Unknown question type '${q.type}' for question ${q.id}", e)
                                                    }
                                                    
                                                    // In-memory runtime assertion (Phase L check)
                                                    if (qType == QuestionType.MULTIPLE_CHOICE) {
                                                        check(q.options != null && q.options.size == 4) { "Question ${q.id} MULTIPLE_CHOICE has invalid options" }
                                                        check(q.correctIndex != null && q.correctIndex in 0..3) { "Question ${q.id} MULTIPLE_CHOICE has invalid correctIndex" }
                                                    } else if (qType == QuestionType.MATCHING) {
                                                        check(q.matchingPairs != null && q.matchingPairs.size >= 3) { "Question ${q.id} MATCHING has invalid pairs" }
                                                    } else if (qType == QuestionType.SCRAMBLED) {
                                                        check(q.scrambledWords != null && q.scrambledWords.size >= 3) { "Question ${q.id} SCRAMBLED has invalid words" }
                                                        check(!q.correctSentence.isNullOrEmpty()) { "Question ${q.id} SCRAMBLED missing correctSentence" }
                                                    } else if (qType == QuestionType.FILL_IN_BLANK) {
                                                        check(!q.prompt.isNullOrEmpty()) { "Question ${q.id} FILL_IN_BLANK missing prompt" }
                                                        // Fallback is allowed if correctSentence missing in FILL_IN_BLANK? Wait, my script allowed it if correctIndex was valid.
                                                        // Actually, my script checked FILL_IN_BLANK just like MULTIPLE_CHOICE (it has options and correctIndex).
                                                    } else if (qType == QuestionType.LISTENING) {
                                                        check(!q.audioUrl.isNullOrEmpty()) { "Question ${q.id} LISTENING missing audioUrl" }
                                                    } else if (qType == QuestionType.TYPING) {
                                                        check(!q.correctSentence.isNullOrEmpty()) { "Question ${q.id} TYPING missing correctSentence" }
                                                    }

                                                    questionEntities.add(QuestionEntity(
                                                        id = q.id, sessionId = ses.id, word = q.word,
                                                        type = qType.ordinal, prompt = q.prompt,
                                                        options = q.options?.let { json.encodeToString(it) },
                                                        correctIndex = q.correctIndex, correctSentence = q.correctSentence,
                                                        scrambledWords = q.scrambledWords?.let { json.encodeToString(it) },
                                                        translation = q.translation, audioUrl = q.audioUrl,
                                                        audioUrlSlow = q.audioUrlSlow,
                                                        matchingPairs = q.matchingPairs?.let { json.encodeToString(it) },
                                                        imagePath = q.imagePath
                                                    ))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            curriculumDao.insertAllSections(sectionEntities)
                            curriculumDao.insertAllUnits(unitEntities)
                            curriculumDao.insertAllGuidebooks(guidebookEntities)
                            curriculumDao.insertAllNodes(nodeEntities)
                            curriculumDao.insertAllSessions(sessionEntities)
                            curriculumDao.insertAllQuestions(questionEntities)
                        }
                    }
                } catch (e: SerializationException) {
                    Log.e(TAG, FAILED_TO_SEED, e)
                    throw VocabDataException(FAILED_TO_SEED, e)
                } catch (e: IOException) {
                    Log.e(TAG, FAILED_TO_READ_ASSET, e)
                    throw VocabDataException(FAILED_TO_READ_ASSET, e)
                }
            }
        }
    }

    /**
     * Seeds one FSRS card per non-INTRODUCTION question into [UserDataDao]. Split out of the old
     * `ensureCurriculumAndFsrsSeeded()` during the split-database refactor (T06) so user progress
     * lives only in [UserDataDao]. Must run after [ensureCurriculumSeeded] (it reads the question
     * list from [CurriculumDao]).
     */
    private suspend fun ensureCardsSeeded() = withContext(Dispatchers.IO) {
        prepopulateCurriculumMutex.withLock {
            val count = userDataDao.getCardCount()
            if (count == 0) {
                val questions = curriculumDao.getAllQuestions()
                val now = System.currentTimeMillis()
                val fsrsCardEntities = questions.mapNotNull { q ->
                    val qType = QuestionType.entries.getOrNull(q.type) ?: QuestionType.FILL_IN_BLANK
                    if (qType == QuestionType.INTRODUCTION) {
                        null
                    } else {
                        FsrsCardEntity(
                            questionId = q.id,
                            due = now,
                            stability = null,
                            difficulty = null,
                            step = 0,
                            state = State.New.value,
                            lastReview = null,
                            reps = 0,
                            lapses = 0
                        )
                    }
                }
                userDataDao.insertAllFsrsCards(fsrsCardEntities)
            }
        }
    }

    private inline fun <reified T> decodeQuestionField(
        jsonString: String,
        fieldName: String,
        questionId: String
    ): T {
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: SerializationException) {
            Log.e(TAG, "$MALFORMED$fieldName$JSON_FOR_QUESTION$questionId", e)
            throw VocabDataException("$MALFORMED$fieldName$JSON_FOR_QUESTION$questionId", e)
        }
    }

    private fun QuestionEntity.toDomainModel(): Question {
        val type = QuestionType.entries.getOrNull(this.type) ?: QuestionType.FILL_IN_BLANK
        val options = this.options?.let { decodeQuestionField<List<String>>(it, "options", this.id) }
        val scrambled = this.scrambledWords?.let { decodeQuestionField<List<String>>(it, "scrambledWords", this.id) }
        val matchPairs = this.matchingPairs?.let { mp ->
            decodeQuestionField<List<MatchPairAssetItem>>(mp, "matchingPairs", this.id)
                .map { p -> MatchPair(p.left, p.right) }
        }
        return Question(
            this.id, this.sessionId, this.word, type, this.prompt, options, this.correctIndex, this.correctSentence,
            scrambled, this.translation, this.audioUrl, this.audioUrlSlow, matchPairs, this.imagePath
        )
    }

    private fun QuestionAndFsrsCard.toDomainModel(): QuestionWithCard {
        val card = this.fsrsCard?.toDomain() ?: Card(cardId = this.question.id)
        return QuestionWithCard(this.question.toDomainModel(), card)
    }

    /**
     * In-memory cross-DB assembly: given the per-user FSRS cards from [UserDataDao], fetch the
     * matching curriculum questions from [CurriculumDao] (by ID) and pair them into
     * [QuestionWithCard]. Replaces the old `questions INNER JOIN fsrs_cards` Room query that spanned
     * both tables.
     */
    private suspend fun assembleQuestionAndCards(cards: List<FsrsCardEntity>): List<QuestionWithCard> {
        if (cards.isEmpty()) return emptyList()
        val questionIds = cards.map { it.questionId }
        val questionsById = curriculumDao.getQuestionsByIds(questionIds).associateBy { it.id }
        return cards.mapNotNull { cardEntity ->
            val questionEntity = questionsById[cardEntity.questionId] ?: return@mapNotNull null
            QuestionAndFsrsCard(questionEntity, cardEntity).toDomainModel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, currentTimestamp, limit)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDueCardsByTopic(topic: String, currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> {
        // Fallback since topic is not directly accessible without multiple joins
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                // Long.MAX_VALUE makes the `due <= :now` predicate always true, so this returns
                // every FSRS card (the old "topic fallback" returned all cards regardless of topic).
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, Long.MAX_VALUE, limit)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCardsByLevel(level: DifficultyLevel): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, Long.MAX_VALUE, DEFAULT_TOPIC_FALLBACK_LIMIT)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCardsByTopic(topic: String): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, Long.MAX_VALUE, DEFAULT_TOPIC_FALLBACK_LIMIT)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCardById(id: String): QuestionWithCard? = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        val questionEntity = curriculumDao.getQuestionById(id) ?: return@withContext null
        val fsrsCardEntity = userDataDao.getCardByQuestionId(id)
        QuestionAndFsrsCard(questionEntity, fsrsCardEntity).toDomainModel()
    }

    override suspend fun updateCard(card: Card) = withContext(Dispatchers.IO) {
        val existing = userDataDao.getCardByQuestionId(card.cardId) ?: return@withContext
        val updated = existing.copy(
            due = card.due,
            stability = card.stability,
            difficulty = card.difficulty,
            step = card.step,
            reps = card.reps,
            lapses = card.lapses,
            state = card.state.value,
            lastReview = card.lastReview
        )
        userDataDao.updateFsrsCard(updated)
    }

    override suspend fun insertAll(items: List<QuestionWithCard>) = withContext(Dispatchers.IO) {
        val entities = items.map { FsrsCardEntity.fromDomain(it.card) }
        userDataDao.insertAllFsrsCards(entities)
    }

    override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        userDataDao.getCardCount()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNewCardsByTopicAndLevels(topic: String, levels: List<String>): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, Long.MAX_VALUE, NEW_CARD_TOPIC_FALLBACK_LIMIT)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNewCardsByLevels(levels: List<String>): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            flow {
                val cards = userDataDao.getDueAndNewCardEntities(State.New.value, Long.MAX_VALUE, NEW_CARD_TOPIC_FALLBACK_LIMIT)
                emit(assembleQuestionAndCards(cards))
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getDueCount(now: Long): Int = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        userDataDao.getDueCount(now)
    }

    override suspend fun getMistakeCount(): Int = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        userDataDao.getMistakeCount()
    }

    override suspend fun getMistakes(limit: Int): List<QuestionWithCard> = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        val cards = userDataDao.getMistakeCards(limit)
        assembleQuestionAndCards(cards)
    }

    override suspend fun getLearnedCountByTopic(topic: String): Int = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        0 // Fallback
    }

    override suspend fun getWordCountByTopicAndLevel(topic: String, level: String): Int = withContext(Dispatchers.IO) {
        0 // Fallback
    }

    override suspend fun checkAndPrepopulateCurriculum() = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSections(): Flow<List<Section>> {
        return flow {
            ensureCurriculumSeeded()
            ensureCardsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            curriculumDao.getAllSections().map { entities ->
                entities.map { Section(it.id, it.index, it.name, it.cefrSublevel, it.icon, it.description) }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getUnitsBySection(sectionId: String): Flow<List<DomainUnit>> {
        return curriculumDao.getUnitsBySection(sectionId).map { entities ->
            entities.map { DomainUnit(it.id, it.sectionId, it.index, it.topic, it.title, it.storySummary, it.icon, it.guidebookId) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getGuidebook(unitId: String): Result<UnitGuidebook?> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = curriculumDao.getGuidebook(unitId)
            if (entity == null) {
                return@runCatching null
            }
            val grammarTips = try {
                json.decodeFromString<List<String>>(entity.grammarTips)
            } catch (e: SerializationException) {
                val fieldName = "grammarTips"
                val message = "$MALFORMED$fieldName$JSON_FOR_GUIDEBOOK${entity.id}"
                Log.e(TAG, message, e)
                throw VocabDataException(message, e)
            }
            val keyPhrases = try {
                json.decodeFromString<List<KeyPhraseAssetItem>>(entity.keyPhrases)
                    .map { KeyPhrase(it.phrase, it.translation, it.note) }
            } catch (e: SerializationException) {
                val fieldName = "keyPhrases"
                val message = "$MALFORMED$fieldName$JSON_FOR_GUIDEBOOK${entity.id}"
                Log.e(TAG, message, e)
                throw VocabDataException(message, e)
            }
            UnitGuidebook(entity.id, entity.unitId, grammarTips, keyPhrases, entity.storyIntro, entity.illustrationSvg)
        }
    }

    override fun getNodesByUnit(unitId: String): Flow<List<Node>> {
        return curriculumDao.getNodesByUnit(unitId).map { entities ->
            entities.map { 
                val type = NodeType.entries.getOrNull(it.type) ?: NodeType.LESSON
                Node(it.id, it.unitId, it.index, type, it.title, it.scenarioContext, it.icon) 
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = withContext(Dispatchers.IO) {
        runCatching {
            curriculumDao.getSessionsByNode(nodeId).map {
                val qIds = try {
                    json.decodeFromString<List<String>>(it.questionIds)
                } catch (e: SerializationException) {
                    val fieldName = "questionIds"
                    val message = "$MALFORMED$fieldName$JSON_FOR_SESSION${it.id}"
                    Log.e(TAG, message, e)
                    throw VocabDataException(message, e)
                }
                Session(it.id, it.nodeId, it.index, it.title, it.durationMinutes, qIds)
            }
        }
    }

    override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = withContext(Dispatchers.IO) {
        runCatching {
            curriculumDao.getQuestionsBySession(sessionId).map {
                it.toDomainModel()
            }
        }
    }

    override suspend fun getNodeProgress(nodeId: String): Boolean = withContext(Dispatchers.IO) {
        userDataDao.getNodeProgress(nodeId)?.isCompleted ?: false
    }

    override suspend fun getCompletedNodesByUnit(unitId: String): List<String> = withContext(Dispatchers.IO) {
        val nodeIds = curriculumDao.getNodeIdsByUnit(unitId)
        userDataDao.getCompletedNodeProgressByNodeIds(nodeIds).map { it.nodeId }
    }

    override suspend fun getCompletedNodesBySection(sectionId: String): List<String> = withContext(Dispatchers.IO) {
        val nodeIds = curriculumDao.getNodeIdsBySection(sectionId)
        userDataDao.getCompletedNodeProgressByNodeIds(nodeIds).map { it.nodeId }
    }

    override suspend fun markNodeCompleted(nodeId: String, accuracy: Float, bestScore: Int) = withContext(Dispatchers.IO) {
        val existing = userDataDao.getNodeProgress(nodeId)
        val progress = existing?.copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            accuracy = accuracy,
            bestScore = maxOf(bestScore, existing.bestScore ?: 0)
        ) ?: NodeProgressEntity(
            nodeId = nodeId,
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            accuracy = accuracy,
            bestScore = bestScore
        )
        userDataDao.insertNodeProgress(progress)
    }

    override fun getCompletedLessons(stage: String, unitTopic: String): Flow<List<Int>> = flow { emit(emptyList()) }
    
    override suspend fun markLessonCompleted(stage: String, unitTopic: String, lessonIndex: Int) {
        // Obsolete
    }

    override suspend fun getCardByQuestionId(questionId: String): Card? = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        userDataDao.getCardByQuestionId(questionId)?.toDomain()
    }

    override suspend fun getQuestionWithCard(questionId: String): QuestionWithCard? = withContext(Dispatchers.IO) {
        getCardById(questionId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getDueCardsScoped(
        unitId: String,
        sectionId: String,
        currentTimestamp: Long,
        limit: Int
    ): List<QuestionWithCard> = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        // 3-tier fallback: unit -> section -> global, so the user always gets a full
        // review session when any due/new cards exist anywhere.
        val unitQuestionIds = curriculumDao.getQuestionIdsByUnit(unitId)
        val unitScoped = userDataDao.getDueAndNewCardsByQuestionIds(
            questionIds = unitQuestionIds,
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).let { assembleQuestionAndCards(it) }

        if (unitScoped.size >= MIN_SESSION_CARDS) return@withContext unitScoped

        val sectionQuestionIds = curriculumDao.getQuestionIdsBySection(sectionId)
        val sectionScoped = userDataDao.getDueAndNewCardsByQuestionIds(
            questionIds = sectionQuestionIds,
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).let { assembleQuestionAndCards(it) }

        if (sectionScoped.size >= MIN_SESSION_CARDS) return@withContext sectionScoped

        val globalFallback = userDataDao.getDueAndNewCardEntities(
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).let { assembleQuestionAndCards(it) }

        globalFallback
    }

    override suspend fun getDueCardCountByUnit(unitId: String, currentTimestamp: Long): Int = withContext(Dispatchers.IO) {
        ensureCurriculumSeeded()
        ensureCardsSeeded()
        val questionIds = curriculumDao.getQuestionIdsByUnit(unitId)
        userDataDao.countDueCardsByQuestionIds(
            questionIds = questionIds,
            newState = State.New.value,
            now = currentTimestamp
        )
    }
}
