package com.nhimz.vocabmaster.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 03-01 (Compose UI Refactoring & Polish) — Task 2/3 data class tests.
 *
 * The `connectedDebugAndroidTest` verify step in the plan is not runnable on
 * Termux aarch64 (no connected device/emulator + no Robolectric Conscrypt
 * native), so these unit tests exercise the data-class contracts that drive
 * the Container/Content split:
 *
 *  - [HomeScreenUiState] (Home Container → Content)
 *  - [SettingsUiModel] + [SettingsActions] (Settings Container → Content)
 *  - [DestructiveDialog] (which destructive dialog is currently shown)
 *
 * These tests run on the JVM and verify that:
 *  - Default values are stable (a Content can render before the Container
 *    has produced real state)
 *  - The data classes carry the fields the Content consumes
 *  - [DestructiveDialog] has the three states the Content branches on
 */
class Plan0301ContainerContentTest {

    @Test
    fun homeScreenUiState_defaults_areSensible() {
        val state = HomeScreenUiState()
        assertEquals(0, state.xpTotal)
        assertEquals(0, state.todayStudySeconds)
        assertEquals(10, state.dailyGoalXp)
        assertEquals(0, state.currentStreak)
        assertEquals(0, state.availableFreezes)
        assertEquals(0, state.dueCount)
        assertEquals(0, state.mistakeCount)
        assertEquals(false, state.showBacklogWarning)
        assertEquals(0f, state.animatedProgress, 0.0001f)
        assertEquals(0, state.minutesStudiedToday)
        assertEquals(false, state.isScrolledAwayFromCurrent)
        assertTrue(state.path.isEmpty())
        assertNull(state.currentNodeId)
    }

    @Test
    fun homeScreenUiState_carriesContentInputs() {
        val state = HomeScreenUiState(
            xpTotal = 1234,
            currentStreak = 7,
            dueCount = 12,
            mistakeCount = 3,
            showBacklogWarning = true,
            animatedProgress = 0.5f,
            minutesStudiedToday = 15,
            isScrolledAwayFromCurrent = true,
            currentNodeId = "node-1"
        )
        assertEquals(1234, state.xpTotal)
        assertEquals(7, state.currentStreak)
        assertEquals(12, state.dueCount)
        assertEquals(3, state.mistakeCount)
        assertEquals(true, state.showBacklogWarning)
        assertEquals(0.5f, state.animatedProgress, 0.0001f)
        assertEquals(15, state.minutesStudiedToday)
        assertEquals(true, state.isScrolledAwayFromCurrent)
        assertEquals("node-1", state.currentNodeId)
    }

    @Test
    fun settingsUiModel_defaults_areSensible() {
        val state = SettingsUiModel()
        assertEquals(10, state.dailyGoalXp)
        assertEquals("SYSTEM", state.theme)
        assertEquals(0.90, state.desiredRetention, 0.0001)
        assertEquals(false, state.isSyncing)
        assertNull(state.syncSuccess)
        assertNull(state.syncError)
        assertEquals(9, state.reminderHour)
        assertEquals(0, state.reminderMinute)
        assertEquals(true, state.reminderEnabled)
        assertEquals(false, state.isDebugBuild)
    }

    @Test
    fun settingsActions_defaults_doNothing() {
        // Default no-op callbacks must not throw when invoked — Content
        // previewables and tests construct SettingsActions() with no
        // overrides and rely on safe no-ops for the unused callbacks.
        val actions = SettingsActions()
        actions.onDailyGoalChange(15)
        actions.onRetentionChange(0.85)
        actions.onThemeChange("DARK")
        actions.onSync()
        actions.onBackup()
        actions.onRestore()
        actions.onReminderTimeChange(8, 30, false)
        actions.onRequestResetProgress()
        actions.onRequestDeleteAccount()
        actions.onResetProgress()
        actions.onDeleteAccount()
        actions.onShowLicenses()
        actions.onNavigateToDebugPanel()
        // If we reached here without exception, the no-op defaults work.
        assertNotNull(actions)
    }

    @Test
    fun destructiveDialog_hasAllThreeStates() {
        // The Content's `when (destructiveDialog)` branches on these values;
        // if any of them is removed the Content's `DestructiveDialog.None ->
        // Unit` would silently swallow a state mismatch.
        val values = DestructiveDialog.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(DestructiveDialog.None))
        assertTrue(values.contains(DestructiveDialog.ResetProgress))
        assertTrue(values.contains(DestructiveDialog.DeleteAccount))
    }
}
