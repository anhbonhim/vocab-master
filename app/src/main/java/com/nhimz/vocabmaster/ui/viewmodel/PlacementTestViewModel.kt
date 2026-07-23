package com.nhimz.vocabmaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.data.remote.ApiClient
import com.nhimz.vocabmaster.data.remote.AnswerItemDto
import com.nhimz.vocabmaster.data.remote.AnswerRequestDto
import com.nhimz.vocabmaster.domain.model.SettingsRepository
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
    private val apiClient: ApiClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = _uiState.asStateFlow()

    private var correctOptionId: Int = -1
    private var questionStartTime: Long = 0
    
    private val answeredItems = mutableListOf<AnswerItemDto>()
    private var currentOptionIds: List<Int> = emptyList()

    init {
        startSession()
    }

    fun startSession() {
        answeredItems.clear()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiClient.placementApi.startSession()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        correctOptionId = body.next_question.correct_option_id
                        currentOptionIds = body.next_question.options.map { opt -> opt.id }
                        questionStartTime = System.currentTimeMillis()
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                sessionId = body.session_id,
                                currentQuestionId = body.next_question.question_id,
                                prompt = body.next_question.prompt,
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
        val currentQuestionId = _uiState.value.currentQuestionId
        val responseTimeMs = (System.currentTimeMillis() - questionStartTime).toInt()
        
        val selectedOptionId = currentOptionIds.getOrNull(selectedOptionIndex) ?: -1
        val isCorrect = selectedOptionId == correctOptionId 

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val request = AnswerRequestDto(
                    responses = answeredItems.toList(),
                    latest_question_id = currentQuestionId,
                    latest_is_correct = isCorrect,
                    latest_response_time_ms = responseTimeMs
                )
                val response = apiClient.placementApi.submitAnswer(sessionId, request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val result = body.result
                        val nextQuestion = body.next_question
                        
                        answeredItems.add(
                            AnswerItemDto(
                                question_id = currentQuestionId,
                                is_correct = isCorrect,
                                response_time_ms = responseTimeMs
                            )
                        )
                        
                        if (body.status == "finished" && result != null) {
                            viewModelScope.launch {
                                settingsRepository.setPlacementLevel(result.final_level)
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isFinished = true,
                                    finalLevel = result.final_level
                                )
                            }
                        } else if (nextQuestion != null) {
                            correctOptionId = nextQuestion.correct_option_id
                            currentOptionIds = nextQuestion.options.map { opt -> opt.id }
                            questionStartTime = System.currentTimeMillis()
                            
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentQuestionId = nextQuestion.question_id,
                                    prompt = nextQuestion.prompt,
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
