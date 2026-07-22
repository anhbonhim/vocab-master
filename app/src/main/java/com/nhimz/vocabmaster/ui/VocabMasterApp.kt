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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.ui.screens.UnitGuidebookScreen
import com.nhimz.vocabmaster.ui.screens.JumpTestScreen
import com.nhimz.vocabmaster.ui.screens.SectionCheckpointScreen
import com.nhimz.vocabmaster.ui.screens.UnitCheckpointScreen
import androidx.compose.runtime.produceState
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.navigation.Screen
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.ui.components.DuoSnackbarHost
import com.nhimz.vocabmaster.ui.screens.SettingsScreen
import com.nhimz.vocabmaster.ui.screens.StatisticsScreen
import com.nhimz.vocabmaster.ui.screens.WelcomeScreen
import com.nhimz.vocabmaster.ui.screens.LoginScreen
import com.nhimz.vocabmaster.ui.screens.DebugPanelScreen
import com.nhimz.vocabmaster.ui.screens.FirstWinScreen
import com.nhimz.vocabmaster.ui.screens.GoalPickerScreen
import com.nhimz.vocabmaster.ui.screens.HomeScreen
import com.nhimz.vocabmaster.ui.screens.PlacementTestScreen
import com.nhimz.vocabmaster.ui.screens.QuizScreen
import com.nhimz.vocabmaster.ui.screens.ResultScreen
import com.nhimz.vocabmaster.ui.theme.VocabMasterTheme
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel
import com.nhimz.vocabmaster.util.LocalLogger

