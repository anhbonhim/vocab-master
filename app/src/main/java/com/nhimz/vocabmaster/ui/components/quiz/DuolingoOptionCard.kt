package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.ErrorRedLight
import com.nhimz.vocabmaster.ui.theme.ErrorRedLightDark
import com.nhimz.vocabmaster.ui.theme.SelectedBlue
import com.nhimz.vocabmaster.ui.theme.SelectedBlueLight
import com.nhimz.vocabmaster.ui.theme.SelectedBlueLightDark
import com.nhimz.vocabmaster.ui.theme.SuccessGreen
import com.nhimz.vocabmaster.ui.theme.SuccessGreenLight
import com.nhimz.vocabmaster.ui.theme.SuccessGreenLightDark

@Composable
fun DuolingoOptionCard(
    optionText: String,
    isSelected: Boolean,
    isCorrect: Boolean?, // null if not checked/unanswered, true if correct, false if selected incorrect
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val containerColor = when {
        isCorrect == true -> if (isDark) SuccessGreenLightDark else SuccessGreenLight
        isCorrect == false -> if (isDark) ErrorRedLightDark else ErrorRedLight
        isSelected -> if (isDark) SelectedBlueLightDark else SelectedBlueLight
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isCorrect == true -> SuccessGreen
        isCorrect == false -> ErrorRed
        isSelected -> SelectedBlue
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    val borderWidth = if (isSelected || isCorrect != null) 2.5.dp else 1.5.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = optionText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
