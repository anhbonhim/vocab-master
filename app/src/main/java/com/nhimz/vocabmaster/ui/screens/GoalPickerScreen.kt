package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart

data class GoalOption(
    val minutes: Int,
    val levelName: String,
    val description: String
)

@Composable
fun GoalPickerScreen(
    onGoalSelected: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(10) }

    val options = listOf(
        GoalOption(5, "Casual", "Học thong thả, chỉ 5 phút mỗi ngày."),
        GoalOption(10, "Regular", "Tiến độ vừa phải, 10 phút mỗi ngày."),
        GoalOption(15, "Serious", "Quyết tâm cao, 15 phút mỗi ngày."),
        GoalOption(20, "Hardcore", "Đam mê học hỏi, 20 phút mỗi ngày.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Mục tiêu hàng ngày",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chọn thời gian học phù hợp với quỹ thời gian của bạn. Có thể điều chỉnh lại sau này.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            options.forEach { option ->
                val isSelected = option.minutes == selectedMinutes
                val cardElevation by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 2.dp,
                    animationSpec = tween(300)
                )
                val cardBorderColor by animateColorAsState(
                    targetValue = if (isSelected) GradientStart else Color.Transparent,
                    animationSpec = tween(300)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedMinutes = option.minutes },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                    border = BorderStroke(2.dp, cardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.levelName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) GradientStart else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // Goal text bubble
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = if (isSelected) {
                                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.LightGray, Color.Gray))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${option.minutes} Phút",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onGoalSelected(selectedMinutes) },
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
                text = "Tiếp tục",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
