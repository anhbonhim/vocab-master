package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.PlacementTestSession
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.usecase.PlacementTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlacementUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentQuestionId: String = "",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val finalLevel: String? = null,
    val estimatedLevel: String = "A2", // Realtime feedback
    val questionsAsked: Int = 0
)

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    private val placementTestUseCase: PlacementTestUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()

    private var session: PlacementTestSession? = null

    init {
        startSession()
    }

    fun startSession() {
        session = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val newSession = placementTestUseCase.startSession()
                session = newSession
                if (newSession.isFinished) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFinished = true,
                            finalLevel = newSession.resultLevel,
                            estimatedLevel = newSession.estimatedLevel
                        )
                    }
                } else {
                    val item = newSession.questionBank[newSession.currentQuestionIndex]
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentQuestionId = item.questionId,
                            prompt = item.prompt,
                            options = item.options,
                            questionsAsked = newSession.totalQuestionsAsked + 1,
                            estimatedLevel = newSession.estimatedLevel
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun submitAnswer(selectedOptionIndex: Int) {
        val current = session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updated = placementTestUseCase.submitAnswer(current, selectedOptionIndex)
                session = updated
                if (updated.isFinished) {
                    val finalLevel = updated.resultLevel
                    if (finalLevel != null) {
                        settingsRepository.setPlacementLevel(finalLevel)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFinished = true,
                            finalLevel = finalLevel,
                            estimatedLevel = updated.estimatedLevel
                        )
                    }
                } else {
                    val item = updated.questionBank[updated.currentQuestionIndex]
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentQuestionId = item.questionId,
                            prompt = item.prompt,
                            options = item.options,
                            estimatedLevel = updated.estimatedLevel,
                            questionsAsked = updated.totalQuestionsAsked + 1
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }
}
