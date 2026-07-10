package com.nhimz.vocabmaster.domain.fsrs

import java.time.LocalDateTime

enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4)
}

enum class State(val value: Int) {
    New(0),
    Learning(1),
    Review(2),
    Relearning(3)
}

data class Card(
    val id: Long = 0,
    val due: LocalDateTime = LocalDateTime.now(),
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val interval: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: State = State.New,
    val lastReview: LocalDateTime? = null
)

data class ReviewLog(
    val rating: Rating,
    val elapsed_days: Int,
    val scheduled_days: Int,
    val stability: Double,
    val difficulty: Double,
    val state: State,
    val timestamp: LocalDateTime
)
