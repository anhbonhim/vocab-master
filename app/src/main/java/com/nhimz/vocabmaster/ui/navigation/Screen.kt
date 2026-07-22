package com.nhimz.vocabmaster.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation routes (D-02, UX-01) — Phase 3 Plan 03-03.
 *
 * Mỗi subtype là một `@Serializable` data class / object implement interface
 * [NavKey] của Navigation 3 (1.0.1). Tất cả routing trong app phải dùng những
 * subtype này — KHÔNG dùng string-based routes hay sealed class "kế thừa cũ".
 *
 * Lý do tại sao là sealed class (thay vì enum hay top-level data class):
 *  - Type-safety đầy đủ: `is Screen.Welcome` smart-cast work như cũ.
 *  - Parametric routes (Result, Quiz, Guidebook, JumpTest, SectionCheckpoint,
 *    UnitCheckpoint) có thể dùng data class với typed fields, compiler check
 *    argument types tại call site.
 *  - @Serializable cho phép truyền qua Bundle / SavedStateHandle / deep link
 *    mà không cần custom Parcelable / NavType boilerplate.
 *
 * Xem thêm:
 *  - PLAN.md D-02: "Convert everything at once" — thay thế toàn bộ Screen
 *    routing sealed class cũ bằng Kotlin Serialization type-safe routes.
 *  - 03-RESEARCH.md "Type-Safe Navigation setup" — code example với
 *    `composable<Route> { entry -> val route = entry.toRoute<Route>() }`
 *    (Nav 2.x) hoặc `entry<Route> { key -> ... }` (Nav 3.x).
 *
 * Ứng với Phase 03-03, route definitions này đã được thay thế hoàn toàn cho
 * legacy Screen — không còn code nào dùng string-based routes.
 */
sealed class Screen : NavKey {
    @Serializable
    data object Welcome : Screen()

    @Serializable
    data object GoalPicker : Screen()

    @Serializable
    data object PlacementTest : Screen()

    @Serializable
    data object FirstWin : Screen()

    @Serializable
    data object Home : Screen()

    @Serializable
    data object Statistics : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object DebugPanel : Screen()

    @Serializable
    data object Login : Screen()

    @Serializable
    data class Quiz(val cardIds: List<String>? = null) : Screen()

    @Serializable
    data class Result(
        val xpGained: Int,
        val durationSeconds: Int,
        val correctCount: Int,
        val totalCount: Int,
        val sessionStability: Double,
        val incorrectCardIds: List<String> = emptyList(),
        val isLevelTest: Boolean = false,
        val isPassedLevelTest: Boolean = false
    ) : Screen()

    @Serializable
    data class Guidebook(val unitId: String) : Screen()

    @Serializable
    data class JumpTest(val unitId: String) : Screen()

    @Serializable
    data class SectionCheckpoint(val sectionId: String) : Screen()

    @Serializable
    data class UnitCheckpoint(val unitId: String) : Screen()
}
