package com.nhimz.vocabmaster.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.ui.components.showSnackbar
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.util.LocalLogger
import kotlinx.coroutines.launch

/**
 * Home screen Container (Plan 03-01, Task 2 — and Plan 03-04 snackbar wiring).
 *
 * Responsibilities:
 *  - Collect UI state from [MainViewModel] flows
 *  - Hold transient local UI state (preview sheet, locked dialog, snackbar)
 *  - Flatten the curriculum into a [List]<[PathItem]> for the Content
 *  - Drive side effects (LaunchedEffect, Toast)
 *  - Render dialogs and bottom sheets
 *  - Wire [viewModel.snackbarMessages] to the global [snackbarHostState]
 *    (Plan 03-04 — D-04 / D-05)
 *
 * Pure UI rendering is delegated to [HomeScreenContent]. Public signature is
 * preserved (with one optional [snackbarHostState] parameter) so existing call
 * sites (e.g. `VocabMasterApp.kt`) need no change.
 */
@Composable
fun HomeScreen(
    onStartQuiz: () -> Unit,
    onStartFlashcard: (List<String>?) -> Unit,
    onStartCustomQuiz: (stage: String?, topic: String?, index: Int?, isLevelTest: Boolean, isReviewGym: Boolean) -> Unit,
    onStartNodeSession: (nodeId: String) -> Unit,
    onStartReviewNode: (nodeId: String, unitId: String?, sectionId: String?) -> Unit,
    onStartJumpTest: (unitId: String) -> Unit,
    onStartSectionCheckpoint: (sectionId: String) -> Unit,
    onStartUnitCheckpoint: (unitId: String) -> Unit,
    onStartGuidebook: (unitId: String) -> Unit,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState? = null
) {
    val xpTotal by viewModel.xpTotal.collectAsState()
    val todayStudySeconds by viewModel.todayStudySeconds.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val dailyGoal by viewModel.dailyGoalXp.collectAsState()
    val dueCount = viewModel.dueCount.collectAsState().value
    val mistakeCount by viewModel.mistakeCount.collectAsState()
    val curriculumStatus = viewModel.curriculumStatus.collectAsState().value
    val availableFreezes by viewModel.availableFreezes.collectAsState()
    val streakFreezeUsed by viewModel.streakFreezeUsedEvent.collectAsState()

    val context = LocalContext.current
    if (streakFreezeUsed) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(
                context,
                "\u2744\uFE0F Streak Freeze đã bảo vệ chuỗi ngày học của bạn!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearStreakFreezeUsedEvent()
        }
    }

    var showLockedDialog by remember { mutableStateOf(false) }
    var activePreview by remember { mutableStateOf<PathEntry?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshCounts()
        viewModel.triggerCurriculumUpdate()
    }

    // Plan 03-04: wire MainViewModel.snackbarMessages to the global
    // SnackbarHostState. `rememberUpdatedState` ensures we always invoke the
    // latest flow / host even if the parent re-emits (D-04 / D-05).
    val currentSnackbarHostState by rememberUpdatedState(snackbarHostState)
    val currentSnackbarMessages = viewModel.snackbarMessages
    LaunchedEffect(currentSnackbarHostState) {
        currentSnackbarHostState?.let { host ->
            currentSnackbarMessages.collect { message: SnackbarMessage ->
                if (message.isError) {
                    LocalLogger.e(
                        tag = "HomeScreen",
                        message = "Snackbar error surfaced: ${message.text}"
                    )
                }
                host.showSnackbar(message)
            }
        }
    }

    val minutesStudiedToday = (todayStudySeconds / 60).coerceAtMost(dailyGoal)
    val progressPercent = if (dailyGoal > 0) {
        minutesStudiedToday.toFloat() / dailyGoal.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(1200)
    )
    val showBacklogWarning = dueCount > 50

    // Flatten the curriculum into a typed [PathItem] list (Plan 03-01 Task 2:
    // 0/1/20+ section edge cases handled naturally by the LazyColumn).
    val path: List<PathItem> = remember(curriculumStatus, dueCount) {
        buildList {
            var globalIndex = 0
            curriculumStatus.forEach { sectionStatus ->
                add(PathItem.SectionHeader(sectionStatus))
                sectionStatus.units.forEach { unitStatus ->
                    add(PathItem.UnitHeader(unitStatus))
                    unitStatus.dynamicReview?.let { drn ->
                        add(PathItem.DynamicNode(drn, unitStatus, globalIndex++))
                    }
                    unitStatus.nodes.forEach { nodeStatus ->
                        add(PathItem.Node(nodeStatus, unitStatus, globalIndex++))
                    }
                }
                add(PathItem.SectionBoss(sectionStatus))
            }
        }
    }

    val currentNodeId = remember(curriculumStatus) {
        curriculumStatus.firstNotNullOfOrNull { section ->
            section.units.firstNotNullOfOrNull { unit ->
                val fromDynamic = unit.dynamicReview?.takeIf { it.dueCount > 0 }?.nodeId
                fromDynamic ?: unit.nodes.firstOrNull { !it.isCompleted && !it.isLocked }?.node?.id
            }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isScrolledAwayFromCurrent by remember {
        derivedStateOf {
            val idx = path.indexOfFirst { item ->
                item is PathItem.Node && item.status.node.id == currentNodeId
            }
            idx >= 0 && (listState.firstVisibleItemIndex > idx + 1)
        }
    }

    val uiState = HomeScreenUiState(
        xpTotal = xpTotal,
        todayStudySeconds = todayStudySeconds,
        dailyGoalXp = dailyGoal,
        currentStreak = currentStreak,
        availableFreezes = availableFreezes,
        dueCount = dueCount,
        mistakeCount = mistakeCount,
        showBacklogWarning = showBacklogWarning,
        animatedProgress = animatedProgress,
        minutesStudiedToday = minutesStudiedToday,
        isScrolledAwayFromCurrent = isScrolledAwayFromCurrent,
        path = path,
        currentNodeId = currentNodeId
    )

    HomeScreenContent(
        state = uiState,
        onStartCustomQuiz = {
            onStartCustomQuiz(null, null, null, false, true)
        },
        onStartFlashcard = { onStartFlashcard(null) },
        onScrollToCurrent = {
            val idx = path.indexOfFirst { item ->
                item is PathItem.Node && item.status.node.id == currentNodeId
            }
            if (idx >= 0) {
                scope.launch { listState.animateScrollToItem(idx) }
            }
        },
        onSectionGuidebook = onStartGuidebook,
        onSectionJumpTest = onStartJumpTest,
        onSectionBossClick = { sectionId, isLocked ->
            if (isLocked) {
                showLockedDialog = true
            } else {
                onStartSectionCheckpoint(sectionId)
            }
        },
        onNodeClick = { item ->
            when (item) {
                is PathItem.Node -> {
                    if (item.status.isLocked) {
                        showLockedDialog = true
                    } else {
                        if (showBacklogWarning && item.status.node.type == NodeType.LESSON) {
                            snackbarMessage = "\uD83D\uDCA1 Đang có $dueCount từ đến hạn — nên ghé REVIEW node trước nếu có thời gian."
                        }
                        activePreview = PathEntry.Real(
                            status = item.status,
                            unitId = item.unitStatus.unit.id,
                            sectionId = item.unitStatus.unit.sectionId
                        )
                    }
                }
                is PathItem.DynamicNode -> {
                    activePreview = PathEntry.DynamicReview(
                        synthetic = item.synthetic,
                        sectionId = item.unitStatus.unit.sectionId,
                        isCompleted = item.synthetic.dueCount == 0,
                        isLocked = false,
                        isCurrent = item.synthetic.nodeId == currentNodeId
                    )
                }
                else -> Unit
            }
        },
        listState = listState
    )

    // ===== Duolingo-style popup preview when tapping an active node =====
    activePreview?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { activePreview = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            NodePreviewContent(
                entry = entry,
                onStart = {
                    activePreview = null
                    when (entry) {
                        is PathEntry.Real -> {
                            val node = entry.status.node
                            when (node.type) {
                                NodeType.REVIEW -> onStartReviewNode(node.id, entry.unitId, entry.sectionId)
                                NodeType.UNIT_CHECKPOINT -> onStartUnitCheckpoint(entry.unitId)
                                else -> onStartNodeSession(node.id)
                            }
                        }
                        is PathEntry.DynamicReview -> {
                            onStartReviewNode(
                                entry.synthetic.nodeId,
                                entry.synthetic.unitId,
                                entry.sectionId
                            )
                        }
                    }
                }
            )
        }
    }

    // ===== Locked content dialog =====
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            title = { Text("\uD83D\uDD12 Nội dung chưa mở khóa") },
            text = {
                Text("Bạn cần hoàn thành các bài học và bài kiểm tra chặng trước đó để mở khóa chủ đề này.")
            },
            confirmButton = {
                Button(onClick = { showLockedDialog = false }) {
                    Text("ĐỒNG Ý")
                }
            }
        )
    }

    // ===== Snackbar (Toast) for soft-nudge =====
    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            snackbarMessage = null
        }
    }
}

