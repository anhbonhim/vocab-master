package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoOptionCard
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoProgressBar
import com.nhimz.vocabmaster.ui.components.quiz.FeedbackBanner
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel

@Composable
fun PlacementTestScreen(
    onTestFinished: (DifficultyLevel) -> Unit,
    viewModel: PlacementTestViewModel
) {
    val session by viewModel.session.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val totalAsked by viewModel.totalQuestionsAsked.collectAsState()

    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswerChecked by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(session.isFinished) {
        if (session.isFinished) {
            onTestFinished(session.resultLevel ?: DifficultyLevel.A2)
        }
    }

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
            // Top Progress Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Đánh giá trình độ đầu vào",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Duolingo Progress Bar
                val currentLevelProgress = (session.questionsAskedInCurrentLevel.toFloat() / 8f).coerceIn(0f, 1f)
                DuolingoProgressBar(progress = currentLevelProgress)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cấp độ hiện tại: ${session.currentLevel} | Câu hỏi: ${session.questionsAskedInCurrentLevel + 1}/8",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Animated Question Area
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "QuestionAnimation",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { question ->
                if (question != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Question Card (Flat design, elevation = 0)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = question.correctItem.partOfSpeech.lowercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GradientStart.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = question.word,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (!question.correctItem.ipa.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question.correctItem.ipa!!,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Options list using DuolingoOptionCard
                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedOption == index
                            val isCorrectState = if (isAnswerChecked) {
                                index == question.correctIndex
                            } else null

                            val isWrongState = if (isAnswerChecked && isSelected && !isCorrectState!!) {
                                false
                            } else null

                            val finalCorrectState = isCorrectState ?: isWrongState

                            DuolingoOptionCard(
                                optionText = option,
                                isSelected = isSelected,
                                isCorrect = finalCorrectState,
                                onClick = {
                                    if (!isAnswerChecked) {
                                        selectedOption = index
                                    }
                                },
                                enabled = !isAnswerChecked,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đang tải câu hỏi...",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Bottom UI: Check button or total stats indicator
            if (!isAnswerChecked) {
                Button(
                    onClick = {
                        if (selectedOption != null && currentQuestion != null) {
                            isAnswerChecked = true
                            isCorrectAnswer = selectedOption == currentQuestion!!.correctIndex
                        }
                    },
                    enabled = selectedOption != null,
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
                        color = if (selectedOption != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Đã trả lời $totalAsked câu hỏi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                // Spacer height to balance the bottom when FeedbackBanner overlays
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        // Overlay Feedback Banner at the absolute bottom
        if (isAnswerChecked && currentQuestion != null) {
            FeedbackBanner(
                isCorrect = isCorrectAnswer,
                correctAnswerText = currentQuestion!!.correctItem.definition,
                onContinueClick = {
                    if (selectedOption != null) {
                        viewModel.submitAnswer(selectedOption!!)
                    }
                    selectedOption = null
                    isAnswerChecked = false
                    isCorrectAnswer = null
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
