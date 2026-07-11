package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Welcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Shared statistics flows
    val xpTotal: StateFlow<Int> = settingsRepository.xpTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStudySeconds: StateFlow<Int> = settingsRepository.todayStudySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentStreak: StateFlow<Int> = settingsRepository.currentStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableFreezes: StateFlow<Int> = settingsRepository.availableFreezes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val dailyGoalMinutes: StateFlow<Int> = settingsRepository.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VI")

    val desiredRetention: StateFlow<Double> = settingsRepository.desiredRetention
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.9)

    val badgeStatus: StateFlow<List<String>> = settingsRepository.badgeStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val placementLevel: StateFlow<String?> = settingsRepository.placementLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        checkOnboardingStatus()
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            val badges = settingsRepository.badgeStatus.first()
            if (badges.contains("onboarding_completed")) {
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.Welcome
            }
            _isLoading.value = false
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.addBadge("onboarding_completed")
            // Give 50 starting XP
            settingsRepository.addXp(50)
            settingsRepository.setCurrentStreak(1)
            settingsRepository.setLastStudyDate(System.currentTimeMillis())
            navigateTo(Screen.Home)
        }
    }

    fun savePlacementLevel(level: DifficultyLevel) {
        viewModelScope.launch {
            settingsRepository.setPlacementLevel(level.name)
        }
    }

    fun addXp(xp: Int) {
        viewModelScope.launch {
            settingsRepository.addXp(xp)
        }
    }

    fun addStudyTime(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.addStudySeconds(seconds)
        }
    }

    fun updateStreak() {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            val lastStudy = settingsRepository.lastStudyDate.first()
            val current = settingsRepository.currentStreak.first()

            val diffMs = today - lastStudy
            val oneDayMs = 24 * 60 * 60 * 1000L

            if (diffMs > oneDayMs * 2) {
                // Streak broken, but check if we have freezes
                val freezes = settingsRepository.availableFreezes.first()
                if (freezes > 0) {
                    settingsRepository.setAvailableFreezes(freezes - 1)
                    // Freeze saved the streak! Keep current streak
                } else {
                    settingsRepository.setCurrentStreak(1)
                }
            } else if (diffMs > oneDayMs) {
                // consecutive day
                val newStreak = current + 1
                settingsRepository.setCurrentStreak(newStreak)
                val longest = settingsRepository.longestStreak.first()
                if (newStreak > longest) {
                    settingsRepository.setLongestStreak(newStreak)
                }
            } else if (current == 0) {
                settingsRepository.setCurrentStreak(1)
            }
            settingsRepository.setLastStudyDate(today)
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoalMinutes(minutes)
        }
    }

    fun setDesiredRetention(retention: Double) {
        viewModelScope.launch {
            settingsRepository.setDesiredRetention(retention)
        }
    }

    fun setTheme(themeName: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(themeName)
        }
    }

    fun setLanguage(langName: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(langName)
        }
    }
}
