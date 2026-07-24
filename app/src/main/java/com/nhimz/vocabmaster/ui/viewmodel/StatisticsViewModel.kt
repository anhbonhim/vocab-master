package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.ReviewStats
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyXp(
    val dayLabel: String,
    val xp: Int
)

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // General review stats
    val reviewStats: StateFlow<ReviewStats?> = reviewRepository.getStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Mistake bank cards (>50% error rate)
    private val _mistakeCards = MutableStateFlow<List<com.nhimz.vocabmaster.domain.model.QuestionWithCard>>(emptyList())
    val mistakeCards: StateFlow<List<com.nhimz.vocabmaster.domain.model.QuestionWithCard>> = _mistakeCards.asStateFlow()

    // 7-day XP history
    private val _xpHistory = MutableStateFlow<List<DailyXp>>(emptyList())
    val xpHistory: StateFlow<List<DailyXp>> = _xpHistory.asStateFlow()

    // Badges list
    private val _badges = MutableStateFlow<List<BadgeItem>>(emptyList())
    val badges: StateFlow<List<BadgeItem>> = _badges.asStateFlow()

    init {
        loadStatisticsData()
    }

    fun loadStatisticsData() {
        viewModelScope.launch {
            // Load mistake bank
            val allCardsList = mutableListOf<com.nhimz.vocabmaster.domain.model.QuestionWithCard>()
            for (level in DifficultyLevel.values()) {
                allCardsList.addAll(vocabularyRepository.getCardsByLevel(level).first())
            }
            val mistakes = allCardsList.filter { item ->
                item.card.reps > 0 && (item.card.lapses.toFloat() / item.card.reps.toFloat()) >= 0.5f
            }
            _mistakeCards.value = mistakes

            // Compute XP history from review logs
            reviewRepository.getAllReviewLogs().collect { logs ->
                val formatter = DateTimeFormatter.ofPattern("E") // Day abbreviation (Mon, Tue, etc.)
                val today = LocalDateTime.now()
                
                // Initialize past 7 days with 0 XP
                val last7DaysMap = LinkedHashMap<String, Int>()
                for (i in 6 downTo 0) {
                    val date = today.minusDays(i.toLong())
                    last7DaysMap[date.format(formatter)] = 0
                }

                // Fill with actual data (estimated XP: 10 XP for Good/Easy, 2 XP for Again/Hard)
                logs.forEach { log ->
                    val logDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(log.reviewDatetime),
                        ZoneId.systemDefault()
                    )
                    val logDay = logDateTime.format(formatter)
                    if (last7DaysMap.containsKey(logDay)) {
                        val xpGained = if (log.rating.value >= 3) 10 else 2
                        last7DaysMap[logDay] = last7DaysMap.getOrDefault(logDay, 0) + xpGained
                    }
                }

                _xpHistory.value = last7DaysMap.map { DailyXp(it.key, it.value) }
            }
        }

        viewModelScope.launch {
            // Unlocked badges sync
            combine(
                settingsRepository.badgeStatus,
                settingsRepository.xpTotal,
                settingsRepository.currentStreak
            ) { unlockedList, _, _ ->
                val list = mutableListOf<BadgeItem>()

                // 1. Onboarding Completed
                list.add(
                    BadgeItem(
                        id = "onboarding_completed",
                        title = "Khởi đầu mới",
                        description = "Hoàn thành bài kiểm tra đầu vào và thiết lập mục tiêu.",
                        icon = "🚀",
                        isUnlocked = unlockedList.contains("onboarding_completed")
                    )
                )
                // 2. Streak 3 days
                list.add(
                    BadgeItem(
                        id = "streak_3",
                        title = "Kiên trì",
                        description = "Đạt chuỗi học tập liên tiếp 3 ngày.",
                        icon = "🔥",
                        isUnlocked = unlockedList.contains("streak_3")
                    )
                )
                // 3. Streak 7 days
                list.add(
                    BadgeItem(
                        id = "streak_7",
                        title = "Chiến binh học tập",
                        description = "Đạt chuỗi học tập liên tiếp 7 ngày.",
                        icon = "👑",
                        isUnlocked = unlockedList.contains("streak_7")
                    )
                )
                // 4. XP 500
                list.add(
                    BadgeItem(
                        id = "xp_500",
                        title = "Tích lũy",
                        description = "Đạt tổng số 500 XP kinh nghiệm.",
                        icon = "💎",
                        isUnlocked = unlockedList.contains("xp_500")
                    )
                )
                // 5. XP 1000
                list.add(
                    BadgeItem(
                        id = "xp_1000",
                        title = "Học giả",
                        description = "Đạt tổng số 1000 XP kinh nghiệm.",
                        icon = "🧙",
                        isUnlocked = unlockedList.contains("xp_1000")
                    )
                )

                _badges.value = list
            }.collect {}
        }
    }
}
