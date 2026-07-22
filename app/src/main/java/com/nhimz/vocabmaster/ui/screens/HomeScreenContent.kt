package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.ui.components.Duo3DCard
import com.nhimz.vocabmaster.ui.theme.GradientGoldEnd
import com.nhimz.vocabmaster.ui.theme.GradientGoldStart
import com.nhimz.vocabmaster.ui.viewmodel.NodeStatus
import com.nhimz.vocabmaster.ui.viewmodel.SectionStatus
import com.nhimz.vocabmaster.ui.viewmodel.SyntheticReviewNode
import com.nhimz.vocabmaster.ui.viewmodel.UnitStatus
import kotlin.math.sin

// Duolingo-style color palette (per design-md/misc/duolingo/DESIGN.md)
// These are local duplicates of the colors used by the existing HomeScreen to
// avoid coupling this file to private file-level constants.
private val DuolingoGreen = Color(0xFF58CC02)
private val DuolingoGreenLedge = Color(0xFF58A700)
private val DuolingoGray = Color(0xFFE5E5E5)
private val DuolingoGrayLock = Color(0xFFAFAFAF)
private val DuolingoGold = Color(0xFFFFC800)
private val DuolingoGoldLedge = Color(0xFFE29F03)
private val DuolingoRing = Color(0xFF89E219)
private val DuolingoPathLine = Color(0xFFE5E5E5)
private val ErrorRed = Color(0xFFEF4444)

/**
 * A node entry on the rendered path: either a real [NodeStatus] authored in the JSON
 * (LESSON / REVIEW / UNIT_CHECKPOINT), or a synthetic [SyntheticReviewNode] auto-inserted
 * by the ViewModel for spaced-repetition practice.
 *
 * Made `internal` so that [HomeScreen] (Container) can construct instances for the
 * bottom-sheet preview callback.
 */
internal sealed class PathEntry {
    data class Real(val status: NodeStatus, val unitId: String, val sectionId: String) : PathEntry()
    data class DynamicReview(
        val synthetic: SyntheticReviewNode,
        val sectionId: String,
        val isCompleted: Boolean,
        val isLocked: Boolean,
        val isCurrent: Boolean
    ) : PathEntry()
}

/**
 * Tag identifying the kind of a single item in the flattened path. Kept as a
 * public string constant so the Container can produce the same list without
 * needing to share private internals with the Content.
 */
internal object PathItemKind {
    const val SECTION_HEADER = "section_header"
    const val UNIT_HEADER = "unit_header"
    const val SECTION_BOSS = "section_boss"
    const val NODE = "node"
}

/**
 * Discriminated-union item for the path. The Container produces a list of
 * these for the Content to render.
 */
internal sealed class PathItem {
    data class SectionHeader(val section: SectionStatus) : PathItem()
    data class UnitHeader(val unit: UnitStatus) : PathItem()
    data class SectionBoss(val section: SectionStatus) : PathItem()
    data class Node(
        val status: NodeStatus,
        val unitStatus: UnitStatus,
        val globalIndex: Int
    ) : PathItem()
    data class DynamicNode(
        val synthetic: SyntheticReviewNode,
        val unitStatus: UnitStatus,
        val globalIndex: Int
    ) : PathItem()
}

/**
 * State-only value object consumed by [HomeScreenContent]. The Container
 * (`HomeScreen`) builds this from the ViewModel's flows and forwards it.
 *
 * Pulling this into a dedicated type means the Content composable is fully
 * previewable and testable without a real ViewModel.
 */
data class HomeScreenUiState(
    val xpTotal: Int = 0,
    val todayStudySeconds: Int = 0,
    val dailyGoalXp: Int = 10,
    val currentStreak: Int = 0,
    val availableFreezes: Int = 0,
    val dueCount: Int = 0,
    val mistakeCount: Int = 0,
    val showBacklogWarning: Boolean = false,
    val animatedProgress: Float = 0f,
    val minutesStudiedToday: Int = 0,
    val isScrolledAwayFromCurrent: Boolean = false,
    val path: List<PathItem> = emptyList(),
    val currentNodeId: String? = null
)

