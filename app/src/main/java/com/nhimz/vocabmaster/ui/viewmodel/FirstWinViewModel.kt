package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.usecase.GenerateDistractorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirstWinQuestion(
    val item: VocabularyItem,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

sealed class FirstWinSessionState {
    object Loading : FirstWinSessionState()
    data class Active(
        val questions: List<FirstWinQuestion>,
        val currentIndex: Int,
        val correctAnswersCount: Int,
        val selectedOption: Int?,
        val isAnswerRevealed: Boolean,
        val isCorrectAnswer: Boolean?
    ) : FirstWinSessionState()
    object Completed : FirstWinSessionState()
}

@HiltViewModel
class FirstWinViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val settingsRepository: SettingsRepository,
    private val generateDistractorsUseCase: GenerateDistractorsUseCase
) : ViewModel() {

    private val _sessionState = MutableStateFlow<FirstWinSessionState>(FirstWinSessionState.Loading)
    val sessionState: StateFlow<FirstWinSessionState> = _sessionState.asStateFlow()

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        viewModelScope.launch {
            _sessionState.value = FirstWinSessionState.Loading

            // 1. Get placement level from Settings
            val levelStr = settingsRepository.placementLevel.first()
            val level = try {
                if (levelStr != null) DifficultyLevel.valueOf(levelStr) else DifficultyLevel.A2
            } catch (e: Exception) {
                DifficultyLevel.A2
            }

            // 2. Load all vocabulary for generating distractors
            val allList = mutableListOf<VocabularyItem>()
            for (l in DifficultyLevel.values()) {
                val levelCards = vocabularyRepository.getCardsByLevel(l).first()
                allList.addAll(levelCards.map { it.vocabulary })
            }

            // 3. Load cards for the specific level
            val levelWords = vocabularyRepository.getCardsByLevel(level).first().map { it.vocabulary }
            
            // 4. Generate 7 random questions
            val selectedWords = if (levelWords.size >= 7) {
                levelWords.shuffled().take(7)
            } else {
                allList.shuffled().take(7)
            }

            val questions = selectedWords.map { wordItem ->
                val distractorsPool = generateDistractorsUseCase.execute(
                    correctItem = wordItem,
                    allVocabulary = allList,
                    count = 20
                )
                val otherTexts = distractorsPool.map { it.definition }.filter { it != wordItem.definition }.distinct().take(3)
                val optionsList = (otherTexts + wordItem.definition).shuffled()
                val correctIdx = optionsList.indexOf(wordItem.definition)

                FirstWinQuestion(
                    item = wordItem,
                    prompt = wordItem.word,
                    options = optionsList,
                    correctIndex = correctIdx
                )
            }

            _sessionState.value = FirstWinSessionState.Active(
                questions = questions,
                currentIndex = 0,
                correctAnswersCount = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                isCorrectAnswer = null
            )
        }
    }

    fun submitAnswer(optionIndex: Int) {
        val state = _sessionState.value as? FirstWinSessionState.Active ?: return
        if (state.isAnswerRevealed) return

        val currentQuestion = state.questions[state.currentIndex]
        val isCorrect = optionIndex == currentQuestion.correctIndex

        val updatedQuestions = if (isCorrect) {
            state.questions
        } else {
            state.questions + currentQuestion
        }

        _sessionState.value = state.copy(
            questions = updatedQuestions,
            selectedOption = optionIndex,
            isAnswerRevealed = true,
            isCorrectAnswer = isCorrect,
            correctAnswersCount = state.correctAnswersCount + (if (isCorrect) 1 else 0)
        )
    }

    fun nextQuestion() {
        val state = _sessionState.value as? FirstWinSessionState.Active ?: return
        if (!state.isAnswerRevealed) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            _sessionState.value = FirstWinSessionState.Completed
        } else {
            _sessionState.value = state.copy(
                currentIndex = nextIndex,
                selectedOption = null,
                isAnswerRevealed = false,
                isCorrectAnswer = null
            )
        }
    }
}
