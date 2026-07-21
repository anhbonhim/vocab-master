@file:Suppress("MagicNumber", "NestedBlockDepth", "ComplexCondition")
package com.nhimz.vocabmaster.domain.fsrs.v6

import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * FSRS-6 optimizer ported from py-fsrs 6.3.1.
 *
 * Computes optimal FSRS parameters from review logs using the same loss
 * (BCE on predicted retrievability vs recall), hyperparameters, and bounds
 * clamping as py-fsrs.
 *
 * Deliberate deviations from py-fsrs (all required because torch/pandas/tqdm
 * have no Kotlin equivalents):
 *
 * 1. **Finite-difference gradients** replace torch autograd. For each parameter
 *    the gradient is approximated with central differences:
 *    `grad_i = (loss(w + h_i*e_i) - loss(w - h_i*e_i)) / (2*h_i)`
 *    where `h_i = 1e-4 * max(|w_i|, 0.01)`. The rest of the training loop
 *    (Adam, cosine annealing, mini-batching, bounds clamping, best-by-epoch
 *    selection) follows py-fsrs exactly.
 *
 * 2. **No torch tensor detachment semantics.** After each mini-batch gradient
 *    step py-fsrs detaches the in-flight card's stability/difficulty from the
 *    graph; with numerical gradients this is a no-op — the [Scheduler] is
 *    recreated with updated parameters after each step.
 *
 * 3. **Shuffle RNG:** py-fsrs uses `random.Random(42)`; Kotlin uses
 *    `kotlin.random.Random(42)`. Shuffle order differs, so training trajectories
 *    are not bit-identical. Tests assert properties (defaults guard, bounds,
 *    loss decrease) rather than exact trained values.
 *
 * 4. **No pandas/tqdm:** `_compute_probs_and_costs` uses plain Kotlin maps and
 *    lists; there is no progress bar.
 *
 * 5. **computeOptimalRetention return type:** py-fsrs declares `list[float]` but
 *    returns a single float; this port returns [Double].
 */
@Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "LongParameterList")
class Optimizer @Inject constructor(reviewLogs: List<ReviewLog>) {

    private val reviewLogs: List<ReviewLog> = reviewLogs.map { it.copy() }
    private val revlogsTrain: Map<String, List<TrainDatum>> = formatRevlogs()

    /**
     * Internal training datum. [recall] is 0 for [Rating.Again], 1 otherwise.
     */
    private data class TrainDatum(
        val reviewDatetimeMillis: Long,
        val rating: Rating,
        val reviewDuration: Long?,
        val recall: Int
    )

    /**
     * Entry kept for a mini-batch so it can be replayed with perturbed parameters
     * during finite-difference gradient estimation.
     */
    private data class MiniBatchEntry(
        val cardId: String,
        val reviewDatetimeMillis: Long,
        val rating: Rating,
        val recall: Int
    )

    init {
        require(reviewLogs.isEmpty() || reviewLogs.all { it.reviewDatetime >= 0L }) {
            "ReviewLog reviewDatetime must be non-negative."
        }
    }

    private fun formatRevlogs(): Map<String, List<TrainDatum>> {
        val grouped = mutableMapOf<String, MutableList<TrainDatum>>()
        for (log in reviewLogs) {
            val datum = TrainDatum(
                reviewDatetimeMillis = log.reviewDatetime,
                rating = log.rating,
                reviewDuration = log.reviewDuration,
                recall = if (log.rating == Rating.Again) 0 else 1
            )
            grouped.getOrPut(log.cardId) { mutableListOf() }.add(datum)
        }
        for (entries in grouped.values) {
            entries.sortBy { it.reviewDatetimeMillis }
        }
        return grouped.toList().sortedBy { it.first }.toMap()
    }

