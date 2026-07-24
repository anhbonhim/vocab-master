package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.displayTitle

@Composable
fun IntroductionCard(
    itemWithCard: QuestionWithCard?,
    prompt: String,
    audioUrl: String?,
    onPlayAudio: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val title = itemWithCard?.question?.displayTitle() ?: prompt
            Text(
                text = title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            audioUrl?.let { url ->
                Button(
                    onClick = { onPlayAudio(url) },
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🔊", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = itemWithCard?.question?.translation ?: "",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (prompt.isNotBlank() && title != prompt) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = prompt,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
