package com.nhimz.vocabmaster.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nhimz.vocabmaster.BuildConfig
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.ui.components.showSnackbar
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.util.LocalLogger

/**
 * Settings screen Container (Plan 03-01, Task 3 — and Plan 03-04 snackbar wiring).
 *
 * Responsibilities:
 *  - Collect state from [MainViewModel] and [SettingsViewModel]
 *  - Hold local UI state (current destructive dialog, licenses dialog)
 *  - Manage Android system primitives (SharedPreferences for reminder time,
 *    Activity Result launchers for backup/restore, Toast notifications)
 *  - Coordinate with [NotificationScheduler] for daily reminders
 *  - Forward every user-intent to a ViewModel or system primitive
 *  - Wire [settingsViewModel.snackbarMessages] to the global
 *    [snackbarHostState] (Plan 03-04 — D-04 / D-05)
 *
 * Pure UI rendering is delegated to [SettingsScreenContent]. Public signature
 * is preserved (with one optional [snackbarHostState] parameter) so existing
 * call sites (e.g. `VocabMasterApp.kt`) need no change.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    notificationScheduler: NotificationScheduler,
    onNavigateToDebugPanel: () -> Unit,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    var showLicensesDialog by remember { mutableStateOf(false) }
    var destructiveDialog by remember { mutableStateOf(DestructiveDialog.None) }

    // Plan 03-04: wire SettingsViewModel.snackbarMessages to the global
    // SnackbarHostState. `rememberUpdatedState` ensures we always invoke the
    // latest flow / host even if the parent re-emits (D-04 / D-05).
    val currentSnackbarHostState by rememberUpdatedState(snackbarHostState)
    val currentSnackbarMessages = settingsViewModel.snackbarMessages
    LaunchedEffect(currentSnackbarHostState) {
        currentSnackbarHostState?.let { host ->
            currentSnackbarMessages.collect { message: SnackbarMessage ->
                if (message.isError) {
                    LocalLogger.e(
                        tag = "SettingsScreen",
                        message = "Snackbar error surfaced: ${message.text}"
                    )
                }
                host.showSnackbar(message)
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            settingsViewModel.backupData(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Sao lưu dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "Sao lưu thất bại: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.restoreData(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Khôi phục dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                    viewModel.checkOnboardingStatus()
                },
                onError = { error ->
                    Toast.makeText(context, "Khôi phục thất bại: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val dailyGoal by settingsViewModel.dailyGoalMinutes.collectAsState()
    val theme by settingsViewModel.theme.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val desiredRetention by settingsViewModel.desiredRetention.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val sharedPrefs = remember { context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE) }
    var hour by remember { mutableIntStateOf(sharedPrefs.getInt("reminder_hour", 9)) }
    var minute by remember { mutableIntStateOf(sharedPrefs.getInt("reminder_minute", 0)) }
    var isReminderEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("reminder_enabled", true)) }

    fun updateReminderTime(h: Int, m: Int, enabled: Boolean) {
        hour = h
        minute = m
        isReminderEnabled = enabled

        sharedPrefs.edit()
            .putInt("reminder_hour", h)
            .putInt("reminder_minute", m)
            .putBoolean("reminder_enabled", enabled)
            .apply()

        if (enabled) {
            notificationScheduler.scheduleDailyNotification(h, m)
        } else {
            notificationScheduler.cancelNotification()
        }
    }

    val state = SettingsUiModel(
        dailyGoalMinutes = dailyGoal,
        theme = theme,
        desiredRetention = desiredRetention,
        reminderHour = hour,
        reminderMinute = minute,
        reminderEnabled = isReminderEnabled,
        isDebugBuild = BuildConfig.DEBUG
    )

    val actions = SettingsActions(
        onDailyGoalChange = { settingsViewModel.setDailyGoal(it) },
        onRetentionChange = { settingsViewModel.setDesiredRetention(it) },
        onThemeChange = { settingsViewModel.setTheme(it) },
        onBackup = {
            exportBackupLauncher.launch("vocab_master_backup.json")
        },
        onRestore = {
            importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onReminderTimeChange = { h, m, enabled -> updateReminderTime(h, m, enabled) },
        onRequestResetProgress = { destructiveDialog = DestructiveDialog.ResetProgress },
        onRequestDeleteAccount = { destructiveDialog = DestructiveDialog.DeleteAccount },
        onResetProgress = {
            settingsViewModel.resetAllProgress { /* dialog dismissed by Content; snackbar emitted by VM */ }
        },
        onDeleteAccount = {
            // Plan 03-01 Task 3: destructive action requires a confirmation
            // dialog (D-05). Full account deletion is wired through a future
            // use-case; log the intent and surface a Toast.
            LocalLogger.w("SettingsScreen", "Delete account confirmed by user")
            Toast.makeText(
                context,
                "Đã ghi nhận yêu cầu xóa tài khoản.",
                Toast.LENGTH_SHORT
            ).show()
        },
        onShowLicenses = { showLicensesDialog = true },
        onNavigateToDebugPanel = onNavigateToDebugPanel
    )

    SettingsScreenContent(
        state = state,
        actions = actions,
        destructiveDialog = destructiveDialog,
        onDestructiveDialogDismiss = { destructiveDialog = DestructiveDialog.None },
        onLicensesDismiss = { showLicensesDialog = false },
        showLicensesDialog = showLicensesDialog
    )
}
