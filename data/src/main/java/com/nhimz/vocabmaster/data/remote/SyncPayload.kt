package com.nhimz.vocabmaster.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsDto(
    val dailyGoalXp: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val availableFreezes: Int,
    val lastStudyDate: Long,
    val xpTotal: Int,
    val desiredRetention: Double,
    val theme: String,
    val language: String,
    val placementLevel: String? = null,
    val selectedTopic: String
)

@Serializable
data class VocabularyCardDto(
    val questionId: String,
    val due: String, // ISO String
    val stability: Double,
    val difficulty: Double,
    val interval: Int,
    val reps: Int,
    val lapses: Int,
    val state: Int,
    val lastReview: String? = null, // ISO String
    val lastModified: Long
)

@Serializable
data class ReviewLogDto(
    val questionId: String,
    val rating: Int,
    val elapsed_days: Int,
    val scheduled_days: Int,
    val stability: Double,
    val difficulty: Double,
    val state: Int,
    val timestamp: String // ISO String
)

@Serializable
data class SyncPayload(
    val userSettings: UserSettingsDto,
    val vocabularyCards: List<VocabularyCardDto>,
    val reviewLogs: List<ReviewLogDto>,
    val lastSyncTimestamp: Long
)