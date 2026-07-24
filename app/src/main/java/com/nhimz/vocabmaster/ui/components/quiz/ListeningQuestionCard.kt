package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import kotlinx.coroutines.delay

@Composable
fun ListeningQuestionCard(
    type: QuizType.Listening,
    hasAnswered: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    onPlayAudio: (String?) -> Unit
) {
    // Local playback state for visual feedback.
    // Toggled on when user taps either play button; auto-resets after
    // a reasonable clip duration since AudioPlayer is fire-and-forget.
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(3_500L)
            isPlaying = false
        }
    }

    // Pulsing scale animation driven by an infinite transition.
    // Only active when isPlaying == true; otherwise the button renders static.
    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Audio Controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            // Normal Speed Button with pulse animation when audio is playing
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(92.dp)
            ) {
                // Outer glow ring — pulses in opposite phase for a breathing effect
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = (1.24f - pulseScale).coerceIn(0f, 0.45f)
                            }
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                    )
                }

                Button(
                    onClick = {
                        isPlaying = true
                        type.audioUrl?.let { url ->
                            onPlayAudio(url)
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .then(
                            if (isPlaying) {
                                Modifier.graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                }
                            } else Modifier
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("🔊", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Slow Speed Button
            Button(
                onClick = {
                    isPlaying = true
                    type.audioUrlSlow?.let { url ->
                        onPlayAudio(url)
                    } ?: type.audioUrl?.let { url ->
                        // Fallback to normal if slow is missing
                        onPlayAudio(url)
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("🐢", fontSize = 24.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Options
        type.options?.forEachIndexed { index, option ->
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
