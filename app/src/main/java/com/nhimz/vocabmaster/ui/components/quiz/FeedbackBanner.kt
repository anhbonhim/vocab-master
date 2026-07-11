package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.ErrorRedLight
import com.nhimz.vocabmaster.ui.theme.ErrorRedLightDark
import com.nhimz.vocabmaster.ui.theme.SuccessGreen
import com.nhimz.vocabmaster.ui.theme.SuccessGreenLight
import com.nhimz.vocabmaster.ui.theme.SuccessGreenLightDark
import com.nhimz.vocabmaster.ui.util.FeedbackHelper

@Composable
fun FeedbackBanner(
    isCorrect: Boolean?, // null if hidden, true if correct, false if incorrect
    correctAnswerText: String,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isCorrect != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
    ) {
        val context = LocalContext.current
        val isDark = isSystemInDarkTheme()

        LaunchedEffect(isCorrect) {
            if (isCorrect == true) {
                FeedbackHelper.playSoundCorrect()
                FeedbackHelper.vibrateCorrect(context)
            } else if (isCorrect == false) {
                FeedbackHelper.playSoundIncorrect()
                FeedbackHelper.vibrateIncorrect(context)
            }
        }

        val backgroundColor = if (isCorrect == true) {
            if (isDark) SuccessGreenLightDark else SuccessGreenLight
        } else {
            if (isDark) ErrorRedLightDark else ErrorRedLight
        }

        val textColor = if (isCorrect == true) SuccessGreen else ErrorRed
        val buttonColor = if (isCorrect == true) SuccessGreen else ErrorRed

        Surface(
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCorrect == true) "✓" else "✗",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCorrect == true) "Chính xác!" else "Chưa chính xác",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                if (isCorrect == false) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đáp án đúng: $correctAnswerText",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onContinueClick,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = if (isCorrect == true) "TIẾP TỤC" else "ĐÃ HIỂU",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