@Composable
fun VocabMasterApp(
    mainViewModel: MainViewModel,
    placementTestViewModel: PlacementTestViewModel,
    quizViewModel: QuizViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    cdnAudioPlayer: CDNAudioPlayer,
    notificationScheduler: NotificationScheduler,
    vocabDatabase: VocabDatabase,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository
) {
    val themeMode = settingsViewModel.theme.collectAsState().value
    val badgeUnlocked by mainViewModel.badgeUnlockedEvent.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (badgeUnlocked != null) {
        androidx.compose.runtime.LaunchedEffect(badgeUnlocked) {
            android.widget.Toast.makeText(context, "🏆 Chúc mừng! Bạn đã mở khoá huy hiệu: $badgeUnlocked", android.widget.Toast.LENGTH_LONG).show()
            mainViewModel.clearBadgeUnlockedEvent()
        }
    }

    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    VocabMasterTheme(darkTheme = darkTheme) {
        val currentScreen by mainViewModel.currentScreen.collectAsState()
        val isLoading by mainViewModel.isLoading.collectAsState()

        // Global SnackbarHost (D-04, D-05): hoisted to the top so any Container
        // screen can push messages via the per-flow `snackbarHostState` param
        // and have them appear in the bottom snackbar slot.
        val snackbarHostState = remember { SnackbarHostState() }

        if (isLoading) {
            LoadingSplash()
        } else {
            when (currentScreen) {
                is Screen.Welcome, is Screen.GoalPicker, is Screen.PlacementTest, is Screen.FirstWin, is Screen.Login -> {
                    OnboardingFlow(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        placementTestViewModel = placementTestViewModel,
                        settingsViewModel = settingsViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
                is Screen.Quiz, is Screen.Result -> {
                    StudyFlow(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        quizViewModel = quizViewModel,
                        cdnAudioPlayer = cdnAudioPlayer,
                        snackbarHostState = snackbarHostState
                    )
                }
                is Screen.Home, is Screen.Statistics, is Screen.Settings, is Screen.DebugPanel, is Screen.Guidebook, is Screen.JumpTest, is Screen.SectionCheckpoint, is Screen.UnitCheckpoint -> {
                    MainAppScaffold(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        quizViewModel = quizViewModel,
                        statisticsViewModel = statisticsViewModel,
                        settingsViewModel = settingsViewModel,
                        notificationScheduler = notificationScheduler,
                        cdnAudioPlayer = cdnAudioPlayer,
                        vocabDatabase = vocabDatabase,
                        vocabularyRepository = vocabularyRepository,
                        reviewRepository = reviewRepository,
                        settingsRepository = settingsRepository,
                        backupRepository = backupRepository,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
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

@Composable
private fun OnboardingFlow(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    placementTestViewModel: PlacementTestViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { DuoSnackbarHost(snackbarHostState) }
    ) { _ ->
        when (currentScreen) {
            is Screen.Welcome -> WelcomeScreen(
                onStartClick = { mainViewModel.navigateTo(Screen.Login) } // Navigate to Login first!
            )
            is Screen.Login -> LoginScreen(
                onLoginSuccess = { mainViewModel.navigateTo(Screen.GoalPicker) }
            )
            is Screen.GoalPicker -> GoalPickerScreen(
                onGoalSelected = { minutes ->
                    settingsViewModel.setDailyGoal(minutes)
                    mainViewModel.navigateTo(Screen.PlacementTest)
                }
            )
            is Screen.PlacementTest -> PlacementTestScreen(
                onFinished = { levelStr ->
                    if (levelStr != null) {
                        mainViewModel.savePlacementLevel(levelStr)
                    }
                    mainViewModel.navigateTo(Screen.FirstWin)
                },
                onBack = { mainViewModel.navigateTo(Screen.Login) },
                viewModel = placementTestViewModel
            )
            is Screen.FirstWin -> FirstWinScreen(
                onFinished = { mainViewModel.completeOnboarding() }
            )
            else -> {}
        }
    }
}

@Composable
private fun StudyFlow(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    quizViewModel: QuizViewModel,
    cdnAudioPlayer: CDNAudioPlayer,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { DuoSnackbarHost(snackbarHostState) }
    ) { _ ->
        when (currentScreen) {
            is Screen.Quiz -> QuizScreen(
                onSessionCompleted = { xp, duration, correct, total, stability, incorrectCardIds, isLevelTest, isPassedLevelTest ->
                    mainViewModel.addStudyTime(duration)
                    mainViewModel.navigateTo(Screen.Result(xp, duration, correct, total, stability, incorrectCardIds, isLevelTest, isPassedLevelTest))
                },
                onBackToHome = { mainViewModel.navigateTo(Screen.Home) },
                cdnAudioPlayer = cdnAudioPlayer,
                viewModel = quizViewModel
            )
            is Screen.Result -> {
                val result = currentScreen as Screen.Result
                ResultScreen(
                    xpGained = result.xpGained,
                    durationSeconds = result.durationSeconds,
                    correctCount = result.correctCount,
                    totalCount = result.totalCount,
                    averageStability = result.sessionStability,
                    incorrectCardIds = result.incorrectCardIds,
                    isLevelTest = result.isLevelTest,
                    isPassedLevelTest = result.isPassedLevelTest,
                    onBackToHome = {
                        mainViewModel.updateStreak()
                        mainViewModel.navigateTo(Screen.Home)
                    },
                    onReviewMistakes = { ids ->
                        // Fallback using old review mechanics, could be updated if ReviewGym changed
                        mainViewModel.navigateTo(Screen.Quiz(ids))
                    },
                    snackbarHostState = snackbarHostState
                )
            }
            else -> {}
        }
    }
}


@Composable
private fun MainAppScaffold(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    quizViewModel: QuizViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    notificationScheduler: NotificationScheduler,
    cdnAudioPlayer: CDNAudioPlayer,
    vocabDatabase: VocabDatabase,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { DuoSnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen is Screen.Home,
                    onClick = { mainViewModel.navigateTo(Screen.Home) },
                    icon = { Text("🏠", fontSize = 20.sp) },
                    label = { Text("Trang chủ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Statistics,
                    onClick = { mainViewModel.navigateTo(Screen.Statistics) },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Thống kê", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentScreen is Screen.Settings,
                    onClick = { mainViewModel.navigateTo(Screen.Settings) },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Cài đặt", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(
                    onStartQuiz = {
                        quizViewModel.startMistakeReview(null)
                        mainViewModel.navigateTo(Screen.Quiz())
                    },
                    onStartFlashcard = { ids ->
                        quizViewModel.startMistakeReview(ids)
                        mainViewModel.navigateTo(Screen.Quiz(ids))
                    },
                    onStartCustomQuiz = { stage, topic, index, isLevelTest, isReviewGym ->
                        quizViewModel.startMistakeReview(null)
                        mainViewModel.navigateTo(Screen.Quiz())
                    },
                    onStartNodeSession = { nodeId ->
                        quizViewModel.startNodeSession(nodeId, 0)
                        mainViewModel.navigateTo(Screen.Quiz())
                    },
                    onStartReviewNode = { nodeId, unitId, sectionId ->
                        quizViewModel.startReviewNode(nodeId, unitId, sectionId)
                        mainViewModel.navigateTo(Screen.Quiz())
                    },
                    onStartJumpTest = { unitId ->
                        mainViewModel.navigateTo(Screen.JumpTest(unitId))
                    },
                    onStartSectionCheckpoint = { sectionId ->
                        mainViewModel.navigateTo(Screen.SectionCheckpoint(sectionId))
                    },
                    onStartUnitCheckpoint = { unitId ->
                        mainViewModel.navigateTo(Screen.UnitCheckpoint(unitId))
                    },
                    onStartGuidebook = { unitId ->
                        mainViewModel.navigateTo(Screen.Guidebook(unitId))
                    },
                    viewModel = mainViewModel
                )
                is Screen.Guidebook -> {
                    val unitId = (currentScreen as Screen.Guidebook).unitId
                    val guidebook by produceState<com.nhimz.vocabmaster.domain.model.UnitGuidebook?>(initialValue = null, unitId) {
                        value = vocabularyRepository.getGuidebook(unitId)
                            .onFailure { LocalLogger.e("VocabMasterApp", "Failed to load guidebook for $unitId", it) }
                            .getOrNull()
                    }
                    if (guidebook != null) {
                        UnitGuidebookScreen(
                            guidebook = guidebook!!,
                            unitTitle = "Sổ tay ngữ pháp", // Could pass unitTitle if needed, simplified here
                            onBack = { mainViewModel.navigateTo(Screen.Home) }
                        )
                    } else {
                        // Loading state placeholder if needed
                    }
                }
                is Screen.JumpTest -> {
                    val unitId = (currentScreen as Screen.JumpTest).unitId
                    JumpTestScreen(
                        onBack = { mainViewModel.navigateTo(Screen.Home) },
                        onStartTest = {
                            quizViewModel.startJumpTest(unitId)
                            mainViewModel.navigateTo(Screen.Quiz())
                        }
                    )
                }
                is Screen.SectionCheckpoint -> {
                    val sectionId = (currentScreen as Screen.SectionCheckpoint).sectionId
                    SectionCheckpointScreen(
                        title = "Bài thi cuối chặng",
                        onBack = { mainViewModel.navigateTo(Screen.Home) },
                        onStartTest = {
                            quizViewModel.startSectionCheckpoint(sectionId, null)
                            mainViewModel.navigateTo(Screen.Quiz())
                        }
                    )
                }
                is Screen.UnitCheckpoint -> {
                    val unitId = (currentScreen as Screen.UnitCheckpoint).unitId
                    UnitCheckpointScreen(
                        title = "Bài thi cuối chủ đề",
                        onBack = { mainViewModel.navigateTo(Screen.Home) },
                        onStartTest = {
                            quizViewModel.startUnitCheckpoint(unitId)
                            mainViewModel.navigateTo(Screen.Quiz())
                        }
                    )
                }
                is Screen.Statistics -> StatisticsScreen(
                    viewModel = statisticsViewModel,
                    onReviewMistakes = { ids ->
                        quizViewModel.startMistakeReview(ids)
                        mainViewModel.navigateTo(Screen.Quiz(ids))
                    }
                )
                is Screen.Settings -> SettingsScreen(
                    viewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    notificationScheduler = notificationScheduler,
                    onNavigateToDebugPanel = { mainViewModel.navigateTo(Screen.DebugPanel) }
                )
                is Screen.DebugPanel -> DebugPanelScreen(
                    onBack = { mainViewModel.navigateTo(Screen.Settings) },
                    cdnAudioPlayer = cdnAudioPlayer,
                    vocabDatabase = vocabDatabase,
                    vocabularyRepository = vocabularyRepository,
                    reviewRepository = reviewRepository,
                    settingsRepository = settingsRepository,
                    backupRepository = backupRepository
                )
                else -> {}
            }
        }
    }
}