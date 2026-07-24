package com.nhimz.vocabmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettingsBackup(
    val currentStreak: Int,
    val longestStreak: Int,
    val availableFreezes: Int,
    val lastStudyDate: Long,
    val xpTotal: Int,
    val badgeStatus: List<String>,
    val dailyGoalMinutes: Int,
    val desiredRetention: Double,
    val theme: String,
    val language: String
)

@Serializable
data class FsrsCardBackup(
    val questionId: String,
    val state: Int,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val due: Long,
    val lastReview: Long?,
    val reps: Int,
    val lapses: Int
)

@Serializable
data class ReviewLogBackup(
    val cardId: String,
    val rating: String,
    val reviewDatetime: String,
    val reviewDuration: Long?
)

@Serializable
data class FlaggedItemBackup(
    val questionId: String,
    val issueType: String,
    val details: String,
    val timestamp: Long
)

@Serializable
data class AppBackup(
    val version: Int = 3,
    val timestamp: Long,
    val settings: UserSettingsBackup,
    val cards: List<FsrsCardBackup>,
    val reviewLogs: List<ReviewLogBackup>,
    val flaggedItems: List<FlaggedItemBackup>
)
