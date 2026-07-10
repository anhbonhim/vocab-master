package com.nhimz.vocabmaster.domain.fsrs

import com.nhimz.vocabmaster.domain.fsrs.reference.FlashCard
import com.nhimz.vocabmaster.domain.fsrs.reference.ReferenceFSRS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FSRSTest {

    private val testParams = listOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658, 0.1542
    )

    @Test
    fun testParityWithoutFuzzing() {
        val fsrs = FSRS(requestRetention = 0.9, params = testParams, enableFuzz = false)
        val refFsrs = ReferenceFSRS(requestRetention = 0.9, params = testParams, enableFuzz = false)

        val baseTime = LocalDateTime.of(2026, 7, 10, 12, 0)

        // Generate test scenarios
        val testCards = listOf(
            // New cards
            Card(state = State.New, stability = 0.0, difficulty = 0.0, interval = 0),
            // Learning cards
            Card(state = State.Learning, stability = 1.5, difficulty = 4.2, interval = 0),
            Card(state = State.Learning, stability = 3.2, difficulty = 2.5, interval = 2),
            // Review cards
            Card(state = State.Review, stability = 12.5, difficulty = 5.6, interval = 10),
            Card(state = State.Review, stability = 45.2, difficulty = 8.1, interval = 35),
            // Relearning cards
            Card(state = State.Relearning, stability = 0.8, difficulty = 6.0, interval = 0),
            Card(state = State.Relearning, stability = 2.1, difficulty = 5.0, interval = 1)
        )

        for (card in testCards) {
            val results = fsrs.calculate(card)

            val refCard = FlashCard(
                stability = card.stability,
                difficulty = card.difficulty,
                interval = card.interval,
                phase = when (card.state) {
                    State.New -> 0
                    State.Learning, State.Relearning -> 1
                    State.Review -> 2
                }
            )
            val refResults = refFsrs.calculate(refCard)

            // Assertions for each rating
            for (rating in Rating.values()) {
                val info = results[rating]!!
                val refInfo = refResults.find { it.choice.name == rating.name }!!

                assertEquals("Stability mismatch for card $card and rating $rating", refInfo.stability, info.stability, 0.0001)
                assertEquals("Difficulty mismatch for card $card and rating $rating", refInfo.difficulty, info.difficulty, 0.0001)
                assertEquals("Interval mismatch for card $card and rating $rating", refInfo.interval, info.interval)
                assertEquals("DurationMillis mismatch for card $card and rating $rating", refInfo.durationMillis, info.durationMillis)
            }
        }
    }

    @Test
    fun testParityWithFuzzingDeterministic() {
        // Use the same seed to test deterministic fuzzing parity
        val seed = 42L
        val fsrs = FSRS(requestRetention = 0.9, params = testParams, enableFuzz = true, seed = seed)
        val refFsrs = ReferenceFSRS(requestRetention = 0.9, params = testParams, enableFuzz = true, seed = seed)

        // Generate review test card
        val card = Card(state = State.Review, stability = 15.0, difficulty = 5.0, interval = 12)
        val refCard = FlashCard(
            stability = card.stability,
            difficulty = card.difficulty,
            interval = card.interval,
            phase = 2 // Review
        )

        val results = fsrs.calculate(card)
        val refResults = refFsrs.calculate(refCard)

        for (rating in Rating.values()) {
            val info = results[rating]!!
            val refInfo = refResults.find { it.choice.name == rating.name }!!

            assertEquals("Fuzzed interval mismatch for rating $rating", refInfo.interval, info.interval)
        }
    }

    @Test
    fun testSchedulingStatesAndLogs() {
        val fsrs = FSRS(requestRetention = 0.9, params = testParams, enableFuzz = false)
        val now = LocalDateTime.of(2026, 7, 10, 12, 0)

        // Test New Card -> Again (Learning)
        val card1 = Card(state = State.New)
        val result1 = fsrs.schedule(card1, Rating.Again, now)
        assertEquals(State.Learning, result1.card.state)
        assertEquals(0, result1.card.interval)
        assertEquals(3, result1.card.due.minute - now.minute)
        assertEquals(0, result1.log.elapsed_days)

        // Test Review Card -> Again (Relearning)
        val card2 = Card(state = State.Review, stability = 10.0, difficulty = 5.0, interval = 8, lastReview = now.minusDays(8))
        val result2 = fsrs.schedule(card2, Rating.Again, now)
        assertEquals(State.Relearning, result2.card.state)
        assertEquals(8, result2.log.elapsed_days)
        assertEquals(8, result2.log.scheduled_days)
        assertEquals(1, result2.card.lapses)

        // Test Learning Card -> Good (Review)
        val card3 = Card(state = State.Learning, stability = 2.0, difficulty = 4.0, interval = 0, lastReview = now)
        val result3 = fsrs.schedule(card3, Rating.Good, now)
        assertEquals(State.Review, result3.card.state)
        assertTrue(result3.card.interval > 0)
        assertEquals(now.plusDays(result3.card.interval.toLong()), result3.card.due)
    }
}
