package com.nhimz.vocabmaster.domain.model

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val dailyGoalMinutes: Flow<Int>
    suspend fun setDailyGoalMinutes(minutes: Int)

    val currentStreak: Flow<Int>
    suspend fun setCurrentStreak(streak: Int)

    val longestStreak: Flow<Int>
    suspend fun setLongestStreak(streak: Int)

    val availableFreezes: Flow<Int>
    suspend fun setAvailableFreezes(freezes: Int)

    val lastStudyDate: Flow<Long>
    suspend fun setLastStudyDate(timestamp: Long)

    val todayStudySeconds: Flow<Int>
    suspend fun addStudySeconds(seconds: Int)

    val xpTotal: Flow<Int>
    suspend fun addXp(xp: Int)
    suspend fun setXpTotal(xp: Int)

    val badgeStatus: Flow<List<String>>
    suspend fun addBadge(badge: String)
    suspend fun setBadgeStatus(badges: List<String>)

    val desiredRetention: Flow<Double>
    suspend fun setDesiredRetention(retention: Double)

    val theme: Flow<String>
    suspend fun setTheme(theme: String)

    val language: Flow<String>
    suspend fun setLanguage(language: String)

    val placementLevel: Flow<String?>
    suspend fun setPlacementLevel(level: String)

    val selectedTopic: Flow<String>
    suspend fun setSelectedTopic(topic: String)
}
