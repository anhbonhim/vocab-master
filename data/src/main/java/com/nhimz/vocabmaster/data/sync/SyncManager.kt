package com.nhimz.vocabmaster.data.sync

import android.content.Context
import android.util.Log
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity
import com.nhimz.vocabmaster.data.remote.ApiClient
import com.nhimz.vocabmaster.data.remote.ReviewLogDto
import com.nhimz.vocabmaster.data.remote.SyncPayload
import com.nhimz.vocabmaster.data.remote.UserSettingsDto
import com.nhimz.vocabmaster.data.remote.VocabularyCardDto
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vocabDao: VocabDao,
    private val settingsRepository: SettingsRepository,
    private val apiClient: ApiClient
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val LAST_SYNC_KEY = "last_sync_timestamp"

    suspend fun sync(): Boolean {
        return try {
            val lastSync = syncPrefs.getLong(LAST_SYNC_KEY, 0L)
            
            val dailyGoal = settingsRepository.dailyGoalMinutes.first()
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
                dailyGoalMinutes = dailyGoal,
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
                    word = it.word,
                    due = it.due.format(formatter),
                    stability = it.stability,
                    difficulty = it.difficulty,
                    interval = it.interval,
                    reps = it.reps,
                    lapses = it.lapses,
                    state = it.state.value,
                    lastReview = it.lastReview?.format(formatter),
                    lastModified = System.currentTimeMillis()
                )
            }
            
            // Map review logs safely resolving word from DB
            val logs = vocabDao.getAllReviewLogsList()
            val logsDtos = logs.mapNotNull {
                val card = vocabDao.getCardById(it.cardId) ?: return@mapNotNull null
                ReviewLogDto(
                    word = card.word,
                    rating = it.rating.value,
                    elapsed_days = it.elapsed_days,
                    scheduled_days = it.scheduled_days,
                    stability = it.stability,
                    difficulty = it.difficulty,
                    state = it.state.value,
                    timestamp = it.timestamp.format(formatter)
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
                settingsRepository.setDailyGoalMinutes(ps.dailyGoalMinutes)
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
                
                for (c in pulledPayload.vocabularyCards) {
                    val existing = vocabDao.getAllCards().find { it.word == c.word }
                    val dueLdt = LocalDateTime.parse(c.due, formatter)
                    val lastReviewLdt = c.lastReview?.let { LocalDateTime.parse(it, formatter) }
                    
                    val stateEnum = com.nhimz.vocabmaster.domain.fsrs.State.values().find { it.value == c.state } 
                        ?: com.nhimz.vocabmaster.domain.fsrs.State.New
                        
                    if (existing != null) {
                        val updated = existing.copy(
                            due = dueLdt,
                            stability = c.stability,
                            difficulty = c.difficulty,
                            interval = c.interval,
                            reps = c.reps,
                            lapses = c.lapses,
                            state = stateEnum,
                            lastReview = lastReviewLdt
                        )
                        vocabDao.updateCard(updated)
                    }
                }
                
                syncPrefs.edit().putLong(LAST_SYNC_KEY, pulledPayload.lastSyncTimestamp).apply()
                true
            } else {
                Log.e("SyncManager", "Sync Pull failed: ${pullResponse.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Synchronization error", e)
            false
        }
    }
}
