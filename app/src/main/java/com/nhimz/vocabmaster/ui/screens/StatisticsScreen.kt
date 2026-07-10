package com.nhimz.vocabmaster.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.fsrs.State
import com.nhimz.vocabmaster.domain.model.VocabularyItemWithCard
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.BadgeItem
import com.nhimz.vocabmaster.ui.viewmodel.DailyXp
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tổng quan", "Huy hiệu", "Sổ sai lầm")

    val stats by viewModel.reviewStats.collectAsState()
    val mistakeCards by viewModel.mistakeCards.collectAsState()
    val xpHistory by viewModel.xpHistory.collectAsState()
    val badges by viewModel.badges.collectAsState()

    // Trigger loading on display
    LaunchedEffect(Unit) {
        viewModel.loadStatisticsData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Thống kê & Thành tựu",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    width = 64.dp,
                    color = GradientStart
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(xpHistory, stats)
                1 -> BadgesTab(badges)
                2 -> MistakeBankTab(mistakeCards)
            }
        }
    }
}

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
            // Chart Card
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

                    // Draw custom XP Bar Chart
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
                                // Canvas Bar Column
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

@Composable
fun BadgesTab(badges: List<BadgeItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(badges) { badge ->
            val alpha = if (badge.isUnlocked) 1f else 0.4f
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (badge.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isUnlocked) 3.dp else 0.dp),
                modifier = Modifier.alpha(alpha)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (badge.isUnlocked) Color(0xFFF59E0B).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (badge.isUnlocked) badge.icon else "🔒",
                            fontSize = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = badge.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badge.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MistakeBankTab(mistakeCards: List<VocabularyItemWithCard>) {
    if (mistakeCards.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🌱",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Không có lỗi sai nào!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Từ vựng của bạn có tỉ lệ chính xác rất cao. Cứ tiếp tục phát huy nhé!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Danh sách các từ vựng bạn trả lời sai trên 50% số lần ôn tập. Hãy lưu ý các từ này nhé!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(mistakeCards) { card ->
                val errorPercent = ((card.card.lapses.toFloat() / card.card.reps.toFloat()) * 100).toInt()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = "${card.vocabulary.word} (${card.vocabulary.partOfSpeech.lowercase()})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = card.vocabulary.definition,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Sai $errorPercent%",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
