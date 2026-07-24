package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.ui.theme.SelectedBlue

/**
 * Renders a Fill-in-the-blank question card:
 *   - The prompt with "_____" placeholder visually highlighted (colored and underlined)
 *   - Word chip options below, reusing [DuolingoOptionCard]
 *
 * State is fully upstream — this composable only renders the given data
 * and calls back on option taps. Matches the Container/Content pattern from
 * S02-S03.
 */
@Composable
fun FillInBlankCard(
    type: QuizType.FillInBlank,
    hasAnswered: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val blankColor = SelectedBlue

    Column(modifier = modifier.fillMaxWidth()) {
        // — Prompt with "_____" highlighted —
        // Split on "_____" so we can style the blank differently
        val parts = type.prompt.split("_____", limit = 2)
        val annotatedPrompt = buildAnnotatedString {
            if (parts.isNotEmpty()) {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(parts[0])
                }
            }
            if (parts.size > 1) {
                withStyle(
                    SpanStyle(
                        color = blankColor,
                        fontWeight = FontWeight.ExtraBold,
                        background = blankColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    )
                ) {
                    append("_____")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(parts[1])
                }
            }
        }

        Text(
            text = annotatedPrompt,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // — Word chip options —
        type.options.forEachIndexed { index, option ->
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
