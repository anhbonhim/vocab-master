package com.nhimz.vocabmaster.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.nhimz.vocabmaster.data.database.UserDataDao
import com.nhimz.vocabmaster.data.database.UserDataDatabase
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.model.AppBackup
import com.nhimz.vocabmaster.data.model.FlaggedItemBackup
import com.nhimz.vocabmaster.data.model.FsrsCardBackup
import com.nhimz.vocabmaster.data.model.ReviewLogBackup
import com.nhimz.vocabmaster.data.model.UserSettingsBackup
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Singleton
@Suppress("LongMethod", "LabeledExpression", "TooGenericExceptionCaught")
class BackupRepositoryImpl @Inject constructor(
    private val userDataDatabase: UserDataDatabase,
    private val userDataDao: UserDataDao,
    private val settingsRepository: SettingsRepository
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    companion object {
        private const val MIN_SUPPORTED_BACKUP_VERSION = 3
        private const val TAG = "BackupRepositoryImpl"
        private const val IMPORT_FAILED_PREFIX = "Backup import failed: "
        private const val MALFORMED_JSON = "malformed JSON"
        private const val INVALID_ENUM = "invalid enum or argument value"
        private const val INVALID_TIMESTAMP = "invalid timestamp format"
    }

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val userSettings = UserSettingsBackup(
            currentStreak = settingsRepository.currentStreak.first(),
            longestStreak = settingsRepository.longestStreak.first(),
            availableFreezes = settingsRepository.availableFreezes.first(),
            lastStudyDate = settingsRepository.lastStudyDate.first(),
            xpTotal = settingsRepository.xpTotal.first(),
            badgeStatus = settingsRepository.badgeStatus.first(),
            dailyGoalXp = settingsRepository.dailyGoalXp.first(),
            desiredRetention = settingsRepository.desiredRetention.first(),
            theme = settingsRepository.theme.first(),
            language = settingsRepository.language.first()
        )

        val cards = userDataDao.getAllCards().map { card ->
            FsrsCardBackup(
                questionId = card.questionId,
                state = card.state,
                step = card.step,
                stability = card.stability,
                difficulty = card.difficulty,
                due = card.due,
                lastReview = card.lastReview,
                reps = card.reps,
                lapses = card.lapses
            )
        }

        val logs = userDataDao.getAllReviewLogsList().map { log ->
            ReviewLogBackup(
                cardId = log.cardId,
                rating = Rating.entries.firstOrNull { it.value == log.rating }?.name ?: Rating.Good.name,
                reviewDatetime = Instant.ofEpochMilli(log.reviewDatetime).atOffset(ZoneOffset.UTC).format(formatter),
                reviewDuration = log.reviewDuration
            )
        }

        val flagged = userDataDao.getAllFlaggedItems().map { flag ->
            FlaggedItemBackup(
                questionId = flag.questionId,
                issueType = flag.issueType,
                details = flag.details,
                timestamp = flag.timestamp
            )
        }

        val backup = AppBackup(
            version = 3,
            timestamp = System.currentTimeMillis(),
            settings = userSettings,
            cards = cards,
            reviewLogs = logs,
            flaggedItems = flagged
        )

        json.encodeToString(backup)
    }

    override suspend fun importBackup(backupJson: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backup = json.decodeFromString<AppBackup>(backupJson)

            // Validate that we have the v3 FSRS format.
            if (backup.version < MIN_SUPPORTED_BACKUP_VERSION) {
                // Reject v2 and earlier: they carry pre-port scheduling data.
                return@withContext Result.success(false)
            }

            userDataDatabase.withTransaction {
                // 1. Clear existing user data (Keep curriculum data!)
                userDataDao.deleteAllCards()
                userDataDao.deleteAllReviewLogs()
                userDataDao.deleteAllFlaggedItems()

                // 2. Restore settings
                settingsRepository.setCurrentStreak(backup.settings.currentStreak)
                settingsRepository.setLongestStreak(backup.settings.longestStreak)
                settingsRepository.setAvailableFreezes(backup.settings.availableFreezes)
                settingsRepository.setLastStudyDate(backup.settings.lastStudyDate)
                settingsRepository.setXpTotal(backup.settings.xpTotal)
                settingsRepository.setBadgeStatus(backup.settings.badgeStatus)
                settingsRepository.updateDailyGoal(backup.settings.dailyGoalXp)
                settingsRepository.setDesiredRetention(backup.settings.desiredRetention)
                settingsRepository.setTheme(backup.settings.theme)
                settingsRepository.setLanguage(backup.settings.language)

                // 3. Restore cards
                val cardEntities = backup.cards.map { card ->
                    FsrsCardEntity(
                        questionId = card.questionId,
                        state = card.state,
                        step = card.step,
                        stability = card.stability,
                        difficulty = card.difficulty,
                        due = card.due,
                        lastReview = card.lastReview,
                        reps = card.reps,
                        lapses = card.lapses
                    )
                }
                userDataDao.insertAllFsrsCards(cardEntities)

                // 4. Restore logs
                val logEntities = backup.reviewLogs.map { log ->
                    ReviewLogEntity(
                        id = 0, // Let autoGenerate assign a new ID to avoid PK conflict
                        cardId = log.cardId,
                        rating = Rating.valueOf(log.rating).value,
                        reviewDatetime = LocalDateTime.parse(log.reviewDatetime, formatter)
                            .toInstant(ZoneOffset.UTC).toEpochMilli(),
                        reviewDuration = log.reviewDuration
                    )
                }
                userDataDao.insertAllReviewLogs(logEntities)

                // 5. Restore flagged items
                val flaggedEntities = backup.flaggedItems.map { flag ->
                    com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity(
                        questionId = flag.questionId,
                        word = null,
                        issueType = flag.issueType,
                        details = flag.details,
                        timestamp = flag.timestamp
                    )
                }
                userDataDao.insertAllFlaggedItems(flaggedEntities)
            }
            Result.success(true)
        } catch (e: SerializationException) {
            val message = "$IMPORT_FAILED_PREFIX$MALFORMED_JSON"
            Log.e(TAG, message, e)
            Result.failure(VocabDataException(message, e))
        } catch (e: IllegalArgumentException) {
            val message = "$IMPORT_FAILED_PREFIX$INVALID_ENUM"
            Log.e(TAG, message, e)
            Result.failure(VocabDataException(message, e))
        } catch (e: DateTimeParseException) {
            val message = "$IMPORT_FAILED_PREFIX$INVALID_TIMESTAMP"
            Log.e(TAG, message, e)
            Result.failure(VocabDataException(message, e))
        }
    }
}
