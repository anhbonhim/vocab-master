package com.nhimz.vocabmaster.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.UnitGuidebook
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.screens.DebugPanelScreen
import com.nhimz.vocabmaster.ui.screens.FirstWinScreen
import com.nhimz.vocabmaster.ui.screens.GoalPickerScreen
import com.nhimz.vocabmaster.ui.screens.HomeScreen
import com.nhimz.vocabmaster.ui.screens.JumpTestScreen
import com.nhimz.vocabmaster.ui.screens.LoginScreen
import com.nhimz.vocabmaster.ui.screens.PlacementTestScreen
import com.nhimz.vocabmaster.ui.screens.QuizScreen
import com.nhimz.vocabmaster.ui.screens.ResultScreen
import com.nhimz.vocabmaster.ui.screens.SectionCheckpointScreen
import com.nhimz.vocabmaster.ui.screens.SettingsScreen
import com.nhimz.vocabmaster.ui.screens.StatisticsScreen
import com.nhimz.vocabmaster.ui.screens.UnitCheckpointScreen
import com.nhimz.vocabmaster.ui.screens.UnitGuidebookScreen
import com.nhimz.vocabmaster.ui.screens.WelcomeScreen
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel
import com.nhimz.vocabmaster.util.LocalLogger

/**
 * Type-safe NavGraph (D-02, UX-01) — Phase 3 Plan 03-03.
 *
 * Mỗi `entry<Route>` đăng ký một Composable cho một [Screen] subtype. NavDisplay
 * sẽ lookup route trong backStack, gọi entryProvider, và render entry tương ứng.
 *
 * Đây là phần **declaration** của routes — được tạo ra 1 lần tại VocabMasterApp
 * và share cho toàn bộ app. Các call site (HomeScreen, QuizScreen, etc.) không
 * cần biết về NavGraph, chỉ cần gọi `mainViewModel.navigateTo(Route.X)`.
 *
 * Tham số:
 *  - Tất cả ViewModels được inject từ VocabMasterApp (giữ nguyên signature
 *    Hilt injection, NavGraph chỉ là "factory" cho entries).
 *  - [snackbarHostState] được pass vào ResultScreen để wire error pipeline
 *    (D-04 / D-05). Các screen khác sẽ được wire trong follow-up plans.
 *
 * Edge cases:
 *  - Guidebook / JumpTest / SectionCheckpoint / UnitCheckpoint có param (unitId
 *    hoặc sectionId) — entry lambda nhận `key` với typed fields.
 *  - Quiz / Result cũng vậy — `key.cardIds`, `key.xpGained`, etc.
 *  - Loading states (Guidebook cần load từ DB): dùng `produceState` an toàn,
 *    KHÔNG dùng `!!` (ARCH-02).
 */
