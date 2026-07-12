package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoOptionCard
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoProgressBar
import com.nhimz.vocabmaster.ui.components.quiz.FeedbackBanner
import com.nhimz.vocabmaster.ui.components.quiz.ScrambledQuizCard
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.QuizSessionState
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuestionDirection
import com.nhimz.vocabmaster.ui.viewmodel.QuizType
import com.nhimz.vocabmaster.ui.viewmodel.QuizQuestion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuizScreen(
    onSessionCompleted: (xpGained: Int, durationSeconds: Int, correctCount: Int, totalCount: Int, averageStability: Double) -> Unit,
    onBackToHome: () -> Unit,
    cdnAudioPlayer: CDNAudioPlayer,
    viewModel: QuizViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionState by viewModel.sessionState.collectAsState()

    // Shake offset for incorrect answers
    val shakeOffset = remember { Animatable(0f) }

    // Local selection tracking
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var selectedWordsForScrambled by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(sessionState) {
        if (sessionState is QuizSessionState.Completed) {
            val completed = sessionState as QuizSessionState.Completed
            onSessionCompleted(
                completed.xpGained,
                completed.durationSeconds,
                completed.correctCount,
                completed.totalCount,
                completed.averageStability
            )
        } else if (sessionState is QuizSessionState.Active) {
            val active = sessionState as QuizSessionState.Active
            val question = active.questions[active.currentIndex]
            // Auto play CDN Audio on new card load
            cdnAudioPlayer.playAudio(question.itemWithCard.vocabulary.audioUrl)
        }
    }

    when (val state = sessionState) {
        is QuizSessionState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đang tải bài học...",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        is QuizSessionState.Active -> {
            val question = state.questions[state.currentIndex]
            val hasAnswered = state.isAnswerRevealed

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trắc nghiệm từ vựng",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Thoát",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onBackToHome() }
                        )
                    }

                    // Duolingo Progress Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val progress = (state.currentIndex.toFloat() / state.questions.size.toFloat())
                        DuolingoProgressBar(progress = progress)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Câu ${state.currentIndex + 1}/${state.questions.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "+${state.xpGained} XP",
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Question Box (Shakeable & Flat elevation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (question.type is QuizType.ScrambledSentence) "Sắp xếp lại câu" else "Dịch nghĩa của từ này",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GradientStart.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Audio play button
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { cdnAudioPlayer.playAudio(question.itemWithCard.vocabulary.audioUrl) }
                                            .padding(6.dp)
                                    ) {
                                        Text("🔊", fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = when (val type = question.type) {
                                        is QuizType.MultipleChoice -> type.prompt
                                        is QuizType.ScrambledSentence -> type.itemWithCard.vocabulary.definition // Show VI meaning as prompt to translate to EN
                                    },
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                if (hasAnswered && !question.itemWithCard.vocabulary.ipa.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question.itemWithCard.vocabulary.ipa!!,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Options list & Reveal details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false)
                    ) {
                        if (hasAnswered) {
                            // Reveal details card (Flat elevation)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp, top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${question.itemWithCard.vocabulary.word} (${question.itemWithCard.vocabulary.partOfSpeech})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = question.itemWithCard.vocabulary.definition,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    if (!question.itemWithCard.vocabulary.example.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ví dụ: ${question.itemWithCard.vocabulary.example}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        // Rendering options based on QuizType
                        when (val type = question.type) {
                            is QuizType.MultipleChoice -> {
                                type.options.forEachIndexed { index, option ->
                                    val isSelected = selectedOptionIndex == index
                                    val isCorrectState = if (hasAnswered) {
                                        index == type.correctIndex
                                    } else null

                                    val isWrongState = if (hasAnswered && isSelected && !isCorrectState!!) {
                                        false
                                    } else null

                                    val finalCorrectState = isCorrectState ?: isWrongState

                                    DuolingoOptionCard(
                                        optionText = option,
                                        isSelected = isSelected,
                                        isCorrect = finalCorrectState,
                                        onClick = {
                                            if (!hasAnswered) {
                                                selectedOptionIndex = index
                                            }
                                        },
                                        enabled = !hasAnswered,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                            is QuizType.ScrambledSentence -> {
                                Spacer(modifier = Modifier.height(16.dp))
                                val isCorrectState = if (hasAnswered) {
                                    val userSentence = selectedWordsForScrambled.joinToString(" ")
                                    userSentence == type.correctSentence
                                } else null
                                
                                ScrambledQuizCard(
                                    scrambledWords = type.scrambledWords,
                                    selectedWords = selectedWordsForScrambled,
                                    isAnswerRevealed = hasAnswered,
                                    isCorrect = isCorrectState,
                                    onWordSelected = { word, _ ->
                                        selectedWordsForScrambled = selectedWordsForScrambled + word
                                    },
                                    onWordUnselected = { word, _ ->
                                        val mutableList = selectedWordsForScrambled.toMutableList()
                                        mutableList.remove(word)
                                        selectedWordsForScrambled = mutableList
                                    }
                                )
                            }
                        }
                    }

                    // Bottom UI Check Button or spacer
                    if (!hasAnswered) {
                        val isSubmitEnabled = when (question.type) {
                            is QuizType.MultipleChoice -> selectedOptionIndex != null
                            is QuizType.ScrambledSentence -> selectedWordsForScrambled.isNotEmpty()
                        }
                        Button(
                            onClick = {
                                if (isSubmitEnabled) {
                                    val isCorrect = when (val type = question.type) {
                                        is QuizType.MultipleChoice -> selectedOptionIndex == type.correctIndex
                                        is QuizType.ScrambledSentence -> selectedWordsForScrambled.joinToString(" ") == type.correctSentence
                                    }
                                    if (!isCorrect) {
                                        scope.launch {
                                            repeat(3) {
                                                shakeOffset.animateTo(15f, tween(40))
                                                shakeOffset.animateTo(-15f, tween(40))
                                            }
                                            shakeOffset.animateTo(0f, tween(40))
                                        }
                                    }
                                    viewModel.submitAnswer(
                                        optionIndex = selectedOptionIndex,
                                        selectedWordsForScrambled = selectedWordsForScrambled
                                    )
                                }
                            },
                            enabled = isSubmitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            Text(
                                text = "KIỂM TRA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSubmitEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    } else {
                        // Spacer to make room for FeedbackBanner overlay
                        Spacer(modifier = Modifier.height(110.dp))
                    }
                }

                // Feedback Banner overlays at the bottom
                if (hasAnswered) {
                    val isCorrectAnswer = when (val type = question.type) {
                        is QuizType.MultipleChoice -> state.selectedOption == type.correctIndex
                        is QuizType.ScrambledSentence -> selectedWordsForScrambled.joinToString(" ") == type.correctSentence
                    }
                    FeedbackBanner(
                        isCorrect = isCorrectAnswer,
                        correctAnswerText = when (val type = question.type) {
                            is QuizType.MultipleChoice -> if (type.direction == QuestionDirection.EN_TO_VI) {
                                question.itemWithCard.vocabulary.definition
                            } else {
                                question.itemWithCard.vocabulary.word
                            }
                            is QuizType.ScrambledSentence -> type.correctSentence
                        },
                        onContinueClick = {
                            viewModel.nextQuestion()
                            selectedOptionIndex = null
                            selectedWordsForScrambled = emptyList()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
        is QuizSessionState.Completed -> {
            // Handled via LaunchedEffect
        }
    }
}
