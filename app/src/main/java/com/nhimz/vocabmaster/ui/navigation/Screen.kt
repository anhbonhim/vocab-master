package com.nhimz.vocabmaster.ui.navigation

sealed class Screen {
    object Welcome : Screen()
    object GoalPicker : Screen()
    object PlacementTest : Screen()
    object FirstWin : Screen()
    object Home : Screen()
    object Statistics : Screen()
    object Settings : Screen()
    object TopicPicker : Screen()
    object DebugPanel : Screen()
    object Login : Screen() // New Login Screen
    object Quiz : Screen()
    object Flashcard : Screen()
    data class Result(
        val xpGained: Int,
        val durationSeconds: Int,
        val correctCount: Int,
        val totalCount: Int,
        val sessionStability: Double
    ) : Screen()
}