@Composable
fun vocabMasterEntryProvider(
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
    backupRepository: BackupRepository,
    snackbarHostState: SnackbarHostState
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    // ===== Onboarding flow (Welcome -> Login -> GoalPicker -> PlacementTest -> FirstWin) =====

    entry<Screen.Welcome> {
        WelcomeScreen(
            onStartClick = { mainViewModel.navigateTo(Screen.Login) }
        )
    }

    entry<Screen.Login> {
        LoginScreen(
            onLoginSuccess = { mainViewModel.navigateTo(Screen.GoalPicker) }
        )
    }

    entry<Screen.GoalPicker> {
        GoalPickerScreen(
            onGoalSelected = { minutes ->
                settingsViewModel.setDailyGoal(minutes)
                mainViewModel.navigateTo(Screen.PlacementTest)
            }
        )
    }

    entry<Screen.PlacementTest> {
        PlacementTestScreen(
            onFinished = { levelStr ->
                if (levelStr != null) {
                    mainViewModel.savePlacementLevel(levelStr)
                }
                mainViewModel.navigateTo(Screen.FirstWin)
            },
            onBack = { mainViewModel.goBack() },
            viewModel = placementTestViewModel
        )
    }

    entry<Screen.FirstWin> {
        FirstWinScreen(
            onFinished = { mainViewModel.completeOnboarding() }
        )
    }

    // ===== Top-level tabs (Home / Statistics / Settings) =====

    entry<Screen.Home> {
        HomeScreen(
            onStartQuiz = {
                quizViewModel.startMistakeReview(null)
                mainViewModel.navigateTo(Screen.Quiz())
            },
            onStartFlashcard = { ids ->
                quizViewModel.startMistakeReview(ids)
                mainViewModel.navigateTo(Screen.Quiz(ids))
            },
            onStartCustomQuiz = { _, _, _, _, _ ->
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
    }

    entry<Screen.Statistics> {
        StatisticsScreen(
            viewModel = statisticsViewModel,
            onReviewMistakes = { ids ->
                quizViewModel.startMistakeReview(ids)
                mainViewModel.navigateTo(Screen.Quiz(ids))
            }
        )
    }

    entry<Screen.Settings> {
        SettingsScreen(
            viewModel = mainViewModel,
            settingsViewModel = settingsViewModel,
            notificationScheduler = notificationScheduler,
            onNavigateToDebugPanel = { mainViewModel.navigateTo(Screen.DebugPanel) }
        )
    }

    entry<Screen.DebugPanel> {
        DebugPanelScreen(
            onBack = { mainViewModel.goBack() },
            cdnAudioPlayer = cdnAudioPlayer,
            vocabDatabase = vocabDatabase,
            vocabularyRepository = vocabularyRepository,
            reviewRepository = reviewRepository,
            settingsRepository = settingsRepository,
            backupRepository = backupRepository
        )
    }

    // ===== Sub-screens (drilled from top-level) =====

    entry<Screen.Quiz> { _ ->
        QuizScreen(
            onSessionCompleted = { xp, duration, correct, total, stability, incorrectCardIds, isLevelTest, isPassedLevelTest ->
                mainViewModel.addStudyTime(duration)
                mainViewModel.navigateTo(
                    Screen.Result(
                        xpGained = xp,
                        durationSeconds = duration,
                        correctCount = correct,
                        totalCount = total,
                        sessionStability = stability,
                        incorrectCardIds = incorrectCardIds,
                        isLevelTest = isLevelTest,
                        isPassedLevelTest = isPassedLevelTest
                    )
                )
            },
            onBackToHome = { mainViewModel.navigateTopLevel(Screen.Home) },
            cdnAudioPlayer = cdnAudioPlayer,
            viewModel = quizViewModel
        )
    }

    entry<Screen.Result> { key ->
        ResultScreen(
            xpGained = key.xpGained,
            durationSeconds = key.durationSeconds,
            correctCount = key.correctCount,
            totalCount = key.totalCount,
            averageStability = key.sessionStability,
            incorrectCardIds = key.incorrectCardIds,
            isLevelTest = key.isLevelTest,
            isPassedLevelTest = key.isPassedLevelTest,
            onBackToHome = {
                mainViewModel.updateStreak()
                mainViewModel.navigateTopLevel(Screen.Home)
            },
            onReviewMistakes = { ids ->
                mainViewModel.navigateTo(Screen.Quiz(ids))
            },
            snackbarHostState = snackbarHostState
        )
    }

    entry<Screen.Guidebook> { key ->
        // Async load via produceState — no `!!` (ARCH-02)
        val guidebookState = produceState<UnitGuidebook?>(initialValue = null, key.unitId) {
            value = vocabularyRepository.getGuidebook(key.unitId)
                .onFailure {
                    LocalLogger.e("NavGraph", "Failed to load guidebook for ${key.unitId}", it)
                }
                .getOrNull()
        }.value
        val loaded = guidebookState
        if (loaded != null) {
            UnitGuidebookScreen(
                guidebook = loaded,
                unitTitle = "Sổ tay ngữ pháp",
                onBack = { mainViewModel.goBack() }
            )
        } else {
            // Loading placeholder — keeps the entry on screen while DB query resolves.
            Box(modifier = Modifier.fillMaxSize())
        }
    }

    entry<Screen.JumpTest> { key ->
        JumpTestScreen(
            onBack = { mainViewModel.goBack() },
            onStartTest = {
                quizViewModel.startJumpTest(key.unitId)
                mainViewModel.navigateTo(Screen.Quiz())
            }
        )
    }

    entry<Screen.SectionCheckpoint> { key ->
        SectionCheckpointScreen(
            title = "Bài thi cuối chặng",
            onBack = { mainViewModel.goBack() },
            onStartTest = {
                quizViewModel.startSectionCheckpoint(key.sectionId, null)
                mainViewModel.navigateTo(Screen.Quiz())
            }
        )
    }

    entry<Screen.UnitCheckpoint> { key ->
        UnitCheckpointScreen(
            title = "Bài thi cuối chủ đề",
            onBack = { mainViewModel.goBack() },
            onStartTest = {
                quizViewModel.startUnitCheckpoint(key.unitId)
                mainViewModel.navigateTo(Screen.Quiz())
            }
        )
    }
}
