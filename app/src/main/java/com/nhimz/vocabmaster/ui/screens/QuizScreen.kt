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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.tts.TTSManager
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.util.FeedbackHelper
import com.nhimz.vocabmaster.ui.viewmodel.QuizSessionState
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuizScreen(
    onSessionCompleted: (xpGained: Int, durationSeconds: Int, correctCount: Int, totalCount: Int) -> Unit,
    onBackToHome: () -> Unit,
    ttsManager: TTSManager,
    viewModel: QuizViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionState by viewModel.sessionState.collectAsState()

    // Shake offset for incorrect answers
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(sessionState) {
        if (sessionState is QuizSessionState.Completed) {
            val completed = sessionState as QuizSessionState.Completed
            onSessionCompleted(
                completed.xpGained,
                completed.durationSeconds,
                completed.correctCount,
                completed.totalCount
            )
        } else if (sessionState is QuizSessionState.Active) {
            val active = sessionState as QuizSessionState.Active
            val question = active.questions[active.currentIndex]
            // Auto play TTS for the English word
            ttsManager.speak(question.itemWithCard.vocabulary.word)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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

                // Progress Indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    val progress = (state.currentIndex.toFloat() / state.questions.size.toFloat())
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GradientStart,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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

                // Question Box (Shakeable)
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                    text = "Dịch nghĩa của từ này",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GradientStart.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // TTS Audio play button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { ttsManager.speak(question.itemWithCard.vocabulary.word) }
                                        .padding(6.dp)
                                ) {
                                    Text("🔊", fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = question.prompt,
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
                        // Reveal details card (IPA, POS, definition, example)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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

                    question.options.forEachIndexed { index, option ->
                        val isSelected = state.selectedOption == index
                        val isCorrectIndex = index == question.correctIndex

                        val containerColor = when {
                            !hasAnswered -> MaterialTheme.colorScheme.surface
                            isCorrectIndex -> Color(0xFF10B981).copy(alpha = 0.15f) // Success transparent
                            isSelected -> Color(0xFFEF4444).copy(alpha = 0.15f) // Error transparent
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        }

                        val borderColor = when {
                            !hasAnswered -> Color.Transparent
                            isCorrectIndex -> Color(0xFF10B981)
                            isSelected -> Color(0xFFEF4444)
                            else -> Color.Transparent
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !hasAnswered) {
                                    viewModel.submitAnswer(index)
                                    if (index == question.correctIndex) {
                                        FeedbackHelper.playSoundCorrect()
                                        FeedbackHelper.vibrateCorrect(context)
                                    } else {
                                        FeedbackHelper.playSoundIncorrect()
                                        FeedbackHelper.vibrateIncorrect(context)
                                        scope.launch {
                                            // Shake card
                                            repeat(3) {
                                                shakeOffset.animateTo(15f, tween(40))
                                                shakeOffset.animateTo(-15f, tween(40))
                                            }
                                            shakeOffset.animateTo(0f, tween(40))
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(1.5.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                !hasAnswered -> GradientStart
                                                isCorrectIndex -> Color(0xFF10B981)
                                                isSelected -> Color(0xFFEF4444)
                                                else -> Color.Gray
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = ('A'.code + index).toChar().toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = option,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Next Button row
                if (hasAnswered) {
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .height(54.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                                shape = RoundedCornerShape(27.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            text = if (state.currentIndex >= state.questions.size - 1) "Xem kết quả" else "Tiếp tục",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(54.dp)) // Spacer placeholder
                }
            }
        }
        is QuizSessionState.Completed -> {
            // Screen transition handled in LaunchedEffect
        }
    }
}