/**
 * Stateless UI for the Home screen (Plan 03-01, Task 2).
 *
 * The Content receives the entire UI state from the Container, plus callback
 * lambdas for every user-intent the screen exposes. It does **not** call into
 * any ViewModel, navigate, or show dialogs/bottom-sheets directly — those are
 * hoisted to the Container.
 *
 * Edge cases covered per 03-UI-SPEC.md:
 *  - 0 sections: [HomeEmptyState] renders with the Copywriting Contract copy
 *  - 1 section: LazyColumn naturally renders the single section's items
 *  - 20+ sections: LazyColumn lazily renders; FAB scrolls to current node
 */
@Composable
fun HomeScreenContent(
    state: HomeScreenUiState,
    onStartCustomQuiz: () -> Unit,
    onStartFlashcard: () -> Unit,
    onScrollToCurrent: () -> Unit,
    onSectionGuidebook: (String) -> Unit,
    onSectionJumpTest: (String) -> Unit,
    onSectionBossClick: (String, Boolean) -> Unit,
    onNodeClick: (PathItem) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== Sticky Header Area =====
            HomeHeader(state = state, onStartCustomQuiz = onStartCustomQuiz)

            // ===== Review Gym / Mistake Bank Shortcut Row =====
            HomeShortcutRow(
                dueCount = state.dueCount,
                mistakeCount = state.mistakeCount,
                onStartCustomQuiz = onStartCustomQuiz,
                onStartFlashcard = onStartFlashcard
            )

            // ===== The Duolingo-style Learning Path =====
            if (state.path.isEmpty()) {
                HomeEmptyState()
            } else {
                HomePathList(
                    state = state,
                    listState = listState,
                    onSectionGuidebook = onSectionGuidebook,
                    onSectionJumpTest = onSectionJumpTest,
                    onSectionBossClick = onSectionBossClick,
                    onNodeClick = onNodeClick
                )
            }
        }

        // ===== Floating "jump to current node" button (Duolingo pattern) =====
        AnimatedVisibility(
            visible = state.isScrolledAwayFromCurrent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = onScrollToCurrent,
                containerColor = DuolingoGreen,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Về node hiện tại")
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeScreenUiState,
    onStartCustomQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(GradientGoldStart, GradientGoldEnd)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "\uD83D\uDD25", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.currentStreak} Ngày",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "\u2744\uFE0F", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.availableFreezes}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "XP",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lv.${1 + state.xpTotal / 100} \u2022 ${state.xpTotal} XP",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mục tiêu ngày: ${state.minutesStudiedToday} / ${state.dailyGoalXp} Phút",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Soft-nudge backlog banner (non-blocking)
        AnimatedVisibility(visible = state.showBacklogWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Overdue",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Có ${state.dueCount} từ đến hạn ôn tập.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "\uD83D\uDD25 Gặp REVIEW node trên path để ôn lại (không bắt buộc).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                                    .copy(alpha = 0.8f)
                            )
                        }
                    }
                    Button(
                        onClick = onStartCustomQuiz,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "REVIEW GYM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcutRow(
    dueCount: Int,
    mistakeCount: Int,
    onStartCustomQuiz: () -> Unit,
    onStartFlashcard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartCustomQuiz,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (dueCount > 0) Color(0xFFF97316) else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (dueCount > 0) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("\uD83D\uDCAA Review Gym ($dueCount)", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onStartFlashcard,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Text("\uD83D\uDCD8 Sổ sai lầm ($mistakeCount)")
        }
    }
}