    /**
     * Computes the mean BCE loss over the full training set for [parameters].
     */
    fun computeBatchLoss(parameters: DoubleArray): Double {
        val scheduler = Scheduler(parameters = parameters.copyOf(), enableFuzzing = false)
        var totalLoss = 0.0
        var count = 0

        for ((cardId, history) in revlogsTrain) {
            var card = Card(cardId = cardId, due = history.first().reviewDatetimeMillis)
            for (datum in history.take(MAX_SEQ_LEN)) {
                val p = scheduler.getCardRetrievability(card, datum.reviewDatetimeMillis)
                val lastReview = card.lastReview
                if (lastReview != null && (datum.reviewDatetimeMillis - lastReview) / MILLIS_PER_DAY > 0) {
                    totalLoss += bce(p, datum.recall.toDouble())
                    count++
                }
                card = scheduler.reviewCard(card, datum.rating, datum.reviewDatetimeMillis).first
            }
        }

        return if (count == 0) 0.0 else totalLoss / count
    }

    /**
     * Computes optimal FSRS parameters from the review logs.
     * Returns [Scheduler.DEFAULT_PARAMETERS] unchanged when fewer than 512
     * non-same-day Review-state reviews are present (py-fsrs early-return guard).
     */
    fun computeOptimalParameters(): DoubleArray {
        val numReviews = countReviewStateReviews()
        if (numReviews < MIN_REVIEWS_FOR_TRAINING) {
            return Scheduler.DEFAULT_PARAMETERS.copyOf()
        }

        val parameters = Scheduler.DEFAULT_PARAMETERS.copyOf()
        val rng = Random(RNG_SEED)
        val cardIds = revlogsTrain.keys.toMutableList()

        val tMax = kotlin.math.ceil(numReviews / MINI_BATCH_SIZE.toDouble()).toInt() * NUM_EPOCHS

        val m = DoubleArray(parameters.size) { 0.0 }
        val v = DoubleArray(parameters.size) { 0.0 }
        var step = 0

        var bestParams = parameters.copyOf()
        var bestLoss = Double.POSITIVE_INFINITY

        repeat(NUM_EPOCHS) {
            cardIds.shuffle(rng)
            val scheduler = Scheduler(parameters = parameters.copyOf(), enableFuzzing = false)
            val cardStates = mutableMapOf<String, Card>()
            val batchEntries = mutableListOf<MiniBatchEntry>()

            for (cardId in cardIds) {
                val history = revlogsTrain[cardId] ?: continue
                for ((index, datum) in history.take(MAX_SEQ_LEN).withIndex()) {
                    val card = cardStates[cardId]
                        ?: Card(cardId = cardId, due = datum.reviewDatetimeMillis).also {
                            cardStates[cardId] = it
                        }

                    val lastReview = card.lastReview
                    val isNonSameDay = lastReview != null &&
                        (datum.reviewDatetimeMillis - lastReview) / MILLIS_PER_DAY > 0

                    if (isNonSameDay) {
                        batchEntries.add(
                            MiniBatchEntry(
                                cardId = cardId,
                                reviewDatetimeMillis = datum.reviewDatetimeMillis,
                                rating = datum.rating,
                                recall = datum.recall
                            )
                        )
                    }

                    val (nextCard, _) = scheduler.reviewCard(
                        card = card,
                        rating = datum.rating,
                        reviewDatetimeMillis = datum.reviewDatetimeMillis
                    )
                    cardStates[cardId] = nextCard

                    if (batchEntries.size == MINI_BATCH_SIZE) {
                        step++
                        val lr = cosineAnnealingLR(step, tMax)
                        gradientStep(parameters, batchEntries, m, v, step, lr)
                        batchEntries.clear()
                    }
                }
            }

            if (batchEntries.isNotEmpty()) {
                step++
                val lr = cosineAnnealingLR(step, tMax)
                gradientStep(parameters, batchEntries, m, v, step, lr)
                batchEntries.clear()
            }

            val epochLoss = computeBatchLoss(parameters)
            if (epochLoss < bestLoss) {
                bestLoss = epochLoss
                bestParams = parameters.copyOf()
            }
        }

        return bestParams
    }

