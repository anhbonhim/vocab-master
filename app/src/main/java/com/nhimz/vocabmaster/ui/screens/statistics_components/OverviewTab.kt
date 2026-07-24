package com.nhimz.vocabmaster.ui.screens.statistics_components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.DailyXp

@Composable
fun OverviewTab(xpHistory: List<DailyXp>, stats: com.nhimz.vocabmaster.domain.model.ReviewStats?) {
    var animateChart by remember { mutableStateOf(false) }
    LaunchedEffect(xpHistory) {
        if (xpHistory.isNotEmpty()) {
            animateChart = true
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            XpHistoryChartCard(xpHistory = xpHistory, animateChart = animateChart)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tổng quan trạng thái từ vựng",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (stats != null) {
            item {
                VocabularyStatusSection(stats = stats)
            }
        } else {
            item {
                Text(
                    text = "Đang tải dữ liệu...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun XpHistoryChartCard(xpHistory: List<DailyXp>, animateChart: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lịch sử XP (7 ngày qua)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left
            )
            Spacer(modifier = Modifier.height(24.dp))

            val maxVal = (xpHistory.maxOfOrNull { it.xp } ?: 100).coerceAtLeast(50)
            val chartHeight = 160.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                xpHistory.forEach { daily ->
                    val heightFraction = if (maxVal > 0) daily.xp.toFloat() / maxVal.toFloat() else 0f
                    val animatedHeightFraction by animateFloatAsState(
                        targetValue = if (animateChart) heightFraction else 0f,
                        animationSpec = tween(1000)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (daily.xp > 0) "${daily.xp}" else "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GradientStart
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .width(18.dp)
                                .height(110.dp)
                        ) {
                            val barH = size.height * animatedHeightFraction
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(GradientStart, GradientEnd)),
                                topLeft = Offset(0f, size.height - barH),
                                size = Size(size.width, barH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = daily.dayLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularyStatusSection(stats: com.nhimz.vocabmaster.domain.model.ReviewStats) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatusStatsCard(
                title = "Đang học (Learning)",
                count = stats.countByState[State.Learning] ?: 0,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatusStatsCard(
                title = "Đã thuộc (Review)",
                count = stats.countByState[State.Review] ?: 0,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatusStatsCard(
                title = "Học lại (Relearn)",
                count = stats.countByState[State.Relearning] ?: 0,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatusStatsCard(
                title = "Từ mới (New)",
                count = stats.countByState[State.New] ?: 0,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatusStatsCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}