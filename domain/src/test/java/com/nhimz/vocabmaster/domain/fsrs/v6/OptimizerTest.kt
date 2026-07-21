package com.nhimz.vocabmaster.domain.fsrs.v6

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Property-based port of py-fsrs optimizer tests.
 *
 * Because Kotlin uses a different RNG and finite-difference gradients replace
 * torch autograd, trained parameter vectors are not bit-identical to Python.
 * Tests therefore assert properties: default guards, bounds, loss decrease,
 * and deterministic retention candidates.
 */
class OptimizerTest {

    private val baseTime: Long = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        .toInstant().toEpochMilli()

    /**
     * Generates a deterministic synthetic fixture with at least 520 non-same-day
     * review-state reviews. Card c's first review is offset by c hours, subsequent
     * reviews are spaced 1-3 days, ratings follow a fixed pattern rotated per card,
     * and review durations are deterministic.
     */
    private fun generateSyntheticLogs(
        cardCount: Int = 40,
        reviewsPerCard: Int = 14
    ): List<ReviewLog> {
        val ratingPattern = listOf(
            Rating.Good, Rating.Good, Rating.Again, Rating.Good, Rating.Easy,
            Rating.Hard, Rating.Good, Rating.Good, Rating.Again, Rating.Easy,
            Rating.Good, Rating.Hard, Rating.Good, Rating.Easy
        )
        val logs = mutableListOf<ReviewLog>()
        var globalIndex = 0

        for (card in 0 until cardCount) {
            var reviewTime = baseTime + card * 3_600_000L // + c hours
            for (reviewIndex in 0 until reviewsPerCard) {
                val rating = ratingPattern[(reviewIndex + card) % ratingPattern.size]
                val duration = 3_000L + (globalIndex % 7) * 500L
                logs.add(
                    ReviewLog(
                        cardId = "card_$card",
                        rating = rating,
                        reviewDatetime = reviewTime,
                        reviewDuration = duration
                    )
                )
                globalIndex++
                // space next review 1-3 days later
                reviewTime += (1 + (reviewIndex % 3)) * 86_400_000L
            }
        }

        return logs
    }

    @Test
    fun test_zero_revlogs() {
        val optimizer = Optimizer(emptyList())
        val optimal = optimizer.computeOptimalParameters()

        assertEquals(Scheduler.DEFAULT_PARAMETERS.size, optimal.size)
        Scheduler.DEFAULT_PARAMETERS.forEachIndexed { index, expected ->
            assertEquals(expected, optimal[index], 0.0)
        }
    }

    @Test
    fun test_few_review_logs() {
        val logs = generateSyntheticLogs(cardCount = 10, reviewsPerCard = 10)
        val optimizer = Optimizer(logs)
        val optimal = optimizer.computeOptimalParameters()

        assertEquals(Scheduler.DEFAULT_PARAMETERS.size, optimal.size)
        Scheduler.DEFAULT_PARAMETERS.forEachIndexed { index, expected ->
            assertEquals(expected, optimal[index], 0.0)
        }
    }

    @Test
    fun test_unordered_review_logs() {
        val logs = generateSyntheticLogs()
        val shuffled1 = logs.shuffled(kotlin.random.Random(42))
        val shuffled2 = logs.shuffled(kotlin.random.Random(123))

        val optimizer1 = Optimizer(shuffled1)
        val optimizer2 = Optimizer(shuffled2)

        // The formatted training data is identical after sorting, so batch loss on
        // defaults is exactly equal for both orderings.
        val loss1 = optimizer1.computeBatchLoss(Scheduler.DEFAULT_PARAMETERS)
        val loss2 = optimizer2.computeBatchLoss(Scheduler.DEFAULT_PARAMETERS)
        assertEquals(loss1, loss2, 0.0)
    }

