package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.nhimz.vocabmaster.R
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.model.displayTitle
import com.nhimz.vocabmaster.domain.model.quiz.QuizQuestion
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoOptionCard
import com.nhimz.vocabmaster.ui.components.quiz.DuolingoProgressBar
import com.nhimz.vocabmaster.ui.components.quiz.FeedbackBanner
import com.nhimz.vocabmaster.ui.components.quiz.IntroductionCard
import com.nhimz.vocabmaster.ui.components.quiz.ListeningQuestionCard
import com.nhimz.vocabmaster.ui.components.quiz.MatchingQuestionCard
import com.nhimz.vocabmaster.ui.components.quiz.ScrambledQuizCard
import com.nhimz.vocabmaster.ui.components.quiz.TypingQuestionCard
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.theme.SuccessGreen
import kotlin.math.roundToInt

/**
 * State-only value object consumed by [QuizScreenContent]. The Container
 * (`QuizScreen`) builds this from [com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel]
 * and forwards it.
 *
 * Pulling this into a dedicated type means the Content composable is fully
 * previewable and testable without a real ViewModel — same pattern as
 * HomeScreenUiState / SettingsUiModel in Plan 03-01.
 *
 * The per-question input scratchpad (typed text, selected scrambled words,
 * FSRS flip flag) is included in this value object so the Content signature
 * stays consistent with the rest of the phase. Compose is smart enough to
 * skip recomposition for unrelated sub-trees, so putting a frequently
 * changing field here is fine.
 */
data class QuizScreenUiState(
    val currentQuestion: QuizQuestion? = null,
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val hasAnswered: Boolean = false,
    val selectedOptionIndex: Int? = null,
    val progress: Float = 0f,
    val isFsrsFlashcard: Boolean = false,
    val isFlipped: Boolean = false,
    val isFsrsRatingSelected: Boolean = false,
    val feedbackBannerCorrect: Boolean? = null,
    val correctAnswerText: String = "",
    val typedText: String = "",
    val selectedScrambledWords: List<String> = emptyList()
)

/**
 * Callbacks for every user-intent the Quiz screen exposes (Plan 03-02, ARCH-01).
 * The Content never calls into the ViewModel — it funnels the user action back
 * up to the Container, which then drives the ViewModel.
 */
data class QuizScreenActions(
    val onBack: () -> Unit,
    val onOptionSelected: (Int) -> Unit,
    val onScrambledWordSelected: (word: String, index: Int) -> Unit,
    val onScrambledWordUnselected: (word: String, index: Int) -> Unit,
    val onTypedTextChanged: (String) -> Unit,
    val onFlipFlashcard: () -> Unit,
    val onSubmit: () -> Unit,
    val onContinue: () -> Unit,
    val onFsrsRating: (Rating) -> Unit,
    val onPlayAudio: (String?) -> Unit
)

/**
 * Local form-state for the per-question input fields. The Content owns this
 * because the values are pure UI concerns (what the user has typed or tapped)
 * — once the user submits, the ViewModel only cares about the right answer
 * key, not the scratchpad. This keeps the ViewModel from needing to round-trip
 * typing/selection state through the StateFlow.
 */
internal data class QuestionFormState(
    val selectedOptionIndex: Int? = null,
    val selectedScrambledWords: List<String> = emptyList(),
    val typedText: String = ""
)

/**
 * Stateless UI for the Quiz screen (Plan 03-02, Task 1).
 *
 * Mirrors the Container/Content split established in Plan 03-01 for HomeScreen
 * and SettingsScreen. The Content:
 *  - Takes a [QuizScreenUiState] value object + [QuizScreenActions] callbacks
 *  - Renders 3D flip animations for correct/incorrect feedback using
 *    [graphicsLayer] + [Animatable] (per 03-UI-SPEC.md UX-03)
 *  - Does **not** call into any ViewModel or trigger navigation
 *
 * The Container (`QuizScreen`) is responsible for collecting the StateFlow,
 * holding transient form state, driving side effects (LaunchedEffect for
 * navigation on Completed), and rendering the leaf states.
 */
