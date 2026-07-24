package com.nhimz.vocabmaster.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.ui.navigation.Screen
import com.nhimz.vocabmaster.domain.usecase.UpdateStreakUseCase
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.Section
import com.nhimz.vocabmaster.domain.model.Unit
import com.nhimz.vocabmaster.domain.model.Node
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NodeStatus(
    val node: Node,
    val isCompleted: Boolean,
    val isLocked: Boolean
)

/**
 * Synthetic REVIEW node auto-inserted on the path when there are due FSRS cards
 * belonging to a completed unit. Ephemeral: not persisted to `node_progress`,
 * survives only within a curriculum refresh cycle so the user sees spaced-repetition
 * practice surfacing directly on the path (Duolingo-style).
 */
data class SyntheticReviewNode(
    val nodeId: String,
    val unitId: String,
    val title: String,
    val dueCount: Int,
    val isCurrent: Boolean = false
)

data class UnitStatus(
    val unit: Unit,
    val nodes: List<NodeStatus>,
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val dynamicReview: SyntheticReviewNode? = null
)

data class SectionStatus(
    val section: Section,
    val units: List<UnitStatus>,
    val isCompleted: Boolean,
    val isLocked: Boolean
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val updateStreakUseCase: UpdateStreakUseCase
) : ViewModel() {

    /**
     * Back stack cho Navigation 3 (D-02 / UX-01) — Phase 3 Plan 03-03.
     *
     * Trước đây: `_currentScreen: MutableStateFlow<Screen>` chỉ track 1 screen
     * duy nhất, dispatch qua `when (currentScreen)` trong `VocabMasterApp.kt`.
     * Hạn chế: không có back stack, không có deep link, navigation history bị
     * reset mỗi lần navigate.
     *
     * Bây giờ: `backStack: SnapshotStateList<NavKey>` là source of truth duy
     * nhất cho navigation state. `VocabMasterApp.kt` đọc backStack để render
     * `NavDisplay` (Navigation 3). Top-level routes (Home/Statistics/Settings)
     * dùng `navigateTopLevel()` để switch tab; sub-routes dùng `navigateTo()`.
     *
     * Lưu ý: `SnapshotStateList` được tạo từ `mutableStateListOf` — Compose
     * tự động observe và trigger recomposition khi list thay đổi. ViewModel
     * vẫn giữ ownership để state survive configuration changes.
     */
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf<NavKey>(Screen.Welcome)

    /**
     * Top-level routes (bottom nav destinations) — clear backStack trước khi
     * thêm route mới để đảm bảo tab switch không bị stack-up.
     */
    val topLevelRoutes: Set<NavKey> = setOf(Screen.Home, Screen.Statistics, Screen.Settings)

    /**
     * `true` khi backStack.last() là một trong các top-level routes — dùng để
     * hiện/ẩn bottom navigation bar (UI layer).
     */
    val isOnTopLevelRoute: Boolean
        get() = backStack.lastOrNull() in topLevelRoutes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Shared statistics flows
    val xpTotal: StateFlow<Int> = settingsRepository.xpTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStudySeconds: StateFlow<Int> = settingsRepository.todayStudySeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentStreak: StateFlow<Int> = settingsRepository.currentStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableFreezes: StateFlow<Int> = settingsRepository.availableFreezes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val _streakFreezeUsedEvent = MutableStateFlow(false)
    val streakFreezeUsedEvent: StateFlow<Boolean> = _streakFreezeUsedEvent.asStateFlow()

    fun clearStreakFreezeUsedEvent() {
        _streakFreezeUsedEvent.value = false
    }

    /**
     * One-shot snackbar messages surfaced from MainViewModel operations.
     * Backed by a `MutableSharedFlow` with `extraBufferCapacity = 8` so the
     * Container can collect them via `LaunchedEffect` even if the emission
     * happens during a recomposition (D-04 / D-05).
     *
     * Container screens (e.g. HomeScreen) read [snackbarMessages] and forward
     * each emission to the global [androidx.compose.material3.SnackbarHostState]
     * hosted in [com.nhimz.vocabmaster.ui.VocabMasterApp].
     */
    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val snackbarMessages: SharedFlow<SnackbarMessage> = _snackbarMessages.asSharedFlow()

    /**
     * Emit a [SnackbarMessage] for the Container to display. Suspends if
     * the buffer is full; callers that want a fire-and-forget signal can
     * wrap in `viewModelScope.launch { emitSnackbar(...) }`.
     */
    suspend fun emitSnackbar(message: SnackbarMessage) {
        _snackbarMessages.emit(message)
    }

    private val _badgeUnlockedEvent = MutableStateFlow<String?>(null)
    val badgeUnlockedEvent: StateFlow<String?> = _badgeUnlockedEvent.asStateFlow()
    val dailyGoalMinutes: StateFlow<Int> = settingsRepository.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    fun clearBadgeUnlockedEvent() {
        _badgeUnlockedEvent.value = null
    }

    private val _dueCount = MutableStateFlow(0)
    val dueCount: StateFlow<Int> = _dueCount.asStateFlow()

    private val _mistakeCount = MutableStateFlow(0)
    val mistakeCount: StateFlow<Int> = _mistakeCount.asStateFlow()

    // Trigger flow to manually force curriculum UI update
    private val _curriculumUpdateTrigger = MutableStateFlow(0)

    val curriculumStatus: StateFlow<List<SectionStatus>> = combine(
        vocabularyRepository.getSections(),
        _curriculumUpdateTrigger,
        settingsRepository.placementLevel
    ) { sections, _, currentLevelStr ->
        // Assuming currentLevelStr is something like "A1.1" matching cefrSublevel
        val statuses = mutableListOf<SectionStatus>()
        var isPreviousSectionCompleted = true
        var isPlacementFound = false

        for (section in sections) {
            // Unlocked if previous section is completed, OR if it's <= placementLevel
            var sectionUnlocked = false
            if (currentLevelStr == null) {
                // If no placement, unlock only first
                sectionUnlocked = isPreviousSectionCompleted
            } else {
                if (section.cefrSublevel == currentLevelStr) {
                    isPlacementFound = true
                    sectionUnlocked = true
                } else if (!isPlacementFound) {
                    // Before or equal to placement level
                    sectionUnlocked = true
                } else {
                    // After placement level
                    sectionUnlocked = isPreviousSectionCompleted
                }
            }

            val units = vocabularyRepository.getUnitsBySection(section.id).first()
            val completedNodesInSection = vocabularyRepository.getCompletedNodesBySection(section.id)
            
            val unitStatuses = mutableListOf<UnitStatus>()
            var isPreviousUnitCompleted = true

            for (unit in units) {
                val nodes = vocabularyRepository.getNodesByUnit(unit.id).first()
                val completedNodesInUnit = vocabularyRepository.getCompletedNodesByUnit(unit.id)

                val nodeStatuses = mutableListOf<NodeStatus>()
                var isPreviousNodeCompleted = true

                for (node in nodes) {
                    val isNodeCompleted = completedNodesInUnit.contains(node.id)
                    val isNodeLocked = !sectionUnlocked || !isPreviousUnitCompleted || !isPreviousNodeCompleted

                    nodeStatuses.add(
                        NodeStatus(
                            node = node,
                            isCompleted = isNodeCompleted,
                            isLocked = isNodeLocked
                        )
                    )
                    // Previous node completed is required to unlock next node
                    isPreviousNodeCompleted = isNodeCompleted
                }

                // Check if the Unit's Checkpoint (last node usually) is completed
                val isUnitCompleted = nodes.lastOrNull()?.let { completedNodesInUnit.contains(it.id) } ?: true
                val isUnitLocked = !sectionUnlocked || !isPreviousUnitCompleted

                // Synthetic REVIEW node: auto-inserted when the unit is complete AND there
                // are due FSRS cards belonging to this unit. Duolingo-style spaced-repetition
                // surface directly on the path. Ephemeral — not persisted.
                val dynamicReview = if (isUnitCompleted) {
                    val dueCount = vocabularyRepository.getDueCardCountByUnit(
                        unit.id,
                        System.currentTimeMillis()
                    )
                    if (dueCount > 0) {
                        SyntheticReviewNode(
                            nodeId = "auto_review_${unit.id}",
                            unitId = unit.id,
                            title = "Ôn tập: ${unit.title}",
                            dueCount = dueCount
                        )
                    } else null
                } else null

                unitStatuses.add(
                    UnitStatus(
                        unit = unit,
                        nodes = nodeStatuses,
                        isCompleted = isUnitCompleted,
                        isLocked = isUnitLocked,
                        dynamicReview = dynamicReview
                    )
                )
                // Previous unit completed is required to unlock next unit
                isPreviousUnitCompleted = isUnitCompleted
            }

            // Section is completed when ALL nodes in ALL units of the section are completed.
            // (Previously: last unit's last node completed. Now: every node in the section.)
            val totalNodesInSection = unitStatuses.sumOf { it.nodes.size }
            val completedNodesCount = unitStatuses.sumOf {
                it.nodes.count { n -> n.isCompleted }
            }
            val isSectionCompleted = totalNodesInSection > 0 &&
                completedNodesCount == totalNodesInSection

            statuses.add(
                SectionStatus(
                    section = section,
                    units = unitStatuses,
                    isCompleted = isSectionCompleted,
                    isLocked = !sectionUnlocked
                )
            )

            isPreviousSectionCompleted = isSectionCompleted
        }

        statuses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkOnboardingStatus()
        refreshCounts()
        monitorBadges()
    }

    fun triggerCurriculumUpdate() {
        _curriculumUpdateTrigger.value += 1
    }

    private fun monitorBadges() {
        viewModelScope.launch {
            combine(
                settingsRepository.badgeStatus,
                settingsRepository.xpTotal,
                settingsRepository.currentStreak
            ) { unlockedList, xpTotal, currentStreak ->
                Triple(unlockedList, xpTotal, currentStreak)
            }.collect { (unlockedList, xpTotal, currentStreak) ->
                val newBadges = mutableListOf<String>()
                if (currentStreak >= 3 && !unlockedList.contains("streak_3")) newBadges.add("streak_3")
                if (currentStreak >= 7 && !unlockedList.contains("streak_7")) newBadges.add("streak_7")
                if (xpTotal >= 500 && !unlockedList.contains("xp_500")) newBadges.add("xp_500")
                if (xpTotal >= 1000 && !unlockedList.contains("xp_1000")) newBadges.add("xp_1000")

                newBadges.forEach { badgeId ->
                    settingsRepository.addBadge(badgeId)
                    val title = when (badgeId) {
                        "streak_3" -> "Kiên trì"
                        "streak_7" -> "Chiến binh học tập"
                        "xp_500" -> "Tích lũy"
                        "xp_1000" -> "Học giả"
                        else -> badgeId
                    }
                    _badgeUnlockedEvent.value = title
                }
            }
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            _dueCount.value = vocabularyRepository.getDueCount(System.currentTimeMillis())
            _mistakeCount.value = vocabularyRepository.getMistakeCount()
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            val badges = settingsRepository.badgeStatus.first()
            val startRoute: NavKey = if (badges.contains("onboarding_completed")) {
                Screen.Home
            } else {
                Screen.Welcome
            }
            // Replace backStack so NavDisplay starts at the correct screen
            // (avoids brief flash of Welcome before redirecting to Home).
            backStack.clear()
            backStack.add(startRoute)
            _isLoading.value = false
        }
    }

    /**
     * Push một route mới lên backStack (Navigation 3 forward navigation).
     * Caller chịu trách nhiệm truyền đúng typed NavKey — compiler sẽ check
     * argument types tại call site (UX-01 / D-02).
     */
    fun navigateTo(route: NavKey) {
        backStack.add(route)
    }

    /**
     * Pop top entry khỏi backStack (back navigation). Nếu chỉ còn 1 entry thì
     * không pop (để user không thoát app khi back ở root).
     */
    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    /**
     * Switch sang top-level route (Home / Statistics / Settings) — clear
     * backStack trước khi add để tránh stack-up khi user tap bottom nav nhiều lần.
     */
    fun navigateTopLevel(route: NavKey) {
        if (route !in topLevelRoutes) {
            // Fallback: treat as normal navigate
            navigateTo(route)
            return
        }
        backStack.clear()
        backStack.add(route)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.addBadge("onboarding_completed")
            // Give 50 starting XP
            settingsRepository.addXp(50)
            settingsRepository.setCurrentStreak(1)
            settingsRepository.setLastStudyDate(System.currentTimeMillis())
            navigateTopLevel(Screen.Home)
        }
    }

    fun savePlacementLevel(level: String) {
        viewModelScope.launch {
            settingsRepository.setPlacementLevel(level)
        }
    }

    fun addXp(xp: Int) {
        viewModelScope.launch {
            settingsRepository.addXp(xp)
        }
    }

    fun addStudyTime(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.addStudySeconds(seconds)
        }
    }

    fun updateStreak() {
        viewModelScope.launch {
            val freezesBefore = settingsRepository.availableFreezes.first()
            val streakBefore = settingsRepository.currentStreak.first()
            updateStreakUseCase.execute()
            val freezesAfter = settingsRepository.availableFreezes.first()
            val streakAfter = settingsRepository.currentStreak.first()
            if (freezesAfter < freezesBefore && streakAfter == streakBefore) {
                _streakFreezeUsedEvent.value = true
            }
        }
    }
}
