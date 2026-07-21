package com.nhimz.vocabmaster.ui.screens

import com.nhimz.vocabmaster.domain.model.displayTitle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.ui.components.quiz.*
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.ui.viewmodel.QuizSessionState
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuizScreen(
    onSessionCompleted: (xpGained: Int, durationSeconds: Int, correctCount: Int, totalCount: Int, averageStability: Double, incorrectCardIds: List<String>, isLevelTest: Boolean, isPassedLevelTest: Boolean) -> Unit,
    onBackToHome: () -> Unit,
    cdnAudioPlayer: CDNAudioPlayer,
    viewModel: QuizViewModel
) {
    val sessionState by viewModel.sessionState.collectAsState()

    // Pass data out to navigate to ResultScreen when Completed
    LaunchedEffect(sessionState) {
        if (sessionState is QuizSessionState.Completed) {
            val completedState = sessionState as QuizSessionState.Completed
            if (completedState.totalCount > 0) {
                onSessionCompleted(
                    completedState.xpGained,
                    completedState.durationSeconds,
                    completedState.correctCount,
                    completedState.totalCount,
                    completedState.averageStability,
                    completedState.incorrectCardIds,
                    completedState.isCheckpointOrJumpTest,
                    completedState.isPassed
                )
            }
        }
    }

    when (val state = sessionState) {
        is QuizSessionState.Loading -> {
            QuizLoadingSkeleton()
        }
        is QuizSessionState.Active -> {
            val question = state.questions[state.currentIndex]
            val hasAnswered = state.isAnswerRevealed
            val shakeOffset = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            var selectedOptionIndex by remember(state.currentIndex) { mutableStateOf<Int?>(null) }
            var selectedWordsForScrambled by remember(state.currentIndex) { mutableStateOf<List<String>>(emptyList()) }
            var typedText by remember(state.currentIndex) { mutableStateOf("") }
            
            // FSRS Rating variables
            var isFlipped by remember(state.currentIndex) { mutableStateOf(false) }
            var selectedFSRSRating by remember(state.currentIndex) { mutableStateOf<Rating?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp) // Provide space for Feedback Banner
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Status
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { onBackToHome() }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        DuolingoProgressBar(
                            progress = if (state.questions.isNotEmpty()) state.currentIndex.toFloat() / state.questions.size else 0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Node Title and Scenario Context Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val promptLabel = when (question.type) {
                            is QuizType.Introduction -> "Giới thiệu từ mới"
                            is QuizType.FSRSTailFlashcard -> "Ôn tập Flashcard"
                            is QuizType.ScrambledSentence -> "Sắp xếp lại câu"
                            is QuizType.Listening -> "Nghe và chọn"
                            is QuizType.Matching -> "Ghép đôi"
                            is QuizType.Typing -> "Nhập câu trả lời"
                            else -> "Điền từ vào chỗ trống"
                        }
                        
                        Text(
                            text = promptLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GradientStart.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question Content Box
                    Box(
                        modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    ) {
                        when (val type = question.type) {
                            is QuizType.Introduction -> {
                                IntroductionCard(
                                    itemWithCard = type.itemWithCard,
                                    prompt = type.prompt,
                                    audioUrl = type.audioUrl,
                                    cdnAudioPlayer = cdnAudioPlayer
                                )
                            }
                            is QuizType.MultipleChoice -> {
                                MultipleChoiceCard(
                                    type = type,
                                    hasAnswered = hasAnswered,
                                    selectedOptionIndex = selectedOptionIndex,
                                    onOptionSelected = { if (!hasAnswered) selectedOptionIndex = it }
                                )
                            }
                            is QuizType.Listening -> {
                                ListeningQuestionCard(
                                    type = type,
                                    hasAnswered = hasAnswered,
                                    selectedOptionIndex = selectedOptionIndex,
                                    onOptionSelected = { if (!hasAnswered) selectedOptionIndex = it },
                                    cdnAudioPlayer = cdnAudioPlayer
                                )
                            }
                            is QuizType.Matching -> {
                                key(state.currentIndex) {
                                    MatchingQuestionCard(
                                        type = type,
                                        hasAnswered = hasAnswered,
                                        onPairsMatched = {
                                            // Auto submit when all pairs are matched
                                            if (!hasAnswered) {
                                                viewModel.submitAnswer()
                                            }
                                        }
                                    )
                                }
                            }
                            is QuizType.Typing -> {
                                TypingQuestionCard(
                                    type = type,
                                    hasAnswered = hasAnswered,
                                    typedText = typedText,
                                    onTextChanged = { if (!hasAnswered) typedText = it },
                                    cdnAudioPlayer = cdnAudioPlayer
                                )
                            }
                            is QuizType.ScrambledSentence -> {
                                val isCorrectState = if (hasAnswered) {
                                    val userSentence = selectedWordsForScrambled.joinToString(" ")
                                    userSentence == type.correctSentence
                                } else null
                                
                                ScrambledQuizCard(
                                    scrambledWords = type.scrambledWords,
                                    selectedWords = selectedWordsForScrambled,
                                    isAnswerRevealed = hasAnswered,
                                    isCorrect = isCorrectState,
                                    onWordSelected = { word, _ -> if (!hasAnswered) selectedWordsForScrambled = selectedWordsForScrambled + word },
                                    onWordUnselected = { word, index -> 
                                        if (!hasAnswered) {
                                            val newList = selectedWordsForScrambled.toMutableList()
                                            if (index in newList.indices) newList.removeAt(index)
                                            selectedWordsForScrambled = newList
                                        }
                                    }
                                )
                            }
                            is QuizType.FSRSTailFlashcard -> {
                                // FSRS Flashcard
                                FSRSFlashcardCard(
                                    type = type,
                                    isFlipped = isFlipped,
                                    onFlip = { isFlipped = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Action Buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    if (question.type is QuizType.FSRSTailFlashcard && isFlipped && !hasAnswered) {
                        // FSRS Rating buttons
                        FSRSRatingButtons(onRatingSelected = { r ->
                            selectedFSRSRating = r
                            viewModel.submitAnswer(fsrsRating = r)
                        })
                    } else if (!hasAnswered && question.type !is QuizType.FSRSTailFlashcard) {
                        // Main Check Button
                        val isSubmitEnabled = when (question.type) {
                            is QuizType.MultipleChoice -> selectedOptionIndex != null
                            is QuizType.Listening -> selectedOptionIndex != null
                            is QuizType.Typing -> typedText.isNotBlank()
                            is QuizType.ScrambledSentence -> selectedWordsForScrambled.isNotEmpty()
                            is QuizType.Introduction -> true
                            is QuizType.Matching -> false // Matching auto-submits when done
                            else -> false
                        }
                        
                        if (question.type !is QuizType.Matching) { // Hide CHECK button for matching
                            Button(
                                onClick = {
                                    if (isSubmitEnabled) {
                                        val isCorrect = when (val type = question.type) {
                                            is QuizType.MultipleChoice -> selectedOptionIndex == type.correctIndex
                                            is QuizType.Listening -> selectedOptionIndex == type.correctIndex
                                            is QuizType.Typing -> {
                                                val cleanedUser = typedText.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                                                val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                                                cleanedUser == cleanedCorrect
                                            }
                                            is QuizType.ScrambledSentence -> selectedWordsForScrambled.joinToString(" ") == type.correctSentence
                                            is QuizType.Introduction -> true
                                            else -> false
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
                                            textAnswer = typedText,
                                            selectedWordsForScrambled = selectedWordsForScrambled
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubmitEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                val buttonText = if (question.type is QuizType.Introduction) "ĐÃ HIỂU" else "KIỂM TRA"
                                Text(
                                    text = buttonText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubmitEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }

                    // Feedback banner moved out of Column
                }

                // Feedback Banner (If Answered) anchored to the bottom of the Box
                if (hasAnswered && question.type !is QuizType.FSRSTailFlashcard) {
                    val isCorrectAnswer = when (val type = question.type) {
                        is QuizType.MultipleChoice -> state.selectedOption == type.correctIndex
                        is QuizType.Listening -> state.selectedOption == type.correctIndex
                        is QuizType.Typing -> {
                            val cleanedUser = typedText.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                            val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
                            cleanedUser == cleanedCorrect
                        }
                        is QuizType.ScrambledSentence -> selectedWordsForScrambled.joinToString(" ") == type.correctSentence
                        is QuizType.Matching -> true
                        is QuizType.Introduction -> true
                        else -> false
                    }
                    FeedbackBanner(
                        isCorrect = isCorrectAnswer,
                        correctAnswerText = when (val type = question.type) {
                            is QuizType.MultipleChoice -> type.options.getOrNull(type.correctIndex) ?: ""
                            is QuizType.Listening -> type.options?.getOrNull(type.correctIndex ?: 0) ?: ""
                            is QuizType.Typing -> type.correctSentence
                            is QuizType.ScrambledSentence -> type.correctSentence
                            else -> ""
                        },
                        onContinueClick = {
                            viewModel.nextQuestion()
                            selectedOptionIndex = null
                            selectedWordsForScrambled = emptyList()
                            typedText = ""
                        },
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    )
                }

                if (question.type is QuizType.FSRSTailFlashcard && state.isFSRSRatingSelected) {
                    FeedbackBanner(
                        isCorrect = true,
                        correctAnswerText = "",
                        onContinueClick = {
                            viewModel.nextQuestion()
                            isFlipped = false
                            selectedFSRSRating = null
                        },
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    )
                }
            }
        }
        is QuizSessionState.Completed -> {
            if (state.totalCount == 0) {
                QuizEmptyState(onBackToHome)
            }
        }
        is QuizSessionState.Error -> {
            quizErrorState(message = state.message, onRetry = onBackToHome)
        }
    }
}

@Composable
private fun quizErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(text = "Thử lại", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(text = "Quay lại trang chủ", fontSize = 14.sp)
        }
    }
}

@Composable
fun MultipleChoiceCard(
    type: QuizType.MultipleChoice,
    hasAnswered: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = type.prompt,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        )
        
        type.options.forEachIndexed { index, option ->
            val isSelected = selectedOptionIndex == index
            val finalCorrectState = if (hasAnswered) {
                if (index == type.correctIndex) true
                else if (isSelected) false
                else null
            } else null
            
            DuolingoOptionCard(
                optionText = option,
                isSelected = isSelected,
                isCorrect = finalCorrectState,
                onClick = { onOptionSelected(index) },
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun FSRSFlashcardCard(
    type: QuizType.FSRSTailFlashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    if (!isFlipped) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { onFlip() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.itemWithCard.question.displayTitle(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "(Chạm để lật bài)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = type.itemWithCard.question.translation ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                // FSRS Flashcard UI may need a different field for example, we'll leave it empty for now
            }
        }
    }
}

@Composable
fun FSRSRatingButtons(onRatingSelected: (Rating) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(onClick = { onRatingSelected(Rating.Again) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)), modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("Lại") }
        Button(onClick = { onRatingSelected(Rating.Hard) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) { Text("Khó") }
        Button(onClick = { onRatingSelected(Rating.Good) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) { Text("Tốt") }
        Button(onClick = { onRatingSelected(Rating.Easy) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Dễ") }
    }
}

@Composable
private fun QuizLoadingSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(24.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.height(16.dp).weight(1f).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)))
        }
        Spacer(modifier = Modifier.height(48.dp))
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)))
        Spacer(modifier = Modifier.height(24.dp))
        repeat(4) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 8.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
private fun QuizEmptyState(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "🌱", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Không có câu hỏi nào", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Chủ đề hoặc lộ trình học này hiện không có câu hỏi nào khả dụng. Vui lòng quay lại sau!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) { Text(text = "Quay lại trang chủ", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}
