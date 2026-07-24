package com.nhimz.vocabmaster.ui.screens.statistics_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart

/**
 * Practice Hub tab — 4th tab in StatisticsScreen.
 *
 * Provides 3 quick-access shortcut cards for common practice workflows:
 * Review Gym (general review), Mistake Review (navigate to MistakeBankTab),
 * and Flashcard Quiz (general flashcard session).
 */
@Composable
fun PracticeHubTab(
    onStartReviewGym: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onStartFlashcard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PracticeCard(
                emoji = "🏋️",
                title = "Phòng gym ôn tập",
                description = "Ôn tập tất cả từ vựng đến hạn dựa trên lịch lặp lại ngắt quãng (SRS).",
                accentColor = GradientStart,
                onClick = onStartReviewGym
            )
        }

        item {
            PracticeCard(
                emoji = "📝",
                title = "Sổ sai lầm",
                description = "Xem danh sách từ vựng bạn thường trả lời sai và luyện tập lại.",
                accentColor = MaterialTheme.colorScheme.error,
                onClick = onNavigateToMistakes
            )
        }

        item {
            PracticeCard(
                emoji = "📇",
                title = "Thẻ ghi nhớ",
                description = "Kiểm tra từ vựng bằng thẻ flashcard hai mặt (Anh — Việt).",
                accentColor = GradientEnd,
                onClick = onStartFlashcard
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PracticeCard(
    emoji: String,
    title: String,
    description: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar + emoji
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }

            // Right arrow indicator
            Text(
                text = "→",
                fontSize = 20.sp,
                color = accentColor.copy(alpha = 0.5f)
            )
        }
    }
}
