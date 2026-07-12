package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.components.quiz.FSRSTreeProgressBar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Confetti Particle model
data class ConfettiParticle(
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
fun ResultScreen(
    xpGained: Int,
    durationSeconds: Int,
    correctCount: Int,
    totalCount: Int,
    averageStability: Double,
    onBackToHome: () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }

    // Count up animations
    val animatedXp by animateIntAsState(
        targetValue = if (animationTriggered) xpGained else 0,
        animationSpec = tween(1500)
    )

    val scaleXp = remember { Animatable(0.5f) }

    // Confetti particles list
    val particles = remember { mutableStateListOf<ConfettiParticle>() }

    LaunchedEffect(Unit) {
        animationTriggered = true
        scaleXp.animateTo(1f, tween(1000))

        // Initialize Confetti Particles
        val colors = listOf(
            Color(0xFF6C63FF), Color(0xFF3B82F6), Color(0xFF10B981),
            Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899)
        )
        for (i in 0..120) {
            val angle = Random.nextFloat() * Math.PI * 2
            val speed = Random.nextFloat() * 15f + 10f
            particles.add(
                ConfettiParticle(
                    x = 540f, // Center pivot coordinates
                    y = 800f,
                    size = Random.nextFloat() * 15f + 15f,
                    color = colors.random(),
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 15f).toFloat(), // Upward force bias
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f,
                    isCircle = Random.nextBoolean()
                )
            )
        }
    }

    // Confetti loop animation
    LaunchedEffect(particles) {
        while (true) {
            withFrameMillis {
                particles.forEachIndexed { index, p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.8f // Gravity effect
                    p.vx *= 0.98f // Wind friction
                    p.rotation += p.rotationSpeed

                    // Recycle out of bounds particles
                    if (p.y > 2200f) {
                        p.y = -50f
                        p.x = Random.nextFloat() * 1080f
                        p.vy = Random.nextFloat() * 5f + 2f
                        p.vx = Random.nextFloat() * 4f - 2f
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Confetti Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                rotate(p.rotation, pivot = Offset(p.x + p.size/2, p.y + p.size/2)) {
                    if (p.isCircle) {
                        drawCircle(color = p.color, radius = p.size / 2, center = Offset(p.x, p.y))
                    } else {
                        drawRect(color = p.color, topLeft = Offset(p.x, p.y), size = Size(p.size, p.size / 2))
                    }
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Celebration Icon or Tree
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (averageStability > 0.0) {
                    FSRSTreeProgressBar(stability = averageStability, modifier = Modifier.height(180.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Text(text = "🎉", fontSize = 48.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Hoàn thành bài học!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Học tập kiên trì tạo nên sự khác biệt.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Stats grid card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleXp.value),
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
                    // XP counting animation
                    Text(
                        text = "+$animatedXp XP",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "Điểm kinh nghiệm nhận được",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Session stats details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Chính xác",
                            value = if (totalCount > 0) "${(correctCount * 100) / totalCount}%" else "100%",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                        StatItem(
                            label = "Thời gian",
                            value = formatDuration(durationSeconds),
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                        StatItem(
                            label = "Câu trả lời",
                            value = "$correctCount/$totalCount",
                            color = GradientStart,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Back button
            Button(
                onClick = onBackToHome,
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
                    text = "Quay lại trang chủ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
