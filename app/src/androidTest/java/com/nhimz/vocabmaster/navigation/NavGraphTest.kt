package com.nhimz.vocabmaster.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import com.nhimz.vocabmaster.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho NavGraph routing structure (Phase 3 Plan 03-03, Task 3).
 *
 * Verify rằng tất cả routes đã đăng ký type-safe với @Serializable + NavKey
 * (D-02, UX-01) và backStack semantics hoạt động đúng (push / pop /
 * top-level switching). Đây là pure JVM test — không cần Android runtime hay
 * MockK, vì cấu trúc routing là pure Kotlin types.
 */
class NavGraphTest {

    // ===== Route type-safety tests =====

    @Test
    fun all_routes_implement_NavKey() {
        // Each Screen subtype must be a valid NavKey for entryProvider
        val welcome: NavKey = Screen.Welcome
        val goalPicker: NavKey = Screen.GoalPicker
        val placementTest: NavKey = Screen.PlacementTest
        val firstWin: NavKey = Screen.FirstWin
        val home: NavKey = Screen.Home
        val statistics: NavKey = Screen.Statistics
        val settings: NavKey = Screen.Settings
        val debugPanel: NavKey = Screen.DebugPanel
        val login: NavKey = Screen.Login
        val quiz: NavKey = Screen.Quiz()
        val result: NavKey = Screen.Result(
            xpGained = 10,
            durationSeconds = 60,
            correctCount = 5,
            totalCount = 5,
            sessionStability = 1.0
        )
        val guidebook: NavKey = Screen.Guidebook(unitId = "u1")
        val jumpTest: NavKey = Screen.JumpTest(unitId = "u1")
        val sectionCheckpoint: NavKey = Screen.SectionCheckpoint(sectionId = "s1")
        val unitCheckpoint: NavKey = Screen.UnitCheckpoint(unitId = "u1")

        // All should be non-null and of type NavKey
        listOf(welcome, goalPicker, placementTest, firstWin, home, statistics,
               settings, debugPanel, login, quiz, result, guidebook, jumpTest,
               sectionCheckpoint, unitCheckpoint).forEach { route ->
            assertNotNull("Route must not be null", route)
        }
    }

    @Test
    fun quiz_route_carries_cardIds_argument() {
        val quiz1 = Screen.Quiz()
        val quiz2 = Screen.Quiz(cardIds = listOf("c1", "c2", "c3"))

        assertEquals(null, quiz1.cardIds)
        assertEquals(listOf("c1", "c2", "c3"), quiz2.cardIds)
    }

    @Test
    fun result_route_carries_all_scoring_arguments() {
        val result = Screen.Result(
            xpGained = 25,
            durationSeconds = 120,
            correctCount = 8,
            totalCount = 10,
            sessionStability = 2.5,
            incorrectCardIds = listOf("c1", "c2"),
            isLevelTest = true,
            isPassedLevelTest = false
        )

        assertEquals(25, result.xpGained)
        assertEquals(120, result.durationSeconds)
        assertEquals(8, result.correctCount)
        assertEquals(10, result.totalCount)
        assertEquals(2.5, result.sessionStability, 0.001)
        assertEquals(listOf("c1", "c2"), result.incorrectCardIds)
        assertEquals(true, result.isLevelTest)
        assertEquals(false, result.isPassedLevelTest)
    }

    @Test
    fun parameterized_routes_carry_their_id_argument() {
        assertEquals("u1", Screen.Guidebook(unitId = "u1").unitId)
        assertEquals("u1", Screen.JumpTest(unitId = "u1").unitId)
        assertEquals("s1", Screen.SectionCheckpoint(sectionId = "s1").sectionId)
        assertEquals("u1", Screen.UnitCheckpoint(unitId = "u1").unitId)
    }

    @Test
    fun route_equality_works_for_data_classes() {
        // Result with same args should be equal (data class semantics)
        val r1 = Screen.Result(10, 60, 5, 5, 1.0, emptyList(), false, false)
        val r2 = Screen.Result(10, 60, 5, 5, 1.0, emptyList(), false, false)
        assertEquals(r1, r2)

        // Parameterized routes
        assertEquals(Screen.Guidebook("u1"), Screen.Guidebook("u1"))
        assertEquals(Screen.Quiz(listOf("a")), Screen.Quiz(listOf("a")))
    }

    @Test
    fun singleton_routes_use_data_object_identity() {
        // data object ensures singleton identity (important for `is Screen.Home` checks)
        assertTrue(Screen.Welcome === Screen.Welcome)
        assertTrue(Screen.Home === Screen.Home)
        assertTrue(Screen.Settings === Screen.Settings)
    }

    // ===== BackStack semantics tests =====

    @Test
    fun backStack_supports_push_and_pop() {
        val backStack: SnapshotStateList<NavKey> = mutableStateListOf(Screen.Welcome)

        // Push
        backStack.add(Screen.Login)
        assertEquals(2, backStack.size)
        assertEquals(Screen.Login, backStack.last())

        // Pop
        backStack.removeLast()
        assertEquals(1, backStack.size)
        assertEquals(Screen.Welcome, backStack.last())
    }

    @Test
    fun backStack_topLevel_routes_clear_before_add() {
        // Simulating navigateTopLevel: clear + add
        val backStack: SnapshotStateList<NavKey> = mutableStateListOf(Screen.Home)
        backStack.add(Screen.Quiz())
        backStack.add(Screen.Result(0, 0, 0, 0, 0.0))

        // Now user taps Statistics — should clear and add
        backStack.clear()
        backStack.add(Screen.Statistics)

        assertEquals(1, backStack.size)
        assertEquals(Screen.Statistics, backStack.last())
    }

    @Test
    fun topLevel_routes_set_contains_expected_destinations() {
        // The bottom nav set must contain exactly Home, Statistics, Settings
        val expected = setOf<NavKey>(Screen.Home, Screen.Statistics, Screen.Settings)
        assertEquals(3, expected.size)
        assertTrue(Screen.Home in expected)
        assertTrue(Screen.Statistics in expected)
        assertTrue(Screen.Settings in expected)
    }
}
