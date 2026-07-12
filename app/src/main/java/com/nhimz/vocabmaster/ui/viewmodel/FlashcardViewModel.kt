package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.fsrs.Card
import com.nhimz.vocabmaster.domain.fsrs.FSRS
import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyItemWithCard
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

sealed class FlashcardSessionState {
    object Loading : FlashcardSessionState()
    data class Active(
        val cards: List<VocabularyItemWithCard>,
        val currentIndex: Int,
        val isFlipped: Boolean,
        val xpGained: Int,
        val startTimeMillis: Long,
        val correctCount: Int
    ) : FlashcardSessionState()
    data class Completed(
        val xpGained: Int,
        val correctCount: Int,
        val totalCount: Int,
        val durationSeconds: Int,
        val averageStability: Double = 0.0
    ) : FlashcardSessionState()
}

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _sessionState = MutableStateFlow<FlashcardSessionState>(FlashcardSessionState.Loading)
    val sessionState: StateFlow<FlashcardSessionState> = _sessionState.asStateFlow()

    private var sessionStartTime: Long = 0

    init {
        startNewSession()
    }

    fun startNewSession() {
        viewModelScope.launch {
            _sessionState.value = FlashcardSessionState.Loading
            sessionStartTime = System.currentTimeMillis()

            val nowSec = System.currentTimeMillis() / 1000
            val dueCards = vocabularyRepository.getDueCards(nowSec, 10).first()

            val listToUse = mutableListOf<VocabularyItemWithCard>()
            listToUse.addAll(dueCards)

            // Fill up with A2/B1 cards if needed
            if (listToUse.size < 10) {
                val levelsToTry = listOf(DifficultyLevel.A2, DifficultyLevel.B1, DifficultyLevel.B2)
                for (level in levelsToTry) {
                    if (listToUse.size >= 10) break
                    val levelCards = vocabularyRepository.getCardsByLevel(level).first()
                    val remaining = 10 - listToUse.size
                    val toAdd = levelCards.filter { card -> card.vocabulary.id !in listToUse.map { it.vocabulary.id } }.take(remaining)
                    listToUse.addAll(toAdd)
                }
            }

            if (listToUse.isEmpty()) {
                _sessionState.value = FlashcardSessionState.Completed(0, 0, 0, 0)
                return@launch
            }

            _sessionState.value = FlashcardSessionState.Active(
                cards = listToUse.take(10),
                currentIndex = 0,
                isFlipped = false,
                xpGained = 0,
                startTimeMillis = System.currentTimeMillis(),
                correctCount = 0
            )
        }
    }

    fun flipCard() {
        val state = _sessionState.value as? FlashcardSessionState.Active ?: return
        _sessionState.value = state.copy(isFlipped = !state.isFlipped)
    }

    fun rateCard(rating: Rating) {
        val state = _sessionState.value as? FlashcardSessionState.Active ?: return
        val currentCard = state.cards[state.currentIndex]

        viewModelScope.launch {
            val desiredRetention = settingsRepository.desiredRetention.first()
            val fsrs = FSRS(requestRetention = desiredRetention)
            val nowDateTime = LocalDateTime.now(ZoneOffset.UTC)
            val scheduleResult = fsrs.schedule(currentCard.card, rating, nowDateTime)

            // Save in DB
            vocabularyRepository.updateCard(scheduleResult.card)
            reviewRepository.insertReviewLog(scheduleResult.card.id, scheduleResult.log)

            // Award XP
            val xpEarned = when (rating) {
                Rating.Easy -> 15
                Rating.Good -> 10
                Rating.Hard -> 5
                Rating.Again -> 2
            }
            settingsRepository.addXp(xpEarned)

            val updatedXp = state.xpGained + xpEarned
            val isSuccess = rating == Rating.Good || rating == Rating.Easy
            val updatedCorrectCount = state.correctCount + (if (isSuccess) 1 else 0)

            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.cards.size) {
                // Completed
                val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                
                // Update streak
                val lastStudy = settingsRepository.lastStudyDate.first()
                val today = System.currentTimeMillis()
                val current = settingsRepository.currentStreak.first()
                if (today - lastStudy > 24 * 60 * 60 * 1000L || current == 0) {
                    val currentStreak = settingsRepository.currentStreak.first()
                    settingsRepository.setCurrentStreak(if (currentStreak == 0) 1 else currentStreak + 1)
                    settingsRepository.setLastStudyDate(today)
                }

            _sessionState.value = FlashcardSessionState.Completed(
                xpGained = state.xpGained,
                correctCount = state.correctCount,
                totalCount = state.cards.size,
                durationSeconds = durationSeconds,
                averageStability = state.cards.map { it.card.stability }.average()
            )
            } else {
                _sessionState.value = state.copy(
                    currentIndex = nextIndex,
                    isFlipped = false,
                    xpGained = updatedXp,
                    correctCount = updatedCorrectCount,
                    startTimeMillis = System.currentTimeMillis()
                )
            }
        }
    }
}
