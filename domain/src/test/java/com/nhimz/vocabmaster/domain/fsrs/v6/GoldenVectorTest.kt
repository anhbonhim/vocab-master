package com.nhimz.vocabmaster.domain.fsrs.v6

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

class GoldenVectorTest {

    @Serializable
    private data class InitialCard(
        val state: String,
        val step: Int? = null,
        val stability: Double? = null,
        val difficulty: Double? = null,
        val due: String,
        val last_review: String? = null
    )

    @Serializable
    private data class ReviewInfo(
        val rating: String,
        val datetime: String
    )

    @Serializable
    private data class ExpectedState(
        val state: String,
        val step: Int? = null,
        val stability: Double? = null,
        val difficulty: Double? = null,
        val due: String,
        val last_review: String? = null,
        val interval_days: Int
    )

    @Serializable
    private data class Vector(
        val id: String,
        val initial_card: InitialCard,
        val reviews: List<ReviewInfo>,
        val expected_after_each: List<ExpectedState>
    )

    @Serializable
    private data class GoldenData(
        val py_fsrs_version: String,
        val enable_fuzzing: Boolean,
        val vectors: List<Vector>
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun parseDate(isoString: String): Long {
        return OffsetDateTime.parse(isoString).toInstant().toEpochMilli()
    }

    private fun mapState(stateStr: String, step: Int?, stability: Double?, difficulty: Double?): State {
        return if (stateStr == "Learning" && step == 0 && stability == null && difficulty == null) {
            State.New
        } else {
            State.valueOf(stateStr)
        }
    }

    private fun mapRating(ratingStr: String): Rating {
        return Rating.valueOf(ratingStr)
    }

    @Test
    fun testGoldenVectors() {
        val inputStream = javaClass.classLoader.getResourceAsStream("fsrs/golden_vectors.json")
            ?: error("golden_vectors.json not found")
        val jsonStr = inputStream.bufferedReader().use { it.readText() }
        val data = json.decodeFromString<GoldenData>(jsonStr)

        val vectors = data.vectors
        assert(vectors.size >= 30) { "Vectors count should be >= 30, but was ${vectors.size}" }

        val scheduler = Scheduler(enableFuzzing = false)

        for (vector in vectors) {
            val initial = vector.initial_card
            var card = Card(
                cardId = vector.id,
                state = mapState(initial.state, initial.step, initial.stability, initial.difficulty),
                step = initial.step,
                stability = initial.stability,
                difficulty = initial.difficulty,
                due = parseDate(initial.due),
                lastReview = initial.last_review?.let { parseDate(it) }
            )

            for (i in vector.reviews.indices) {
                val review = vector.reviews[i]
                val expected = vector.expected_after_each[i]

                val result = scheduler.reviewCard(
                    card = card,
                    rating = mapRating(review.rating),
                    reviewDatetimeMillis = parseDate(review.datetime)
                )
                card = result.first

                val expectedState = mapState(expected.state, expected.step, expected.stability, expected.difficulty)
                assertEquals("Vector ${vector.id} (review $i) state", expectedState, card.state)
                assertEquals("Vector ${vector.id} (review $i) step", expected.step, card.step)
                
                if (expected.stability != null) {
                    assertEquals("Vector ${vector.id} (review $i) stability", expected.stability, card.stability!!, 1e-6)
                } else {
                    assertEquals("Vector ${vector.id} (review $i) stability", null, card.stability)
                }

                if (expected.difficulty != null) {
                    assertEquals("Vector ${vector.id} (review $i) difficulty", expected.difficulty, card.difficulty!!, 1e-6)
                } else {
                    assertEquals("Vector ${vector.id} (review $i) difficulty", null, card.difficulty)
                }

                assertEquals("Vector ${vector.id} (review $i) due", parseDate(expected.due), card.due)
                assertEquals("Vector ${vector.id} (review $i) lastReview", expected.last_review?.let { parseDate(it) }, card.lastReview)

                val intervalDays = if (card.lastReview != null) {
                    ((card.due - card.lastReview) / 86_400_000L).toInt()
                } else 0
                assertEquals("Vector ${vector.id} (review $i) interval_days", expected.interval_days, intervalDays)
            }
        }
    }
}
