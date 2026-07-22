package com.nhimz.vocabmaster.ui.screens

import com.nhimz.vocabmaster.domain.model.displayTitle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.nhimz.vocabmaster.R
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoOptionCard
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoProgressBar
import com.nhimz.vocabmaster.ui.components.quiz.FeedbackBanner
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.FirstWinQuestion
import com.nhimz.vocabmaster.ui.viewmodel.FirstWinSessionState
import com.nhimz.vocabmaster.ui.viewmodel.FirstWinViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Confetti Particle model
data class FirstWinConfettiParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val color: Color,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun FirstWinScreen(
    onFinished: () -> Unit,
    viewModel: FirstWinViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionState by viewModel.sessionState.collectAsState()

    // Shake offset for incorrect answers
    val shakeOffset = remember { Animatable(0f) }

    // Local selection tracking
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }

    // Confetti particles list
    val particles = remember { mutableStateListOf<FirstWinConfettiParticle>() }

    // Celebration screen states
    var celebrationTriggered by remember { mutableStateOf(false) }
    val animatedXp by animateIntAsState(
        targetValue = if (celebrationTriggered) 50 else 0,
        animationSpec = tween(1500),
        label = "xpCountUpAnimation"
    )

    // Celebration confetti simulation loop
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(sessionState) {
        if (sessionState is FirstWinSessionState.Completed) {
            celebrationTriggered = true

            // Initialize Confetti Particles
            val colors = listOf(
                Color(0xFF6C63FF), Color(0xFF3B82F6), Color(0xFF10B981),
                Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899)
            )
            for (i in 0..120) {
                val angle = Random.nextFloat() * Math.PI * 2
                val speed = Random.nextFloat() * 15f + 10f
                particles.add(
                    FirstWinConfettiParticle(
                        x = screenWidthPx / 2f,
                        y = screenHeightPx * 0.4f,
                        size = Random.nextFloat() * 15f + 15f,
                        color = colors.random(),
                        vx = (cos(angle) * speed).toFloat(),
                        vy = (sin(angle) * speed - 15f).toFloat(),
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = Random.nextFloat() * 10f - 5f,
                        isCircle = Random.nextBoolean()
                    )
                )
            }
        }
    }

    LaunchedEffect(particles) {
        if (particles.isNotEmpty()) {
            while (true) {
                withFrameMillis {
                    particles.forEach { p ->
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += 0.8f // Gravity
                        p.vx *= 0.98f // Drag
                        p.rotation += p.rotationSpeed

                        // Recycle falling particles
                        if (p.y > screenHeightPx) {
                            p.y = -50f
                            p.x = Random.nextFloat() * screenWidthPx
                            p.vy = Random.nextFloat() * 5f + 2f
                            p.vx = Random.nextFloat() * 4f - 2f
                        }
                    }
                }
            }
        }
    }

    when (val state = sessionState) {
        is FirstWinSessionState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đang tải bài học đầu tiên...",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        is FirstWinSessionState.Active -> {
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
                    // Top Progress Bar Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Chiến thắng đầu tiên của bạn",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val progress = (state.currentIndex.toFloat() / state.questions.size.toFloat())
                        DuolingoProgressBar(progress = progress)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Câu hỏi ${state.currentIndex + 1}/${state.questions.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Animated Question Area
                    AnimatedContent(
                        targetState = question,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "FirstWinQuestionAnimation",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { activeQuestion ->
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
                                    .padding(bottom = 24.dp)
                                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
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
                                        text = activeQuestion.prompt,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Dynamic options with DuolingoOptionCard
                            activeQuestion.options.forEachIndexed { index, option ->
                                val isSelected = selectedOptionIndex == index
                                val finalCorrectState = if (hasAnswered) {
                                    if (index == activeQuestion.correctIndex) {
                                        true
                                    } else if (isSelected) {
                                        false
                                    } else {
                                        null
                                    }
                                } else null

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
                    }

                    // Check Answer Button / spacer
                    if (!hasAnswered) {
                        Button(
                            onClick = {
                                val pickedIndex = selectedOptionIndex ?: return@Button
                                val isCorrect = pickedIndex == question.correctIndex
                                if (!isCorrect) {
                                    scope.launch {
                                        repeat(3) {
                                            shakeOffset.animateTo(15f, tween(40))
                                            shakeOffset.animateTo(-15f, tween(40))
                                        }
                                        shakeOffset.animateTo(0f, tween(40))
                                    }
                                }
                                viewModel.submitAnswer(pickedIndex)
                            },
                            enabled = selectedOptionIndex != null,
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
                                color = if (selectedOptionIndex != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(110.dp))
                    }
                }

                // Bottom Feedback Banner
                if (hasAnswered) {
                    FeedbackBanner(
                        isCorrect = state.isCorrectAnswer,
                        correctAnswerText = question.item.question.translation ?: "",
                        onContinueClick = {
                            viewModel.nextQuestion()
                            selectedOptionIndex = null
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
        is FirstWinSessionState.Completed -> {
            // Celebration Phase
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Confetti particle simulator Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    particles.forEach { p ->
                        rotate(p.rotation, pivot = Offset(p.x + p.size / 2, p.y + p.size / 2)) {
                            if (p.isCircle) {
                                drawCircle(color = p.color, radius = p.size / 2, center = Offset(p.x, p.y))
                            } else {
                                drawRect(color = p.color, topLeft = Offset(p.x, p.y), size = Size(p.size, p.size / 2))
                            }
                        }
                    }
                }

                // Celebration content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                        // Lottie animation player
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration))
                        LottieAnimation(
                            composition = composition,
                            modifier = Modifier.size(220.dp),
                            iterations = LottieConstants.IterateForever
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Chúc mừng bạn!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Bạn đã hoàn thành bài kiểm tra đầu tiên xuất sắc. Hành trình học tập và làm chủ Tiếng Anh đã chính thức bắt đầu!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Animated XP counter indicator
                        Text(
                            text = "+$animatedXp XP Thưởng đầu vào",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B) // Gold color
                        )
                    }

                    // CTA button (Duolingo Style: flat, large, bottom-anchored)
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
            }
        }
    }
}
