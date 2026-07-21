package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.Session
import com.nhimz.vocabmaster.domain.usecase.*
import com.nhimz.vocabmaster.ui.viewmodel.fakes.FakeReviewRepository
import com.nhimz.vocabmaster.ui.viewmodel.fakes.FakeSettingsRepository
import com.nhimz.vocabmaster.ui.viewmodel.fakes.FakeVocabularyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vocabRepo: FakeVocabularyRepository
    private lateinit var reviewRepo: FakeReviewRepository
    private lateinit var settingsRepo: FakeSettingsRepository

    private lateinit var loadQuizSessionUseCase: LoadQuizSessionUseCase
    private lateinit var evaluateAnswerUseCase: EvaluateAnswerUseCase
    private lateinit var submitReviewUseCase: SubmitReviewUseCase
    private lateinit var completeQuizSessionUseCase: CompleteQuizSessionUseCase
    private lateinit var mapRatingUseCase: MapRatingUseCase
    private lateinit var updateStreakUseCase: UpdateStreakUseCase

    @Before
    fun setUp() {
        vocabRepo = FakeVocabularyRepository()
        reviewRepo = FakeReviewRepository()
        settingsRepo = FakeSettingsRepository()

        mapRatingUseCase = MapRatingUseCase()
        updateStreakUseCase = UpdateStreakUseCase(settingsRepo)

        loadQuizSessionUseCase = LoadQuizSessionUseCase(vocabRepo)
        evaluateAnswerUseCase = EvaluateAnswerUseCase()
        submitReviewUseCase = SubmitReviewUseCase(reviewRepo, settingsRepo, mapRatingUseCase)
        completeQuizSessionUseCase = CompleteQuizSessionUseCase(vocabRepo, settingsRepo, updateStreakUseCase)
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): QuizViewModel {
        return QuizViewModel(
            savedStateHandle = savedStateHandle,
            loadQuizSessionUseCase = loadQuizSessionUseCase,
            evaluateAnswerUseCase = evaluateAnswerUseCase,
            submitReviewUseCase = submitReviewUseCase,
            completeQuizSessionUseCase = completeQuizSessionUseCase
        )
    }

    @Test
    fun `startNodeSession with fake repo failure emits QuizUiState Error`() = runTest {
        vocabRepo.failure = RuntimeException("Database error")
        val viewModel = createViewModel()

        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is QuizUiState.Error)
        assertEquals("Database error", (state as QuizUiState.Error).message)
    }

    @Test
    fun `startNodeSession happy path transitions to Active state`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Active state but got $state", state is QuizUiState.Active)
        val active = state as QuizUiState.Active
        assertEquals(1, active.questions.size)
        assertEquals(0, active.currentIndex)
        assertEquals("node_1", active.nodeId)
        assertEquals("s1", active.sessionId)
    }

    @Test
    fun `submitAnswer happy path updates active state and requeues incorrect answer`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))
        vocabRepo.getCardByQuestionIdResult = Card(cardId = "card_1")

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Submit incorrect answer (index 1)
        viewModel.submitAnswer(optionIndex = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Active state but got $state", state is QuizUiState.Active)
        val active = state as QuizUiState.Active
        assertTrue(active.isAnswerRevealed)
        assertEquals(2, active.xpGained) // Incorrect standard question awards 2 XP
        assertEquals(0, active.correctAnswersCount)
        assertEquals(listOf("card_1"), active.incorrectCardIds)
        assertEquals(2, active.questions.size) // Question requeued at end
    }

    @Test
    fun `rapid double submit in same frame calls SubmitReviewUseCase exactly once`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))
        vocabRepo.getCardByQuestionIdResult = Card(cardId = "card_1")

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Submit twice synchronously without advancing dispatcher
        viewModel.submitAnswer(optionIndex = 0)
        viewModel.submitAnswer(optionIndex = 0)
        advanceUntilIdle()

        assertEquals(1, reviewRepo.recordReviewCalls)
    }

    @Test
    fun `SubmitReviewUseCase failure emits QuizUiState Error`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))
        vocabRepo.getCardByQuestionIdResult = Card(cardId = "card_1")
        reviewRepo.recordReviewFailure = RuntimeException("Failed to persist review")

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        viewModel.submitAnswer(optionIndex = 0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state on review submit failure but got $state", state is QuizUiState.Error)
        assertEquals("Failed to persist review", (state as QuizUiState.Error).message)
    }

    @Test
    fun `EvaluateAnswerUseCase failure emits QuizUiState Error`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Submit without optionIndex for MultipleChoice -> EvaluateAnswerUseCase fails
        viewModel.submitAnswer(optionIndex = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state on evaluate failure but got $state", state is QuizUiState.Error)
    }

    @Test
    fun `nextQuestion on last question completes session and double tap guard works`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(
            id = "q1",
            type = QuestionType.MULTIPLE_CHOICE,
            prompt = "Hello",
            options = listOf("Xin chào", "Tạm biệt"),
            correctIndex = 0
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        viewModel.submitAnswer(optionIndex = 0)
        advanceUntilIdle()

        // Double trigger nextQuestion
        viewModel.nextQuestion()
        viewModel.nextQuestion()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Completed state but got $state", state is QuizUiState.Completed)
        assertEquals(1, vocabRepo.markNodeCompletedCalls)
    }
}
