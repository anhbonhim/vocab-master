package com.nhimz.vocabmaster.data.sync

import android.content.Context
import android.util.Log
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.remote.ApiClient
import com.nhimz.vocabmaster.data.remote.ReviewLogDto
import com.nhimz.vocabmaster.data.remote.SyncPayload
import com.nhimz.vocabmaster.data.remote.UserSettingsDto
import com.nhimz.vocabmaster.data.remote.VocabularyCardDto
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "StringLiteralDuplication",
    "LabeledExpression",
    "TooGenericExceptionCaught",
    "VariableNaming"
)
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vocabDao: VocabDao,
    private val settingsRepository: SettingsRepository,
    private val apiClient: ApiClient
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val LAST_SYNC_KEY = "last_sync_timestamp"

    /**
     * Pulls the most recent settings, review logs, and FSRS card state from
     * the cloud and merges them into the local Room database; pushes any
     * local deltas back to the server.
     *
     * Returns `true` when push + pull both succeed, `false` for any
     * recoverable error (network failure, 5xx, malformed payload, etc.).
     * Coroutine cancellation is rethrown so callers can still observe it
     * via structured concurrency.
     */
    suspend fun sync(): Boolean {
        return try {
            val lastSync = syncPrefs.getLong(LAST_SYNC_KEY, 0L)
            
            val dailyGoal = settingsRepository.dailyGoalXp.first()
            val currentStreak = settingsRepository.currentStreak.first()
            val longestStreak = settingsRepository.longestStreak.first()
            val freezes = settingsRepository.availableFreezes.first()
            val lastStudy = settingsRepository.lastStudyDate.first()
            val xp = settingsRepository.xpTotal.first()
            val retention = settingsRepository.desiredRetention.first()
            val theme = settingsRepository.theme.first()
            val lang = settingsRepository.language.first()
            val placement = settingsRepository.placementLevel.first()
            val topic = settingsRepository.selectedTopic.first()
            
            val userSettingsDto = UserSettingsDto(
                dailyGoalXp = dailyGoal,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                availableFreezes = freezes,
                lastStudyDate = lastStudy,
                xpTotal = xp,
                desiredRetention = retention,
                theme = theme,
                language = lang,
                placementLevel = placement,
                selectedTopic = topic
            )
            
            val activeCards = vocabDao.getAllCards().filter { it.reps > 0 }
            val cardsDtos = activeCards.map {
                VocabularyCardDto(
                    questionId = it.questionId,
                    due = Instant.ofEpochMilli(it.due).atOffset(ZoneOffset.UTC).format(formatter),
                    stability = it.stability ?: 0.0,
                    difficulty = it.difficulty ?: 0.0,
                    // TODO(SYNC-02, Phase 4): server contract for v3 card shape; interval removed from v8 schema.
                    interval = 0,
                    reps = it.reps,
                    lapses = it.lapses,
                    state = it.state,
                    lastReview = it.lastReview?.let { millis ->
                        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                    },
                    lastModified = System.currentTimeMillis()
                )
            }
            
            // Map review logs safely resolving word from DB
            val logs = vocabDao.getAllReviewLogsList()
            val logsDtos = logs.mapNotNull {
                val card = vocabDao.getCardByQuestionId(it.cardId) ?: return@mapNotNull null
                ReviewLogDto(
                    questionId = card.questionId,
                    rating = it.rating,
                    // TODO(SYNC-02, Phase 4): server contract for v3 log shape; these telemetry
                    // fields are no longer stored locally and are sent as placeholders.
                    elapsed_days = 0,
                    scheduled_days = 0,
                    stability = 0.0,
                    difficulty = 0.0,
                    state = 0,
                    timestamp = Instant.ofEpochMilli(it.reviewDatetime).atOffset(ZoneOffset.UTC).format(formatter)
                )
            }
            
            val payload = SyncPayload(
                userSettings = userSettingsDto,
                vocabularyCards = cardsDtos,
                reviewLogs = logsDtos,
                lastSyncTimestamp = lastSync
            )
            
            val pushResponse = apiClient.syncApi.pushSync(payload)
            if (!pushResponse.isSuccessful) {
                Log.e("SyncManager", "Sync Push failed: ${pushResponse.code()}")
                return false
            }
            
            val pullResponse = apiClient.syncApi.pullSync(lastSync)
            if (pullResponse.isSuccessful && pullResponse.body() != null) {
                val pulledPayload = pullResponse.body()!!
                
                val ps = pulledPayload.userSettings
                settingsRepository.updateDailyGoal(ps.dailyGoalXp)
                settingsRepository.setCurrentStreak(ps.currentStreak)
                settingsRepository.setLongestStreak(ps.longestStreak)
                settingsRepository.setAvailableFreezes(ps.availableFreezes)
                settingsRepository.setLastStudyDate(ps.lastStudyDate)
                settingsRepository.setXpTotal(ps.xpTotal)
                settingsRepository.setDesiredRetention(ps.desiredRetention)
                settingsRepository.setTheme(ps.theme)
                settingsRepository.setLanguage(ps.language)
                ps.placementLevel?.let { settingsRepository.setPlacementLevel(it) }
                settingsRepository.setSelectedTopic(ps.selectedTopic)
                
                val allCardsList = vocabDao.getAllCards()
                for (c in pulledPayload.vocabularyCards) {
                    val existing = allCardsList.find { it.questionId == c.questionId }
                    val dueLdt = LocalDateTime.parse(c.due, formatter)
                    val lastReviewLdt = c.lastReview?.let { LocalDateTime.parse(it, formatter) }
                    
                    val stateEnum = State.entries.find { it.value == c.state } ?: State.New
                        
                    if (existing != null) {
                        val updated = existing.copy(
                            due = dueLdt.toInstant(ZoneOffset.UTC).toEpochMilli(),
                            stability = c.stability,
                            difficulty = c.difficulty,
                            step = existing.step,
                            reps = c.reps,
                            lapses = c.lapses,
                            state = stateEnum.value,
                            lastReview = lastReviewLdt?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                        )
                        vocabDao.updateFsrsCard(updated)
                    } else {
                        val newCard = FsrsCardEntity(
                            questionId = c.questionId,
                            due = dueLdt.toInstant(ZoneOffset.UTC).toEpochMilli(),
                            stability = c.stability,
                            difficulty = c.difficulty,
                            step = 0,
                            reps = c.reps,
                            lapses = c.lapses,
                            state = stateEnum.value,
                            lastReview = lastReviewLdt?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                        )
                        vocabDao.insertCard(newCard)
                    }
                }

                // Process pulled review logs
                val refreshedCardsList = vocabDao.getAllCards()
                for (logDto in pulledPayload.reviewLogs) {
                    val card = refreshedCardsList.find { it.questionId == logDto.questionId }
                    if (card != null) {
                        val logTime = LocalDateTime.parse(logDto.timestamp, formatter)
                            .toInstant(ZoneOffset.UTC).toEpochMilli()
                        val existingLogs = vocabDao.getReviewLogs(card.questionId)
                        val alreadyExists = existingLogs.any { it.reviewDatetime == logTime }
                        if (!alreadyExists) {
                            val ratingEnum = Rating.entries.find { it.value == logDto.rating } ?: Rating.Good
                            val stateEnum = State.entries.find { it.value == logDto.state } ?: State.New
                            
                            val logEntity = ReviewLogEntity(
                                cardId = card.questionId,
                                rating = ratingEnum.value,
                                reviewDatetime = logTime,
                                reviewDuration = null
                            )
                            vocabDao.insertReviewLog(logEntity)
                        }
                    }
                }
                
                syncPrefs.edit().putLong(LAST_SYNC_KEY, pulledPayload.lastSyncTimestamp).apply()
                true
            } else {
                Log.e("SyncManager", "Sync Pull failed: ${pullResponse.code()}")
                false
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Rethrow cancellation so structured concurrency can react.
            throw e
        } catch (e: java.io.IOException) {
            // OkHttp / Retrofit network failures (timeout, unknown host, lost
            // connection, etc.) — recoverable; the caller can show a Retry UI.
            Log.e("SyncManager", "Network failure during sync", e)
            false
        } catch (e: retrofit2.HttpException) {
            // Non-2xx response that somehow escaped the isSuccessful branches
            // (e.g. RxJava-style usage); treat as recoverable failure.
            Log.e("SyncManager", "HTTP error during sync: ${e.code()}", e)
            false
        } catch (e: Exception) {
            Log.e("SyncManager", "Synchronization error", e)
            false
        }
    }
}
