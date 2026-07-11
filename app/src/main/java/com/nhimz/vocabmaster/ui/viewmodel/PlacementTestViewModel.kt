package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.usecase.GenerateDistractorsUseCase
import com.nhimz.vocabmaster.domain.usecase.PlacementTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionState(
    val correctItem: VocabularyItem,
    val word: String,
    val options: List<String>,
    val correctIndex: Int
)

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val placementTestUseCase: PlacementTestUseCase,
    private val generateDistractorsUseCase: GenerateDistractorsUseCase
) : ViewModel() {

    private val _session = MutableStateFlow(PlacementTestSession())
    val session: StateFlow<PlacementTestSession> = _session.asStateFlow()

    private val _currentQuestion = MutableStateFlow<QuestionState?>(null)
    val currentQuestion: StateFlow<QuestionState?> = _currentQuestion.asStateFlow()

    private val _totalQuestionsAsked = MutableStateFlow(0)
    val totalQuestionsAsked: StateFlow<Int> = _totalQuestionsAsked.asStateFlow()

    private var allVocabularyList: List<VocabularyItem> = emptyList()
    private var currentLevelWords: List<VocabularyItem> = emptyList()
    private val askedWords = mutableSetOf<String>()

    init {
        loadVocabulary()
    }

    private fun loadVocabulary() {
        viewModelScope.launch {
            // Prepopulate database if empty and load all vocabulary
            // To get all vocabulary, we can load C2 and C1 or others, or since database prepopulates A1-C2,
            // we can get cards for a level and accumulate them.
            // Let's load A1, A2, B1, B2, C1, C2 cards to construct our full pool
            val allList = mutableListOf<VocabularyItem>()
            for (level in DifficultyLevel.values()) {
                val levelCards = vocabularyRepository.getCardsByLevel(level).first()
                allList.addAll(levelCards.map { it.vocabulary })
            }
            allVocabularyList = allList

            // Start the test
            generateNextQuestion()
        }
    }

    fun submitAnswer(selectedOptionIndex: Int) {
        val question = _currentQuestion.value ?: return
        val isCorrect = selectedOptionIndex == question.correctIndex

        _totalQuestionsAsked.value += 1

        val currentSession = _session.value
        val updatedSession = placementTestUseCase.answerQuestion(currentSession, isCorrect)

        _session.value = updatedSession

        if (updatedSession.isFinished) {
            _currentQuestion.value = null
        } else {
            generateNextQuestion()
        }
    }

    private fun generateNextQuestion() {
        viewModelScope.launch {
            val level = _session.value.currentLevel

            // Load cards for the current level if not already loaded or if level changed
            if (currentLevelWords.isEmpty() || currentLevelWords.first().difficultyLevel != level) {
                currentLevelWords = vocabularyRepository.getCardsByLevel(level).first().map { it.vocabulary }
            }

            // Filter out words already asked
            val availableWords = currentLevelWords.filter { it.word !in askedWords }

            val selectedWord = if (availableWords.isNotEmpty()) {
                availableWords.random()
            } else {
                currentLevelWords.random() // Fallback
            }

            askedWords.add(selectedWord.word)

            val distractorsPool = generateDistractorsUseCase.execute(
                correctItem = selectedWord,
                allVocabulary = allVocabularyList,
                count = 20
            )

            val otherTexts = distractorsPool.map { it.definition }.filter { it != selectedWord.definition }.distinct().take(3)
            val optionsList = (otherTexts + selectedWord.definition).shuffled()
            val correctIdx = optionsList.indexOf(selectedWord.definition)

            _currentQuestion.value = QuestionState(
                correctItem = selectedWord,
                word = selectedWord.word,
                options = optionsList,
                correctIndex = correctIdx
            )
        }
    }
}
