package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.domain.model.quiz.QuizType

@Composable
fun MatchingQuestionCard(
    type: QuizType.Matching,
    hasAnswered: Boolean,
    onPairsMatched: () -> Unit
) {
    // State to track matched pairs and selections
    val allLefts = remember { type.pairs.map { it.left }.shuffled() }
    val allRights = remember { type.pairs.map { it.right }.shuffled() }
    
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    var selectedRight by remember { mutableStateOf<String?>(null) }
    
    val matchedLefts = remember { mutableStateListOf<String>() }
    val matchedRights = remember { mutableStateListOf<String>() }

    // Check match when both are selected
    LaunchedEffect(selectedLeft, selectedRight) {
        val leftPick = selectedLeft
        val rightPick = selectedRight
        if (leftPick != null && rightPick != null) {
            val pair = type.pairs.find { it.left == leftPick && it.right == rightPick }
            if (pair != null) {
                // Match correct
                matchedLefts.add(leftPick)
                matchedRights.add(rightPick)

                // Check if all matched
                if (matchedLefts.size == type.pairs.size) {
                    onPairsMatched()
                }
            }
            // Clear selection after a small delay
            kotlinx.coroutines.delay(300)
            selectedLeft = null
            selectedRight = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column
        Column(modifier = Modifier.weight(1f)) {
            allLefts.forEach { text ->
                val isMatched = matchedLefts.contains(text)
                val isSelected = selectedLeft == text
                MatchingItem(
                    text = text,
                    isSelected = isSelected,
                    isMatched = isMatched,
                    onClick = { if (!isMatched && !hasAnswered) selectedLeft = text }
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Right Column
        Column(modifier = Modifier.weight(1f)) {
            allRights.forEach { text ->
                val isMatched = matchedRights.contains(text)
                val isSelected = selectedRight == text
                MatchingItem(
                    text = text,
                    isSelected = isSelected,
                    isMatched = isMatched,
                    onClick = { if (!isMatched && !hasAnswered) selectedRight = text }
                )
            }
        }
    }
}

@Composable
private fun MatchingItem(
    text: String,
    isSelected: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isMatched -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isMatched -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    val textColor = when {
        isMatched -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(60.dp)
            .clickable(enabled = !isMatched, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMatched) 0.dp else 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
