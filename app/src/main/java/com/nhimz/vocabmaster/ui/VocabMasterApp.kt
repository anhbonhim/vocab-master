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
import com.nhimz.vocabmaster.ui.screens.FirstWinScreen
import com.nhimz.vocabmaster.ui.screens.FlashcardScreen
import com.nhimz.vocabmaster.ui.screens.GoalPickerScreen
import com.nhimz.vocabmaster.ui.screens.HomeScreen
import com.nhimz.vocabmaster.ui.screens.PlacementTestScreen
import com.nhimz.vocabmaster.ui.screens.QuizScreen
import com.nhimz.vocabmaster.ui.screens.ResultScreen
import com.nhimz.vocabmaster.ui.screens.SettingsScreen
import com.nhimz.vocabmaster.ui.screens.StatisticsScreen
import com.nhimz.vocabmaster.ui.screens.WelcomeScreen
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
    notificationScheduler: NotificationScheduler
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
                is Screen.Welcome, is Screen.GoalPicker, is Screen.PlacementTest, is Screen.FirstWin -> {
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
                is Screen.Home, is Screen.Statistics, is Screen.Settings -> {
                    MainAppScaffold(
                        currentScreen = currentScreen,
                        mainViewModel = mainViewModel,
                        quizViewModel = quizViewModel,
                        flashcardViewModel = flashcardViewModel,
                        statisticsViewModel = statisticsViewModel,
                        settingsViewModel = settingsViewModel,
                        notificationScheduler = notificationScheduler
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
            onStartClick = { mainViewModel.navigateTo(Screen.GoalPicker) }
        )
        is Screen.GoalPicker -> GoalPickerScreen(
            onGoalSelected = { minutes ->
                mainViewModel.setDailyGoal(minutes)
                mainViewModel.navigateTo(Screen.PlacementTest)
            }
        )
        is Screen.PlacementTest -> PlacementTestScreen(
            onTestFinished = { level ->
                mainViewModel.savePlacementLevel(level)
                mainViewModel.navigateTo(Screen.FirstWin)
            },
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
            onSessionCompleted = { xp, duration, correct, total ->
                mainViewModel.addStudyTime(duration)
                mainViewModel.navigateTo(Screen.Result(xp, duration, correct, total))
            },
            onBackToHome = { mainViewModel.navigateTo(Screen.Home) },
            cdnAudioPlayer = cdnAudioPlayer,
            viewModel = quizViewModel
        )
        is Screen.Flashcard -> FlashcardScreen(
            onSessionCompleted = { xp, duration, correct, total ->
                mainViewModel.addStudyTime(duration)
                mainViewModel.navigateTo(Screen.Result(xp, duration, correct, total))
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
    notificationScheduler: NotificationScheduler
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
                    notificationScheduler = notificationScheduler
                )
                else -> {}
            }
        }
    }
}
