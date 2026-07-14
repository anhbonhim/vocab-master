package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.data.remote.ApiClient
import com.nhimz.vocabmaster.data.remote.AnswerRequestDto
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
    val sessionId: String? = null,
    val currentWord: String = "",
    val options: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val finalLevel: String? = null,
    val estimatedLevel: String = "A2", // Realtime feedback
    val questionsAsked: Int = 0
)

@HiltViewModel
class PlacementTestViewModel @Inject constructor(
    private val apiClient: ApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()

    private var currentVocabId: Int = -1
    private var questionStartTime: Long = 0

    init {
        startSession()
    }

    fun startSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiClient.placementApi.startSession()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        currentVocabId = body.next_question.vocab_id
                        questionStartTime = System.currentTimeMillis()
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                sessionId = body.session_id,
                                currentWord = body.next_question.word,
                                options = body.next_question.options.map { opt -> opt.text },
                                questionsAsked = 1
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load question") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to connect to server: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun submitAnswer(selectedOptionIndex: Int) {
        val sessionId = _uiState.value.sessionId ?: return
        val responseTimeMs = (System.currentTimeMillis() - questionStartTime).toInt()
        
        // We assume the first option is correct for this dummy implementation,
        // but normally the server handles correct validation based on option ID.
        // Option 0 is correct in dummy/initial response.
        val isCorrect = selectedOptionIndex == 0 

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val request = AnswerRequestDto(currentVocabId, isCorrect, responseTimeMs)
                val response = apiClient.placementApi.submitAnswer(sessionId, request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val result = body.result
                        val nextQuestion = body.next_question
                        
                        if (body.status == "finished" && result != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isFinished = true,
                                    finalLevel = result.final_level
                                )
                            }
                        } else if (nextQuestion != null) {
                            currentVocabId = nextQuestion.vocab_id
                            questionStartTime = System.currentTimeMillis()
                            
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentWord = nextQuestion.word,
                                    options = nextQuestion.options.map { opt -> opt.text },
                                    estimatedLevel = body.estimated_level,
                                    questionsAsked = it.questionsAsked + 1
                                )
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Invalid response from server") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Server error") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Network error") }
            }
        }
    }
}