    /**
     * Computes the mean BCE over [batch] starting from pristine cards and using
     * [parameters]. Used for finite-difference gradient estimation.
     */
    private fun computeMiniBatchLoss(parameters: DoubleArray, batch: List<MiniBatchEntry>): Double {
        if (batch.isEmpty()) return 0.0
        val scheduler = Scheduler(parameters = parameters.copyOf(), enableFuzzing = false)
        val cards = mutableMapOf<String, Card>()
        var loss = 0.0

        for (entry in batch) {
            val card = cards[entry.cardId]
                ?: Card(cardId = entry.cardId, due = entry.reviewDatetimeMillis).also {
                    cards[entry.cardId] = it
                }
            val p = scheduler.getCardRetrievability(card, entry.reviewDatetimeMillis)
            loss += bce(p, entry.recall.toDouble())
            val (nextCard, _) = scheduler.reviewCard(
                card = card,
                rating = entry.rating,
                reviewDatetimeMillis = entry.reviewDatetimeMillis
            )
            cards[entry.cardId] = nextCard
        }

        return loss / batch.size
    }

    private fun gradientStep(
        parameters: DoubleArray,
        batch: List<MiniBatchEntry>,
        m: DoubleArray,
        v: DoubleArray,
        step: Int,
        learningRate: Double
    ) {
        val gradient = finiteDifferenceGradient(parameters, batch)

        for (i in parameters.indices) {
            m[i] = BETA_1 * m[i] + (1.0 - BETA_1) * gradient[i]
            v[i] = BETA_2 * v[i] + (1.0 - BETA_2) * gradient[i] * gradient[i]

            val mHat = m[i] / (1.0 - BETA_1.pow(step))
            val vHat = v[i] / (1.0 - BETA_2.pow(step))

            parameters[i] -= learningRate * mHat / (sqrt(vHat) + EPSILON)
            parameters[i] = min(
                max(parameters[i], Scheduler.LOWER_BOUNDS_PARAMETERS[i]),
                Scheduler.UPPER_BOUNDS_PARAMETERS[i]
            )
        }
    }

    private fun finiteDifferenceGradient(
        parameters: DoubleArray,
        batch: List<MiniBatchEntry>
    ): DoubleArray {
        val gradient = DoubleArray(parameters.size)
        for (i in parameters.indices) {
            val h = FINITE_DIFF_STEP * maxOf(kotlin.math.abs(parameters[i]), 0.01)
            val plus = parameters.copyOf().apply { this[i] += h }
            val minus = parameters.copyOf().apply { this[i] -= h }
            gradient[i] = (computeMiniBatchLoss(plus, batch) - computeMiniBatchLoss(minus, batch)) / (2.0 * h)
        }
        return gradient
    }

    private fun cosineAnnealingLR(step: Int, tMax: Int): Double {
        return LEARNING_RATE * 0.5 * (1.0 + cos(Math.PI * step / tMax))
    }

    private fun countReviewStateReviews(): Int {
        val scheduler = Scheduler(enableFuzzing = false)
        var count = 0
        for ((cardId, history) in revlogsTrain) {
            var card = Card(cardId = cardId, due = history.first().reviewDatetimeMillis)
            for (datum in history.take(MAX_SEQ_LEN)) {
                val lastReview = card.lastReview
                if (lastReview != null && (datum.reviewDatetimeMillis - lastReview) / MILLIS_PER_DAY > 0) {
                    count++
                }
                card = scheduler.reviewCard(card, datum.rating, datum.reviewDatetimeMillis).first
            }
        }
        return count
    }

    private fun bce(prediction: Double, target: Double): Double {
        val p = min(max(prediction, CLAMP_EPSILON), 1.0 - CLAMP_EPSILON)
        return -(target * ln(p) + (1.0 - target) * ln(1.0 - p))
    }