/**
 * Preview content for the bottom sheet shown when a node is tapped. Kept here
 * (not in Content) because it is a modal that the Container must coordinate.
 */
@Composable
private fun NodePreviewContent(entry: PathEntry, onStart: () -> Unit) {
    val title: String
    val subtitle: String
    val icon: String
    val actionLabel: String
    when (entry) {
        is PathEntry.Real -> {
            val node = entry.status.node
            title = node.title
            subtitle = node.scenarioContext
            icon = when (node.type) {
                NodeType.LESSON -> "\u2B50"
                NodeType.REVIEW -> "\uD83C\uDFCB\uFE0F"
                NodeType.UNIT_CHECKPOINT -> "\uD83C\uDFC6"
                else -> "\u2B50"
            }
            actionLabel = when (node.type) {
                NodeType.REVIEW -> "\uD83D\uDCAA LUYỆN TẬP ÔN TẬP"
                NodeType.UNIT_CHECKPOINT -> "\uD83C\uDFC6 VÀO BÀI THI UNIT"
                else -> "\u25B6 BẮT ĐẦU BÀI HỌC"
            }
        }
        is PathEntry.DynamicReview -> {
            val drn = entry.synthetic
            title = drn.title
            subtitle = "Có ${drn.dueCount} từ vựng đến hạn ôn tập (spaced repetition FSRS). Luyện lại để củng cố trí nhớ dài hạn."
            icon = "\uD83C\uDFCB\uFE0F"
            actionLabel = "\uD83D\uDCAA LUYỆN TẬP ÔN TẬP"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = androidx.compose.ui.graphics.Color(0xFF58CC02).copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon, fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF58CC02))
        ) {
            Text(actionLabel, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
    }
}
