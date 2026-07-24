package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

open class UpdateStreakUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    open suspend fun execute() {
        val today = System.currentTimeMillis()
        val lastStudy = settingsRepository.lastStudyDate.first()
        val current = settingsRepository.currentStreak.first()

        if (lastStudy == 0L) {
            settingsRepository.setCurrentStreak(1)
            settingsRepository.setLastStudyDate(today)
            return
        }

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
            // Award freeze every 7 consecutive days
            if (newStreak % 7 == 0) {
                val freezes = settingsRepository.availableFreezes.first()
                settingsRepository.setAvailableFreezes(freezes + 1)
            }
        } else if (current == 0) {
            settingsRepository.setCurrentStreak(1)
        }
        settingsRepository.setLastStudyDate(today)
    }
}
