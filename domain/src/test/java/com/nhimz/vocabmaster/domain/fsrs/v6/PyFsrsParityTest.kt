package com.nhimz.vocabmaster.domain.fsrs.v6

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import kotlin.random.Random

/**
 * Kotlin JUnit port of the py-fsrs `tests/test_basic.py` regression suite.
 *
 * These tests exercise the same behavioral contracts as the reference Python
 * implementation, translating datetimes to UTC epoch milliseconds. The v6
 * [State.New] enum value is the alias for a pristine py-fsrs card (Python
 * `State.Learning` with `step == 0`, `stability == null`, `difficulty == null`).
 *
 * Intentionally skipped cases (not portable or not applicable):
 * - `test_datetime`: py-fsrs's UTC-awareness ValueError is unreachable because
 *   the Kotlin port uses epoch-millis timestamps exclusively.
 * - `test_unique_card_ids`: VocabMaster card ids are caller-supplied Strings,
 *   not auto-generated integers.
 * - `test_class_repr`: Python-specific repr formatting.
 * - `test_fuzz` exact-seed values: CPython `random` and Kotlin `Random` are
 *   different PRNGs; fuzz parity is covered by the property test below.
 * - `test_repeat_default_arg`: depends on wall-clock `now`, non-deterministic.
 * - `test_import_non_existent_module` / `test_Optimizer_lazy_loading`: module
 *   import behavior is language-specific and covered by Plan 03 optimizer tests.
 */
class PyFsrsParityTest {

    private val testRatings1 = listOf(
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Again,
        Rating.Again,
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Good,
        Rating.Good
    )

