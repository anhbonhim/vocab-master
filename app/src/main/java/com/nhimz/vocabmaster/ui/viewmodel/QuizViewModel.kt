package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.domain.usecase.*
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loadQuizSessionUseCase: LoadQuizSessionUseCase,
    private val evaluateAnswerUseCase: EvaluateAnswerUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    private val completeQuizSessionUseCase: CompleteQuizSessionUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
        private const val DEFAULT_LOAD_ERROR = "Không tải được nội dung bài học"

        // Keys for SavedStateHandle persistence
        private const val KEY_QUIZ_KIND = "quiz_kind"
        private const val KEY_NODE_ID = "quiz_node_id"
        private const val KEY_SESSION_INDEX = "quiz_session_index"
        private const val KEY_CURRENT_INDEX = "quiz_current_index"
        private const val KEY_UNIT_ID = "quiz_unit_id"
        private const val KEY_SECTION_ID = "quiz_section_id"
        private const val KEY_NEXT_CEFR = "quiz_next_cefr"
        
        // Whitelist of valid persistence keys for verification in tests
        val PERSISTENCE_KEYS = setOf(
            KEY_QUIZ_KIND,
            KEY_NODE_ID,
            KEY_SESSION_INDEX,
            KEY_CURRENT_INDEX,
            KEY_UNIT_ID,
            KEY_SECTION_ID,
            KEY_NEXT_CEFR
        )
    }

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var sessionStartTime: Long = 0
    private var pendingRestoreIndex: Int? = null

    init {
        val restoredKind = savedStateHandle.get<String>(KEY_QUIZ_KIND)
        if (restoredKind != null) {
            val restoredIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX) ?: 0
            pendingRestoreIndex = restoredIndex
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
        pendingRestoreIndex = null
        val targetIndex = restoredIndex.coerceIn(0, data.questions.lastIndex)
        savedStateHandle[KEY_CURRENT_INDEX] = targetIndex

        _uiState.value = QuizUiState.Active(
            questions = data.questions,
            currentIndex = targetIndex,
            correctAnswersCount = 0,
            xpGained = 0,
            selectedOption = null,
            isAnswerRevealed = false,
            startTimeMillis = System.currentTimeMillis(),
            incorrectCardIds = emptyList(),
            nodeId = data.nodeId,
            sessionId = data.sessionId,
            isSectionCheckpoint = data.isSectionCheckpoint,
            isJumpTest = data.isJumpTest,
            isUnitCheckpoint = data.isUnitCheckpoint,
            nextSectionCefr = data.nextSectionCefr,
            unitIdForJumpTest = data.unitIdForJumpTest,
            unitIdForUnitCheckpoint = data.unitIdForUnitCheckpoint
        )
    }

    private fun clearPersistenceKeys() {
        savedStateHandle.remove<String>(KEY_QUIZ_KIND)
        savedStateHandle.remove<String>(KEY_NODE_ID)
        savedStateHandle.remove<Int>(KEY_SESSION_INDEX)
        savedStateHandle.remove<Int>(KEY_CURRENT_INDEX)
        savedStateHandle.remove<String>(KEY_UNIT_ID)
        savedStateHandle.remove<String>(KEY_SECTION_ID)
        savedStateHandle.remove<String>(KEY_NEXT_CEFR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
                    _uiState.value = QuizUiState.Error(error.message ?: DEFAULT_LOAD_ERROR)
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
            question = currentQuestion,
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
                _uiState.value = state.copy(
                    questions = updatedQuestions,
                    selectedOption = optionIndex,
                    isAnswerRevealed = true,
                    correctAnswersCount = updatedCorrectCount,
                    xpGained = updatedXpGained,
                    isFSRSRatingSelected = fsrsRating != null || answerResult.rating != null,
                    incorrectCardIds = updatedIncorrectCardIds.distinct()
                )

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
                            _uiState.value = QuizUiState.Error(error.message ?: "Lỗi ghi nhận kết quả ôn tập")
                        }
                    )
                }
            },
            onFailure = { error ->
                LocalLogger.e(TAG, "Failed to evaluate answer", error)
                _uiState.value = QuizUiState.Error(error.message ?: "Lỗi đánh giá câu trả lời")
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
                        _uiState.value = QuizUiState.Error(error.message ?: "Lỗi hoàn thành bài học")
                    }
                )
            }
        } else {
            savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                selectedOption = null,
                isAnswerRevealed = false,
                isFSRSRatingSelected = false,
                startTimeMillis = System.currentTimeMillis()
            )
        }
    }
}
