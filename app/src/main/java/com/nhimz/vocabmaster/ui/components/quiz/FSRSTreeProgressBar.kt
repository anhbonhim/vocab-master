package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FSRSTreeProgressBar(
    stability: Double, // FSRS stability value
    maxStability: Double = 30.0, // Assume 1 month is a fully grown tree
    modifier: Modifier = Modifier
) {
    // Normalize progress between 0f and 1f
    val progress = (stability / maxStability).coerceIn(0.0, 1.0).toFloat()
    
    // Animation state
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "Tree Growth Animation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val stageLabel = when {
            animatedProgress < 0.05f -> "Hạt giống mầm"
            animatedProgress < 0.4f -> "Cây non"
            animatedProgress < 0.8f -> "Cây đang lớn"
            else -> "Cây trưởng thành"
        }

        val treeColor = SuccessGreen
        val trunkColor = Color(0xFF8B5A2B) // Brown
        val dirtColor = Color(0xFF654321)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val baseY = canvasHeight - 10f // Dirt level

                // 1. Draw Dirt (Ground)
                drawRoundRect(
                    color = dirtColor.copy(alpha = 0.5f),
                    topLeft = Offset(centerX - 60f, baseY),
                    size = Size(120f, 15f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                if (animatedProgress > 0f) {
                    // Tree metrics based on progress
                    val maxTrunkHeight = 60f
                    val trunkHeight = maxTrunkHeight * (0.2f + 0.8f * animatedProgress)
                    
                    val maxTrunkWidth = 12f
                    val trunkWidth = maxTrunkWidth * (0.3f + 0.7f * animatedProgress)

                    // 2. Draw Trunk
                    drawLine(
                        color = trunkColor,
                        start = Offset(centerX, baseY),
                        end = Offset(centerX, baseY - trunkHeight),
                        strokeWidth = trunkWidth,
                        cap = StrokeCap.Round
                    )

                    // 3. Draw Leaves/Branches based on stage
                    if (animatedProgress >= 0.05f) {
                        // Sprout stage (small leaf)
                        val leafScale = if (animatedProgress < 0.4f) (animatedProgress - 0.05f) / 0.35f else 1f
                        drawCircle(
                            color = treeColor.copy(alpha = 0.8f),
                            radius = 15f * leafScale,
                            center = Offset(centerX, baseY - trunkHeight)
                        )
                    }

                    if (animatedProgress >= 0.4f) {
                        // Growing tree branches
                        val branchScale = if (animatedProgress < 0.8f) (animatedProgress - 0.4f) / 0.4f else 1f
                        
                        // Left branch
                        drawLine(
                            color = trunkColor,
                            start = Offset(centerX, baseY - trunkHeight * 0.6f),
                            end = Offset(centerX - 25f * branchScale, baseY - trunkHeight - 10f * branchScale),
                            strokeWidth = trunkWidth * 0.6f,
                            cap = StrokeCap.Round
                        )
                        // Left Leaves
                        drawCircle(
                            color = treeColor.copy(alpha = 0.9f),
                            radius = 20f * branchScale,
                            center = Offset(centerX - 25f * branchScale, baseY - trunkHeight - 10f * branchScale)
                        )

                        // Right branch
                        drawLine(
                            color = trunkColor,
                            start = Offset(centerX, baseY - trunkHeight * 0.4f),
                            end = Offset(centerX + 30f * branchScale, baseY - trunkHeight - 5f * branchScale),
                            strokeWidth = trunkWidth * 0.6f,
                            cap = StrokeCap.Round
                        )
                        // Right Leaves
                        drawCircle(
                            color = treeColor.copy(alpha = 0.9f),
                            radius = 22f * branchScale,
                            center = Offset(centerX + 30f * branchScale, baseY - trunkHeight - 5f * branchScale)
                        )
                    }

                    if (animatedProgress >= 0.8f) {
                        // Full mature canopy
                        val canopyScale = (animatedProgress - 0.8f) / 0.2f
                        drawCircle(
                            color = treeColor,
                            radius = 35f * canopyScale,
                            center = Offset(centerX, baseY - trunkHeight - 15f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stageLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Độ vững chắc: ${"%.1f".format(stability)} ngày",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
