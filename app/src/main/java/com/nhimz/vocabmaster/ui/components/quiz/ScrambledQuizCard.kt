package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.nhimz.vocabmaster.ui.theme.SuccessGreen

@Composable
fun ScrambledQuizCard(
    scrambledWords: List<String>,
    selectedWords: List<String>,
    isAnswerRevealed: Boolean,
    isCorrect: Boolean?,
    onWordSelected: (String, Int) -> Unit,
    onWordUnselected: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isAnswerRevealed && isCorrect == true -> SuccessGreen
        isAnswerRevealed && isCorrect == false -> ErrorRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }

    val selectedIndices = ScrambledWordMapper.calculateSelectedIndices(
        selectedWords = selectedWords,
        scrambledWords = scrambledWords
    ).toSet()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SelectedWordsArea(
            selectedWords = selectedWords,
            scrambledWords = scrambledWords,
            isAnswerRevealed = isAnswerRevealed,
            borderColor = borderColor,
            onWordUnselected = onWordUnselected
        )

        Spacer(modifier = Modifier.height(24.dp))

        WordBankArea(
            scrambledWords = scrambledWords,
            selectedIndices = selectedIndices,
            isAnswerRevealed = isAnswerRevealed,
            onWordSelected = onWordSelected
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedWordsArea(
    selectedWords: List<String>,
    scrambledWords: List<String>,
    isAnswerRevealed: Boolean,
    borderColor: Color,
    onWordUnselected: (String, Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (selectedWords.isEmpty()) {
                Text(
                    text = "Chạm vào các từ bên dưới để ghép câu",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentSelectedList = mutableListOf<String>()
                    selectedWords.forEach { word ->
                        currentSelectedList.add(word)
                        val scrambledIdx = ScrambledWordMapper.calculateScrambledIndex(
                            word = word,
                            selectedWords = currentSelectedList.toList().dropLast(1),
                            scrambledWords = scrambledWords
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isAnswerRevealed) {
                                    onWordUnselected(word, scrambledIdx)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordBankArea(
    scrambledWords: List<String>,
    selectedIndices: Set<Int>,
    isAnswerRevealed: Boolean,
    onWordSelected: (String, Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        scrambledWords.forEachIndexed { index, word ->
            val isSelected = selectedIndices.contains(index)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        } else {
                            if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isSelected && !isAnswerRevealed) {
                        onWordSelected(word, index)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
