package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.util.FeedbackHelper

data class EasyQuestion(
    val word: String,
    val translation: String,
    val options: List<String>,
    val correctIndex: Int
)

@Composable
fun FirstWinScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    val questions = listOf(
        EasyQuestion("Hello", "Xin chào", listOf("Xin chào", "Tạm biệt", "Cảm ơn", "Xin lỗi"), 0),
        EasyQuestion("Book", "Quyển sách", listOf("Cái bút", "Quyển sách", "Cái bàn", "Cái ghế"), 1),
        EasyQuestion("Water", "Nước", listOf("Lửa", "Đất", "Nước", "Gió"), 2)
    )

    fun handleAnswer(index: Int) {
        val question = questions[currentQuestionIndex]
        if (index == question.correctIndex) {
            FeedbackHelper.playSoundCorrect()
            FeedbackHelper.vibrateCorrect(context)

            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex += 1
            } else {
                isFinished = true
            }
        } else {
            FeedbackHelper.playSoundIncorrect()
            FeedbackHelper.vibrateIncorrect(context)
        }
    }

    if (isFinished) {
        // Success celebration view
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Trophy/Success box
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 64.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Chúc mừng bạn!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bạn đã hoàn thành bài kiểm tra đầu tiên với 3 câu trả lời chính xác liên tiếp. Hành trình làm chủ Tiếng Anh đã sẵn sàng!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "+50 XP Thưởng đầu vào",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B) // Gold color
                )
            }

            Button(
                onClick = onFinished,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(28.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Bắt đầu học ngay",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        // Simple game questions
        val currentQuestion = questions[currentQuestionIndex]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Chiến thắng đầu tiên của bạn",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentQuestionIndex.toFloat() / questions.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GradientStart,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Câu hỏi ${currentQuestionIndex + 1}/${questions.size}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "FirstWinQuestion",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { question ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nghĩa của từ này là gì?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GradientStart.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = question.word,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    question.options.forEachIndexed { index, option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { handleAnswer(index) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                            brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
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
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
