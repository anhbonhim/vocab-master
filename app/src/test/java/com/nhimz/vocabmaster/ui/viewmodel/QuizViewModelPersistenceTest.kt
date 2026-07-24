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
        val fakeAudioPlayer = object : com.nhimz.vocabmaster.domain.audio.AudioPlayer {
            override fun playAudio(url: String?) {}
            override fun stop() {}
            override fun shutdown() {}
        }
        val audioPlayerUseCase = AudioPlayerUseCase(fakeAudioPlayer)
        return QuizViewModel(
            savedStateHandle = savedStateHandle,
            loadQuizSessionUseCase = loadQuizSessionUseCase,
            evaluateAnswerUseCase = evaluateAnswerUseCase,
            submitReviewUseCase = submitReviewUseCase,
            completeQuizSessionUseCase = completeQuizSessionUseCase,
            audioPlayerUseCase = audioPlayerUseCase
        )
    }

    @Test
    fun `restored ViewModel with handle containing NODE and saved index resumes at saved index`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val questions = listOf(
            Question(id = "q1", type = QuestionType.MULTIPLE_CHOICE, prompt = "Prompt 1", options = listOf("O1", "O2"), correctIndex = 0),
            Question(id = "q2", type = QuestionType.MULTIPLE_CHOICE, prompt = "Prompt 2", options = listOf("O1", "O2"), correctIndex = 0),
            Question(id = "q3", type = QuestionType.MULTIPLE_CHOICE, prompt = "Prompt 3", options = listOf("O1", "O2"), correctIndex = 0),
            Question(id = "q4", type = QuestionType.MULTIPLE_CHOICE, prompt = "Prompt 4", options = listOf("O1", "O2"), correctIndex = 0)
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
    fun `assert SavedStateHandle only contains whitelisted String Int Boolean or ArrayList keys`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(id = "q1", type = QuestionType.MULTIPLE_CHOICE, prompt = "Hello", options = listOf("Xin chào", "Tạm biệt"), correctIndex = 0)
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle)

        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Plan 03-02: the persistence whitelist now also covers Boolean and
        // ArrayList<String> for the per-question answer state and the
        // cumulative incorrect-card list. Anything else written to the
        // SavedStateHandle is a contract violation.
        for (key in handle.keys()) {
            assertTrue("Key '$key' is not in the whitelist", QuizViewModel.PERSISTENCE_KEYS.contains(key))
            val value = handle.get<Any>(key)
            if (value != null) {
                val ok = value is String ||
                    value is Int ||
                    value is Boolean ||
                    value is ArrayList<*>
                assertTrue(
                    "Value for key '$key' is ${value.javaClass.simpleName}, expected String/Int/Boolean/ArrayList",
                    ok
                )
            }
        }
    }

    @Test
    fun `completing the quiz clears all SavedStateHandle keys`() = runTest {
        val session = Session("s1", "node_1", 0, "Session 1")
        val question = Question(id = "q1", type = QuestionType.MULTIPLE_CHOICE, prompt = "Hello", options = listOf("Xin chào", "Tạm biệt"), correctIndex = 0)
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
