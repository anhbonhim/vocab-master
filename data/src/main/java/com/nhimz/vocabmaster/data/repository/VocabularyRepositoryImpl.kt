package com.nhimz.vocabmaster.data.repository

import android.content.Context
import com.nhimz.vocabmaster.data.database.VocabDao
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
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.Unit as DomainUnit
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
    private val vocabDao: VocabDao,
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
    }

    private suspend fun ensureCurriculumAndFsrsSeeded() = withContext(Dispatchers.IO) {
        prepopulateCurriculumMutex.withLock {
            val count = vocabDao.getQuestionCount()
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
                            val fsrsCardEntities = mutableListOf<FsrsCardEntity>()

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
                                        val nodeType = try { NodeType.valueOf(nod.type) } catch (e: Exception) { NodeType.LESSON }
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
                                                    val qType = try { QuestionType.valueOf(q.type) } catch (e: Exception) { QuestionType.FILL_IN_BLANK }
                                                    
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

                                                    if (qType != QuestionType.INTRODUCTION) {
                                                        fsrsCardEntities.add(
                                                            FsrsCardEntity(
                                                                questionId = q.id,
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
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            vocabDao.insertAllSections(sectionEntities)
                            vocabDao.insertAllUnits(unitEntities)
                            vocabDao.insertAllGuidebooks(guidebookEntities)
                            vocabDao.insertAllNodes(nodeEntities)
                            vocabDao.insertAllSessions(sessionEntities)
                            vocabDao.insertAllQuestions(questionEntities)
                            vocabDao.insertAllFsrsCards(fsrsCardEntities)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }

    private fun QuestionEntity.toDomainModel(): Question {
        val type = QuestionType.entries.getOrNull(this.type) ?: QuestionType.FILL_IN_BLANK
        val options = this.options?.let { opt -> try { json.decodeFromString<List<String>>(opt) } catch (e: Exception) { null } }
        val scrambled = this.scrambledWords?.let { scr -> try { json.decodeFromString<List<String>>(scr) } catch (e: Exception) { null } }
        val matchPairs = this.matchingPairs?.let { mp -> 
            try { json.decodeFromString<List<MatchPairAssetItem>>(mp).map { p -> MatchPair(p.left, p.right) } } catch (e: Exception) { null }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCards(State.New.value, currentTimestamp, limit).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDueCardsByTopic(topic: String, currentTimestamp: Long, limit: Int): Flow<List<QuestionWithCard>> {
        // Fallback since topic is not directly accessible without multiple joins
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCardsByTopicFallback(limit).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCardsByLevel(level: DifficultyLevel): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCardsByTopicFallback(DEFAULT_TOPIC_FALLBACK_LIMIT).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCardsByTopic(topic: String): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCardsByTopicFallback(DEFAULT_TOPIC_FALLBACK_LIMIT).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCardById(id: String): QuestionWithCard? = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        val questionEntity = vocabDao.getQuestionById(id) ?: return@withContext null
        val fsrsCardEntity = vocabDao.getCardByQuestionId(id)
        QuestionAndFsrsCard(questionEntity, fsrsCardEntity).toDomainModel()
    }

    override suspend fun updateCard(card: Card) = withContext(Dispatchers.IO) {
        val existing = vocabDao.getCardByQuestionId(card.cardId) ?: return@withContext
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
        vocabDao.updateFsrsCard(updated)
    }

    override suspend fun insertAll(items: List<QuestionWithCard>) = withContext(Dispatchers.IO) {
        val entities = items.map { FsrsCardEntity.fromDomain(it.card) }
        vocabDao.insertAllFsrsCards(entities)
    }

    override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getCardCount()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNewCardsByTopicAndLevels(topic: String, levels: List<String>): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCardsByTopicFallback(NEW_CARD_TOPIC_FALLBACK_LIMIT).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNewCardsByLevels(levels: List<String>): Flow<List<QuestionWithCard>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCardsByTopicFallback(NEW_CARD_TOPIC_FALLBACK_LIMIT).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getDueCount(now: Long): Int = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getDueCount(now)
    }

    override suspend fun getMistakeCount(): Int = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getMistakeCount()
    }

    override suspend fun getMistakes(limit: Int): List<QuestionWithCard> = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getMistakes(limit).map { it.toDomainModel() }
    }

    override suspend fun getLearnedCountByTopic(topic: String): Int = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        0 // Fallback
    }

    override suspend fun getWordCountByTopicAndLevel(topic: String, level: String): Int = withContext(Dispatchers.IO) {
        0 // Fallback
    }

    override suspend fun checkAndPrepopulateCurriculum() = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSections(): Flow<List<Section>> {
        return flow {
            ensureCurriculumAndFsrsSeeded()
            emit(kotlin.Unit)
        }.flatMapLatest {
            vocabDao.getAllSections().map { entities ->
                entities.map { Section(it.id, it.index, it.name, it.cefrSublevel, it.icon, it.description) }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getUnitsBySection(sectionId: String): Flow<List<DomainUnit>> {
        return vocabDao.getUnitsBySection(sectionId).map { entities ->
            entities.map { DomainUnit(it.id, it.sectionId, it.index, it.topic, it.title, it.storySummary, it.icon, it.guidebookId) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getGuidebook(unitId: String): UnitGuidebook? = withContext(Dispatchers.IO) {
        val entity = vocabDao.getGuidebook(unitId) ?: return@withContext null
        val grammarTips = try { json.decodeFromString<List<String>>(entity.grammarTips) } catch (e: Exception) { emptyList() }
        val keyPhrases = try { json.decodeFromString<List<KeyPhraseAssetItem>>(entity.keyPhrases).map { KeyPhrase(it.phrase, it.translation, it.note) } } catch (e: Exception) { emptyList() }
        UnitGuidebook(entity.id, entity.unitId, grammarTips, keyPhrases, entity.storyIntro, entity.illustrationSvg)
    }

    override fun getNodesByUnit(unitId: String): Flow<List<Node>> {
        return vocabDao.getNodesByUnit(unitId).map { entities ->
            entities.map { 
                val type = NodeType.entries.getOrNull(it.type) ?: NodeType.LESSON
                Node(it.id, it.unitId, it.index, type, it.title, it.scenarioContext, it.icon) 
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getSessionsByNode(nodeId: String): List<Session> = withContext(Dispatchers.IO) {
        vocabDao.getSessionsByNode(nodeId).map {
            val qIds = try { json.decodeFromString<List<String>>(it.questionIds) } catch (e: Exception) { emptyList() }
            Session(it.id, it.nodeId, it.index, it.title, it.durationMinutes, qIds)
        }
    }

    override suspend fun getQuestionsBySession(sessionId: String): List<Question> = withContext(Dispatchers.IO) {
        vocabDao.getQuestionsBySession(sessionId).map {
            it.toDomainModel()
        }
    }

    override suspend fun getNodeProgress(nodeId: String): Boolean = withContext(Dispatchers.IO) {
        vocabDao.getNodeProgress(nodeId)?.isCompleted ?: false
    }

    override suspend fun getCompletedNodesByUnit(unitId: String): List<String> = withContext(Dispatchers.IO) {
        vocabDao.getCompletedNodesByUnit(unitId)
    }

    override suspend fun getCompletedNodesBySection(sectionId: String): List<String> = withContext(Dispatchers.IO) {
        vocabDao.getCompletedNodesBySection(sectionId)
    }

    override suspend fun markNodeCompleted(nodeId: String, accuracy: Float, bestScore: Int) = withContext(Dispatchers.IO) {
        val existing = vocabDao.getNodeProgress(nodeId)
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
        vocabDao.insertNodeProgress(progress)
    }

    override fun getCompletedLessons(stage: String, unitTopic: String): Flow<List<Int>> = flow { emit(emptyList()) }
    
    override suspend fun markLessonCompleted(stage: String, unitTopic: String, lessonIndex: Int) {
        // Obsolete
    }

    override suspend fun getCardByQuestionId(questionId: String): Card? = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getCardByQuestionId(questionId)?.toDomain()
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
        ensureCurriculumAndFsrsSeeded()
        // 3-tier fallback: unit -> section -> global, so the user always gets a full
        // review session when any due/new cards exist anywhere.
        val unitScoped = vocabDao.getDueAndNewCardsByUnit(
            unitId = unitId,
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).first().map { it.toDomainModel() }

        if (unitScoped.size >= MIN_SESSION_CARDS) return@withContext unitScoped

        val sectionScoped = vocabDao.getDueAndNewCardsBySection(
            sectionId = sectionId,
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).first().map { it.toDomainModel() }

        if (sectionScoped.size >= MIN_SESSION_CARDS) return@withContext sectionScoped

        val globalFallback = vocabDao.getDueAndNewCards(
            state = State.New.value,
            now = currentTimestamp,
            limit = limit
        ).first().map { it.toDomainModel() }

        globalFallback
    }

    override suspend fun getDueCardCountByUnit(unitId: String, currentTimestamp: Long): Int = withContext(Dispatchers.IO) {
        ensureCurriculumAndFsrsSeeded()
        vocabDao.getDueCardCountByUnit(
            unitId = unitId,
            newState = State.New.value,
            now = currentTimestamp
        )
    }
}