@Composable
private fun HomePathList(
    state: HomeScreenUiState,
    listState: LazyListState,
    onSectionGuidebook: (String) -> Unit,
    onSectionJumpTest: (String) -> Unit,
    onSectionBossClick: (String, Boolean) -> Unit,
    onNodeClick: (PathItem) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 96.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            items = state.path,
            key = { item ->
                when (item) {
                    is PathItem.SectionHeader -> "section_${item.section.section.id}"
                    is PathItem.UnitHeader -> "unit_${item.unit.unit.id}"
                    is PathItem.SectionBoss -> "boss_${item.section.section.id}"
                    is PathItem.Node -> "node_${item.status.node.id}_${item.globalIndex}"
                    is PathItem.DynamicNode -> "dyn_${item.synthetic.nodeId}_${item.globalIndex}"
                }
            }
        ) { item ->
            when (item) {
                is PathItem.SectionHeader -> StageHeaderItem(item.section)
                is PathItem.UnitHeader -> UnitHeaderItem(
                    unit = item.unit,
                    onGuidebook = { onSectionGuidebook(item.unit.unit.id) },
                    onJumpTest = { onSectionJumpTest(item.unit.unit.id) }
                )
                is PathItem.SectionBoss -> LevelTestNodeItem(
                    isLocked = !item.section.isCompleted || item.section.units.isEmpty(),
                    title = "Bài thi vượt chặng ${item.section.section.cefrSublevel}",
                    onClick = {
                        val isLocked = !item.section.isCompleted || item.section.units.isEmpty()
                        onSectionBossClick(item.section.section.id, isLocked)
                    }
                )
                is PathItem.Node -> PathNodeItem(
                    entry = PathEntry.Real(
                        status = item.status,
                        unitId = item.unitStatus.unit.id,
                        sectionId = item.unitStatus.unit.sectionId
                    ),
                    globalIndex = item.globalIndex,
                    isCurrent = item.status.node.id == state.currentNodeId,
                    onClick = { onNodeClick(item) }
                )
                is PathItem.DynamicNode -> PathNodeItem(
                    entry = PathEntry.DynamicReview(
                        synthetic = item.synthetic,
                        sectionId = item.unitStatus.unit.sectionId,
                        isCompleted = item.synthetic.dueCount == 0,
                        isLocked = false,
                        isCurrent = item.synthetic.nodeId == state.currentNodeId
                    ),
                    globalIndex = item.globalIndex,
                    isCurrent = item.synthetic.nodeId == state.currentNodeId,
                    onClick = { onNodeClick(item) }
                )
            }
        }
    }
}

/**
 * Empty state (Plan 03-01 must_haves: "0 sections").
 *
 * Renders the Copywriting Contract copy from 03-UI-SPEC.md when the curriculum
 * has no sections. Uses a [Duo3DCard] to satisfy the 3D design system.
 */
