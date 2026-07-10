package com.nhimz.vocabmaster.data.repository

import android.content.Context
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity
import com.nhimz.vocabmaster.domain.fsrs.Card
import com.nhimz.vocabmaster.domain.fsrs.State
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import com.nhimz.vocabmaster.domain.model.VocabularyItemWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class VocabularyAssetItem(
    val word: String,
    val level: String,
    val type: String,
    val translation: String,
    val phonetic: String? = null,
    val exampleBeginner: String? = null,
    val exampleBeginnerTranslation: String? = null,
    val exampleIntermediate: String? = null,
    val exampleIntermediateTranslation: String? = null,
    val exampleAdvanced: String? = null,
    val exampleAdvancedTranslation: String? = null
)

@Singleton
class VocabularyRepositoryImpl @Inject constructor(
    private val vocabDao: VocabDao,
    @param:ApplicationContext private val context: Context
) : VocabularyRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private suspend fun checkAndPrepopulate() {
        val count = vocabDao.getCardCount()
        if (count == 0) {
            try {
                context.assets.open("vocabulary.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val assetList = json.decodeFromString<List<VocabularyAssetItem>>(reader.readText())
                        val entities = assetList.map { asset ->
                            val exampleText = when {
                                !asset.exampleBeginner.isNullOrEmpty() -> {
                                    if (!asset.exampleBeginnerTranslation.isNullOrEmpty()) {
                                        "${asset.exampleBeginner} (${asset.exampleBeginnerTranslation})"
                                    } else {
                                        asset.exampleBeginner
                                    }
                                }
                                !asset.exampleIntermediate.isNullOrEmpty() -> {
                                    if (!asset.exampleIntermediateTranslation.isNullOrEmpty()) {
                                        "${asset.exampleIntermediate} (${asset.exampleIntermediateTranslation})"
                                    } else {
                                        asset.exampleIntermediate
                                    }
                                }
                                !asset.exampleAdvanced.isNullOrEmpty() -> {
                                    if (!asset.exampleAdvancedTranslation.isNullOrEmpty()) {
                                        "${asset.exampleAdvanced} (${asset.exampleAdvancedTranslation})"
                                    } else {
                                        asset.exampleAdvanced
                                    }
                                }
                                else -> null
                            }
                            VocabularyCardEntity(
                                word = asset.word,
                                definition = asset.translation,
                                partOfSpeech = asset.type,
                                difficultyLevel = asset.level,
                                example = exampleText,
                                ipa = asset.phonetic,
                                due = LocalDateTime.now(),
                                stability = 0.0,
                                difficulty = 0.0,
                                interval = 0,
                                reps = 0,
                                lapses = 0,
                                state = State.New,
                                lastReview = null
                            )
                        }
                        vocabDao.insertAllCards(entities)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<VocabularyItemWithCard>> {
        val now = LocalDateTime.ofEpochSecond(currentTimestamp, 0, ZoneOffset.UTC)
        return flow {
            checkAndPrepopulate()
            emit(Unit)
        }.flatMapLatest {
            vocabDao.getDueAndNewCards(State.New, now, limit).map { entities ->
                entities.map { it.toDomain() }
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCardsByLevel(level: DifficultyLevel): Flow<List<VocabularyItemWithCard>> {
        return flow {
            checkAndPrepopulate()
            emit(Unit)
        }.flatMapLatest {
            vocabDao.getCardsByLevel(level.name).map { entities ->
                entities.map { it.toDomain() }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCardById(id: Long): VocabularyItemWithCard? = withContext(Dispatchers.IO) {
        checkAndPrepopulate()
        vocabDao.getCardById(id)?.toDomain()
    }

    override suspend fun updateCard(card: Card) = withContext(Dispatchers.IO) {
        val existing = vocabDao.getCardById(card.id) ?: return@withContext
        val updated = existing.copy(
            due = card.due,
            stability = card.stability,
            difficulty = card.difficulty,
            interval = card.interval,
            reps = card.reps,
            lapses = card.lapses,
            state = card.state,
            lastReview = card.lastReview
        )
        vocabDao.updateCard(updated)
    }

    override suspend fun insertCard(word: VocabularyItem, card: Card): Long = withContext(Dispatchers.IO) {
        val entity = VocabularyCardEntity.fromDomain(word, card)
        vocabDao.insertCard(entity)
    }

    override suspend fun insertAll(items: List<VocabularyItemWithCard>) = withContext(Dispatchers.IO) {
        val entities = items.map { VocabularyCardEntity.fromDomain(it.vocabulary, it.card) }
        vocabDao.insertAllCards(entities)
    }

    override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        checkAndPrepopulate()
        vocabDao.getCardCount()
    }
}
