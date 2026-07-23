package com.nhimz.vocabmaster.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.ui.components.showSnackbar
import com.nhimz.vocabmaster.ui.viewmodel.QuizUiState
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.util.LocalLogger

/**
 * Quiz screen Container (Plan 03-02, Task 1 — and Plan 03-04 snackbar wiring).
 *
 * Responsibilities (mirrors HomeScreen Container from Plan 03-01):
 *  - Collect UI state from [QuizViewModel]'s StateFlow
 *  - Hold transient local UI state (per-question input scratchpad: typed
 *    text, selected option index, scrambled word list, FSRS flip state)
 *  - Drive side effects: navigate to ResultScreen on Completed
 *  - Render the leaf states (Loading / Error / Empty) directly when the
 *    state machine reaches them
 *  - Wire [viewModel.snackbarMessages] to the global [snackbarHostState]
 *    (Plan 03-04 — D-04 / D-05)
 *
 * Pure UI rendering is delegated to [QuizScreenContent]. Public signature
 * is preserved (with one optional [snackbarHostState] parameter) so existing
 * call sites in [com.nhimz.vocabmaster.ui.VocabMasterApp] need no change.
 */
@Composable
fun QuizScreen(
    onSessionCompleted: (xpGained: Int, durationSeconds: Int, correctCount: Int, totalCount: Int, averageStability: Double, incorrectCardIds: List<String>, isLevelTest: Boolean, isPassedLevelTest: Boolean) -> Unit,
    onBackToHome: () -> Unit,
    viewModel: QuizViewModel,
    snackbarHostState: SnackbarHostState? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    // Plan 03-04: wire QuizViewModel.snackbarMessages to the global
    // SnackbarHostState. `rememberUpdatedState` ensures we always invoke the
    // latest flow / host even if the parent re-emits (D-04 / D-05).
    val currentSnackbarHostState by rememberUpdatedState(snackbarHostState)
    val currentSnackbarMessages = viewModel.snackbarMessages
    LaunchedEffect(currentSnackbarHostState) {
        currentSnackbarHostState?.let { host ->
            currentSnackbarMessages.collect { message: SnackbarMessage ->
                if (message.isError) {
                    LocalLogger.e(
                        tag = "QuizScreen",
                        message = "Snackbar error surfaced: ${message.text}"
                    )
                }
                host.showSnackbar(message)
            }
        }
    }

    // Pass data out to navigate to ResultScreen when Completed
    LaunchedEffect(uiState) {
        if (uiState is QuizUiState.Completed) {
            val completedState = uiState as QuizUiState.Completed
            if (completedState.totalCount > 0) {
                onSessionCompleted(
                    completedState.xpGained,
                    completedState.durationSeconds,
                    completedState.correctCount,
                    completedState.totalCount,
                    completedState.averageStability,
                    completedState.incorrectCardIds,
                    completedState.isCheckpointOrJumpTest,
                    completedState.isPassed
                )
            }
        }
    }

    when (val state = uiState) {
        is QuizUiState.Loading -> {
            QuizLoadingSkeleton()
        }
        is QuizUiState.Active -> {
            // The question list and current index are part of the Active state.
            // Per-question input scratchpad lives here in the Container so the
            // Content stays purely a renderer. The keys are bound to the
            // current question index so advancing to the next question wipes
            // the scratchpad cleanly.
            val question = state.questions.getOrNull(state.currentIndex)
            if (question == null) {
                // Defensive: should not happen if the state machine is well-formed
                QuizErrorState(message = "Không tìm thấy câu hỏi hiện tại", onRetry = onBackToHome)
                return
            }

            // Local form state. These are deliberately not part of QuizUiState
            // because they are scratch UI values that have no meaning in the
            // ViewModel until the user submits.
            var typedText by remember(state.currentIndex) { mutableStateOf("") }
            var selectedScrambledWords by remember(state.currentIndex) { mutableStateOf<List<String>>(emptyList()) }
            var isFlipped by remember(state.currentIndex) { mutableStateOf(false) }
            // The selected option index is hoisted to the Container (not the
            // Content) because the ViewModel needs it on submit. The Content
            // only renders the value; tapping an option calls into the
            // [QuizScreenActions.onOptionSelected] callback which writes here.
            var selectedOptionIndex by remember(state.currentIndex) { mutableStateOf<Int?>(null) }

            // Build the value-object state for the Content. This is the only
            // thing the Content sees — it never references the ViewModel.
            val progress = if (state.questions.isNotEmpty()) {
                state.currentIndex.toFloat() / state.questions.size
            } else 0f
            val isFsrs = question.type is QuizType.FSRSTailFlashcard
            val contentState = QuizScreenUiState(
                currentQuestion = question,
                currentIndex = state.currentIndex,
                totalQuestions = state.questions.size,
                hasAnswered = state.isAnswerRevealed,
                selectedOptionIndex = selectedOptionIndex ?: state.selectedOption,
                progress = progress,
                isFsrsFlashcard = isFsrs,
                isFlipped = isFlipped,
                isFsrsRatingSelected = state.isFSRSRatingSelected,
                feedbackBannerCorrect = if (state.isAnswerRevealed) {
                    computeAnswerCorrectness(
                        question = question,
                        selectedOptionIndex = state.selectedOption,
                        typedText = typedText,
                        selectedScrambledWords = selectedScrambledWords
                    )
                } else null,
                correctAnswerText = correctAnswerTextFor(question),
                typedText = typedText,
                selectedScrambledWords = selectedScrambledWords
            )

            val actions = QuizScreenActions(
                onBack = onBackToHome,
                onOptionSelected = { index ->
                    if (!state.isAnswerRevealed) selectedOptionIndex = index
                },
                onScrambledWordSelected = { word, _ ->
                    if (!state.isAnswerRevealed) {
                        selectedScrambledWords = selectedScrambledWords + word
                    }
                },
                onScrambledWordUnselected = { _, index ->
                    if (!state.isAnswerRevealed) {
                        val newList = selectedScrambledWords.toMutableList()
                        if (index in newList.indices) newList.removeAt(index)
                        selectedScrambledWords = newList
                    }
                },
                onTypedTextChanged = { newText ->
                    if (!state.isAnswerRevealed) typedText = newText
                },
                onFlipFlashcard = { isFlipped = true },
                onSubmit = {
                    viewModel.submitAnswer(
                        optionIndex = selectedOptionIndex,
                        textAnswer = typedText.takeIf { it.isNotBlank() },
                        selectedWordsForScrambled = selectedScrambledWords
                            .takeIf { it.isNotEmpty() }
                    )
                },
                onContinue = {
                    viewModel.nextQuestion()
                    // Reset the scratchpad for the next question. Using key
                    // (state.currentIndex) above already re-creates the
                    // remember state, but explicit reset is defensive.
                    typedText = ""
                    selectedScrambledWords = emptyList()
                    isFlipped = false
                    selectedOptionIndex = null
                },
                onFsrsRating = { r ->
                    viewModel.submitAnswer(fsrsRating = r)
                },
                onPlayAudio = { url ->
                    viewModel.playAudio(url)
                }
            )

            key(state.currentIndex) {
                QuizScreenContent(
                    state = contentState,
                    actions = actions
                )
            }
        }
        is QuizUiState.Completed -> {
            if (state.totalCount == 0) {
                QuizEmptyState(onBack = onBackToHome)
            }
            // Non-zero Completed states are handled by the LaunchedEffect above
            // which navigates to ResultScreen.
        }
        is QuizUiState.Error -> {
            QuizErrorState(message = state.message, onRetry = onBackToHome)
        }
    }
}