    /**
     * Computes first-review and conditional non-first review probabilities
     * plus average review durations per rating. Used by [simulateCost].
     */
    fun computeProbsAndCosts(): Map<String, Double> {
        val sortedLogs = reviewLogs.sortedWith(
            compareBy<ReviewLog> { it.cardId }.thenBy { it.reviewDatetime }
        )

        val firstReviews = mutableListOf<ReviewLog>()
        val nonFirstReviews = mutableListOf<ReviewLog>()
        val seenCards = mutableSetOf<String>()

        for (log in sortedLogs) {
            if (seenCards.add(log.cardId)) {
                firstReviews.add(log)
            } else {
                nonFirstReviews.add(log)
            }
        }

        val result = mutableMapOf<String, Double>()
        val firstGroups = firstReviews.groupBy { it.rating }
        val firstTotal = firstReviews.size.toDouble().coerceAtLeast(1.0)

        for (rating in Rating.values()) {
            result["prob_first_${rating.name.lowercase()}"] =
                (firstGroups[rating]?.size ?: 0) / firstTotal
        }

        for (rating in Rating.values()) {
            result["avg_first_${rating.name.lowercase()}_review_duration"] =
                firstGroups[rating]?.mapNotNull { it.reviewDuration }?.averageOrZero() ?: 0.0
        }

        val recallGroups = nonFirstReviews.groupBy { it.rating }
        val recallTotal = (recallGroups[Rating.Hard]?.size ?: 0) +
            (recallGroups[Rating.Good]?.size ?: 0) +
            (recallGroups[Rating.Easy]?.size ?: 0)
        val recallTotalDouble = recallTotal.toDouble().coerceAtLeast(1.0)

        result["prob_hard"] = (recallGroups[Rating.Hard]?.size ?: 0) / recallTotalDouble
        result["prob_good"] = (recallGroups[Rating.Good]?.size ?: 0) / recallTotalDouble
        result["prob_easy"] = (recallGroups[Rating.Easy]?.size ?: 0) / recallTotalDouble

        result["avg_again_review_duration"] =
            recallGroups[Rating.Again]?.mapNotNull { it.reviewDuration }?.averageOrZero() ?: 0.0
        result["avg_hard_review_duration"] =
            recallGroups[Rating.Hard]?.mapNotNull { it.reviewDuration }?.averageOrZero() ?: 0.0
        result["avg_good_review_duration"] =
            recallGroups[Rating.Good]?.mapNotNull { it.reviewDuration }?.averageOrZero() ?: 0.0
        result["avg_easy_review_duration"] =
            recallGroups[Rating.Easy]?.mapNotNull { it.reviewDuration }?.averageOrZero() ?: 0.0

        return result
    }

    private fun List<Long>.averageOrZero(): Double {
        if (isEmpty()) return 0.0
        return sum().toDouble() / size
    }

    /**
     * Simulates a cohort of [numCards] over one calendar year (2025-01-01 UTC to
     * 2026-01-01 UTC) with the given [parameters] and [desiredRetention].
     * Returns the average cost per unit of retained knowledge.
     */
    fun simulateCost(
        desiredRetention: Double,
        parameters: DoubleArray,
        numCards: Int,
        probsAndCosts: Map<String, Double>
    ): Double {
        val rng = Random(RNG_SEED)
        val scheduler = Scheduler(
            parameters = parameters.copyOf(),
            desiredRetention = desiredRetention,
            enableFuzzing = false
        )

        val firstWeights = doubleArrayOf(
            probsAndCosts.getValue("prob_first_again"),
            probsAndCosts.getValue("prob_first_hard"),
            probsAndCosts.getValue("prob_first_good"),
            probsAndCosts.getValue("prob_first_easy")
        )
        val firstDurations = doubleArrayOf(
            probsAndCosts.getValue("avg_first_again_review_duration"),
            probsAndCosts.getValue("avg_first_hard_review_duration"),
            probsAndCosts.getValue("avg_first_good_review_duration"),
            probsAndCosts.getValue("avg_first_easy_review_duration")
        )

        val recallWeights = doubleArrayOf(
            probsAndCosts.getValue("prob_hard"),
            probsAndCosts.getValue("prob_good"),
            probsAndCosts.getValue("prob_easy")
        )
        val recallDurations = doubleArrayOf(
            probsAndCosts.getValue("avg_hard_review_duration"),
            probsAndCosts.getValue("avg_good_review_duration"),
            probsAndCosts.getValue("avg_easy_review_duration")
        )

        var totalDuration = 0.0
        val startMillis = START_DATE_MILLIS
        val endMillis = END_DATE_MILLIS

        repeat(numCards) {
            var card = Card(cardId = "")
            var currentDate = startMillis

            while (currentDate < endMillis) {
                val (rating, duration) = if (currentDate == startMillis) {
                    val index = weightedChoice(firstWeights, rng)
                    Rating.values()[index] to firstDurations[index]
                } else {
                    val recall = rng.nextDouble() < desiredRetention
                    if (recall) {
                        val index = weightedChoice(recallWeights, rng)
                        // Shift recall indices (Hard/Good/Easy) to match Rating enum positions 1..3
                        Rating.values()[index + 1] to recallDurations[index]
                    } else {
                        Rating.Again to probsAndCosts.getValue("avg_again_review_duration")
                    }
                }

                totalDuration += duration
                val (nextCard, _) = scheduler.reviewCard(
                    card = card,
                    rating = rating,
                    reviewDatetimeMillis = currentDate
                )
                card = nextCard
                currentDate = card.due
            }
        }

        return totalDuration / (desiredRetention * numCards)
    }

