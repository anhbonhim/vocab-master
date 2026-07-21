package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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
class QuizViewModelPersistenceTest {

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
    fun `restored ViewModel with handle containing NODE and saved index resumes at saved index`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val questions = listOf(
            Question("q1", QuestionType.MULTIPLE_CHOICE, "Prompt 1", listOf("O1", "O2"), 0),
            Question("q2", QuestionType.MULTIPLE_CHOICE, "Prompt 2", listOf("O1", "O2"), 0),
            Question("q3", QuestionType.MULTIPLE_CHOICE, "Prompt 3", listOf("O1", "O2"), 0),
            Question("q4", QuestionType.MULTIPLE_CHOICE, "Prompt 4", listOf("O1", "O2"), 0)
        )
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(questions)

        val handle = SavedStateHandle(
            mapOf(
                "quiz_kind" to "NODE",
                "quiz_node_id" to "node_1",
                "quiz_session_index" to 0,
                "quiz_current_index" to 2
            )
        )

        val viewModel = createViewModel(handle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Active)
        assertEquals(2, (state as QuizUiState.Active).currentIndex)
    }

    @Test
    fun `assert SavedStateHandle only contains whitelisted String or Int keys`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question("q1", QuestionType.MULTIPLE_CHOICE, "Hello", listOf("Xin chào", "Tạm biệt"), 0)
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)

        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Validate SavedStateHandle keys and values types
        for (key in handle.keys()) {
            assertTrue("Key '$key' is not in the whitelist", QuizViewModel.PERSISTENCE_KEYS.contains(key))
            val value = handle.get<Any>(key)
            if (value != null) {
                assertTrue(
                    "Value for key '$key' is ${value.javaClass.simpleName}, expected String or Int",
                    value is String || value is Int
                )
            }
        }
    }

    @Test
    fun `completing the quiz clears all SavedStateHandle keys`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question("q1", QuestionType.MULTIPLE_CHOICE, "Hello", listOf("Xin chào", "Tạm biệt"), 0)
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)

        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        viewModel.submitAnswer(optionIndex = 0)
        advanceUntilIdle()

        viewModel.nextQuestion()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is QuizUiState.Completed)
        
        // Assert all whitelisted persistence keys are removed
        for (key in QuizViewModel.PERSISTENCE_KEYS) {
            assertFalse("SavedStateHandle should not contain key '$key' after completion", handle.contains(key))
        }
    }

    @Test
    fun `fresh ViewModel with empty handle stays in Loading state`() = runTest {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is QuizUiState.Loading)
    }
}