@Composable
fun HomeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\uD83D\uDCDA",
            fontSize = 48.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Chưa có bài học nào",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hãy chọn một khóa học để bắt đầu hành trình học từ vựng của bạn.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Duo3DCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Bạn có thể import bài học từ phần Cài đặt > Sao lưu & Khôi phục.",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UnitHeaderItem(
    unit: UnitStatus,
    onGuidebook: () -> Unit,
    onJumpTest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = context.resources.getIdentifier(unit.unit.icon, "drawable", context.packageName)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (resId != 0) {
                    Icon(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    Text(text = "\uD83D\uDCD8", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.unit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = unit.unit.storySummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onGuidebook,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("\uD83D\uDCD6 Sổ tay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onJumpTest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("\uD83D\uDEAA Jump Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StageHeaderItem(sectionStatus: SectionStatus) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = context.resources.getIdentifier(
        sectionStatus.section.icon, "drawable", context.packageName
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (resId != 0) {
                    Icon(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = "${sectionStatus.section.name.uppercase()} (${sectionStatus.section.cefrSublevel})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun PathNodeItem(
    entry: PathEntry,
    globalIndex: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val amplitude = 56f
    val xOffset = sin(globalIndex * 1.5f) * amplitude
    val primaryColor = MaterialTheme.colorScheme.primary

    // Visual state per Duolingo spec
    val isLocked: Boolean
    val isCompleted: Boolean
    val iconText: String
    val containerColor: Color
    val ledgeColor: Color
    when (entry) {
        is PathEntry.Real -> {
            val nodeType = entry.status.node.type
            isLocked = entry.status.isLocked
            isCompleted = entry.status.isCompleted
            iconText = when (nodeType) {
                NodeType.LESSON -> "\u2B50"
                NodeType.REVIEW -> "\uD83C\uDFCB\uFE0F"
                NodeType.UNIT_CHECKPOINT -> "\uD83C\uDFC6"
                else -> "\u2B50"
            }
            val goldForCheckpoint = nodeType == NodeType.UNIT_CHECKPOINT
            containerColor = when {
                isLocked -> DuolingoGray
                isCompleted -> if (goldForCheckpoint) DuolingoGold else DuolingoGreen
                isCurrent -> if (goldForCheckpoint) DuolingoGold else DuolingoGreen
                else -> primaryColor
            }
            ledgeColor = when {
                isLocked -> DuolingoGrayLock
                goldForCheckpoint -> DuolingoGoldLedge
                else -> DuolingoGreenLedge
            }
        }
        is PathEntry.DynamicReview -> {
            isLocked = entry.isLocked
            isCompleted = entry.isCompleted
            iconText = "\uD83C\uDFCB\uFE0F"
            containerColor = when {
                isLocked -> DuolingoGray
                isCompleted -> DuolingoGreen
                else -> DuolingoGreen
            }
            ledgeColor = if (isLocked) DuolingoGrayLock else DuolingoGreenLedge
        }
    }

    val lineColor = if (isLocked) DuolingoGray.copy(alpha = 0.7f) else DuolingoPathLine

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = size.width / 2 + xOffset.dp.toPx()
            val startY = size.height / 2 + 36.dp.toPx()
            val endX = size.width / 2 + sin((globalIndex + 1) * 1.5f) * amplitude * 1.dp.toPx()
            val endY = size.height + size.height / 2 - 36.dp.toPx()
            drawLine(
                color = lineColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }

        Column(
            modifier = Modifier.offset(x = xOffset.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Surface(
                        modifier = Modifier
                            .size(if (isCurrent) 84.dp else 72.dp)
                            .scale(pulseScale),
                        shape = CircleShape,
                        color = DuolingoRing.copy(alpha = pulseAlpha)
                    ) {}
                }

                val nodeSize = if (isCurrent) 84.dp else 72.dp
                Surface(
                    modifier = Modifier
                        .size(nodeSize)
                        .clickable(enabled = !isLocked, onClick = onClick),
                    shape = CircleShape,
                    color = containerColor,
                    border = BorderStroke(3.dp, ledgeColor),
                    shadowElevation = if (isLocked) 0.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = DuolingoGrayLock,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Text(
                                text = if (isCompleted) "\u2713" else iconText,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val nodeTitle: String = when (entry) {
                is PathEntry.Real -> entry.status.node.title
                is PathEntry.DynamicReview -> entry.synthetic.title
            }
            val titleColor = when {
                isLocked -> DuolingoGrayLock
                isCurrent -> DuolingoGreenLedge
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (isLocked) 0.4f else 1f
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Text(
                    text = nodeTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelTestNodeItem(isLocked: Boolean, title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = size.width / 2
            val startY = size.height / 2 + 35.dp.toPx()
            val endX = size.width / 2
            val endY = size.height + size.height / 2 - 35.dp.toPx()
            drawLine(
                color = if (isLocked) DuolingoGray.copy(alpha = 0.5f) else DuolingoGold.copy(alpha = 0.3f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(onClick = onClick),
                shape = CircleShape,
                color = if (isLocked) DuolingoGray else DuolingoGold,
                border = BorderStroke(3.dp, if (isLocked) DuolingoGrayLock else DuolingoGoldLedge),
                shadowElevation = if (isLocked) 0.dp else 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLocked) "\uD83D\uDD12" else "\uD83C\uDFC6",
                        fontSize = 32.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DuolingoGold.copy(alpha = 0.5f))
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isLocked) DuolingoGrayLock else Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