    /**
     * Returns the desired retention from [DESIRED_RETENTIONS] that minimizes
     * simulated review cost for the given [parameters].
     *
     * @throws IllegalArgumentException if fewer than 512 review logs are present
     * or any log lacks a reviewDuration.
     */
    fun computeOptimalRetention(parameters: DoubleArray): Double {
        require(reviewLogs.size >= MIN_REVIEWS_FOR_TRAINING) {
            "Not enough ReviewLog's: at least 512 ReviewLog objects are required to compute optimal retention"
        }
        require(reviewLogs.none { it.reviewDuration == null }) {
            "ReviewLog.reviewDuration cannot be null when computing optimal retention"
        }

        val probsAndCosts = computeProbsAndCosts()
        var bestRetention = DESIRED_RETENTIONS.first()
        var bestCost = Double.POSITIVE_INFINITY

        for (retention in DESIRED_RETENTIONS) {
            val cost = simulateCost(
                desiredRetention = retention,
                parameters = parameters,
                numCards = NUM_CARDS_SIMULATE,
                probsAndCosts = probsAndCosts
            )
            if (cost < bestCost) {
                bestCost = cost
                bestRetention = retention
            }
        }

        return bestRetention
    }

    private fun weightedChoice(weights: DoubleArray, rng: Random): Int {
        val total = weights.sum()
        require(total > 0.0) { "Weights must sum to a positive value" }
        val target = rng.nextDouble() * total
        var cumulative = 0.0
        for (i in weights.indices) {
            cumulative += weights[i]
            if (target < cumulative) return i
        }
        return weights.lastIndex
    }

    companion object {
        private const val NUM_EPOCHS = 5
        private const val MINI_BATCH_SIZE = 512
        private const val LEARNING_RATE = 4e-2
        private const val MAX_SEQ_LEN = 64
        private const val MIN_REVIEWS_FOR_TRAINING = 512
        private const val RNG_SEED = 42L
        private const val BETA_1 = 0.9
        private const val BETA_2 = 0.999
        private const val EPSILON = 1e-8
        private const val FINITE_DIFF_STEP = 1e-4
        private const val CLAMP_EPSILON = 1e-15
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val NUM_CARDS_SIMULATE = 1000
        private val DESIRED_RETENTIONS = doubleArrayOf(0.7, 0.75, 0.8, 0.85, 0.9, 0.95)
        private const val START_DATE_MILLIS = 1_704_067_200_000L // 2025-01-01T00:00:00Z
        private const val END_DATE_MILLIS = 1_736_499_600_000L // 2026-01-01T00:00:00Z
    }
}
