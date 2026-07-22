package com.nhimz.vocabmaster.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.ui.components.showSnackbar
import com.nhimz.vocabmaster.util.LocalLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Container cho màn hình Result (Plan 03-03, Task 2 — ARCH-01 / ARCH-02).
 *
 * Tách Container/Content: Composable này chỉ làm 3 việc:
 *  1. Nhận các tham số rời rạc từ caller (giữ nguyên signature cũ để không phá
 *     VocabMasterApp.kt) và convert sang [ResultUiState] bất biến.
 *  2. Nếu [errorMessages] là SharedFlow<SnackbarMessage>, collect và gọi
 *     [snackbarHostState.showSnackbar] theo D-04 (Log and display) + D-05
 *     (Prioritize Snackbar).
 *  3. Delegate 100% rendering cho [ResultScreenContent].
 *
 * Lý do giữ Container ở đây (thay vì nhét hết vào Content):
 *  - Có chỗ để wire [snackbarHostState] mà không leak Material3 vào pure UI.
 *  - Có chỗ để gọi [LocalLogger.e] cho mỗi error trước khi show (D-04).
 *  - Có chỗ để pre-warm ResultUiState với derived values (accuracy, duration).
 *
 * Tham số [snackbarHostState] optional: nếu `null`, Container sẽ chỉ
 * delegate thẳng sang Content (back-compat cho caller cũ). Khi [errorMessages]
 * cũng `null`, không có gì được collect.
 */
@Composable
fun ResultScreen(
    xpGained: Int,
    durationSeconds: Int,
    correctCount: Int,
    totalCount: Int,
    averageStability: Double,
    incorrectCardIds: List<String> = emptyList(),
    isLevelTest: Boolean = false,
    isPassedLevelTest: Boolean = false,
    onBackToHome: () -> Unit,
    onReviewMistakes: (List<String>) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    errorMessages: Flow<SnackbarMessage> = emptyFlow()
) {
    val state = ResultUiState(
        xpGained = xpGained,
        durationSeconds = durationSeconds,
        correctCount = correctCount,
        totalCount = totalCount,
        averageStability = averageStability,
        incorrectCardIds = incorrectCardIds,
        isLevelTest = isLevelTest,
        isPassedLevelTest = isPassedLevelTest
    )

    // Wire error pipeline: D-04 — log + display; D-05 — snackbar over dialog.
    // The `rememberUpdatedState` ensures we always invoke the latest flow
    // even if the parent re-emits (e.g., orientation change after the
    // initial composition).
    val currentErrorMessages by rememberUpdatedState(errorMessages)
    val currentSnackbarHostState by rememberUpdatedState(snackbarHostState)

    LaunchedEffect(state.xpGained, state.isLevelTest) {
        if (currentSnackbarHostState != null) {
            currentErrorMessages.collect { message ->
                if (message.isError) {
                    LocalLogger.e(
                        tag = "ResultScreen",
                        message = "Snackbar error surfaced: ${message.text}"
                    )
                }
                currentSnackbarHostState?.showSnackbar(message)
            }
        }
    }

    ResultScreenContent(
        state = state,
        onBackToHome = onBackToHome,
        onReviewMistakes = onReviewMistakes
    )
}