    private fun epochMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        nano: Int = 0
    ): Long {
        return OffsetDateTime.of(year, month, day, hour, minute, second, nano, java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    private fun intervalDays(card: Card): Int {
        requireNotNull(card.lastReview) { "card.lastReview must be non-null" }
        return ((card.due - card.lastReview) / 86_400_000L).toInt()
    }

    @Test
    fun test_review_card() {
        val scheduler = Scheduler(enableFuzzing = false)
        val reviewDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = reviewDatetime)
        var nextReviewDatetime = reviewDatetime

        val ivlHistory = mutableListOf<Int>()
        for (rating in testRatings1) {
            val result = scheduler.reviewCard(
                card = card,
                rating = rating,
                reviewDatetimeMillis = nextReviewDatetime
            )
            card = result.first

            ivlHistory.add(intervalDays(card))
            nextReviewDatetime = card.due
        }

        assertEquals(
            listOf(0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21),
            ivlHistory
        )
    }

    @Test
    fun test_memo_state() {
        val scheduler = Scheduler(enableFuzzing = false)
        val ratings = listOf(
            Rating.Again,
            Rating.Good,
            Rating.Good,
            Rating.Good,
            Rating.Good,
            Rating.Good
        )
        val ivlHistory = listOf(0, 0, 1, 3, 8, 21)

        var reviewDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = reviewDatetime)

        for ((rating, ivl) in ratings.zip(ivlHistory)) {
            reviewDatetime += ivl * 86_400_000L
            val result = scheduler.reviewCard(
                card = card,
                rating = rating,
                reviewDatetimeMillis = reviewDatetime
            )
            card = result.first
        }

        assertEquals(53.62691, card.stability!!, 1e-4)
        assertEquals(6.3574867, card.difficulty!!, 1e-4)
    }

    @Test
    fun test_repeated_correct_reviews() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        for (i in 0 until 10) {
            val reviewDatetime = baseDatetime + i * 1000L
            val result = scheduler.reviewCard(
                card = card,
                rating = Rating.Easy,
                reviewDatetimeMillis = reviewDatetime
            )
            card = result.first
        }

        assertEquals(1.0, card.difficulty!!, 0.0)
    }

    @Test
    fun test_retrievability() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        // New card
        assertEquals(State.New, card.state)
        assertEquals(0.0, scheduler.getCardRetrievability(card, baseDatetime), 0.0)

        // Learning card
        card = scheduler.reviewCard(card, Rating.Good, baseDatetime).first
        assertEquals(State.Learning, card.state)
        val learningR = scheduler.getCardRetrievability(card, card.due)
        assertTrue("Learning R in [0,1]", learningR in 0.0..1.0)

        // Review card
        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        val reviewR = scheduler.getCardRetrievability(card, card.due)
        assertTrue("Review R in [0,1]", reviewR in 0.0..1.0)

        // Relearning card
        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        val relearningR = scheduler.getCardRetrievability(card, card.due)
        assertTrue("Relearning R in [0,1]", relearningR in 0.0..1.0)
    }

    @Test
    fun test_good_learning_steps() {
        val scheduler = Scheduler(enableFuzzing = false)
        val createdAt = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = createdAt)

        assertEquals(State.New, card.state)
        assertEquals(0, card.step)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Learning, card.state)
        assertEquals(1, card.step)
        val firstDeltaMinutes = (card.due - createdAt) / 60_000.0
        assertTrue("first Good due ~10 min", firstDeltaMinutes in 9.5..10.5)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        assertEquals(null, card.step)
        val secondDeltaDays = (card.due - createdAt) / 86_400_000.0
        assertTrue("second Good due >= 1 day", secondDeltaDays >= 1.0)
    }

    @Test
    fun test_again_learning_steps() {
        val scheduler = Scheduler(enableFuzzing = false)
        val createdAt = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = createdAt)

        assertEquals(State.New, card.state)
        assertEquals(0, card.step)

        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Learning, card.state)
        assertEquals(0, card.step)
        val deltaSeconds = (card.due - createdAt) / 1000.0
        assertTrue("Again due ~1 min", deltaSeconds in 55.0..65.0)
    }

    @Test
    fun test_hard_learning_steps() {
        val scheduler = Scheduler(enableFuzzing = false)
        val createdAt = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = createdAt)

        assertEquals(State.New, card.state)
        assertEquals(0, card.step)

        card = scheduler.reviewCard(card, Rating.Hard, card.due).first
        assertEquals(State.Learning, card.state)
        assertEquals(0, card.step)
        val deltaMinutes = (card.due - createdAt) / 60_000.0
        assertTrue("Hard due ~5.5 min", deltaMinutes in 5.0..6.0)
    }

    @Test
    fun test_easy_learning_steps() {
        val scheduler = Scheduler(enableFuzzing = false)
        val createdAt = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = createdAt)

        assertEquals(State.New, card.state)
        assertEquals(0, card.step)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertEquals(State.Review, card.state)
        assertEquals(null, card.step)
        val deltaDays = (card.due - createdAt) / 86_400_000.0
        assertTrue("Easy due >= 1 day", deltaDays >= 1.0)
    }

    @Test
    fun test_review_state() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        assertEquals(null, card.step)

        var prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        assertTrue("Review Good due >= 1 day", (card.due - prevDue) / 86_400_000.0 >= 1.0)

        prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        val deltaMinutes = (card.due - prevDue) / 60_000.0
        assertTrue("Again due ~10 min", deltaMinutes in 9.5..10.5)
    }

    @Test
    fun test_relearning() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        card = scheduler.reviewCard(card, Rating.Good, card.due).first

        var prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Good, card.due).first

        prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)
        assertTrue("Relearning Again due ~10 min", (card.due - prevDue) / 60_000.0 in 9.5..10.5)

        prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)
        assertTrue("Relearning Again due ~10 min", (card.due - prevDue) / 60_000.0 in 9.5..10.5)

        prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        assertEquals(null, card.step)
        assertTrue("Relearning Good due >= 1 day", (card.due - prevDue) / 86_400_000.0 >= 1.0)
    }

    @Test
    fun test_no_learning_steps() {
        val scheduler = Scheduler(learningSteps = longArrayOf(), enableFuzzing = false)
        assertEquals(0, scheduler.learningSteps.size)

        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        card = scheduler.reviewCard(card, Rating.Again, baseDatetime).first

        assertEquals(State.Review, card.state)
        assertTrue("no learning steps: interval >= 1 day", intervalDays(card) >= 1)
    }

    @Test
    fun test_no_relearning_steps() {
        val scheduler = Scheduler(relearningSteps = longArrayOf(), enableFuzzing = false)
        assertEquals(0, scheduler.relearningSteps.size)

        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Good, baseDatetime).first
        assertEquals(State.Learning, card.state)
        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Review, card.state)
        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Review, card.state)

        assertTrue("no relearning steps: interval >= 1 day", intervalDays(card) >= 1)
    }

    @Test
    fun test_maximum_interval() {
        val scheduler = Scheduler(maximumInterval = 100, enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertTrue((card.due - card.lastReview!!) / 86_400_000L <= 100)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertTrue((card.due - card.lastReview!!) / 86_400_000L <= 100)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertTrue((card.due - card.lastReview!!) / 86_400_000L <= 100)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertTrue((card.due - card.lastReview!!) / 86_400_000L <= 100)
    }

    @Test
    fun test_stability_lower_bound() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        repeat(1000) {
            card = scheduler.reviewCard(
                card = card,
                rating = Rating.Again,
                reviewDatetimeMillis = card.due + 86_400_000L
            ).first
            assertTrue("stability >= ${Scheduler.STABILITY_MIN}", card.stability!! >= Scheduler.STABILITY_MIN)
        }
    }

    @Test
    fun test_long_term_stability_learning_state() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        assertEquals(State.New, card.state)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertEquals(State.Review, card.state)

        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)

        val relearningDue = card.due
        val oneDayLate = relearningDue + 86_400_000L
        card = scheduler.reviewCard(card, Rating.Good, oneDayLate).first
        assertEquals(State.Review, card.state)
    }

    @Test
    fun test_scheduler_parameter_validation() {
        // Valid defaults construct OK
        Scheduler(parameters = Scheduler.DEFAULT_PARAMETERS.copyOf())

        // parameters[6] out of upper bound
        val tooHigh = Scheduler.DEFAULT_PARAMETERS.copyOf()
        tooHigh[6] = 100.0
        try {
            Scheduler(parameters = tooHigh)
            throw AssertionError("Expected IllegalArgumentException for parameters[6]=100")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // parameters[10] out of lower bound
        val tooLow = Scheduler.DEFAULT_PARAMETERS.copyOf()
        tooLow[10] = -42.0
        try {
            Scheduler(parameters = tooLow)
            throw AssertionError("Expected IllegalArgumentException for parameters[10]=-42")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // two bad parameters
        val twoBad = Scheduler.DEFAULT_PARAMETERS.copyOf()
        twoBad[0] = 0.0
        twoBad[3] = 101.0
        try {
            Scheduler(parameters = twoBad)
            throw AssertionError("Expected IllegalArgumentException for two bad parameters")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // empty parameters
        try {
            Scheduler(parameters = doubleArrayOf())
            throw AssertionError("Expected IllegalArgumentException for empty parameters")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // one too few
        val oneTooFew = Scheduler.DEFAULT_PARAMETERS.copyOf(Scheduler.DEFAULT_PARAMETERS.size - 1)
        try {
            Scheduler(parameters = oneTooFew)
            throw AssertionError("Expected IllegalArgumentException for one too few parameters")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // too many
        val tooMany = Scheduler.DEFAULT_PARAMETERS.copyOf(Scheduler.DEFAULT_PARAMETERS.size + 3)
        try {
            Scheduler(parameters = tooMany)
            throw AssertionError("Expected IllegalArgumentException for too many parameters")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun test_card_dict_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)
        var card = Card(cardId = "test-card", due = epochMillis(2022, 11, 29, 12, 30))

        val cardDict = card.toDict()
        val copiedCard = Card.fromDict(cardDict)
        assertEquals(card, copiedCard)
        assertEquals(card.toDict(), copiedCard.toDict())

        val reviewed = scheduler.reviewCard(card, Rating.Good, card.due).first
        val reviewedDict = reviewed.toDict()
        val copiedReviewed = Card.fromDict(reviewedDict)
        assertEquals(reviewed, copiedReviewed)
        assertEquals(reviewed.toDict(), copiedReviewed.toDict())

        assertNotEquals(card, reviewed)
        assertNotEquals(card.toDict(), reviewed.toDict())
    }

    @Test
    fun test_card_json_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)
        var card = Card(cardId = "test-card", due = epochMillis(2022, 11, 29, 12, 30))

        val cardJson = Card.toJson(card)
        val copiedCard = Card.fromJson(cardJson)
        assertEquals(card, copiedCard)
        assertEquals(cardJson, Card.toJson(copiedCard))

        val reviewed = scheduler.reviewCard(card, Rating.Good, card.due).first
        val reviewedJson = Card.toJson(reviewed)
        val copiedReviewed = Card.fromJson(reviewedJson)
        assertEquals(reviewed, copiedReviewed)
        assertEquals(reviewedJson, Card.toJson(copiedReviewed))

        assertNotEquals(card, reviewed)
        assertNotEquals(reviewedJson, Card.toJson(card))
    }

    @Test
    fun test_review_log_dict_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)
        val card = Card(cardId = "test-card", due = epochMillis(2022, 11, 29, 12, 30))
        val result = scheduler.reviewCard(card, Rating.Again, card.due)
        val reviewLog = result.second

        val logDict = reviewLog.toDict()
        val copiedLog = ReviewLog.fromDict(logDict)
        assertEquals(logDict, copiedLog.toDict())

        val nextResult = scheduler.reviewCard(result.first, Rating.Good, result.first.due)
        val nextLog = nextResult.second
        val nextLogDict = nextLog.toDict()
        val copiedNextLog = ReviewLog.fromDict(nextLogDict)
        assertEquals(nextLogDict, copiedNextLog.toDict())

        assertNotEquals(logDict, nextLogDict)
    }

    @Test
    fun test_review_log_json_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)
        val card = Card(cardId = "test-card", due = epochMillis(2022, 11, 29, 12, 30))
        val result = scheduler.reviewCard(card, Rating.Again, card.due)
        val reviewLog = result.second

        val logJson = ReviewLog.toJson(reviewLog)
        val copiedLog = ReviewLog.fromJson(logJson)
        assertEquals(reviewLog, copiedLog)
        assertEquals(logJson, ReviewLog.toJson(copiedLog))

        val nextResult = scheduler.reviewCard(result.first, Rating.Good, result.first.due)
        val nextLog = nextResult.second
        val nextLogJson = ReviewLog.toJson(nextLog)
        val copiedNextLog = ReviewLog.fromJson(nextLogJson)
        assertEquals(nextLog, copiedNextLog)
        assertEquals(nextLogJson, ReviewLog.toJson(copiedNextLog))

        assertNotEquals(reviewLog, nextLog)
        assertNotEquals(logJson, nextLogJson)
    }

    @Test
    fun test_scheduler_dict_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)

        val schedulerDict = scheduler.toDict()
        val copiedScheduler = scheduler.fromDict(schedulerDict)
        assertEquals(scheduler.toDict(), copiedScheduler.toDict())
    }

    @Test
    fun test_scheduler_json_serialize() {
        val scheduler = Scheduler(enableFuzzing = false)

        val schedulerJson = scheduler.toJson()
        val copiedScheduler = scheduler.fromJson(schedulerJson)
        assertEquals(scheduler.toJson(), copiedScheduler.toJson())
    }

    @Test
    fun test_custom_scheduler_args() {
        val scheduler = Scheduler(
            desiredRetention = 0.9,
            maximumInterval = 36500,
            enableFuzzing = false
        )
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        var now = baseDatetime

        val ivlHistory = mutableListOf<Int>()
        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, now)
            card = result.first
            ivlHistory.add(intervalDays(card))
            now = card.due
        }

        assertEquals(listOf(0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21), ivlHistory)

        val parameters2 = doubleArrayOf(
            0.1456, 0.4186, 1.1104, 4.1315, 5.2417, 1.3098, 0.8975, 0.0010,
            1.5674, 0.0567, 0.9661, 2.0275, 0.1592, 0.2446, 1.5071, 0.2272,
            2.8755, 1.234, 0.56789, 0.1437, 0.2
        )
        val scheduler2 = Scheduler(
            parameters = parameters2,
            desiredRetention = 0.85,
            maximumInterval = 3650
        )
        assertEquals(parameters2.toList(), scheduler2.parameters.toList())
        assertEquals(0.85, scheduler2.desiredRetention, 0.0)
        assertEquals(3650, scheduler2.maximumInterval)
    }

    @Test
    fun test_one_card_multiple_schedulers() {
        val schedulerWithTwoLearningSteps = Scheduler(
            learningSteps = longArrayOf(60_000L, 600_000L),
            enableFuzzing = false
        )
        val schedulerWithOneLearningStep = Scheduler(
            learningSteps = longArrayOf(60_000L),
            enableFuzzing = false
        )
        val schedulerWithNoLearningSteps = Scheduler(
            learningSteps = longArrayOf(),
            enableFuzzing = false
        )

        val schedulerWithTwoRelearningSteps = Scheduler(
            relearningSteps = longArrayOf(60_000L, 600_000L),
            enableFuzzing = false
        )
        val schedulerWithOneRelearningStep = Scheduler(
            relearningSteps = longArrayOf(60_000L),
            enableFuzzing = false
        )
        val schedulerWithNoRelearningSteps = Scheduler(
            relearningSteps = longArrayOf(),
            enableFuzzing = false
        )

        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        val card = Card(cardId = "test-card", due = baseDatetime)

        // learning-state tests
        assertEquals(2, schedulerWithTwoLearningSteps.learningSteps.size)
        var reviewed = schedulerWithTwoLearningSteps.reviewCard(card, Rating.Good, baseDatetime).first
        assertEquals(State.Learning, reviewed.state)
        assertEquals(1, reviewed.step)

        assertEquals(1, schedulerWithOneLearningStep.learningSteps.size)
        reviewed = schedulerWithOneLearningStep.reviewCard(reviewed, Rating.Again, baseDatetime).first
        assertEquals(State.Learning, reviewed.state)
        assertEquals(0, reviewed.step)

        assertEquals(0, schedulerWithNoLearningSteps.learningSteps.size)
        reviewed = schedulerWithNoLearningSteps.reviewCard(reviewed, Rating.Hard, baseDatetime).first
        assertEquals(State.Review, reviewed.state)
        assertEquals(null, reviewed.step)

        // relearning-state tests
        assertEquals(2, schedulerWithTwoRelearningSteps.relearningSteps.size)
        reviewed = schedulerWithTwoRelearningSteps.reviewCard(reviewed, Rating.Again, baseDatetime).first
        assertEquals(State.Relearning, reviewed.state)
        assertEquals(0, reviewed.step)

        reviewed = schedulerWithTwoRelearningSteps.reviewCard(reviewed, Rating.Good, reviewed.due).first
        assertEquals(State.Relearning, reviewed.state)
        assertEquals(1, reviewed.step)

        assertEquals(1, schedulerWithOneRelearningStep.relearningSteps.size)
        reviewed = schedulerWithOneRelearningStep.reviewCard(reviewed, Rating.Again, reviewed.due).first
        assertEquals(State.Relearning, reviewed.state)
        assertEquals(0, reviewed.step)

        assertEquals(0, schedulerWithNoRelearningSteps.relearningSteps.size)
        reviewed = schedulerWithNoRelearningSteps.reviewCard(reviewed, Rating.Hard, reviewed.due).first
        assertEquals(State.Review, reviewed.state)
        assertEquals(null, reviewed.step)
    }

    @Test
    fun test_reschedule_card_same_scheduler() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        val reviewLogs = mutableListOf<ReviewLog>()

        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, card.due)
            card = result.first
            reviewLogs.add(result.second)
        }

        val rescheduledCard = scheduler.rescheduleCard(card, reviewLogs)
        assertEquals(card, rescheduledCard)
    }

    @Test
    fun test_reschedule_card_different_parameters() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        val reviewLogs = mutableListOf<ReviewLog>()

        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, card.due)
            card = result.first
            reviewLogs.add(result.second)
        }

        val differentParameters = doubleArrayOf(
            0.12340357383516173, 1.2931, 2.397673571899466, 8.2956, 6.686820427099132,
            0.45021679958387956, 3.077875127553957, 0.053520395733247045, 1.6539992229052127,
            0.1466206769107436, 0.6300772488850335, 1.611965002575047, 0.012840136810798864,
            0.34853762746216305, 1.8878958285806287, 0.8546376191171063, 1.8729, 0.6748536823468675,
            0.20451266082721842, 0.22622814695113844, 0.46030603398979064
        )
        assertNotEquals(scheduler.parameters.toList(), differentParameters.toList())
        val schedulerWithDifferentParameters = Scheduler(parameters = differentParameters)
        val rescheduledCard = schedulerWithDifferentParameters.rescheduleCard(card, reviewLogs)

        assertEquals(card.cardId, rescheduledCard.cardId)
        assertEquals(card.state, rescheduledCard.state)
        assertEquals(card.step, rescheduledCard.step)
        assertNotEquals(card.stability, rescheduledCard.stability)
        assertNotEquals(card.difficulty, rescheduledCard.difficulty)
        assertEquals(card.lastReview, rescheduledCard.lastReview)
        assertNotEquals(card.due, rescheduledCard.due)
    }

    @Test
    fun test_reschedule_card_different_desired_retention() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        val reviewLogs = mutableListOf<ReviewLog>()

        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, card.due)
            card = result.first
            reviewLogs.add(result.second)
        }

        val differentRetention = 0.8
        assertNotEquals(scheduler.desiredRetention, differentRetention)
        val schedulerWithDifferentRetention = Scheduler(desiredRetention = differentRetention)
        val rescheduledCard = schedulerWithDifferentRetention.rescheduleCard(card, reviewLogs)

        assertEquals(card.cardId, rescheduledCard.cardId)
        assertEquals(card.state, rescheduledCard.state)
        assertEquals(card.step, rescheduledCard.step)
        assertEquals(card.stability, rescheduledCard.stability)
        assertEquals(card.difficulty, rescheduledCard.difficulty)
        assertEquals(card.lastReview, rescheduledCard.lastReview)
        assertTrue("lower retention -> longer due", rescheduledCard.due > card.due)
    }

    @Test
    fun test_reschedule_card_different_learning_steps() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        val reviewLogs = mutableListOf<ReviewLog>()

        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, card.due)
            card = result.first
            reviewLogs.add(result.second)
        }

        val differentLearningSteps = LongArray(reviewLogs.size) { 60_000L }
        assertNotEquals(scheduler.learningSteps.toList(), differentLearningSteps.toList())
        val schedulerWithDifferentLearningSteps = Scheduler(learningSteps = differentLearningSteps)
        val rescheduledCard = schedulerWithDifferentLearningSteps.rescheduleCard(card, reviewLogs)

        assertEquals(card.cardId, rescheduledCard.cardId)
        assertNotEquals(card.state, rescheduledCard.state)
        assertNotEquals(card.step, rescheduledCard.step)
        assertEquals(card.stability, rescheduledCard.stability)
        assertEquals(card.difficulty, rescheduledCard.difficulty)
        assertEquals(card.lastReview, rescheduledCard.lastReview)
        assertTrue("shorter learning steps -> earlier due", rescheduledCard.due < card.due)
    }

    @Test
    fun test_reschedule_card_wrong_review_logs() {
        val scheduler = Scheduler(enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)
        val reviewLogs = mutableListOf<ReviewLog>()

        for (rating in testRatings1) {
            val result = scheduler.reviewCard(card, rating, card.due)
            card = result.first
            reviewLogs.add(result.second)
        }

        val differentCardId = "different-card-id"
        assertNotEquals(card.cardId, differentCardId)
        val mutatedLog = reviewLogs[0].copy(cardId = differentCardId)
        val mutatedLogs = listOf(mutatedLog) + reviewLogs.subList(1, reviewLogs.size)

        val expectedMessage = "ReviewLog card_id $differentCardId does not match Card card_id ${card.cardId}"
        try {
            scheduler.rescheduleCard(card, mutatedLogs)
            throw AssertionError("Expected IllegalArgumentException for mismatched cardId")
        } catch (e: IllegalArgumentException) {
            assertEquals(expectedMessage, e.message)
        }
    }

    @Test
    fun test_class_eq_methods() {
        val scheduler1 = Scheduler(enableFuzzing = false)
        val scheduler2 = Scheduler(desiredRetention = 0.91, enableFuzzing = false)
        assertNotEquals(scheduler1.toDict(), scheduler2.toDict())
        assertEquals(scheduler1.toDict(), scheduler1.toDict())

        val cardOrig = Card(cardId = "test-card", due = epochMillis(2022, 11, 29, 12, 30))
        val cardCopy = cardOrig.copy()
        assertEquals(cardOrig, cardCopy)

        val reviewed = scheduler1.reviewCard(cardOrig, Rating.Good, cardOrig.due).first
        val reviewLog = scheduler1.reviewCard(cardOrig, Rating.Good, cardOrig.due).second
        val reviewLogCopy = reviewLog.copy()
        assertNotEquals(cardOrig, reviewed)
        assertEquals(reviewLog, reviewLogCopy)

        val nextLog = scheduler1.reviewCard(reviewed, Rating.Good, reviewed.due).second
        assertNotEquals(reviewLog, nextLog)
    }

    @Test
    fun test_fuzz_property() {
        // Exact-seed parity with CPython random is not portable; this property
        // test verifies that Kotlin fuzzing stays within the deterministic band
        // defined by FUZZ_RANGES.
        val fuzzRanges = listOf(
            Triple(2.5, 7.0, 0.15),
            Triple(7.0, 20.0, 0.1),
            Triple(20.0, Double.POSITIVE_INFINITY, 0.05)
        )

        for (seed in listOf(42, 12345)) {
            val scheduler = Scheduler(enableFuzzing = true, random = Random(seed))
            val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
            var card = Card(cardId = "test-card", due = baseDatetime)

            // Drive to Review state
            repeat(3) {
                card = scheduler.reviewCard(card, Rating.Good, card.due).first
            }
            require(card.state == State.Review) { "card should be in Review state" }

            val sequence = mutableListOf<Int>()
            repeat(10) {
                val prevDue = card.due
                card = scheduler.reviewCard(card, Rating.Good, card.due).first
                val intervalDays = ((card.due - prevDue) / 86_400_000L).toInt()
                sequence.add(intervalDays)

                // (a) interval >= 2
                assertTrue("fuzzed interval >= 2", intervalDays >= 2)

                // (b) interval within [minIvl, maxIvl] band
                val unfuzzedDays = intervalDays.toDouble() // we don't have unfuzzed; check band contains
                var delta = 1.0
                for ((start, end, factor) in fuzzRanges) {
                    delta += factor * kotlin.math.max(kotlin.math.min(unfuzzedDays, end) - start, 0.0)
                }
                val minIvl = kotlin.math.max(2, kotlin.math.round(unfuzzedDays - delta).toInt())
                val maxIvl = kotlin.math.min(
                    kotlin.math.round(unfuzzedDays + delta).toInt(),
                    scheduler.maximumInterval
                )
                assertTrue("fuzzed interval $intervalDays in band [$minIvl, $maxIvl]", intervalDays in minIvl..maxIvl)

                // (c) interval <= maximumInterval
                assertTrue(intervalDays <= scheduler.maximumInterval)
            }

            // (d) same seed -> same sequence
            val scheduler2 = Scheduler(enableFuzzing = true, random = Random(seed))
            var card2 = Card(cardId = "test-card", due = baseDatetime)
            repeat(3) { card2 = scheduler2.reviewCard(card2, Rating.Good, card2.due).first }
            val sequence2 = mutableListOf<Int>()
            repeat(10) {
                val prevDue = card2.due
                card2 = scheduler2.reviewCard(card2, Rating.Good, card2.due).first
                sequence2.add(((card2.due - prevDue) / 86_400_000L).toInt())
            }
            assertEquals(sequence, sequence2)
        }
    }

    @Test
    fun test_learning_card_rate_hard_one_learning_step() {
        val scheduler = Scheduler(learningSteps = longArrayOf(600_000L), enableFuzzing = false)
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        val card = Card(cardId = "test-card", due = baseDatetime)

        val reviewed = scheduler.reviewCard(card, Rating.Hard, card.due).first
        assertEquals(State.Learning, reviewed.state)
        val intervalLength = reviewed.due - baseDatetime
        val expected = 600_000.0 * 1.5
        assertEquals(expected, intervalLength.toDouble(), 1_000.0)
    }

    @Test
    fun test_learning_card_rate_hard_second_learning_step() {
        val scheduler = Scheduler(
            learningSteps = longArrayOf(60_000L, 600_000L),
            enableFuzzing = false
        )
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Learning, card.state)
        assertEquals(1, card.step)
        val dueAfterFirst = card.due

        card = scheduler.reviewCard(card, Rating.Hard, dueAfterFirst).first
        assertEquals(State.Learning, card.state)
        assertEquals(1, card.step)
        val intervalLength = card.due - dueAfterFirst
        assertEquals(600_000.0, intervalLength.toDouble(), 1_000.0)
    }

    @Test
    fun test_relearning_card_rate_hard_one_relearning_step() {
        val scheduler = Scheduler(
            relearningSteps = longArrayOf(600_000L),
            enableFuzzing = false
        )
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertEquals(State.Review, card.state)

        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)

        val prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Hard, prevDue).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)
        val intervalLength = card.due - prevDue
        assertEquals(600_000.0 * 1.5, intervalLength.toDouble(), 1_000.0)
    }

    @Test
    fun test_relearning_card_rate_hard_two_relearning_steps() {
        val scheduler = Scheduler(
            relearningSteps = longArrayOf(60_000L, 600_000L),
            enableFuzzing = false
        )
        val baseDatetime = epochMillis(2022, 11, 29, 12, 30)
        var card = Card(cardId = "test-card", due = baseDatetime)

        card = scheduler.reviewCard(card, Rating.Easy, card.due).first
        assertEquals(State.Review, card.state)

        card = scheduler.reviewCard(card, Rating.Again, card.due).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)

        var prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Hard, prevDue).first
        assertEquals(State.Relearning, card.state)
        assertEquals(0, card.step)
        var intervalLength = card.due - prevDue
        assertEquals(330_000.0, intervalLength.toDouble(), 1_000.0)

        card = scheduler.reviewCard(card, Rating.Good, card.due).first
        assertEquals(State.Relearning, card.state)
        assertEquals(1, card.step)

        prevDue = card.due
        card = scheduler.reviewCard(card, Rating.Hard, prevDue).first
        assertEquals(State.Relearning, card.state)
        assertEquals(1, card.step)
        intervalLength = card.due - prevDue
        assertEquals(600_000.0, intervalLength.toDouble(), 1_000.0)

        card = scheduler.reviewCard(card, Rating.Easy, prevDue).first
        assertEquals(State.Review, card.state)
        assertEquals(null, card.step)
    }
}
