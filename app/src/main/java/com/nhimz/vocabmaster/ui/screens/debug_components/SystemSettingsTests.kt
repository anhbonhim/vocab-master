package com.nhimz.vocabmaster.ui.screens.debug_components

import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.first

suspend fun testGamificationState(
    settingsRepository: SettingsRepository
): TestResult {
    return runTest(
        name = "Gamification State Test",
        description = "Validates consistency and reads current values of all gamification metrics."
    ) { log, assertions ->
        log.appendLine("Reading current settings from DataStore...")
        val xp = settingsRepository.xpTotal.first()
        val currentStreak = settingsRepository.currentStreak.first()
        val longestStreak = settingsRepository.longestStreak.first()
        val freezes = settingsRepository.availableFreezes.first()
        val badges = settingsRepository.badgeStatus.first()
        val goal = settingsRepository.dailyGoalXp.first()
        val retention = settingsRepository.desiredRetention.first()
        val topic = settingsRepository.selectedTopic.first()
        val theme = settingsRepository.theme.first()
        val language = settingsRepository.language.first()

        log.appendLine("Current gamification metrics:")
        log.appendLine("  XP total: $xp")
        log.appendLine("  Current Streak: $currentStreak")
        log.appendLine("  Longest Streak: $longestStreak")
        log.appendLine("  Available Freezes: $freezes")
        log.appendLine("  Badges: $badges")
        log.appendLine("  Daily Goal Minutes: $goal")
        log.appendLine("  Desired Retention: $retention")
        log.appendLine("  Selected Topic: $topic")
        log.appendLine("  Theme: $theme")
        log.appendLine("  Language: $language")

        assertions.add(AssertionResult("XP total is non-negative ($xp)", xp >= 0))
        assertions.add(AssertionResult("Current streak is non-negative ($currentStreak)", currentStreak >= 0))
        assertions.add(AssertionResult("Longest streak is >= current streak ($longestStreak >= $currentStreak)", longestStreak >= currentStreak))
        assertions.add(AssertionResult("Available freezes is non-negative ($freezes)", freezes >= 0))
        assertions.add(AssertionResult("Daily goal is within valid range [5, 60] ($goal)", goal in 5..60))
        assertions.add(AssertionResult("Retention is within valid range [0.8, 0.95] ($retention)", retention in 0.8..0.95))

        val passed = (xp >= 0) && (currentStreak >= 0) && (longestStreak >= currentStreak) && (freezes >= 0) && (goal in 5..60) && (retention in 0.8..0.95)
        if (passed) TestStatus.PASS else TestStatus.FAIL
    }
}

suspend fun testSettingsPersistenceRoundtrip(
    settingsRepository: SettingsRepository
): TestResult {
    return runTest(
        name = "Settings Persistence Roundtrip",
        description = "Performs write-then-read cycle tests for theme, language, topic, and badge serialization."
    ) { log, assertions ->
        // Backup
        val originalTheme = settingsRepository.theme.first()
        val originalLanguage = settingsRepository.language.first()
        val originalTopic = settingsRepository.selectedTopic.first()
        val originalRetention = settingsRepository.desiredRetention.first()
        val originalGoal = settingsRepository.dailyGoalXp.first()
        val originalBadges = settingsRepository.badgeStatus.first()

        log.appendLine("Backed up current settings.")

        try {
            // 1. Test Theme switching
            log.appendLine("1. Testing Theme setting persistence...")
            settingsRepository.setTheme("DARK")
            val themeRead = settingsRepository.theme.first()
            assertions.add(AssertionResult("Theme successfully updated to DARK", themeRead == "DARK", "Got: $themeRead"))

            // 2. Test Language switching
            log.appendLine("2. Testing Language setting persistence...")
            settingsRepository.setLanguage("EN")
            val langRead = settingsRepository.language.first()
            assertions.add(AssertionResult("Language successfully updated to EN", langRead == "EN", "Got: $langRead"))

            // 3. Test Daily Goal minutes
            log.appendLine("3. Testing Daily Goal persistence...")
            settingsRepository.updateDailyGoal(25)
            val goalRead = settingsRepository.dailyGoalXp.first()
            assertions.add(AssertionResult("Daily goal successfully updated to 25", goalRead == 25, "Got: $goalRead"))

            // 4. Test Desired Retention
            log.appendLine("4. Testing Desired Retention persistence...")
            settingsRepository.setDesiredRetention(0.85)
            val retentionRead = settingsRepository.desiredRetention.first()
            assertions.add(AssertionResult("Retention successfully updated to 0.85", retentionRead == 0.85, "Got: $retentionRead"))

            // 5. Test Topic selection
            log.appendLine("5. Testing Topic selection persistence...")
            settingsRepository.setSelectedTopic("technology")
            val topicRead = settingsRepository.selectedTopic.first()
            assertions.add(AssertionResult("Topic successfully updated to technology", topicRead == "technology", "Got: $topicRead"))

            // 6. Test Badge list serialization including badges with comma-like chars or simple list persistence
            log.appendLine("6. Testing Badge list serialization...")
            val testBadges = listOf("badge_1", "badge_2", "badge_3")
            settingsRepository.setBadgeStatus(testBadges)
            val badgesRead = settingsRepository.badgeStatus.first()
            assertions.add(AssertionResult("Badge list matches written list $testBadges", badgesRead == testBadges, "Got: $badgesRead"))
            
            // Add a badge
            settingsRepository.addBadge("badge_4")
            val badgesAfterAdd = settingsRepository.badgeStatus.first()
            assertions.add(AssertionResult("Badge add works (contains badge_4)", badgesAfterAdd.contains("badge_4"), "Got: $badgesAfterAdd"))

        } finally {
            // Restore original state
            log.appendLine("Restoring original settings state...")
            settingsRepository.setTheme(originalTheme)
            settingsRepository.setLanguage(originalLanguage)
            settingsRepository.setSelectedTopic(originalTopic)
            settingsRepository.setDesiredRetention(originalRetention)
            settingsRepository.updateDailyGoal(originalGoal)
            settingsRepository.setBadgeStatus(originalBadges)
        }

        TestStatus.PASS
    }
}