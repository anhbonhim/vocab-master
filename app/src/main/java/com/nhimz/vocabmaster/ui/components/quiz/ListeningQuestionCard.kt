package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.domain.model.quiz.QuizType

@Composable
fun ListeningQuestionCard(
    type: QuizType.Listening,
    hasAnswered: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    cdnAudioPlayer: CDNAudioPlayer
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Audio Controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            // Normal Speed Button
            Button(
                onClick = { 
                    type.audioUrl?.let { url ->
                        cdnAudioPlayer.playAudio(url)
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("🔊", fontSize = 32.sp)
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // Slow Speed Button
            Button(
                onClick = {
                    type.audioUrlSlow?.let { url ->
                        cdnAudioPlayer.playAudio(url)
                    } ?: type.audioUrl?.let { url ->
                        // Fallback to normal if slow is missing
                        cdnAudioPlayer.playAudio(url)
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("🐢", fontSize = 24.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Options
        type.options?.forEachIndexed { index, option ->
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
