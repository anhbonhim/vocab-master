package com.nhimz.vocabmaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.ui.NavDisplay
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.data.database.CurriculumDao
import com.nhimz.vocabmaster.data.database.UserDataDao
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.components.DuoSnackbarHost
import com.nhimz.vocabmaster.ui.navigation.Screen
import com.nhimz.vocabmaster.ui.navigation.vocabMasterEntryProvider
import com.nhimz.vocabmaster.ui.theme.VocabMasterTheme
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel

/**
 * Top-level Composable của VocabMaster app (Phase 3 Plan 03-03, Task 3).
 *
 * Phase 3 refactor:
 *  - Type-Safe Navigation Compose (Navigation 3 1.0.1) thay cho `when (currentScreen)`
 *    sealed class routing cũ (D-02 / UX-01).
 *  - backStack được own bởi [MainViewModel] (SnapshotStateList<NavKey>) — survive
 *    configuration changes, observe được từ NavDisplay.
 *  - SnackbarHost hoisted lên top-level để mọi route đều có thể show snackbar
 *    (D-04 / D-05). ResultScreen nhận snackbarHostState qua param.
 *  - Bottom nav chỉ hiện khi backStack.last() là top-level route (Home / Stats / Settings).
 *    Khi user tap, `navigateTopLevel()` clear backStack và add route mới.
 */
@Composable
fun VocabMasterApp(
    mainViewModel: MainViewModel,
    placementTestViewModel: PlacementTestViewModel,
    quizViewModel: QuizViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    cdnAudioPlayer: CDNAudioPlayer,
    notificationScheduler: NotificationScheduler,
    curriculumDao: CurriculumDao,
    userDataDao: UserDataDao,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository
) {
    val themeMode = settingsViewModel.theme.collectAsState().value
    val badgeUnlocked by mainViewModel.badgeUnlockedEvent.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (badgeUnlocked != null) {
        LaunchedEffect(badgeUnlocked) {
            android.widget.Toast.makeText(
                context,
                "🏆 Chúc mừng! Bạn đã mở khoá huy hiệu: $badgeUnlocked",
                android.widget.Toast.LENGTH_LONG
            ).show()
            mainViewModel.clearBadgeUnlockedEvent()
        }
    }

    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    VocabMasterTheme(darkTheme = darkTheme) {
        val isLoading by mainViewModel.isLoading.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        if (isLoading) {
            LoadingSplash()
        } else {
            VocabMasterNavScaffold(
                mainViewModel = mainViewModel,
                placementTestViewModel = placementTestViewModel,
                quizViewModel = quizViewModel,
                statisticsViewModel = statisticsViewModel,
                settingsViewModel = settingsViewModel,
                cdnAudioPlayer = cdnAudioPlayer,
                notificationScheduler = notificationScheduler,
                curriculumDao = curriculumDao,
                userDataDao = userDataDao,
                vocabularyRepository = vocabularyRepository,
                reviewRepository = reviewRepository,
                settingsRepository = settingsRepository,
                backupRepository = backupRepository,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

/**
 * Top-level Scaffold + NavDisplay. Hiển thị bottom nav có điều kiện (chỉ
 * khi user đang ở top-level route) và pass entryProvider cho NavDisplay.
 */
@Composable
private fun VocabMasterNavScaffold(
    mainViewModel: MainViewModel,
    placementTestViewModel: PlacementTestViewModel,
    quizViewModel: QuizViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    cdnAudioPlayer: CDNAudioPlayer,
    notificationScheduler: NotificationScheduler,
    curriculumDao: CurriculumDao,
    userDataDao: UserDataDao,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository,
    snackbarHostState: SnackbarHostState
) {
    // Stable reference to current top-level route — drives the `selected` flag
    // of each NavigationBarItem and conditional bottom bar visibility.
    val backStack = mainViewModel.backStack
    val currentTopLevel = backStack.lastOrNull()
    val showBottomBar = currentTopLevel in mainViewModel.topLevelRoutes

    Scaffold(
        snackbarHost = { DuoSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                VocabMasterBottomBar(
                    currentRoute = currentTopLevel,
                    onSelect = { route -> mainViewModel.navigateTopLevel(route) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavDisplay(
                backStack = backStack,
                onBack = { mainViewModel.goBack() },
                entryProvider = vocabMasterEntryProvider(
                    mainViewModel = mainViewModel,
                    placementTestViewModel = placementTestViewModel,
                    quizViewModel = quizViewModel,
                    statisticsViewModel = statisticsViewModel,
                    settingsViewModel = settingsViewModel,
                    cdnAudioPlayer = cdnAudioPlayer,
                    notificationScheduler = notificationScheduler,
                    curriculumDao = curriculumDao,
                    userDataDao = userDataDao,
                    vocabularyRepository = vocabularyRepository,
                    reviewRepository = reviewRepository,
                    settingsRepository = settingsRepository,
                    backupRepository = backupRepository,
                    snackbarHostState = snackbarHostState
                )
            )
        }
    }
}

/**
 * Bottom navigation bar — Home / Statistics / Settings. Tương ứng với
 * `topLevelRoutes` trong MainViewModel.
 */
@Composable
private fun VocabMasterBottomBar(
    currentRoute: Any?,
    onSelect: (com.nhimz.vocabmaster.ui.navigation.Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute is Screen.Home,
            onClick = { onSelect(Screen.Home) },
            icon = { Text("🏠", fontSize = 20.sp) },
            label = { Text("Trang chủ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute is Screen.Statistics,
            onClick = { onSelect(Screen.Statistics) },
            icon = { Text("📊", fontSize = 20.sp) },
            label = { Text("Thống kê", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = currentRoute is Screen.Settings,
            onClick = { onSelect(Screen.Settings) },
            icon = { Text("⚙️", fontSize = 20.sp) },
            label = { Text("Cài đặt", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
private fun LoadingSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Vocab Master",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
