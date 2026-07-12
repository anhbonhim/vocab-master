package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.components.quiz.FSRSTreeProgressBar
import com.nhimz.vocabmaster.ui.util.FeedbackHelper
import com.nhimz.vocabmaster.ui.viewmodel.FlashcardSessionState
import com.nhimz.vocabmaster.ui.viewmodel.FlashcardViewModel

@Composable
fun FlashcardScreen(
    onSessionCompleted: (xpGained: Int, durationSeconds: Int, correctCount: Int, totalCount: Int, averageStability: Double) -> Unit,
    onBackToHome: () -> Unit,
    cdnAudioPlayer: CDNAudioPlayer,
    viewModel: FlashcardViewModel
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()

    LaunchedEffect(sessionState) {
        if (sessionState is FlashcardSessionState.Completed) {
            val completed = sessionState as FlashcardSessionState.Completed
            onSessionCompleted(
                completed.xpGained,
                completed.durationSeconds,
                completed.correctCount,
                completed.totalCount,
                completed.averageStability
            )
        } else if (sessionState is FlashcardSessionState.Active) {
            val active = sessionState as FlashcardSessionState.Active
            val card = active.cards[active.currentIndex]
            // Play TTS on new card load
            cdnAudioPlayer.playAudio(card.vocabulary.audioUrl)
        }
    }

    when (val state = sessionState) {
        is FlashcardSessionState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đang tải Flashcard...",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        is FlashcardSessionState.Active -> {
            val currentCard = state.cards[state.currentIndex]
            val isFlipped = state.isFlipped

            val rotation by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 500)
            )

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
                        text = "Ôn tập Flashcard (FSRS)",
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

                // Progress Bar and Tree
                Column(modifier = Modifier.fillMaxWidth()) {
                    val progress = (state.currentIndex.toFloat() / state.cards.size.toFloat())
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
                            text = "Thẻ ${state.currentIndex + 1}/${state.cards.size}",
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

                    // Show FSRS Tree for current card
                    FSRSTreeProgressBar(
                        stability = currentCard.card.stability,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // 3D Flip Card Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12 * density
                            }
                            .clickable { viewModel.flipCard() },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        if (rotation < 90f) {
                            // Front of Card
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentCard.vocabulary.partOfSpeech.lowercase(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GradientStart.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { cdnAudioPlayer.playAudio(currentCard.vocabulary.audioUrl) }
                                            .padding(6.dp)
                                    ) {
                                        Text("🔊", fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = currentCard.vocabulary.word,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                if (!currentCard.vocabulary.ipa.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentCard.vocabulary.ipa!!,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                Text(
                                    text = "Chạm để lật thẻ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Back of Card (Mirror it back to draw text normally)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationY = 180f
                                    }
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentCard.vocabulary.definition,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (!currentCard.vocabulary.example.isNullOrEmpty()) {
                                    Text(
                                        text = "Ví dụ: ${currentCard.vocabulary.example}",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                Text(
                                    text = "Chạm để xem từ vựng",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Spaced Repetition Buttons (Again, Hard, Good, Easy) at the bottom
                if (isFlipped) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Again Button
                        FSRSButton(
                            label = "Again",
                            description = "Quên",
                            color = Color(0xFFEF4444),
                            onClick = {
                                FeedbackHelper.playSoundIncorrect()
                                FeedbackHelper.vibrateIncorrect(context)
                                viewModel.rateCard(Rating.Again)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Hard Button
                        FSRSButton(
                            label = "Hard",
                            description = "Khó",
                            color = Color(0xFFF59E0B),
                            onClick = {
                                FeedbackHelper.playSoundCorrect()
                                FeedbackHelper.vibrateCorrect(context)
                                viewModel.rateCard(Rating.Hard)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Good Button
                        FSRSButton(
                            label = "Good",
                            description = "Được",
                            color = Color(0xFF3B82F6),
                            onClick = {
                                FeedbackHelper.playSoundCorrect()
                                FeedbackHelper.vibrateCorrect(context)
                                viewModel.rateCard(Rating.Good)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Easy Button
                        FSRSButton(
                            label = "Easy",
                            description = "Dễ",
                            color = Color(0xFF10B981),
                            onClick = {
                                FeedbackHelper.playSoundCorrect()
                                FeedbackHelper.vibrateCorrect(context)
                                viewModel.rateCard(Rating.Easy)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.flipCard() },
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
                            text = "Lật mặt sau",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        is FlashcardSessionState.Completed -> {
            // Screen transition handled in LaunchedEffect
        }
    }
}

@Composable
fun FSRSButton(
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        border = BorderStroke(1.5.dp, color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}
