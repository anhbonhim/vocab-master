package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.fsrs.Card
import com.nhimz.vocabmaster.domain.fsrs.FSRS
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import com.nhimz.vocabmaster.domain.model.VocabularyItemWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.usecase.GenerateDistractorsUseCase
import com.nhimz.vocabmaster.domain.usecase.MapRatingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

enum class QuestionDirection {
    EN_TO_VI, VI_TO_EN
}

sealed class QuizType {
    data class MultipleChoice(
        val itemWithCard: VocabularyItemWithCard,
        val direction: QuestionDirection,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int
    ) : QuizType()

    data class ScrambledSentence(
        val itemWithCard: VocabularyItemWithCard,
        val scrambledWords: List<String>,
        val correctSentence: String
    ) : QuizType()
}

data class QuizQuestion(
    val type: QuizType
) {
    val itemWithCard: VocabularyItemWithCard
        get() = when (type) {
            is QuizType.MultipleChoice -> type.itemWithCard
            is QuizType.ScrambledSentence -> type.itemWithCard
        }
}

sealed class QuizSessionState {
    object Loading : QuizSessionState()
    data class Active(
        val questions: List<QuizQuestion>,
        val currentIndex: Int,
        val correctAnswersCount: Int,
        val xpGained: Int,
        val selectedOption: Int?,
        val isAnswerRevealed: Boolean,
        val startTimeMillis: Long
    ) : QuizSessionState()
    data class Completed(
        val xpGained: Int,
        val correctCount: Int,
        val totalCount: Int,
        val durationSeconds: Int,
        val averageStability: Double = 0.0
    ) : QuizSessionState()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val generateDistractorsUseCase: GenerateDistractorsUseCase,
    private val mapRatingUseCase: MapRatingUseCase
) : ViewModel() {

    private val _sessionState = MutableStateFlow<QuizSessionState>(QuizSessionState.Loading)
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private var sessionStartTime: Long = 0

    init {
        startNewSession()
    }

    fun startNewSession() {
        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Loading
            sessionStartTime = System.currentTimeMillis()

            val nowSec = System.currentTimeMillis() / 1000
            val selectedTopic = settingsRepository.selectedTopic.first()

            val dueCards = vocabularyRepository.getDueCardsByTopic(selectedTopic, nowSec, 10).first()

            val listToUse = mutableListOf<VocabularyItemWithCard>()
            listToUse.addAll(dueCards)

            // If due cards are less than 10, fill up from A2/B1/B2 levels of the selected topic
            if (listToUse.size < 10) {
                val topicCards = vocabularyRepository.getCardsByTopic(selectedTopic).first()
                val remaining = 10 - listToUse.size
                val toAdd = topicCards.filter { card -> card.vocabulary.id !in listToUse.map { it.vocabulary.id } }.take(remaining)
                listToUse.addAll(toAdd)
            }

            // Fallback to general cards if topic cards are exhausted
            if (listToUse.size < 10) {
                val generalCards = vocabularyRepository.getCardsByLevel(DifficultyLevel.A2).first()
                val remaining = 10 - listToUse.size
                val toAdd = generalCards.filter { card -> card.vocabulary.id !in listToUse.map { it.vocabulary.id } }.take(remaining)
                listToUse.addAll(toAdd)
            }

            // If database is completely empty (unlikely because of prepopulation), handle empty
            if (listToUse.isEmpty()) {
                _sessionState.value = QuizSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            // Shuffle pool of vocabulary for generating distractors
            val allVocabPool = listToUse.map { it.vocabulary }

            // Build standard questions
            val questions = listToUse.take(10).map { card ->
                // Decide question type based on availability of scrambledSentenceData
                val hasScrambledData = !card.vocabulary.scrambledSentenceData.isNullOrEmpty()
                val isScrambledType = hasScrambledData && Math.random() > 0.6 // 40% chance for scrambled if available

                if (isScrambledType) {
                    val rawData = card.vocabulary.scrambledSentenceData!!
                    // Basic JSON array parsing "[ \"I\", \"have\" ]" -> List<String>
                    val parsedWords = rawData
                        .removePrefix("[")
                        .removeSuffix("]")
                        .split(",")
                        .map { it.trim().removePrefix("\"").removeSuffix("\"") }
                        .filter { it.isNotEmpty() }

                    val correctSentence = parsedWords.joinToString(" ")
                    val shuffledWords = parsedWords.shuffled()

                    QuizQuestion(
                        type = QuizType.ScrambledSentence(
                            itemWithCard = card,
                            scrambledWords = shuffledWords,
                            correctSentence = correctSentence
                        )
                    )
                } else {
                    val direction = if (Math.random() > 0.5) QuestionDirection.EN_TO_VI else QuestionDirection.VI_TO_EN
                    val distractorsPool = generateDistractorsUseCase.execute(card.vocabulary, allVocabPool, 20)

                    val prompt: String
                    val options: List<String>
                    val correctIndex: Int

                    if (direction == QuestionDirection.EN_TO_VI) {
                        prompt = card.vocabulary.word
                        val correctText = card.vocabulary.definition
                        val otherTexts = distractorsPool.map { it.definition }.filter { it != correctText }.distinct().take(3)
                        val shuffledOptions = (otherTexts + correctText).shuffled()
                        options = shuffledOptions
                        correctIndex = shuffledOptions.indexOf(correctText)
                    } else {
                        prompt = card.vocabulary.definition
                        val correctText = card.vocabulary.word
                        val otherTexts = distractorsPool.map { it.word }.filter { it != correctText }.distinct().take(3)
                        val shuffledOptions = (otherTexts + correctText).shuffled()
                        options = shuffledOptions
                        correctIndex = shuffledOptions.indexOf(correctText)
                    }

                    QuizQuestion(
                        type = QuizType.MultipleChoice(
                            itemWithCard = card,
                            direction = direction,
                            prompt = prompt,
                            options = options,
                            correctIndex = correctIndex
                        )
                    )
                }
            }

            _sessionState.value = QuizSessionState.Active(
                questions = questions,
                currentIndex = 0,
                correctAnswersCount = 0,
                xpGained = 0,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis()
            )
        }
    }

    fun submitAnswer(optionIndex: Int? = null, selectedWordsForScrambled: List<String>? = null) {
        val state = _sessionState.value as? QuizSessionState.Active ?: return
        if (state.isAnswerRevealed) return // Prevent multiple submissions

        val currentQuestion = state.questions[state.currentIndex]
        
        val isCorrect = when (val type = currentQuestion.type) {
            is QuizType.MultipleChoice -> {
                requireNotNull(optionIndex) { "optionIndex must be provided for MultipleChoice" }
                optionIndex == type.correctIndex
            }
            is QuizType.ScrambledSentence -> {
                requireNotNull(selectedWordsForScrambled) { "selectedWordsForScrambled must be provided for ScrambledSentence" }
                val userSentence = selectedWordsForScrambled.joinToString(" ")
                userSentence == type.correctSentence
            }
        }

        val responseTimeMs = System.currentTimeMillis() - state.startTimeMillis

        viewModelScope.launch {
            // Apply FSRS Spaced Repetition scheduling
            val rating = mapRatingUseCase.execute(isCorrect, responseTimeMs)
            val desiredRetention = settingsRepository.desiredRetention.first()
            val fsrs = FSRS(requestRetention = desiredRetention)
            
            val nowDateTime = LocalDateTime.now(ZoneOffset.UTC)
            val scheduleResult = fsrs.schedule(currentQuestion.itemWithCard.card, rating, nowDateTime)

            // Save updated card and review log to database
            vocabularyRepository.updateCard(scheduleResult.card)
            reviewRepository.insertReviewLog(scheduleResult.card.id, scheduleResult.log)

            // Award XP
            val xpEarned = if (isCorrect) 10 else 2
            settingsRepository.addXp(xpEarned)

            val updatedCorrectCount = state.correctAnswersCount + (if (isCorrect) 1 else 0)
            val updatedXpGained = state.xpGained + xpEarned
            val updatedQuestions = if (isCorrect) {
                state.questions
            } else {
                state.questions + currentQuestion
            }

            _sessionState.value = state.copy(
                questions = updatedQuestions,
                selectedOption = optionIndex,
                isAnswerRevealed = true,
                correctAnswersCount = updatedCorrectCount,
                xpGained = updatedXpGained
            )
        }
    }

    fun nextQuestion() {
        val state = _sessionState.value as? QuizSessionState.Active ?: return
        if (!state.isAnswerRevealed) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            // End session
            val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
            
            // Check streak updating
            viewModelScope.launch {
                val lastStudy = settingsRepository.lastStudyDate.first()
                val today = System.currentTimeMillis()
                val current = settingsRepository.currentStreak.first()
                if (today - lastStudy > 24 * 60 * 60 * 1000L || current == 0) {
                    // Update streak
                    val currentStreak = settingsRepository.currentStreak.first()
                    settingsRepository.setCurrentStreak(if (currentStreak == 0) 1 else currentStreak + 1)
                    settingsRepository.setLastStudyDate(today)
                }
            }

            _sessionState.value = QuizSessionState.Completed(
                xpGained = state.xpGained,
                correctCount = state.correctAnswersCount,
                totalCount = state.questions.size,
                durationSeconds = durationSeconds,
                averageStability = state.questions.map { it.itemWithCard.card.stability }.average()
            )
        } else {
            _sessionState.value = state.copy(
                currentIndex = nextIndex,
                selectedOption = null,
                isAnswerRevealed = false,
                startTimeMillis = System.currentTimeMillis()
            )
        }
    }
}