@Composable
fun QuizScreenContent(
    state: QuizScreenUiState,
    actions: QuizScreenActions,
    modifier: Modifier = Modifier
) {
    val question = state.currentQuestion ?: return

    // The selected option index lives in the Container's scratchpad (so the
    // ViewModel can read it on submit). The Content just renders the value
    // and delegates taps through [QuizScreenActions.onOptionSelected].
    val resolvedSelectedOption = state.selectedOptionIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp) // Provide space for Feedback Banner
                .verticalScroll(rememberScrollState())
        ) {
            // Header Status
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✕",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { actions.onBack() }
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                DuolingoProgressBar(
                    progress = state.progress,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question Type Label
            Text(
                text = promptLabelFor(question.type),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GradientStart.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Flip wrapper applies a rotationY animation to the question card
            // so correct/incorrect feedback pops with depth. The shake overlay
            // for wrong answers is layered on top via [QuestionContentWithShake].
            QuestionContentWithShake(
                question = question,
                hasAnswered = state.hasAnswered,
                isFlipped = state.isFlipped,
                selectedOptionIndex = resolvedSelectedOption,
                onOptionSelected = actions.onOptionSelected,
                onPlayAudio = actions.onPlayAudio,
                typedText = state.typedText,
                onTypedTextChanged = actions.onTypedTextChanged,
                selectedScrambledWords = state.selectedScrambledWords,
                onScrambledWordSelected = actions.onScrambledWordSelected,
                onScrambledWordUnselected = actions.onScrambledWordUnselected,
                onFlipFlashcard = actions.onFlipFlashcard,
                onMatchingComplete = { if (!state.hasAnswered) actions.onSubmit() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            if (state.isFsrsFlashcard && state.isFlipped && !state.hasAnswered) {
                FSRSRatingButtons(onRatingSelected = actions.onFsrsRating)
            } else if (!state.hasAnswered && !state.isFsrsFlashcard) {
                val isSubmitEnabled = isSubmitEnabledFor(
                    question = question,
                    selectedOptionIndex = resolvedSelectedOption,
                    typedText = state.typedText,
                    selectedScrambledWords = state.selectedScrambledWords
                )
                if (!isMatchingQuestion(question)) {
                    Button(
                        onClick = actions.onSubmit,
                        enabled = isSubmitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubmitEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (isIntroductionQuestion(question)) "ĐÃ HIỂU" else "KIỂM TRA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSubmitEnabled) androidx.compose.ui.graphics.Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }

        val showLottie = state.hasAnswered && state.feedbackBannerCorrect == true
        if (showLottie) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    iterations = 1,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Feedback Banner (If Answered) anchored to the bottom of the Box
        if (state.hasAnswered && !state.isFsrsFlashcard) {
            FeedbackBanner(
                isCorrect = state.feedbackBannerCorrect,
                correctAnswerText = state.correctAnswerText,
                onContinueClick = actions.onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }

        if (state.isFsrsFlashcard && state.isFsrsRatingSelected) {
            FeedbackBanner(
                isCorrect = true,
                correctAnswerText = "",
                onContinueClick = actions.onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

// region 3D Flip Animations

/**
 * Wraps the question content in a 3D rotationY animation. When [hasAnswered]
 * changes to true, the card flips 180° on the Y axis to reveal the answer
 * feedback. The flip direction is chosen by the previous render state so it
 * always feels like "turning the card over" rather than a half-rotation reset.
 */
@Composable
private fun QuestionContentWithShake(
    question: QuizQuestion,
    hasAnswered: Boolean,
    isFlipped: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    onPlayAudio: (String?) -> Unit,
    typedText: String,
    onTypedTextChanged: (String) -> Unit,
    selectedScrambledWords: List<String>,
    onScrambledWordSelected: (word: String, index: Int) -> Unit,
    onScrambledWordUnselected: (word: String, index: Int) -> Unit,
    onFlipFlashcard: () -> Unit,
    onMatchingComplete: () -> Unit
) {
    // Drive the 3D rotation with an Animatable so it runs on the render thread
    // and the content beneath the rotation can update without restarting the
    // animation. cameraDistance is set high so perspective doesn't clip the
    // card at the apex of the rotation.
    val rotationY = remember { Animatable(0f) }
    LaunchedEffect(hasAnswered, isFlipped) {
        val target = when {
            hasAnswered -> 180f
            isFlipped -> 0f
            else -> 0f
        }
        rotationY.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    // Horizontal shake for incorrect answers (UX-03 spec). Triggers on the
    // same event as the 3D flip but with a faster cadence.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(hasAnswered) {
        if (hasAnswered) {
            // Three back-and-forth oscillations + return to center.
            repeat(3) {
                shakeOffset.animateTo(15f, tween(40))
                shakeOffset.animateTo(-15f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    // The back face is shown once the rotation crosses 90° so the back reads
    // the way the user expects (otherwise the back of the card appears upside
    // down for the second half of the rotation).
    val showBackFace by remember {
        derivedStateOf { rotationY.value > 90f }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationY = rotationY.value
                cameraDistance = 12f * density
            }
    ) {
        if (!showBackFace) {
            QuestionFrontFace(
                question = question,
                hasAnswered = hasAnswered,
                isFlipped = isFlipped,
                selectedOptionIndex = selectedOptionIndex,
                onOptionSelected = onOptionSelected,
                onPlayAudio = onPlayAudio,
                typedText = typedText,
                onTypedTextChanged = onTypedTextChanged,
                selectedScrambledWords = selectedScrambledWords,
                onScrambledWordSelected = onScrambledWordSelected,
                onScrambledWordUnselected = onScrambledWordUnselected,
                onFlipFlashcard = onFlipFlashcard,
                onMatchingComplete = onMatchingComplete
            )
        } else {
            // Back face: reveal the answer summary card with a tinted
            // background that matches the correct/incorrect state.
            AnswerBackFace(
                question = question,
                isCorrect = hasAnswered && (question.type is QuizType.Introduction ||
                    question.type is QuizType.Matching ||
                    question.type is QuizType.FSRSTailFlashcard ||
                    correctAnswerMatchesInput(question, selectedOptionIndex, typedText, selectedScrambledWords))
            )
        }
    }
}

/**
 * Pure helper that decides whether the user's input matches the correct
 * answer for [question]. Used by the back face to pick the correct/incorrect
 * color tint. Mirrors [computeAnswerCorrectness] but kept as a separate
 * function so the animation code path doesn't need to import the whole UI
 * helper module.
 */
private fun correctAnswerMatchesInput(
    question: QuizQuestion,
    selectedOptionIndex: Int?,
    typedText: String,
    selectedScrambledWords: List<String>
): Boolean = when (val type = question.type) {
    is QuizType.MultipleChoice -> selectedOptionIndex == type.correctIndex
    is QuizType.Listening -> selectedOptionIndex == type.correctIndex
    is QuizType.Typing -> {
        val cleanedUser = typedText.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
        val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
        cleanedUser == cleanedCorrect
    }
    is QuizType.ScrambledSentence -> selectedScrambledWords.joinToString(" ") == type.correctSentence
    else -> false
}

/**
 * The "front" of the 3D card — the question content that the user sees before
 * submitting an answer. Identical to the previous monolithic QuizScreen's
 * content but lifted out so the flip wrapper can rotate the whole surface.
 */
@Composable
private fun QuestionFrontFace(
    question: QuizQuestion,
    hasAnswered: Boolean,
    isFlipped: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    onPlayAudio: (String?) -> Unit,
    typedText: String,
    onTypedTextChanged: (String) -> Unit,
    selectedScrambledWords: List<String>,
    onScrambledWordSelected: (word: String, index: Int) -> Unit,
    onScrambledWordUnselected: (word: String, index: Int) -> Unit,
    onFlipFlashcard: () -> Unit,
    onMatchingComplete: () -> Unit
) {
    when (val type = question.type) {
        is QuizType.Introduction -> {
            IntroductionCard(
                itemWithCard = type.itemWithCard,
                prompt = type.prompt,
                audioUrl = type.audioUrl,
                onPlayAudio = onPlayAudio
            )
        }
        is QuizType.MultipleChoice -> {
            MultipleChoiceCard(
                type = type,
                hasAnswered = hasAnswered,
                selectedOptionIndex = selectedOptionIndex,
                onOptionSelected = onOptionSelected
            )
        }
        is QuizType.Listening -> {
            ListeningQuestionCard(
                type = type,
                hasAnswered = hasAnswered,
                selectedOptionIndex = selectedOptionIndex,
                onOptionSelected = onOptionSelected,
                onPlayAudio = onPlayAudio
            )
        }
        is QuizType.Matching -> {
            key(question.itemWithCard) {
                MatchingQuestionCard(
                    type = type,
                    hasAnswered = hasAnswered,
                    onPairsMatched = onMatchingComplete
                )
            }
        }
        is QuizType.Typing -> {
            TypingQuestionCard(
                type = type,
                hasAnswered = hasAnswered,
                typedText = typedText,
                onTextChanged = onTypedTextChanged,
                onPlayAudio = onPlayAudio
            )
        }
        is QuizType.ScrambledSentence -> {
            val isCorrectState = if (hasAnswered) {
                val userSentence = selectedScrambledWords.joinToString(" ")
                userSentence == type.correctSentence
            } else null

            ScrambledQuizCard(
                scrambledWords = type.scrambledWords,
                selectedWords = selectedScrambledWords,
                isAnswerRevealed = hasAnswered,
                isCorrect = isCorrectState,
                onWordSelected = onScrambledWordSelected,
                onWordUnselected = onScrambledWordUnselected
            )
        }
        is QuizType.FSRSTailFlashcard -> {
            FSRSFlashcardCard(
                type = type,
                isFlipped = isFlipped,
                onFlip = onFlipFlashcard
            )
        }
    }
}

/**
 * The "back" of the 3D card — the post-answer reveal. Tinted green/red based
 * on the [isCorrect] state, mirroring the Duolingo feedback color palette
 * from 03-UI-SPEC.md.
 */
@Composable
private fun AnswerBackFace(
    question: QuizQuestion,
    isCorrect: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isCorrect) SuccessGreen else ErrorRed
    val containerColor = tintColor.copy(alpha = if (isDark) 0.18f else 0.10f)
    val borderColor = tintColor.copy(alpha = if (isDark) 0.45f else 0.35f)
    val headline = if (isCorrect) "Chính xác!" else "Chưa chính xác"
    val detail = when (val type = question.type) {
        is QuizType.MultipleChoice -> type.options.getOrNull(type.correctIndex) ?: ""
        is QuizType.Listening -> type.options?.getOrNull(type.correctIndex ?: 0) ?: ""
        is QuizType.Typing -> type.correctSentence
        is QuizType.ScrambledSentence -> type.correctSentence
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCorrect) "✓" else "✗",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = headline,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
            if (!isCorrect && detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Đáp án đúng: $detail",
                    fontSize = 15.sp,
                    color = tintColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Public multiple-choice card (kept here from the prior QuizScreen so that
 * the call sites in [QuestionFrontFace] still compile without changes). Renders
 * the question prompt + option list with the existing Duolingo styling.
 */
@Composable
fun MultipleChoiceCard(
    type: QuizType.MultipleChoice,
    hasAnswered: Boolean,
    selectedOptionIndex: Int?,
    onOptionSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = type.prompt,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        )

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

/**
 * FSRS flashcard with explicit tap-to-flip affordance. The 3D rotation lives
 * on the wrapper; this composable just renders the front and back faces.
 */
@Composable
fun FSRSFlashcardCard(
    type: QuizType.FSRSTailFlashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    if (!isFlipped) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { onFlip() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.itemWithCard.question.displayTitle(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "(Chạm để lật bài)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = type.itemWithCard.question.translation ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FSRSRatingButtons(onRatingSelected: (Rating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { onRatingSelected(Rating.Again) },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF44336)),
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) { Text("Lại") }
        Button(
            onClick = { onRatingSelected(Rating.Hard) },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800)),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        ) { Text("Khó") }
        Button(
            onClick = { onRatingSelected(Rating.Good) },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
        ) { Text("Tốt") }
        Button(
            onClick = { onRatingSelected(Rating.Easy) },
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2196F3)),
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        ) { Text("Dễ") }
    }
}

// endregion

// region Loading / Error / Empty states (lifted out of the Container so the
// Content can render them directly when the state machine reaches a leaf)

@Composable
fun QuizLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .weight(1f)
                    .background(
                        androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f),
                    RoundedCornerShape(16.dp)
                )
        )
        Spacer(modifier = Modifier.height(24.dp))
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 8.dp)
                    .background(
                        androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
            )
        }
    }
}

@Composable
fun QuizEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🌱", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không có câu hỏi nào",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Chủ đề hoặc lộ trình học này hiện không có câu hỏi nào khả dụng. Vui lòng quay lại sau!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Quay lại trang chủ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuizErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Thử lại",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(text = "Quay lại trang chủ", fontSize = 14.sp)
        }
    }
}

// endregion

// region Pure helper functions (testable in isolation)

/**
 * Returns the prompt label for the given question type, used as the screen
 * sub-heading above the question card. Mirrors the labels previously
 * inlined in the monolithic QuizScreen.
 */
internal fun promptLabelFor(type: QuizType): String = when (type) {
    is QuizType.Introduction -> "Giới thiệu từ mới"
    is QuizType.FSRSTailFlashcard -> "Ôn tập Flashcard"
    is QuizType.ScrambledSentence -> "Sắp xếp lại câu"
    is QuizType.Listening -> "Nghe và chọn"
    is QuizType.Matching -> "Ghép đôi"
    is QuizType.Typing -> "Nhập câu trả lời"
    else -> "Điền từ vào chỗ trống"
}

/** Whether the submit button should be enabled for the given input state. */
internal fun isSubmitEnabledFor(
    question: QuizQuestion,
    selectedOptionIndex: Int?,
    typedText: String,
    selectedScrambledWords: List<String>
): Boolean = when (val type = question.type) {
    is QuizType.MultipleChoice -> selectedOptionIndex != null
    is QuizType.Listening -> selectedOptionIndex != null
    is QuizType.Typing -> typedText.isNotBlank()
    is QuizType.ScrambledSentence -> selectedScrambledWords.isNotEmpty()
    is QuizType.Introduction -> true
    is QuizType.Matching -> false // Matching auto-submits when done
    is QuizType.FSRSTailFlashcard -> false
}

internal fun isMatchingQuestion(question: QuizQuestion): Boolean =
    question.type is QuizType.Matching

internal fun isIntroductionQuestion(question: QuizQuestion): Boolean =
    question.type is QuizType.Introduction

/**
 * Compute the correct/incorrect outcome for a given question + user input.
 * Used to drive the 3D flip color and the feedback banner copy. The actual
 * evaluation that hits the FSRS scheduler is still done in the ViewModel
 * (this is a UI-side preview so the back-face can show a tinted color even
 * before the ViewModel emits the next Active state).
 */
internal fun computeAnswerCorrectness(
    question: QuizQuestion,
    selectedOptionIndex: Int?,
    typedText: String,
    selectedScrambledWords: List<String>
): Boolean = when (val type = question.type) {
    is QuizType.MultipleChoice -> selectedOptionIndex == type.correctIndex
    is QuizType.Listening -> selectedOptionIndex == type.correctIndex
    is QuizType.Typing -> {
        val cleanedUser = typedText.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
        val cleanedCorrect = type.correctSentence.replace(Regex("[^A-Za-z0-9 ]"), "").lowercase()
        cleanedUser == cleanedCorrect
    }
    is QuizType.ScrambledSentence -> selectedScrambledWords.joinToString(" ") == type.correctSentence
    is QuizType.Introduction -> true
    is QuizType.Matching -> true
    is QuizType.FSRSTailFlashcard -> true
}

/** The "correct answer" string for the feedback banner / back-face. */
internal fun correctAnswerTextFor(question: QuizQuestion): String = when (val type = question.type) {
    is QuizType.MultipleChoice -> type.options.getOrNull(type.correctIndex) ?: ""
    is QuizType.Listening -> type.options?.getOrNull(type.correctIndex ?: 0) ?: ""
    is QuizType.Typing -> type.correctSentence
    is QuizType.ScrambledSentence -> type.correctSentence
    else -> ""
}

// endregion
