package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.Scheduler
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.domain.usecase.MapRatingUseCase
import com.nhimz.vocabmaster.domain.usecase.UpdateStreakUseCase
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuizSessionState {
    object Loading : QuizSessionState()
    data class Active(
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val correctAnswersCount: Int,
        val xpGained: Int,
        val selectedOption: Int?,
        val isAnswerRevealed: Boolean,
        val startTimeMillis: Long,
        val isFSRSRatingSelected: Boolean = false,
        val incorrectCardIds: List<String> = emptyList(),
        // Configuration metadata
        val nodeId: String? = null,
        val sessionId: String? = null,
        val isSectionCheckpoint: Boolean = false,
        val isJumpTest: Boolean = false,
        val isUnitCheckpoint: Boolean = false,
        val nextSectionCefr: String? = null,
        val unitIdForJumpTest: String? = null,
        val unitIdForUnitCheckpoint: String? = null
    ) : QuizSessionState()
    data class Completed(
        val xpGained: Int,
        val correctCount: Int,
        val totalCount: Int,
        val durationSeconds: Int,
        val averageStability: Double = 0.0,
        val isPassed: Boolean = false,
        val incorrectCardIds: List<String> = emptyList(),
        val isCheckpointOrJumpTest: Boolean = false
    ) : QuizSessionState()

    data class Error(val message: String) : QuizSessionState()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val mapRatingUseCase: MapRatingUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
        private const val DEFAULT_LOAD_ERROR = "Không tải được nội dung bài học"
    }

    private val _sessionState = MutableStateFlow<QuizSessionState>(QuizSessionState.Loading)
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private var sessionStartTime: Long = 0

    fun startNodeSession(nodeId: String, sessionIndex: Int) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()

            val sessions = vocabularyRepository.getSessionsByNode(nodeId).getOrElse { error ->
                LocalLogger.e(TAG, "Failed to load sessions for node $nodeId", error)
                _sessionState.value = QuizSessionState.Error(
                    error.message ?: DEFAULT_LOAD_ERROR
                )
                return@launch
            }
            val sessionToRun = sessions.getOrNull(sessionIndex)

            if (sessionToRun == null) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            val rawQuestions = vocabularyRepository.getQuestionsBySession(sessionToRun.id).getOrElse { error ->
                LocalLogger.e(TAG, "Failed to load questions for session ${sessionToRun.id}", error)
                _sessionState.value = QuizSessionState.Error(
                    error.message ?: DEFAULT_LOAD_ERROR
                )
                return@launch
            }
            val questionsList = mutableListOf<QuizQuestion>()

            for (q in rawQuestions) {
                val card = vocabularyRepository.getCardByQuestionId(q.id)?.let { QuestionWithCard(q, it) }

                val quizType = when (q.type) {
                    QuestionType.INTRODUCTION -> {
                        QuizType.Introduction(card, q.prompt, q.audioUrl)
                    }
                    QuestionType.FILL_IN_BLANK, QuestionType.MULTIPLE_CHOICE -> {
                        QuizType.MultipleChoice(
                            itemWithCard = card,
                            direction = QuestionDirection.EN_TO_VI,
                            prompt = q.prompt,
                            options = q.options ?: emptyList(),
                            correctIndex = q.correctIndex ?: 0
                        )
                    }
                    QuestionType.SCRAMBLED -> {
                        QuizType.ScrambledSentence(
                            itemWithCard = card,
                            scrambledWords = q.scrambledWords ?: emptyList(),
                            correctSentence = q.correctSentence ?: ""
                        )
                    }
                    QuestionType.LISTENING -> {
                        QuizType.Listening(
                            itemWithCard = card,
                            prompt = q.prompt,
                            audioUrl = q.audioUrl,
                            audioUrlSlow = q.audioUrlSlow,
                            options = q.options,
                            correctIndex = q.correctIndex
                        )
                    }
                    QuestionType.MATCHING -> {
                        QuizType.Matching(
                            itemWithCard = card,
                            prompt = q.prompt,
                            pairs = q.matchingPairs ?: emptyList()
                        )
                    }
                    QuestionType.TYPING -> {
                        QuizType.Typing(
                            itemWithCard = card,
                            prompt = q.prompt,
                            correctSentence = q.correctSentence ?: "",
                            audioUrl = q.audioUrl,
                            audioUrlSlow = q.audioUrlSlow
                        )
                    }
                }
                questionsList.add(QuizQuestion(quizType))
            }

            if (questionsList.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            _sessionState.value = QuizSessionState.Active(
                questions = questionsList,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                incorrectCardIds = emptyList(),
                nodeId = nodeId,
                sessionId = sessionToRun.id
            )
        }
    }

    fun startReviewNode(nodeId: String, unitId: String? = null, sectionId: String? = null) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()

            val now = System.currentTimeMillis()
            // 3-tier fallback handled in repository: unit -> section -> global.
            val dueCards = if (unitId != null && sectionId != null) {
                vocabularyRepository.getDueCardsScoped(unitId, sectionId, now, 15)
            } else {
                vocabularyRepository.getDueCards(now, 15).first()
            }

            if (dueCards.isEmpty()) {
                // If nothing is due, gracefully complete
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            val questionsList = dueCards.map {
                QuizQuestion(QuizType.FSRSTailFlashcard(it))
            }

            _sessionState.value = QuizSessionState.Active(
                questions = questionsList,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                incorrectCardIds = emptyList(),
                nodeId = nodeId
            )
        }
    }

    /**
     * Start a Unit Checkpoint test — loads representative questions from all LESSON/REVIEW
     * nodes in the unit (excluding the UNIT_CHECKPOINT node itself, GUIDEBOOK and JUMP_TEST).
     * Pass >= 80% → mark the UNIT_CHECKPOINT node for this unit as completed.
     */
    fun startUnitCheckpoint(unitId: String) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()

            val nodes = vocabularyRepository.getNodesByUnit(unitId).first()
            val quizNodeTypes = setOf(
                com.nhimz.vocabmaster.domain.model.NodeType.LESSON,
                com.nhimz.vocabmaster.domain.model.NodeType.REVIEW
            )
            val quizNodes = nodes.filter { it.type in quizNodeTypes }

            val questionsList = mutableListOf<QuizQuestion>()
            for (node in quizNodes) {
                val sessions = vocabularyRepository.getSessionsByNode(node.id).getOrElse { error ->
                    LocalLogger.e(TAG, "Failed to load sessions for node ${node.id}", error)
                    _sessionState.value = QuizSessionState.Error(
                        error.message ?: DEFAULT_LOAD_ERROR
                    )
                    return@launch
                }
                for (session in sessions) {
                    val rawQuestions = vocabularyRepository.getQuestionsBySession(session.id).getOrElse { error ->
                        LocalLogger.e(TAG, "Failed to load questions for session ${session.id}", error)
                        _sessionState.value = QuizSessionState.Error(
                            error.message ?: DEFAULT_LOAD_ERROR
                        )
                        return@launch
                    }
                    for (q in rawQuestions) {
                        val card = vocabularyRepository.getCardByQuestionId(q.id)?.let { QuestionWithCard(q, it) }
                        val quizType = when (q.type) {
                            com.nhimz.vocabmaster.domain.model.QuestionType.INTRODUCTION ->
                                QuizType.Introduction(card, q.prompt, q.audioUrl)
                            com.nhimz.vocabmaster.domain.model.QuestionType.FILL_IN_BLANK,
                            com.nhimz.vocabmaster.domain.model.QuestionType.MULTIPLE_CHOICE ->
                                QuizType.MultipleChoice(
                                    itemWithCard = card,
                                    direction = QuestionDirection.EN_TO_VI,
                                    prompt = q.prompt,
                                    options = q.options ?: emptyList(),
                                    correctIndex = q.correctIndex ?: 0
                                )
                            com.nhimz.vocabmaster.domain.model.QuestionType.SCRAMBLED ->
                                QuizType.ScrambledSentence(
                                    itemWithCard = card,
                                    scrambledWords = q.scrambledWords ?: emptyList(),
                                    correctSentence = q.correctSentence ?: ""
                                )
                            com.nhimz.vocabmaster.domain.model.QuestionType.LISTENING ->
                                QuizType.Listening(
                                    itemWithCard = card,
                                    prompt = q.prompt,
                                    audioUrl = q.audioUrl,
                                    audioUrlSlow = q.audioUrlSlow,
                                    options = q.options,
                                    correctIndex = q.correctIndex
                                )
                            com.nhimz.vocabmaster.domain.model.QuestionType.MATCHING ->
                                QuizType.Matching(
                                    itemWithCard = card,
                                    prompt = q.prompt,
                                    pairs = q.matchingPairs ?: emptyList()
                                )
                            com.nhimz.vocabmaster.domain.model.QuestionType.TYPING ->
                                QuizType.Typing(
                                    itemWithCard = card,
                                    prompt = q.prompt,
                                    correctSentence = q.correctSentence ?: "",
                                    audioUrl = q.audioUrl,
                                    audioUrlSlow = q.audioUrlSlow
                                )
                        }
                        questionsList.add(QuizQuestion(quizType))
                    }
                }
            }

            // Cap to 16 representative questions (Duolingo-style checkpoint length).
            val capped = questionsList.shuffled().take(16)

            if (capped.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            _sessionState.value = QuizSessionState.Active(
                questions = capped,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                incorrectCardIds = emptyList(),
                isUnitCheckpoint = true,
                unitIdForUnitCheckpoint = unitId
            )
        }
    }

    fun startJumpTest(unitId: String) {
        viewModelScope.launch {
            // Simplified for MVP, load some cards or placeholder logic
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()
            
            // Assume loading from repo
            val questionsList = mutableListOf<QuizQuestion>()
            // ...
            
            if (questionsList.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            _sessionState.value = QuizSessionState.Active(
                questions = questionsList,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                incorrectCardIds = emptyList(),
                isJumpTest = true,
                unitIdForJumpTest = unitId
            )
        }
    }

    fun startMistakeReview(cardIds: List<String>?) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()
            
            val mistakeCards = vocabularyRepository.getMistakes(20)
            if (mistakeCards.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }
            
            val questionsList = mutableListOf<QuizQuestion>()
            for (cardItem in mistakeCards) {
                val q = cardItem.question
                val title = q.word ?: q.prompt
                val definition = q.translation ?: ""
                
                // Introduction card
                questionsList.add(QuizQuestion(
                    QuizType.Introduction(
                        itemWithCard = cardItem,
                        prompt = "$title: $definition",
                        audioUrl = q.audioUrl
                    )
                ))
                // Typing card
                questionsList.add(QuizQuestion(
                    QuizType.Typing(
                        itemWithCard = cardItem,
                        prompt = "Type the word: $definition",
                        correctSentence = q.word ?: title,
                        audioUrl = q.audioUrl,
                        audioUrlSlow = null
                    )
                ))
            }
            
            _sessionState.value = QuizSessionState.Active(
                questions = questionsList,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                isFSRSRatingSelected = false,
                incorrectCardIds = emptyList(),
                nodeId = null,
                sessionId = null,
                isSectionCheckpoint = false,
                isJumpTest = false,
                nextSectionCefr = null
            )
        }
    }

    fun startSectionCheckpoint(sectionId: String, nextSectionCefr: String?) {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()
            
            val questionsList = mutableListOf<QuizQuestion>()
            // ... load random questions from section
            
            if (questionsList.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            _sessionState.value = QuizSessionState.Active(
                questions = questionsList,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis(),
                incorrectCardIds = emptyList(),
                isSectionCheckpoint = true,
                nextSectionCefr = nextSectionCefr
            )
        }
    }

    fun submitAnswer(optionIndex: Int? = null, textAnswer: String? = null, selectedWordsForScrambled: List<String>? = null, fsrsRating: Rating? = null) {
        val state = _sessionState.value as? QuizSessionState.Active ?: return
        if (state.isAnswerRevealed) return

        val currentQuestion = state.questions[state.currentIndex]
        val responseTimeMs = System.currentTimeMillis() - state.startTimeMillis

        viewModelScope.launch {
            var isCorrect = false
            var xpEarned = 0
            
            when (val type = currentQuestion.type) {
                is QuizType.Introduction -> {
                    isCorrect = true
                    xpEarned = 1
                }
                is QuizType.MultipleChoice -> {
                    requireNotNull(optionIndex) { "optionIndex must be provided for MultipleChoice" }
                    isCorrect = (optionIndex == type.correctIndex)
                    xpEarned = if (isCorrect) 10 else 2
                }
                is QuizType.Listening -> {
                    requireNotNull(optionIndex) { "optionIndex must be provided for Listening" }
                    isCorrect = (optionIndex == type.correctIndex)
                    xpEarned = if (isCorrect) 10 else 2
                }
                is QuizType.ScrambledSentence -> {
                    requireNotNull(selectedWordsForScrambled) { "selectedWordsForScrambled must be provided for ScrambledSentence" }
                    val userSentence = selectedWordsForScrambled.joinToString(" ")
                    isCorrect = (userSentence == type.correctSentence)
                    xpEarned = if (isCorrect) 10 else 2
                }
                is QuizType.Typing -> {
                    requireNotNull(textAnswer) { "textAnswer must be provided for Typing" }
                    // Simple check ignoring case and punctuation
                    val cleanedUser = textAnswer.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                    val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                    isCorrect = (cleanedUser == cleanedCorrect)
                    xpEarned = if (isCorrect) 10 else 2
                }
                is QuizType.Matching -> {
                    isCorrect = true // Simple representation
                    xpEarned = 15
                }
                is QuizType.FSRSTailFlashcard -> {
                    val rating = fsrsRating ?: Rating.Good
                    type.userRating = rating
                    xpEarned = when (rating) {
                        Rating.Easy -> 15
                        Rating.Good -> 10
                        Rating.Hard -> 5
                        Rating.Again -> 2
                    }
                    isCorrect = (rating != Rating.Again)
                }
            }

            // Apply FSRS scheduling if card exists and NOT an Introduction
            currentQuestion.itemWithCard?.let { cardItem ->
                if (currentQuestion.type !is QuizType.Introduction) {
                    val rating = if (currentQuestion.type is QuizType.FSRSTailFlashcard) {
                        fsrsRating ?: Rating.Good
                    } else {
                        mapRatingUseCase.execute(isCorrect, responseTimeMs)
                    }
                    val desiredRetention = settingsRepository.desiredRetention.first()
                    val scheduler = Scheduler(desiredRetention = desiredRetention)
                    val now = System.currentTimeMillis()
                    val (updatedCard, log) = scheduler.reviewCard(
                        cardItem.card,
                        rating,
                        now,
                        reviewDurationMillis = responseTimeMs
                    )

                    reviewRepository.recordReview(updatedCard, log)
                }
            }

            settingsRepository.addXp(xpEarned)

            val updatedCorrectCount = state.correctAnswersCount + (if (isCorrect) 1 else 0)
            val updatedXpGained = state.xpGained + xpEarned
            
            val updatedIncorrectCardIds = if (!isCorrect && currentQuestion.type !is QuizType.Introduction) {
                val cardId = currentQuestion.itemWithCard?.card?.cardId
                cardId?.let { state.incorrectCardIds + it } ?: state.incorrectCardIds
            } else {
                state.incorrectCardIds
            }

            // Retry incorrect standard questions at the end
            val updatedQuestions = if (isCorrect || currentQuestion.type is QuizType.FSRSTailFlashcard || currentQuestion.type is QuizType.Introduction) {
                state.questions
            } else {
                state.questions + currentQuestion
            }

            _sessionState.value = state.copy(
                questions = updatedQuestions,
                selectedOption = optionIndex,
                isAnswerRevealed = true,
                correctAnswersCount = updatedCorrectCount,
                xpGained = updatedXpGained,
                isFSRSRatingSelected = fsrsRating != null,
                incorrectCardIds = updatedIncorrectCardIds.distinct()
            )
        }
    }

    fun nextQuestion() {
        val state = _sessionState.value as? QuizSessionState.Active ?: return
        if (!state.isAnswerRevealed) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
            
            viewModelScope.launch {
                updateStreakUseCase.execute()

                val accuracy = if (state.questions.isNotEmpty()) state.correctAnswersCount.toFloat() / state.questions.size else 0f
                var isPassed = false

                if (state.isJumpTest || state.isSectionCheckpoint || state.isUnitCheckpoint) {
                    isPassed = accuracy >= 0.8f
                    if (isPassed) {
                        if (state.isSectionCheckpoint && state.nextSectionCefr != null) {
                            settingsRepository.setPlacementLevel(state.nextSectionCefr)
                        } else if (state.isJumpTest && state.unitIdForJumpTest != null) {
                            // Logic to mark all nodes in unit as completed
                            val nodes = vocabularyRepository.getNodesByUnit(state.unitIdForJumpTest).first()
                            nodes.forEach {
                                vocabularyRepository.markNodeCompleted(it.id, 1.0f, state.xpGained)
                            }
                        } else if (state.isUnitCheckpoint && state.unitIdForUnitCheckpoint != null) {
                            // Only mark the UNIT_CHECKPOINT node for this unit as completed.
                            val nodes = vocabularyRepository.getNodesByUnit(state.unitIdForUnitCheckpoint).first()
                            val unitCheckpointNode = nodes.firstOrNull {
                                it.type == com.nhimz.vocabmaster.domain.model.NodeType.UNIT_CHECKPOINT
                            }
                            unitCheckpointNode?.let {
                                vocabularyRepository.markNodeCompleted(it.id, accuracy, state.xpGained)
                            }
                        }
                    }
                } else {
                    if (state.nodeId != null && accuracy >= 0.7f) {
                        vocabularyRepository.markNodeCompleted(state.nodeId, accuracy, state.xpGained)
                    }
                }

                _sessionState.value = QuizSessionState.Completed(
                    xpGained = state.xpGained,
                    correctCount = state.correctAnswersCount,
                    totalCount = state.questions.size,
                    durationSeconds = durationSeconds,
                    averageStability = 0.0,
                    isPassed = isPassed,
                    incorrectCardIds = state.incorrectCardIds,
                    isCheckpointOrJumpTest = state.isSectionCheckpoint || state.isJumpTest || state.isUnitCheckpoint
                )
            }
        } else {
            _sessionState.value = state.copy(
                currentIndex = nextIndex,
                selectedOption = null,
                isAnswerRevealed = false,
                isFSRSRatingSelected = false,
                startTimeMillis = System.currentTimeMillis()
            )
        }
    }
}