    @Test
    fun test_training_improves_loss_and_respects_bounds() {
        val logs = generateSyntheticLogs()
        val optimizer = Optimizer(logs)

        val startingLoss = optimizer.computeBatchLoss(Scheduler.DEFAULT_PARAMETERS)
        val trained = optimizer.computeOptimalParameters()

        assertNotEquals(Scheduler.DEFAULT_PARAMETERS.toList(), trained.toList())

        for (index in trained.indices) {
            assertTrue(
                "trained[$index] = ${trained[index]} below lower bound ${Scheduler.LOWER_BOUNDS_PARAMETERS[index]}",
                trained[index] >= Scheduler.LOWER_BOUNDS_PARAMETERS[index]
            )
            assertTrue(
                "trained[$index] = ${trained[index]} above upper bound ${Scheduler.UPPER_BOUNDS_PARAMETERS[index]}",
                trained[index] <= Scheduler.UPPER_BOUNDS_PARAMETERS[index]
            )
            assertTrue(
                "trained[$index] is NaN",
                !trained[index].isNaN()
            )
        }

        val trainedLoss = optimizer.computeBatchLoss(trained)
        assertTrue(
            "Expected trained loss $trainedLoss to be lower than starting loss $startingLoss",
            trainedLoss < startingLoss
        )
    }

    @Test
    fun test_optimize_review_logs_with_difficulty_1_cards() {
        // Many easy first-reviews drive difficulty toward 1.0.
        val scheduler = Scheduler()
        val logs = mutableListOf<ReviewLog>()
        repeat(100) { cardIndex ->
            var card = Card(cardId = "easy_$cardIndex")
            repeat(100) { dayIndex ->
                val reviewTime = baseTime + dayIndex * 86_400_000L
                val (nextCard, reviewLog) = scheduler.reviewCard(
                    card = card,
                    rating = Rating.Easy,
                    reviewDatetimeMillis = reviewTime
                )
                card = nextCard
                logs.add(reviewLog)
            }
            assertEquals(1.0, card.difficulty ?: 0.0, 1e-6)
        }

        val optimizer = Optimizer(logs)
        val trained = optimizer.computeOptimalParameters()

        for (index in trained.indices) {
            assertTrue(
                "trained[$index] is NaN",
                !trained[index].isNaN()
            )
            assertTrue(
                "trained[$index] out of bounds",
                trained[index] in Scheduler.LOWER_BOUNDS_PARAMETERS[index]..Scheduler.UPPER_BOUNDS_PARAMETERS[index]
            )
        }
    }

    @Test
    fun test_optimal_retention() {
        val logs = generateSyntheticLogs()
        val optimizer = Optimizer(logs)

        val result = optimizer.computeOptimalRetention(Scheduler.DEFAULT_PARAMETERS)
        assertTrue(
            "Expected result $result to be one of [0.7, 0.75, 0.8, 0.85, 0.9, 0.95]",
            result in setOf(0.7, 0.75, 0.8, 0.85, 0.9, 0.95)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_optimal_retention_zero_review_logs() {
        val optimizer = Optimizer(emptyList())
        optimizer.computeOptimalRetention(Scheduler.DEFAULT_PARAMETERS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_optimal_retention_few_review_logs() {
        val logs = generateSyntheticLogs(cardCount = 10, reviewsPerCard = 10)
        val optimizer = Optimizer(logs)
        optimizer.computeOptimalRetention(Scheduler.DEFAULT_PARAMETERS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_optimal_retention_no_review_duration() {
        val logs = generateSyntheticLogs().toMutableList()
        logs.add(
            ReviewLog(
                cardId = "missing_duration",
                rating = Rating.Good,
                reviewDatetime = baseTime,
                reviewDuration = null
            )
        )
        val optimizer = Optimizer(logs)
        optimizer.computeOptimalRetention(Scheduler.DEFAULT_PARAMETERS)
    }

    @Test
    fun test_simulated_costs() {
        val logs = generateSyntheticLogs()
        val optimizer = Optimizer(logs)
        val probsAndCosts = optimizer.computeProbsAndCosts()
        val cost = optimizer.simulateCost(
            desiredRetention = 0.9,
            parameters = Scheduler.DEFAULT_PARAMETERS,
            numCards = 1000,
            probsAndCosts = probsAndCosts
        )

        assertTrue("Expected positive cost, got $cost", cost > 0.0)
        assertTrue("Expected finite cost, got $cost", cost.isFinite())
    }
}
