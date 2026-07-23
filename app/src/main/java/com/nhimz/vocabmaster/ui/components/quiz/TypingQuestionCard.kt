package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.quiz.QuizType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingQuestionCard(
    type: QuizType.Typing,
    hasAnswered: Boolean,
    typedText: String,
    onTextChanged: (String) -> Unit,
    onPlayAudio: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!type.audioUrl.isNullOrEmpty()) {
            // Typing from Audio
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Button(
                    onClick = { onPlayAudio(type.audioUrl) },
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("🔊", fontSize = 28.sp)
                }
                
                type.audioUrlSlow?.let { slowUrl ->
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onPlayAudio(slowUrl) },
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("🐢", fontSize = 20.sp)
                    }
                }
            }
        } else {
            // Typing from Translation
            Text(
                text = type.prompt,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Field
        OutlinedTextField(
            value = typedText,
            onValueChange = onTextChanged,
            enabled = !hasAnswered,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Nhập câu trả lời bằng tiếng Anh...") },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            singleLine = false,
            maxLines = 3
        )
    }
}
