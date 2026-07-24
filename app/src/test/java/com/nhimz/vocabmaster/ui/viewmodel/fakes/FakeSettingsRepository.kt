package com.nhimz.vocabmaster.ui.viewmodel.fakes

import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    override val dailyGoalMinutes: MutableStateFlow<Int> = MutableStateFlow(0)
    override val currentStreak: MutableStateFlow<Int> = MutableStateFlow(0)
    override val longestStreak: MutableStateFlow<Int> = MutableStateFlow(0)
    override val availableFreezes: MutableStateFlow<Int> = MutableStateFlow(0)
    override val lastStudyDate: MutableStateFlow<Long> = MutableStateFlow(0L)
    override val todayStudySeconds: MutableStateFlow<Int> = MutableStateFlow(0)
    override val xpTotal: MutableStateFlow<Int> = MutableStateFlow(0)
    override val badgeStatus: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val desiredRetention: MutableStateFlow<Double> = MutableStateFlow(0.9)
    override val theme: MutableStateFlow<String> = MutableStateFlow("system")
    override val language: MutableStateFlow<String> = MutableStateFlow("vi")
    override val placementLevel: MutableStateFlow<String?> = MutableStateFlow(null)
    override val selectedTopic: MutableStateFlow<String> = MutableStateFlow("")
    override val useLocalDevServer: MutableStateFlow<Boolean> = MutableStateFlow(false)

    var addXpCalls: Int = 0
    var lastAddedXp: Int? = null
    var setPlacementLevelValue: String? = null

    override suspend fun addXp(xp: Int) {
        addXpCalls++
        lastAddedXp = xp
        xpTotal.value = xpTotal.value + xp
    }

    override suspend fun setPlacementLevel(level: String) {
        setPlacementLevelValue = level
        placementLevel.value = level
    }

    override suspend fun updateDailyGoal(minutes: Int) = TODO("not needed for these tests")
    override suspend fun setCurrentStreak(streak: Int) {
        currentStreak.value = streak
    }
    override suspend fun setLongestStreak(streak: Int) {
        longestStreak.value = streak
    }
    override suspend fun setAvailableFreezes(freezes: Int) {
        availableFreezes.value = freezes
    }
    override suspend fun setLastStudyDate(timestamp: Long) {
        lastStudyDate.value = timestamp
    }
    override suspend fun addStudySeconds(seconds: Int) = TODO("not needed for these tests")
    override suspend fun setXpTotal(xp: Int) {
        xpTotal.value = xp
    }
    override suspend fun addBadge(badge: String) = TODO("not needed for these tests")
    override suspend fun setBadgeStatus(badges: List<String>) {
        badgeStatus.value = badges
    }
    override suspend fun setDesiredRetention(retention: Double) {
        desiredRetention.value = retention
    }
    override suspend fun setTheme(theme: String) = TODO("not needed for these tests")
    override suspend fun setLanguage(language: String) = TODO("not needed for these tests")
    override suspend fun setSelectedTopic(topic: String) = TODO("not needed for these tests")
    override suspend fun setUseLocalDevServer(enabled: Boolean) = TODO("not needed for these tests")

    var resetAllProgressCalls: Int = 0

    override suspend fun resetAllProgress() {
        resetAllProgressCalls++
        currentStreak.value = 0
        longestStreak.value = 0
        availableFreezes.value = 1
        lastStudyDate.value = 0L
        todayStudySeconds.value = 0
        xpTotal.value = 0
        badgeStatus.value = emptyList()
    }
}
