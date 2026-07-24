package com.nhimz.vocabmaster.ui.components.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.SuccessGreen
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ScrambledQuizCard(
    scrambledWords: List<String>,
    selectedWords: List<String>,
    isAnswerRevealed: Boolean,
    isCorrect: Boolean?,
    onWordSelected: (String, Int) -> Unit,
    onWordUnselected: (String, Int) -> Unit,
    onWordReordered: (Int, Int) -> Unit = { _, _ -> },
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

    // Track selected area bounds for cross-area drag-from-bank
    var selectedAreaWindowBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SelectedWordsArea(
            selectedWords = selectedWords,
            scrambledWords = scrambledWords,
            isAnswerRevealed = isAnswerRevealed,
            borderColor = borderColor,
            onWordUnselected = onWordUnselected,
            onWordReordered = onWordReordered,
            onAreaPositioned = { selectedAreaWindowBounds = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        WordBankArea(
            scrambledWords = scrambledWords,
            selectedIndices = selectedIndices,
            isAnswerRevealed = isAnswerRevealed,
            onWordSelected = onWordSelected,
            selectedAreaWindowBounds = selectedAreaWindowBounds
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
    onWordUnselected: (String, Int) -> Unit,
    onWordReordered: (Int, Int) -> Unit,
    onAreaPositioned: (Rect) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // Drag state for reordering
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val chipWindowPositions = remember { mutableStateMapOf<Int, Offset>() }

    // Track our own bounds for drag-to-remove threshold
    var areaWindowBounds by remember { mutableStateOf<Rect?>(null) }

    // Threshold in px for drag-to-remove (60dp below card bottom)
    val removeThresholdPx = with(density) { 60.dp.toPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val origin = coords.positionInWindow()
                val sz = coords.size
                val rect = Rect(
                    left = origin.x,
                    top = origin.y,
                    right = origin.x + sz.width,
                    bottom = origin.y + sz.height
                )
                areaWindowBounds = rect
                onAreaPositioned(rect)
            }
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
                    selectedWords.forEachIndexed { index, word ->
                        currentSelectedList.add(word)
                        val scrambledIdx = ScrambledWordMapper.calculateScrambledIndex(
                            word = word,
                            selectedWords = currentSelectedList.toList().dropLast(1),
                            scrambledWords = scrambledWords
                        )
                        val isDragging = draggedIndex == index

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .zIndex(1f)
                                            .graphicsLayer {
                                                scaleX = 1.05f
                                                scaleY = 1.05f
                                                shadowElevation = 8f
                                            }
                                            .offset {
                                                IntOffset(
                                                    dragOffset.x.roundToInt(),
                                                    dragOffset.y.roundToInt()
                                                )
                                            }
                                    } else {
                                        Modifier.zIndex(0f)
                                    }
                                )
                                .onGloballyPositioned { coords ->
                                    if (!isDragging) {
                                        chipWindowPositions[index] = coords.positionInWindow()
                                    }
                                }
                                .pointerInput(index, isAnswerRevealed) {
                                    if (!isAnswerRevealed) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                val chipStartPos =
                                                    chipWindowPositions[index] ?: Offset.Zero
                                                val endPos = chipStartPos + dragOffset

                                                // Drag-to-remove: if dragged below the card area
                                                val bounds = areaWindowBounds
                                                if (bounds != null && endPos.y > bounds.bottom + removeThresholdPx) {
                                                    onWordUnselected(word, scrambledIdx)
                                                } else {
                                                    // Find closest chip for reorder
                                                    var closestIdx = index
                                                    var closestDist = Float.MAX_VALUE
                                                    chipWindowPositions.forEach { (i, pos) ->
                                                        if (i != index) {
                                                            val dx = pos.x - endPos.x
                                                            val dy = pos.y - endPos.y
                                                            val dist = sqrt(dx * dx + dy * dy)
                                                            if (dist < closestDist) {
                                                                closestDist = dist
                                                                closestIdx = i
                                                            }
                                                        }
                                                    }
                                                    // Only reorder if close enough (< 200px)
                                                    if (closestIdx != index && closestDist < 200f) {
                                                        onWordReordered(index, closestIdx)
                                                    }
                                                }

                                                draggedIndex = -1
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedIndex = -1
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                                }
                                .clickable(enabled = !isAnswerRevealed && !isDragging) {
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
    onWordSelected: (String, Int) -> Unit,
    selectedAreaWindowBounds: Rect? = null
) {
    val isDark = isSystemInDarkTheme()

    // Drag-from-bank state
    var draggedBankIndex by remember { mutableIntStateOf(-1) }
    var bankDragOffset by remember { mutableStateOf(Offset.Zero) }
    val bankChipWindowPositions = remember { mutableStateMapOf<Int, Offset>() }

    FlowRow(
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        scrambledWords.forEachIndexed { index, word ->
            val isSelected = selectedIndices.contains(index)
            val isDraggingFromBank = draggedBankIndex == index

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .background(
                        color = if (isSelected && !isDraggingFromBank) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        } else {
                            if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected && !isDraggingFromBank) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isDraggingFromBank) {
                            Modifier
                                .zIndex(1f)
                                .graphicsLayer {
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    shadowElevation = 8f
                                }
                                .offset {
                                    IntOffset(
                                        bankDragOffset.x.roundToInt(),
                                        bankDragOffset.y.roundToInt()
                                    )
                                }
                        } else {
                            Modifier
                        }
                    )
                    .onGloballyPositioned { coords ->
                        if (!isDraggingFromBank) {
                            bankChipWindowPositions[index] = coords.positionInWindow()
                        }
                    }
                    .pointerInput(index, isSelected, isAnswerRevealed) {
                        if (!isSelected && !isAnswerRevealed) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedBankIndex = index
                                    bankDragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    bankDragOffset += dragAmount
                                },
                                onDragEnd = {
                                    val startPos =
                                        bankChipWindowPositions[index] ?: Offset.Zero
                                    val endPos = startPos + bankDragOffset

                                    // If dropped within selected area bounds, add word
                                    val bounds = selectedAreaWindowBounds
                                    if (bounds != null && bounds.contains(endPos)) {
                                        onWordSelected(word, index)
                                    }

                                    draggedBankIndex = -1
                                    bankDragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggedBankIndex = -1
                                    bankDragOffset = Offset.Zero
                                }
                            )
                        }
                    }
                    .clickable(enabled = !isSelected && !isAnswerRevealed) {
                        onWordSelected(word, index)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected && !isDraggingFromBank) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
