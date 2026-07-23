package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.domain.usecase.*
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quiz session ViewModel (Plan 03-02, Task 2).
 *
 * Survives configuration changes AND process death via [SavedStateHandle].
 * The whitelisted persistence keys cover everything the user-visible Quiz UI
 * needs to render correctly after the host process is killed (low memory)
 * and recreated:
 *
 *  - Quiz kind (NODE / REVIEW / UNIT_CHECKPOINT / JUMP_TEST /
 *    SECTION_CHECKPOINT / MISTAKE_REVIEW) so the matching
 *    [LoadQuizSessionUseCase] request can be rebuilt.
 *  - The IDs/parameters that distinguish the quiz (nodeId, unitId, etc.)
 *  - The active question index so the user lands back on the same question.
 *  - The per-session cumulative state (correctAnswersCount, xpGained,
 *    incorrectCardIds) so progress is not lost.
 *  - The revealed-answer state of the current question (selectedOption,
 *    isAnswerRevealed, isFSRSRatingSelected) so the user does not have to
 *    re-answer a question they already submitted.
 *
 * Per the threat model in 03-02-PLAN.md (T-03-02), only primitive
 * Bundle-safe values (String / Int / Boolean / ArrayList<String>) are stored
 * — no PII, no card bodies, no full question lists. Large payloads would
 * also risk the ~1MB Bundle limit on rotation.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loadQuizSessionUseCase: LoadQuizSessionUseCase,
    private val evaluateAnswerUseCase: EvaluateAnswerUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    private val completeQuizSessionUseCase: CompleteQuizSessionUseCase,
    private val audioPlayerUseCase: AudioPlayerUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
        private const val DEFAULT_LOAD_ERROR = "Không tải được nội dung bài học"

        // ====== Keys for SavedStateHandle persistence ======
        // Quiz-kind discriminator + identifying parameters
        private const val KEY_QUIZ_KIND = "quiz_kind"
        private const val KEY_NODE_ID = "quiz_node_id"
        private const val KEY_SESSION_INDEX = "quiz_session_index"
        private const val KEY_UNIT_ID = "quiz_unit_id"
        private const val KEY_SECTION_ID = "quiz_section_id"
        private const val KEY_NEXT_CEFR = "quiz_next_cefr"

        // Active question + per-question UI state
        private const val KEY_CURRENT_INDEX = "quiz_current_index"
        private const val KEY_SELECTED_OPTION = "quiz_selected_option"
        private const val KEY_IS_ANSWER_REVEALED = "quiz_is_answer_revealed"
        private const val KEY_IS_FSRS_RATING_SELECTED = "quiz_is_fsrs_rating_selected"

        // Cumulative session progress (Plan 03-02 hardening)
        private const val KEY_CORRECT_COUNT = "quiz_correct_count"
        private const val KEY_XP_GAINED = "quiz_xp_gained"
        private const val KEY_INCORRECT_CARD_IDS = "quiz_incorrect_card_ids"

        /**
         * Whitelist of valid persistence keys for verification in tests.
         * If a new key is added to [SavedStateHandle] in this ViewModel, it
         * MUST be added here so the persistence test stays accurate.
         */
        val PERSISTENCE_KEYS = setOf(
            KEY_QUIZ_KIND,
            KEY_NODE_ID,
            KEY_SESSION_INDEX,
            KEY_CURRENT_INDEX,
            KEY_SELECTED_OPTION,
            KEY_IS_ANSWER_REVEALED,
            KEY_IS_FSRS_RATING_SELECTED,
            KEY_UNIT_ID,
            KEY_SECTION_ID,
            KEY_NEXT_CEFR,
            KEY_CORRECT_COUNT,
            KEY_XP_GAINED,
            KEY_INCORRECT_CARD_IDS
        )
    }

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    /**
     * One-shot snackbar messages surfaced from quiz-session operations
     * (load failures, submit failures, etc.). Backed by a [MutableSharedFlow]
     * with a small buffer so the Container can collect via `LaunchedEffect`
     * even if the emission happens during a recomposition.
     *
     * Container ([com.nhimz.vocabmaster.ui.screens.QuizScreen]) reads
     * [snackbarMessages] and forwards each emission to the global
     * [androidx.compose.material3.SnackbarHostState] hosted in
     * [com.nhimz.vocabmaster.ui.VocabMasterApp].
     */
    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val snackbarMessages: SharedFlow<SnackbarMessage> = _snackbarMessages.asSharedFlow()

    /**
     * Emit a [SnackbarMessage] for the Container to display. Used in error
     * paths (e.g. session load failure, answer submit failure) to surface
     * a user-visible notification rather than silently swallowing.
     */
    private suspend fun emitSnackbar(message: SnackbarMessage) {
        _snackbarMessages.emit(message)
    }

    private var sessionStartTime: Long = 0
    private var pendingRestoreIndex: Int? = null
    private var pendingRestoreFromSavedState: Boolean = false

    init {
        val restoredKind = savedStateHandle.get<String>(KEY_QUIZ_KIND)
        if (restoredKind != null) {
            val restoredIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
            pendingRestoreIndex = restoredIndex
            pendingRestoreFromSavedState = true
            restoreSession(restoredKind)
        }
    }

    private fun restoreSession(kind: String) {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            val request = when (kind) {
                "NODE" -> {
                    val nodeId = savedStateHandle.get<String>(KEY_NODE_ID) ?: return@launch
                    val sessionIndex = savedStateHandle.get<Int>(KEY_SESSION_INDEX) ?: 0
                    QuizSessionRequest.NodeSession(nodeId, sessionIndex)
                }
                "REVIEW" -> {
                    val nodeId = savedStateHandle.get<String>(KEY_NODE_ID) ?: return@launch
                    val unitId = savedStateHandle.get<String>(KEY_UNIT_ID)
                    val sectionId = savedStateHandle.get<String>(KEY_SECTION_ID)
                    QuizSessionRequest.ReviewNode(nodeId, unitId, sectionId)
                }
                "UNIT_CHECKPOINT" -> {
                    val unitId = savedStateHandle.get<String>(KEY_UNIT_ID) ?: return@launch
                    QuizSessionRequest.UnitCheckpoint(unitId)
                }
                "JUMP_TEST" -> {
                    val unitId = savedStateHandle.get<String>(KEY_UNIT_ID) ?: return@launch
                    QuizSessionRequest.JumpTest(unitId)
                }
                "SECTION_CHECKPOINT" -> {
                    val sectionId = savedStateHandle.get<String>(KEY_SECTION_ID) ?: return@launch
                    val nextCefr = savedStateHandle.get<String>(KEY_NEXT_CEFR)
                    QuizSessionRequest.SectionCheckpoint(sectionId, nextCefr)
                }
                "MISTAKE_REVIEW" -> {
                    QuizSessionRequest.MistakeReview(null)
                }
                else -> return@launch
            }

            loadQuizSessionUseCase(request).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to restore session", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    private fun handleLoadedSession(data: QuizSessionData) {
        if (data.questions.isEmpty()) {
            clearPersistenceKeys()
            _uiState.value = QuizUiState.Completed(0, 0, 0, 0)
            return
        }

        val restoredIndex = pendingRestoreIndex ?: 0
        val isRestoring = pendingRestoreFromSavedState
        pendingRestoreIndex = null
        pendingRestoreFromSavedState = false
        val targetIndex = restoredIndex.coerceIn(0, data.questions.lastIndex)
        savedStateHandle[KEY_CURRENT_INDEX] = targetIndex

        // Plan 03-02 hardening: if we are restoring from a saved state, pull
        // the cumulative progress + per-question answer state back out of the
        // SavedStateHandle. On a fresh start, default everything to zero/null.
        val restoredCorrectCount = if (isRestoring) {
            savedStateHandle.get<Int>(KEY_CORRECT_COUNT) ?: 0
        } else 0
        val restoredXpGained = if (isRestoring) {
            savedStateHandle.get<Int>(KEY_XP_GAINED) ?: 0
        } else 0
        val restoredIncorrectCardIds = if (isRestoring) {
            savedStateHandle.get<ArrayList<String>>(KEY_INCORRECT_CARD_IDS)?.toList()
                ?: emptyList()
        } else emptyList()
        val restoredSelectedOption = if (isRestoring) {
            savedStateHandle.get<Int>(KEY_SELECTED_OPTION)
        } else null
        val restoredIsAnswerRevealed = if (isRestoring) {
            savedStateHandle.get<Boolean>(KEY_IS_ANSWER_REVEALED) ?: false
        } else false
        val restoredIsFsrsRatingSelected = if (isRestoring) {
            savedStateHandle.get<Boolean>(KEY_IS_FSRS_RATING_SELECTED) ?: false
        } else false

        val active = QuizUiState.Active(
            questions = data.questions,
            currentIndex = targetIndex,
            correctAnswersCount = restoredCorrectCount,
            xpGained = restoredXpGained,
            selectedOption = restoredSelectedOption,
            isAnswerRevealed = restoredIsAnswerRevealed,
            startTimeMillis = System.currentTimeMillis(),
            incorrectCardIds = restoredIncorrectCardIds,
            isFSRSRatingSelected = restoredIsFsrsRatingSelected,
            nodeId = data.nodeId,
            sessionId = data.sessionId,
            isSectionCheckpoint = data.isSectionCheckpoint,
            isJumpTest = data.isJumpTest,
            isUnitCheckpoint = data.isUnitCheckpoint,
            nextSectionCefr = data.nextSectionCefr,
            unitIdForJumpTest = data.unitIdForJumpTest,
            unitIdForUnitCheckpoint = data.unitIdForUnitCheckpoint
        )
        _uiState.value = active

        // Persist the resolved cumulative state so a subsequent process
        // death immediately after restore still sees the same numbers.
        persistActiveState(active)
    }

    /**
     * Mirror every persistable field of an [QuizUiState.Active] into the
     * [SavedStateHandle]. Called after every state transition that updates
     * any of these fields.
     */
    private fun persistActiveState(state: QuizUiState.Active) {
        savedStateHandle[KEY_CURRENT_INDEX] = state.currentIndex
        savedStateHandle[KEY_SELECTED_OPTION] = state.selectedOption
        savedStateHandle[KEY_IS_ANSWER_REVEALED] = state.isAnswerRevealed
        savedStateHandle[KEY_IS_FSRS_RATING_SELECTED] = state.isFSRSRatingSelected
        savedStateHandle[KEY_CORRECT_COUNT] = state.correctAnswersCount
        savedStateHandle[KEY_XP_GAINED] = state.xpGained
        // ArrayList is a Bundle-safe collection type; List<String> would
        // crash the SavedStateHandle.encode pipeline.
        savedStateHandle[KEY_INCORRECT_CARD_IDS] = ArrayList(state.incorrectCardIds)
    }

    private fun clearPersistenceKeys() {
        savedStateHandle.remove<String>(KEY_QUIZ_KIND)
        savedStateHandle.remove<String>(KEY_NODE_ID)
        savedStateHandle.remove<Int>(KEY_SESSION_INDEX)
        savedStateHandle.remove<Int>(KEY_CURRENT_INDEX)
        savedStateHandle.remove<Int>(KEY_SELECTED_OPTION)
        savedStateHandle.remove<Boolean>(KEY_IS_ANSWER_REVEALED)
        savedStateHandle.remove<Boolean>(KEY_IS_FSRS_RATING_SELECTED)
        savedStateHandle.remove<String>(KEY_UNIT_ID)
        savedStateHandle.remove<String>(KEY_SECTION_ID)
        savedStateHandle.remove<String>(KEY_NEXT_CEFR)
        savedStateHandle.remove<Int>(KEY_CORRECT_COUNT)
        savedStateHandle.remove<Int>(KEY_XP_GAINED)
        savedStateHandle.remove<ArrayList<String>>(KEY_INCORRECT_CARD_IDS)
    }

    private fun setupPersistenceKeys(kind: String, block: () -> Unit) {
        clearPersistenceKeys()
        savedStateHandle[KEY_QUIZ_KIND] = kind
        savedStateHandle[KEY_CURRENT_INDEX] = 0
        block()
    }

    fun startNodeSession(nodeId: String, sessionIndex: Int) {
        setupPersistenceKeys("NODE") {
            savedStateHandle[KEY_NODE_ID] = nodeId
            savedStateHandle[KEY_SESSION_INDEX] = sessionIndex
        }
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.NodeSession(nodeId, sessionIndex)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load node session", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun startReviewNode(nodeId: String, unitId: String? = null, sectionId: String? = null) {
        setupPersistenceKeys("REVIEW") {
            savedStateHandle[KEY_NODE_ID] = nodeId
            if (unitId != null) savedStateHandle[KEY_UNIT_ID] = unitId
            if (sectionId != null) savedStateHandle[KEY_SECTION_ID] = sectionId
        }
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.ReviewNode(nodeId, unitId, sectionId)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load review node", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun startUnitCheckpoint(unitId: String) {
        setupPersistenceKeys("UNIT_CHECKPOINT") {
            savedStateHandle[KEY_UNIT_ID] = unitId
        }
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.UnitCheckpoint(unitId)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load unit checkpoint", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun startJumpTest(unitId: String) {
        setupPersistenceKeys("JUMP_TEST") {
            savedStateHandle[KEY_UNIT_ID] = unitId
        }
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.JumpTest(unitId)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load jump test", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun startMistakeReview(cardIds: List<String>?) {
        setupPersistenceKeys("MISTAKE_REVIEW") {}
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.MistakeReview(cardIds)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load mistake review", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun startSectionCheckpoint(sectionId: String, nextSectionCefr: String?) {
        setupPersistenceKeys("SECTION_CHECKPOINT") {
            savedStateHandle[KEY_SECTION_ID] = sectionId
            if (nextSectionCefr != null) savedStateHandle[KEY_NEXT_CEFR] = nextSectionCefr
        }
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            sessionStartTime = System.currentTimeMillis()

            loadQuizSessionUseCase(QuizSessionRequest.SectionCheckpoint(sectionId, nextSectionCefr)).fold(
                onSuccess = { data ->
                    handleLoadedSession(data)
                },
                onFailure = { error ->
                    LocalLogger.e(TAG, "Failed to load section checkpoint", error)
                    val msg = error.message ?: DEFAULT_LOAD_ERROR
                    _uiState.value = QuizUiState.Error(msg)
                    emitSnackbar(SnackbarMessage(text = msg, isError = true))
                }
            )
        }
    }

    fun submitAnswer(
        optionIndex: Int? = null,
        textAnswer: String? = null,
        selectedWordsForScrambled: List<String>? = null,
        fsrsRating: Rating? = null
    ) {
        val state = _uiState.value as? QuizUiState.Active ?: return
        if (state.isAnswerRevealed) return

        val currentQuestion = state.questions[state.currentIndex]
        val responseTimeMs = System.currentTimeMillis() - state.startTimeMillis

        val evaluationResult = evaluateAnswerUseCase(
            type = currentQuestion.type,
            optionIndex = optionIndex,
            textAnswer = textAnswer,
            selectedWordsForScrambled = selectedWordsForScrambled,
            fsrsRating = fsrsRating
        )

        evaluationResult.fold(
            onSuccess = { answerResult ->
                val isCorrect = answerResult.isCorrect
                val xpEarned = answerResult.xpEarned

                val updatedCorrectCount = state.correctAnswersCount + (if (isCorrect) 1 else 0)
                val updatedXpGained = state.xpGained + xpEarned

                val updatedIncorrectCardIds = if (!isCorrect && currentQuestion.type !is QuizType.Introduction) {
                    val cardId = currentQuestion.itemWithCard?.card?.cardId
                    cardId?.let { state.incorrectCardIds + it } ?: state.incorrectCardIds
                } else {
                    state.incorrectCardIds
                }

                val updatedQuestions = if (isCorrect || currentQuestion.type is QuizType.FSRSTailFlashcard || currentQuestion.type is QuizType.Introduction) {
                    state.questions
                } else {
                    state.questions + currentQuestion
                }

                // Synchronously flip the state to revealed Active to guard against rapid double tap
                val nextState = state.copy(
                    questions = updatedQuestions,
                    selectedOption = optionIndex,
                    isAnswerRevealed = true,
                    correctAnswersCount = updatedCorrectCount,
                    xpGained = updatedXpGained,
                    isFSRSRatingSelected = fsrsRating != null || answerResult.rating != null,
                    incorrectCardIds = updatedIncorrectCardIds.distinct()
                )
                _uiState.value = nextState

                // Plan 03-02 hardening: persist the updated cumulative state
                // so a process death at this exact moment does not lose the
                // correct/incorrect count, the XP, or the selected option.
                persistActiveState(nextState)

                // Launch coroutine to submit the review asynchronously
                viewModelScope.launch {
                    submitReviewUseCase(
                        question = currentQuestion,
                        isCorrect = isCorrect,
                        responseTimeMs = responseTimeMs,
                        xpEarned = xpEarned,
                        explicitRating = fsrsRating ?: answerResult.rating
                    ).fold(
                        onSuccess = {
                            // Do nothing, UI state is already updated
                        },
                        onFailure = { error ->
                            LocalLogger.e(TAG, "Failed to submit review", error)
                            val msg = error.message ?: "Lỗi ghi nhận kết quả ôn tập"
                            _uiState.value = QuizUiState.Error(msg)
                            emitSnackbar(SnackbarMessage(text = msg, isError = true))
                        }
                    )
                }
            },
            onFailure = { error ->
                LocalLogger.e(TAG, "Failed to evaluate answer", error)
                val msg = error.message ?: "Lỗi đánh giá câu trả lời"
                _uiState.value = QuizUiState.Error(msg)
                emitSnackbar(SnackbarMessage(text = msg, isError = true))
            }
        )
    }

    fun nextQuestion() {
        val state = _uiState.value as? QuizUiState.Active ?: return
        if (!state.isAnswerRevealed) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()

            // Synchronously transition to Loading first to guard against double nextQuestion trigger
            _uiState.value = QuizUiState.Loading

            viewModelScope.launch {
                val input = QuizCompletionInput(
                    correctCount = state.correctAnswersCount,
                    totalQuestions = state.questions.size,
                    xpGained = state.xpGained,
                    nodeId = state.nodeId,
                    isJumpTest = state.isJumpTest,
                    isSectionCheckpoint = state.isSectionCheckpoint,
                    isUnitCheckpoint = state.isUnitCheckpoint,
                    nextSectionCefr = state.nextSectionCefr,
                    unitIdForJumpTest = state.unitIdForJumpTest,
                    unitIdForUnitCheckpoint = state.unitIdForUnitCheckpoint
                )

                completeQuizSessionUseCase(input).fold(
                    onSuccess = { outcome ->
                        clearPersistenceKeys()
                        _uiState.value = QuizUiState.Completed(
                            xpGained = state.xpGained,
                            correctCount = state.correctAnswersCount,
                            totalCount = state.questions.size,
                            durationSeconds = durationSeconds,
                            averageStability = 0.0,
                            isPassed = outcome.isPassed,
                            incorrectCardIds = state.incorrectCardIds,
                            isCheckpointOrJumpTest = state.isSectionCheckpoint || state.isJumpTest || state.isUnitCheckpoint
                        )
                    },
                    onFailure = { error ->
                        LocalLogger.e(TAG, "Failed to complete quiz session", error)
                        val msg = error.message ?: "Lỗi hoàn thành bài học"
                        _uiState.value = QuizUiState.Error(msg)
                        emitSnackbar(SnackbarMessage(text = msg, isError = true))
                    }
                )
            }
        } else {
            // Plan 03-02 hardening: when advancing to the next question we
            // clear the per-question answer state in the SavedStateHandle
            // (selectedOption / isAnswerRevealed / isFSRSRatingSelected) but
            // keep the cumulative progress (correctCount, xpGained,
            // incorrectCardIds) untouched.
            savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
            savedStateHandle[KEY_SELECTED_OPTION] = null
            savedStateHandle[KEY_IS_ANSWER_REVEALED] = false
            savedStateHandle[KEY_IS_FSRS_RATING_SELECTED] = false
            val nextState = state.copy(
                currentIndex = nextIndex,
                selectedOption = null,
                isAnswerRevealed = false,
                isFSRSRatingSelected = false,
                startTimeMillis = System.currentTimeMillis()
            )
            _uiState.value = nextState
            // Re-persist so currentIndex + cleared per-question state are
            // durable together.
            persistActiveState(nextState)
        }
    }

    fun playAudio(url: String?) {
        audioPlayerUseCase.playAudio(url)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerUseCase.stop()
    }
}
