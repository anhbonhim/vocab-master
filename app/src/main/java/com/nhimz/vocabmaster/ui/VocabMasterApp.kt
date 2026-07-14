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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.navigation.Screen
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.ui.screens.DebugPanelScreen
import com.nhimz.vocabmaster.ui.screens.FirstWinScreen
import com.nhimz.vocabmaster.ui.screens.FlashcardScreen
import com.nhimz.vocabmaster.ui.screens.GoalPickerScreen
import com.nhimz.vocabmaster.ui.screens.HomeScreen
import com.nhimz.vocabmaster.ui.screens.PlacementTestScreen
import com.nhimz.vocabmaster.ui.screens.QuizScreen
import com.nhimz.vocabmaster.ui.screens.ResultScreen
import com.nhimz.vocabmaster.ui.screens.SettingsScreen
import com.nhimz.vocabmaster.ui.screens.StatisticsScreen
import com.nhimz.vocabmaster.ui.screens.TopicPickerScreen
import com.nhimz.vocabmaster.ui.screens.WelcomeScreen
import com.nhimz.vocabmaster.ui.screens.LoginScreen
import com.nhimz.vocabmaster.ui.theme.VocabMasterTheme
import com.nhimz.vocabmaster.ui.viewmodel.FlashcardViewModel
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel

@Composable
fun VocabMasterApp(
    mainViewModel: MainViewModel,
    placementTestViewModel: PlacementTestViewModel,
    quizViewModel: QuizViewModel,
    flashcardViewModel: FlashcardViewModel,
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
    val themeMode by mainViewModel.theme.collectAsState()
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    VocabMasterTheme(darkTheme = darkTheme) {
        val currentScreen by mainViewModel.currentScreen.collectAsState()
        val isLoading by mainViewModel.isLoading.collectAsState()

        if (isLoading) {
            LoadingSplash()
        } else {
            when (currentScreen) {
                is Screen.Welcome, is Screen.GoalPicker, is Screen.PlacementTest, is Screen.FirstWin, is Screen.Login -> {
                    OnboardingFlow(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        placementTestViewModel = placementTestViewModel
                    )
                }
                is Screen.Quiz, is Screen.Flashcard, is Screen.Result -> {
                    StudyFlow(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        quizViewModel = quizViewModel,
                        flashcardViewModel = flashcardViewModel,
                        cdnAudioPlayer = cdnAudioPlayer
                    )
                }
                is Screen.Home, is Screen.Statistics, is Screen.Settings, is Screen.TopicPicker, is Screen.DebugPanel -> {
                    MainAppScaffold(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        quizViewModel = quizViewModel,
                        flashcardViewModel = flashcardViewModel,
                        statisticsViewModel = statisticsViewModel,
                        settingsViewModel = settingsViewModel,
                        notificationScheduler = notificationScheduler,
                        cdnAudioPlayer = cdnAudioPlayer,
                        vocabDatabase = vocabDatabase,
                        vocabularyRepository = vocabularyRepository,
                        reviewRepository = reviewRepository,
                        settingsRepository = settingsRepository,
                        backupRepository = backupRepository
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
    placementTestViewModel: PlacementTestViewModel
) {
    when (currentScreen) {
        is Screen.Welcome -> WelcomeScreen(
            onStartClick = { mainViewModel.navigateTo(Screen.Login) } // Navigate to Login first!
        )
        is Screen.Login -> LoginScreen(
            onLoginSuccess = { mainViewModel.navigateTo(Screen.GoalPicker) }
        )
        is Screen.GoalPicker -> GoalPickerScreen(
            onGoalSelected = { minutes ->
                mainViewModel.setDailyGoal(minutes)
                mainViewModel.navigateTo(Screen.PlacementTest)
            }
        )
        is Screen.PlacementTest -> PlacementTestScreen(
            onFinished = { levelStr ->
                if (levelStr != null) {
                    try {
                        val levelEnum = com.nhimz.vocabmaster.domain.model.DifficultyLevel.valueOf(levelStr)
                        mainViewModel.savePlacementLevel(levelEnum)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

@Composable
private fun StudyFlow(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    quizViewModel: QuizViewModel,
    flashcardViewModel: FlashcardViewModel,
    cdnAudioPlayer: CDNAudioPlayer
) {
    when (currentScreen) {
        is Screen.Quiz -> QuizScreen(
            onSessionCompleted = { xp, duration, correct, total, stability ->
                mainViewModel.addStudyTime(duration)
                mainViewModel.navigateTo(Screen.Result(xp, duration, correct, total, stability))
            },
            onBackToHome = { mainViewModel.navigateTo(Screen.Home) },
            cdnAudioPlayer = cdnAudioPlayer,
            viewModel = quizViewModel
        )
        is Screen.Flashcard -> FlashcardScreen(
            onSessionCompleted = { xp, duration, correct, total, stability ->
                mainViewModel.addStudyTime(duration)
                mainViewModel.navigateTo(Screen.Result(xp, duration, correct, total, stability))
            },
            onBackToHome = { mainViewModel.navigateTo(Screen.Home) },
            cdnAudioPlayer = cdnAudioPlayer,
            viewModel = flashcardViewModel
        )
        is Screen.Result -> {
            val result = currentScreen as Screen.Result
            ResultScreen(
                xpGained = result.xpGained,
                durationSeconds = result.durationSeconds,
                correctCount = result.correctCount,
                totalCount = result.totalCount,
                averageStability = result.sessionStability,
                onBackToHome = {
                    mainViewModel.updateStreak()
                    mainViewModel.navigateTo(Screen.Home)
                }
            )
        }
        else -> {}
    }
}

@Composable
private fun MainAppScaffold(
    currentScreen: Screen,
    mainViewModel: MainViewModel,
    quizViewModel: QuizViewModel,
    flashcardViewModel: FlashcardViewModel,
    statisticsViewModel: StatisticsViewModel,
    settingsViewModel: SettingsViewModel,
    notificationScheduler: NotificationScheduler,
    cdnAudioPlayer: CDNAudioPlayer,
    vocabDatabase: VocabDatabase,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository
) {
    Scaffold(
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
                    selected = false,
                    onClick = {
                        quizViewModel.startNewSession()
                        mainViewModel.navigateTo(Screen.Quiz)
                    },
                    icon = { Text("✍️", fontSize = 20.sp) },
                    label = { Text("Luyện tập", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                        quizViewModel.startNewSession()
                        mainViewModel.navigateTo(Screen.Quiz)
                    },
                    onStartFlashcard = {
                        flashcardViewModel.startNewSession()
                        mainViewModel.navigateTo(Screen.Flashcard)
                    },
                    viewModel = mainViewModel
                )
                is Screen.Statistics -> StatisticsScreen(viewModel = statisticsViewModel)
                is Screen.Settings -> SettingsScreen(
                    viewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    notificationScheduler = notificationScheduler,
                    onNavigateToTopicPicker = { mainViewModel.navigateTo(Screen.TopicPicker) },
                    onNavigateToDebugPanel = { mainViewModel.navigateTo(Screen.DebugPanel) }
                )
                is Screen.TopicPicker -> TopicPickerScreen(
                    viewModel = settingsViewModel,
                    onBack = { mainViewModel.navigateTo(Screen.Settings) }
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