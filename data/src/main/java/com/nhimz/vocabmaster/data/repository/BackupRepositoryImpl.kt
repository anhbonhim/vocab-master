package com.nhimz.vocabmaster.data.repository

import androidx.room.withTransaction
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity
import com.nhimz.vocabmaster.data.model.*
import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.domain.fsrs.State
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: VocabDatabase,
    private val vocabDao: VocabDao,
    private val settingsRepository: SettingsRepository
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val userSettings = UserSettingsBackup(
            currentStreak = settingsRepository.currentStreak.first(),
            longestStreak = settingsRepository.longestStreak.first(),
            availableFreezes = settingsRepository.availableFreezes.first(),
            lastStudyDate = settingsRepository.lastStudyDate.first(),
            xpTotal = settingsRepository.xpTotal.first(),
            badgeStatus = settingsRepository.badgeStatus.first(),
            dailyGoalMinutes = settingsRepository.dailyGoalMinutes.first(),
            desiredRetention = settingsRepository.desiredRetention.first(),
            theme = settingsRepository.theme.first(),
            language = settingsRepository.language.first()
        )

        val cards = vocabDao.getAllCards().map { card ->
            VocabularyCardBackup(
                id = card.id,
                word = card.word,
                definition = card.definition,
                partOfSpeech = card.partOfSpeech,
                difficultyLevel = card.difficultyLevel,
                example = card.example,
                ipa = card.ipa,
                due = card.due,
                stability = card.stability,
                difficulty = card.difficulty,
                interval = card.interval,
                reps = card.reps,
                lapses = card.lapses,
                state = card.state.name,
                lastReview = card.lastReview,
                topic = card.topic,
                audioUrl = card.audioUrl,
                scrambledSentenceData = card.scrambledSentenceData
            )
        }

        val logs = vocabDao.getAllReviewLogsList().map { log ->
            ReviewLogBackup(
                id = log.id,
                cardId = log.cardId,
                rating = log.rating.name,
                elapsed_days = log.elapsed_days,
                scheduled_days = log.scheduled_days,
                stability = log.stability,
                difficulty = log.difficulty,
                state = log.state.name,
                timestamp = log.timestamp
            )
        }

        val payload = BackupPayload(
            userSettings = userSettings,
            vocabularyCards = cards,
            reviewLogs = logs
        )

        json.encodeToString(payload)
    }

    override suspend fun importBackup(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)

            // Convert backup items back to Room entities
            val cardEntities = payload.vocabularyCards.map { card ->
                VocabularyCardEntity(
                    id = card.id,
                    word = card.word,
                    definition = card.definition,
                    partOfSpeech = card.partOfSpeech,
                    difficultyLevel = card.difficultyLevel,
                    example = card.example,
                    ipa = card.ipa,
                    due = card.due,
                    stability = card.stability,
                    difficulty = card.difficulty,
                    interval = card.interval,
                    reps = card.reps,
                    lapses = card.lapses,
                    state = State.valueOf(card.state),
                    lastReview = card.lastReview,
                    topic = card.topic ?: "general",
                    audioUrl = card.audioUrl,
                    scrambledSentenceData = card.scrambledSentenceData
                )
            }

            val logEntities = payload.reviewLogs.map { log ->
                ReviewLogEntity(
                    id = log.id,
                    cardId = log.cardId,
                    rating = Rating.valueOf(log.rating),
                    elapsed_days = log.elapsed_days,
                    scheduled_days = log.scheduled_days,
                    stability = log.stability,
                    difficulty = log.difficulty,
                    state = State.valueOf(log.state),
                    timestamp = log.timestamp
                )
            }

            // Perform Database operations in a transaction
            database.withTransaction {
                vocabDao.deleteAllReviewLogs()
                vocabDao.deleteAllCards()
                vocabDao.insertAllCards(cardEntities)
                vocabDao.insertAllReviewLogs(logEntities)
            }

            // Update user settings in DataStore
            val settings = payload.userSettings
            settingsRepository.setCurrentStreak(settings.currentStreak)
            settingsRepository.setLongestStreak(settings.longestStreak)
            settingsRepository.setAvailableFreezes(settings.availableFreezes)
            settingsRepository.setLastStudyDate(settings.lastStudyDate)
            settingsRepository.setXpTotal(settings.xpTotal)
            settingsRepository.setBadgeStatus(settings.badgeStatus)
            settingsRepository.setDailyGoalMinutes(settings.dailyGoalMinutes)
            settingsRepository.setDesiredRetention(settings.desiredRetention)
            settingsRepository.setTheme(settings.theme)
            settingsRepository.setLanguage(settings.language)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
