package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.core.Animatable
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
import com.nhimz.vocabmaster.ui.components.quiz.FSRSTreeProgressBar
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Value-only state cho màn hình kết quả sau khi hoàn thành quiz.
 *
 * Được Container ([ResultScreen]) khởi tạo từ ViewModel/parameters và truyền
 * vào đây. Toàn bộ giá trị đều non-null và có default — Content không phải
 * xử lý null, nên không cần `!!` hay `as` (ARCH-02).
 */
data class ResultUiState(
    val xpGained: Int = 0,
    val durationSeconds: Int = 0,
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val averageStability: Double = 0.0,
    val incorrectCardIds: List<String> = emptyList(),
    val isLevelTest: Boolean = false,
    val isPassedLevelTest: Boolean = false
) {
    /**
     * Đếm tỷ lệ chính xác dưới dạng phần trăm 0..100. Trả về 100 nếu
     * `totalCount == 0` (phiên rỗng — defensive default để tránh chia 0).
     */
    val accuracyPercent: Int
        get() = if (totalCount > 0) ((correctCount.toFloat() / totalCount) * 100).toInt() else 100

    /**
     * Định dạng `durationSeconds` thành chuỗi `mm:ss` (Locale.US để tránh
     * NumberFormatException ở các locale có dấu phân cách khác — FSRS-03).
     */
    val durationFormatted: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }

    val hasMistakes: Boolean get() = accuracyPercent < 100
    val hasStability: Boolean get() = averageStability > 0.0
}

/**
 * Confetti particle model — chỉ dùng trong Composable scope, không cần
 * expose ra ngoài. Giữ `internal` để Container có thể pre-warm nếu cần.
 */
internal data class ConfettiParticle(
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

/**
 * Stateless UI cho màn hình Result (Plan 03-03, Task 2).
 *
 * Tuân thủ Container/Content pattern (ARCH-01):
 *  - Không gọi ViewModel
 *  - Không navigate trực tiếp
 *  - Không show snackbar/dialog
 *  - Chỉ nhận [state] (immutable) và phát ra event qua callback.
 *
 * Edge cases (03-UI-SPEC.md):
 *  - `totalCount == 0`: hiển thị 100% accuracy và stat row trống.
 *  - `accuracyPercent < 100`: thêm nút "Ôn lại các từ đã sai" với `incorrectCardIds`.
 *  - `averageStability > 0`: hiển thị cây FSRS; ngược lại hiển thị emoji 🎉.
 *  - `isLevelTest` đổi tiêu đề + copy theo level-test template.
 *
 * Animation state (animationTriggered, particles, scaleXp) là purely local — chỉ
 * thuộc về Content, không leak ra ngoài.
 */
@Composable
fun ResultScreenContent(
    state: ResultUiState,
    onBackToHome: () -> Unit,
    onReviewMistakes: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var animationTriggered by remember { mutableStateOf(false) }

    // Count-up XP animation
    val animatedXp by animateIntAsState(
        targetValue = if (animationTriggered) state.xpGained else 0,
        animationSpec = tween(1500)
    )

    val scaleXp = remember { Animatable(0.5f) }

    // Confetti particles list
    val particles = remember { mutableStateListOf<ConfettiParticle>() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(state.xpGained) {
        animationTriggered = true
        scaleXp.animateTo(1f, tween(1000))

        // Initialize Confetti Particles (only once per screen entry — keyed by xp)
        if (particles.isEmpty()) {
            val colors = listOf(
                Color(0xFF6C63FF), Color(0xFF3B82F6), Color(0xFF10B981),
                Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899)
            )
            for (i in 0..120) {
                val angle = Random.nextFloat() * Math.PI * 2
                val speed = Random.nextFloat() * 15f + 10f
                particles.add(
                    ConfettiParticle(
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

    // Confetti loop animation
    LaunchedEffect(particles) {
        while (true) {
            withFrameMillis {
                particles.forEachIndexed { _, p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.8f
                    p.vx *= 0.98f
                    p.rotation += p.rotationSpeed

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Confetti Canvas
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
                if (state.hasStability) {
                    FSRSTreeProgressBar(stability = state.averageStability, modifier = Modifier.height(180.dp))
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
                    text = if (state.isLevelTest) {
                        if (state.isPassedLevelTest) "🏆 Vượt cấp thành công!" else "⚠️ Chưa đạt yêu cầu"
                    } else "Hoàn thành bài học!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (state.isLevelTest) {
                        if (state.isPassedLevelTest) "Chúc mừng! Trình độ của bạn đã được nâng cấp."
                        else "Bạn cần đúng ít nhất 80% số câu để vượt cấp. Hãy tiếp tục luyện tập nhé!"
                    } else "Học tập kiên trì tạo nên sự khác biệt.",
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
                    Text(
                        text = "+$animatedXp XP",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    if (state.xpGained > 0) {
                        Text(
                            text = "Đã thêm vào Mục tiêu ngày!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = "Điểm kinh nghiệm nhận được",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 24.dp, top = if (state.xpGained > 0) 4.dp else 0.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Chính xác",
                            value = "${state.accuracyPercent}%",
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
                            value = state.durationFormatted,
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
                            value = "${state.correctCount}/${state.totalCount}",
                            color = GradientStart,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Back and Mistake buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.hasMistakes) {
                    Button(
                        onClick = { onReviewMistakes(state.incorrectCardIds) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = "Ôn lại các từ đã sai",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
}

@Composable
private fun StatItem(
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
