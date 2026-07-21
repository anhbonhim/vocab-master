@file:Suppress("MagicNumber", "NestedBlockDepth", "ComplexCondition", "UseRequire", "UseCheckOrError")
package com.nhimz.vocabmaster.domain.fsrs.v6

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random

/**
 * FSRS-6 scheduler ported verbatim from py-fsrs 6.3.1.
 *
 * Deliberate deviations from py-fsrs are documented on the relevant fields:
 * - [enableFuzzing] defaults to `false` for deterministic tests and UI parity
 *   (py-fsrs defaults to `true`).
 * - [learningSteps] and [relearningSteps] are stored as milliseconds ([LongArray])
 *   instead of Python `timedelta` objects.
 *
 * All scheduler math uses pure `Double` / `Int` / `Long`; no `String.format` or
 * `Locale` usage is present in this package (FSRS-03).
 */
@Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "LongParameterList", "MagicNumber")
class Scheduler @Inject constructor(
    val parameters: DoubleArray = DEFAULT_PARAMETERS.copyOf(),
    val desiredRetention: Double = 0.9,
    val learningSteps: LongArray = longArrayOf(60_000L, 600_000L),
    val relearningSteps: LongArray = longArrayOf(600_000L),
    val maximumInterval: Int = 36500,
    val enableFuzzing: Boolean = false,
    val random: Random = Random.Default
) {
    init {
        validateParameters(parameters)
    }

    companion object {
        const val STABILITY_MIN = 0.001
        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0

        /**
         * FSRS-6 default parameters from py-fsrs 6.3.1.
         *
         * NOTE: CONTEXT D-06 listed w[0] = 0.2172, but the installed py-fsrs 6.3.1
         * package ships 0.212. The port follows the installed-package ground truth so
         * that Kotlin output matches vectors generated from the same package.
         */
        val DEFAULT_PARAMETERS = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
            1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
            1.8729, 0.5425, 0.0912, 0.0658, 0.1542
        )

        val LOWER_BOUNDS_PARAMETERS = doubleArrayOf(
            STABILITY_MIN, STABILITY_MIN, STABILITY_MIN, STABILITY_MIN,
            1.0, 0.001, 0.001, 0.001, 0.0, 0.0, 0.001, 0.001, 0.001, 0.001,
            0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.1
        )

        val UPPER_BOUNDS_PARAMETERS = doubleArrayOf(
            100.0, 100.0, 100.0, 100.0,
            10.0, 4.0, 4.0, 0.75, 4.5, 0.8, 3.5, 5.0, 0.25, 0.9, 4.0, 1.0,
            6.0, 2.0, 2.0, 0.8, 0.8
        )

        private const val MILLIS_PER_DAY = 86_400_000L

        private val FUZZ_RANGES = listOf(
            Triple(2.5, 7.0, 0.15),
            Triple(7.0, 20.0, 0.1),
            Triple(20.0, Double.POSITIVE_INFINITY, 0.05)
        )
    }

    private val decay = -parameters[20]
    private val factor = 0.9.pow(1.0 / decay) - 1

    private fun validateParameters(parameters: DoubleArray) {
        require(parameters.size == LOWER_BOUNDS_PARAMETERS.size) {
            "Expected ${LOWER_BOUNDS_PARAMETERS.size} parameters, got ${parameters.size}."
        }

        val errors = mutableListOf<String>()
        for (index in parameters.indices) {
            val value = parameters[index]
            val lower = LOWER_BOUNDS_PARAMETERS[index]
            val upper = UPPER_BOUNDS_PARAMETERS[index]
            if (value < lower || value > upper) {
                errors.add("parameters[$index] = $value is out of bounds: ($lower, $upper)")
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(
                "One or more parameters are out of bounds:\n" + errors.joinToString("\n")
            )
        }
    }

    fun getCardRetrievability(card: Card, currentDatetimeMillis: Long): Double {
        if (card.lastReview == null || card.stability == null) return 0.0
        val elapsedDays = max(0, (currentDatetimeMillis - card.lastReview) / MILLIS_PER_DAY)
        return (1 + factor * elapsedDays / card.stability).pow(decay)
    }

    fun reviewCard(
        card: Card,
        rating: Rating,
        reviewDatetimeMillis: Long,
        reviewDurationMillis: Long? = null
    ): Pair<Card, ReviewLog> {
        val daysSinceLastReview = card.lastReview?.let {
            max(0, (reviewDatetimeMillis - it) / MILLIS_PER_DAY)
        }

        // New is an alias for a pristine py-fsrs Learning card; after the first review
        // it becomes Learning or Review and never returns to New.
        var newState = if (card.state == State.New) State.Learning else card.state
        var newStep = card.step
        var newStability = card.stability
        var newDifficulty = card.difficulty
        val nextIntervalMillis: Long

        when (newState) {
            State.Learning -> {
                // Initialize stability/difficulty on the first review of a new card.
                if (newStability == null || newDifficulty == null) {
                    newStability = initialStability(rating)
                    newDifficulty = initialDifficulty(rating, clamp = true)
                } else if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    newStability = shortTermStability(newStability, rating)
                    newDifficulty = nextDifficulty(newDifficulty, rating)
                } else {
                    newStability = nextStability(
                        difficulty = newDifficulty,
                        stability = newStability,
                        retrievability = getCardRetrievability(card, reviewDatetimeMillis),
                        rating = rating
                    )
                    newDifficulty = nextDifficulty(newDifficulty, rating)
                }

                // Edge case: card was scheduled with more learning steps than current scheduler.
                if (learningSteps.isEmpty() ||
                    (newStep != null && newStep >= learningSteps.size &&
                        rating in setOf(Rating.Hard, Rating.Good, Rating.Easy))
                ) {
                    newState = State.Review
                    newStep = null
                    nextIntervalMillis = nextInterval(newStability) * MILLIS_PER_DAY
                } else {
                    nextIntervalMillis = when (rating) {
                        Rating.Again -> {
                            newStep = 0
                            learningSteps[newStep!!]
                        }

                        Rating.Hard -> {
                            if (newStep == 0 && learningSteps.size == 1) {
                                (learningSteps[0] * 1.5).toLong()
                            } else if (newStep == 0 && learningSteps.size >= 2) {
                                ((learningSteps[0] + learningSteps[1]) / 2.0).toLong()
                            } else {
                                learningSteps[newStep!!]
                            }
                        }

                        Rating.Good -> {
                            if (newStep!! + 1 == learningSteps.size) {
                                newState = State.Review
                                newStep = null
                                nextInterval(newStability) * MILLIS_PER_DAY
                            } else {
                                newStep = newStep + 1
                                learningSteps[newStep]
                            }
                        }

                        Rating.Easy -> {
                            newState = State.Review
                            newStep = null
                            nextInterval(newStability) * MILLIS_PER_DAY
                        }
                    }
                }
            }

            State.Review -> {
                if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    newStability = shortTermStability(newStability!!, rating)
                } else {
                    newStability = nextStability(
                        difficulty = newDifficulty!!,
                        stability = newStability!!,
                        retrievability = getCardRetrievability(card, reviewDatetimeMillis),
                        rating = rating
                    )
                }
                newDifficulty = nextDifficulty(newDifficulty!!, rating)

                nextIntervalMillis = when (rating) {
                    Rating.Again -> {
                        if (relearningSteps.isEmpty()) {
                            nextInterval(newStability) * MILLIS_PER_DAY
                        } else {
                            newState = State.Relearning
                            newStep = 0
                            relearningSteps[0]
                        }
                    }

                    Rating.Hard, Rating.Good, Rating.Easy -> {
                        nextInterval(newStability) * MILLIS_PER_DAY
                    }
                }
            }

            State.Relearning -> {
                if (daysSinceLastReview != null && daysSinceLastReview < 1) {
                    newStability = shortTermStability(newStability!!, rating)
                    newDifficulty = nextDifficulty(newDifficulty!!, rating)
                } else {
                    newStability = nextStability(
                        difficulty = newDifficulty!!,
                        stability = newStability!!,
                        retrievability = getCardRetrievability(card, reviewDatetimeMillis),
                        rating = rating
                    )
                    newDifficulty = nextDifficulty(newDifficulty!!, rating)
                }

                if (relearningSteps.isEmpty() ||
                    (newStep != null && newStep >= relearningSteps.size &&
                        rating in setOf(Rating.Hard, Rating.Good, Rating.Easy))
                ) {
                    newState = State.Review
                    newStep = null
                    nextIntervalMillis = nextInterval(newStability) * MILLIS_PER_DAY
                } else {
                    nextIntervalMillis = when (rating) {
                        Rating.Again -> {
                            newStep = 0
                            relearningSteps[newStep!!]
                        }

                        Rating.Hard -> {
                            if (newStep == 0 && relearningSteps.size == 1) {
                                (relearningSteps[0] * 1.5).toLong()
                            } else if (newStep == 0 && relearningSteps.size >= 2) {
                                ((relearningSteps[0] + relearningSteps[1]) / 2.0).toLong()
                            } else {
                                relearningSteps[newStep!!]
                            }
                        }

                        Rating.Good -> {
                            if (newStep!! + 1 == relearningSteps.size) {
                                newState = State.Review
                                newStep = null
                                nextInterval(newStability) * MILLIS_PER_DAY
                            } else {
                                newStep = newStep + 1
                                relearningSteps[newStep]
                            }
                        }

                        Rating.Easy -> {
                            newState = State.Review
                            newStep = null
                            nextInterval(newStability) * MILLIS_PER_DAY
                        }
                    }
                }
            }

            State.New -> throw IllegalStateException("New state should have been normalized to Learning")
        }

        // Apply fuzz only when enabled and the final state is Review.
        val fuzzedIntervalMillis = if (enableFuzzing && newState == State.Review) {
            getFuzzedInterval(nextIntervalMillis)
        } else {
            nextIntervalMillis
        }

        val nextReps = card.reps + 1
        val nextLapses = if (card.state == State.Review && rating == Rating.Again) {
            card.lapses + 1
        } else {
            card.lapses
        }

        val newCard = Card(
            cardId = card.cardId,
            state = newState,
            step = newStep,
            stability = newStability,
            difficulty = newDifficulty,
            due = reviewDatetimeMillis + fuzzedIntervalMillis,
            lastReview = reviewDatetimeMillis,
            reps = nextReps,
            lapses = nextLapses
        )

        val reviewLog = ReviewLog(
            cardId = card.cardId,
            rating = rating,
            reviewDatetime = reviewDatetimeMillis,
            reviewDuration = reviewDurationMillis
        )

        return newCard to reviewLog
    }

    fun rescheduleCard(card: Card, reviewLogs: List<ReviewLog>): Card {
        for (log in reviewLogs) {
            if (log.cardId != card.cardId) {
                throw IllegalArgumentException(
                    "ReviewLog card_id ${log.cardId} does not match Card card_id ${card.cardId}"
                )
            }
        }

        val sortedLogs = reviewLogs.sortedBy { it.reviewDatetime }
        var rescheduledCard = Card(cardId = card.cardId, due = card.due)

        for (log in sortedLogs) {
            val (updatedCard, _) = reviewCard(
                card = rescheduledCard,
                rating = log.rating,
                reviewDatetimeMillis = log.reviewDatetime
            )
            rescheduledCard = updatedCard
        }

        return rescheduledCard
    }

    /**
     * Returns a plain [Map] representation using py-fsrs key names.
     * Learning/relearning steps are stored as seconds (as py-fsrs does).
     */
    fun toDict(): Map<String, Any?> = mapOf(
        "parameters" to parameters.toList(),
        "desired_retention" to desiredRetention,
        "learning_steps" to learningSteps.map { it / 1000L },
        "relearning_steps" to relearningSteps.map { it / 1000L },
        "maximum_interval" to maximumInterval,
        "enable_fuzzing" to enableFuzzing
    )

    /**
     * Builds a [Scheduler] from a py-fsrs-style [Map].
     */
    @Suppress("UNCHECKED_CAST")
    fun fromDict(dict: Map<String, Any?>): Scheduler {
        return Scheduler(
            parameters = (dict["parameters"] as List<Number>).map { it.toDouble() }.toDoubleArray(),
            desiredRetention = (dict["desired_retention"] as Number).toDouble(),
            learningSteps = (dict["learning_steps"] as List<Number>).map { it.toLong() * 1000L }.toLongArray(),
            relearningSteps = (dict["relearning_steps"] as List<Number>).map { it.toLong() * 1000L }.toLongArray(),
            maximumInterval = (dict["maximum_interval"] as Number).toInt(),
            enableFuzzing = dict["enable_fuzzing"] as Boolean
        )
    }

    /**
     * JSON-serializable DTO for [Scheduler].
     */
    @Serializable
    private data class SchedulerJson(
        @SerialName("parameters") val parameters: List<Double>,
        @SerialName("desired_retention") val desiredRetention: Double,
        @SerialName("learning_steps") val learningSteps: List<Long>,
        @SerialName("relearning_steps") val relearningSteps: List<Long>,
        @SerialName("maximum_interval") val maximumInterval: Int,
        @SerialName("enable_fuzzing") val enableFuzzing: Boolean
    )

    fun toJson(): String {
        val dto = SchedulerJson(
            parameters = parameters.toList(),
            desiredRetention = desiredRetention,
            learningSteps = learningSteps.map { it / 1000L },
            relearningSteps = relearningSteps.map { it / 1000L },
            maximumInterval = maximumInterval,
            enableFuzzing = enableFuzzing
        )
        return Json.encodeToString(SchedulerJson.serializer(), dto)
    }

    fun fromJson(source: String): Scheduler {
        val dto = Json.decodeFromString(SchedulerJson.serializer(), source)
        return Scheduler(
            parameters = dto.parameters.toDoubleArray(),
            desiredRetention = dto.desiredRetention,
            learningSteps = dto.learningSteps.map { it * 1000L }.toLongArray(),
            relearningSteps = dto.relearningSteps.map { it * 1000L }.toLongArray(),
            maximumInterval = dto.maximumInterval,
            enableFuzzing = dto.enableFuzzing
        )
    }

    private fun clampStability(stability: Double): Double {
        return max(stability, STABILITY_MIN)
    }

    private fun clampDifficulty(difficulty: Double): Double {
        return min(max(difficulty, MIN_DIFFICULTY), MAX_DIFFICULTY)
    }

    private fun initialStability(rating: Rating): Double {
        return clampStability(parameters[rating.value - 1])
    }

    private fun initialDifficulty(rating: Rating, clamp: Boolean): Double {
        val initialDifficulty = parameters[4] - exp(parameters[5] * (rating.value - 1)) + 1
        return if (clamp) clampDifficulty(initialDifficulty) else initialDifficulty
    }

    private fun nextInterval(stability: Double): Int {
        val nextInterval = (stability / factor) * (desiredRetention.pow(1.0 / decay) - 1)
        // Banker's rounding (round-half-to-even), matching Python's built-in round().
        val rounded = round(nextInterval).toInt()
        return min(max(rounded, 1), maximumInterval)
    }

    private fun shortTermStability(stability: Double, rating: Rating): Double {
        val increase = exp(parameters[17] * (rating.value - 3 + parameters[18])) * stability.pow(-parameters[19])
        val clampedIncrease = if (rating in setOf(Rating.Good, Rating.Easy)) max(increase, 1.0) else increase
        return clampStability(stability * clampedIncrease)
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val linearDamping = { delta: Double, d: Double -> (10.0 - d) * delta / 9.0 }
        val meanReversion = { arg1: Double, arg2: Double -> parameters[7] * arg1 + (1 - parameters[7]) * arg2 }

        val arg1 = initialDifficulty(Rating.Easy, clamp = false)
        val deltaDifficulty = -(parameters[6] * (rating.value - 3))
        val arg2 = difficulty + linearDamping(deltaDifficulty, difficulty)

        return clampDifficulty(meanReversion(arg1, arg2))
    }

    private fun nextStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating
    ): Double {
        val next = if (rating == Rating.Again) {
            nextForgetStability(difficulty, stability, retrievability)
        } else {
            nextRecallStability(difficulty, stability, retrievability, rating)
        }
        return clampStability(next)
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double
    ): Double {
        val longTerm = parameters[11] *
            difficulty.pow(-parameters[12]) *
            ((stability + 1).pow(parameters[13]) - 1) *
            exp((1 - retrievability) * parameters[14])
        val shortTerm = stability / exp(parameters[17] * parameters[18])
        return min(longTerm, shortTerm)
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) parameters[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) parameters[16] else 1.0

        return stability * (
            1 + exp(parameters[8]) *
                (11 - difficulty) *
                stability.pow(-parameters[9]) *
                (exp((1 - retrievability) * parameters[10]) - 1) *
                hardPenalty *
                easyBonus
            )
    }

    private fun getFuzzedInterval(intervalMillis: Long): Long {
        val intervalDays = (intervalMillis / MILLIS_PER_DAY).toInt()
        if (intervalDays < 2.5) return intervalMillis

        var delta = 1.0
        for ((start, end, factor) in FUZZ_RANGES) {
            delta += factor * max(min(intervalDays.toDouble(), end) - start, 0.0)
        }

        var minIvl = round(intervalDays - delta).toInt()
        var maxIvl = round(intervalDays + delta).toInt()

        minIvl = max(2, minIvl)
        maxIvl = min(maxIvl, maximumInterval)
        minIvl = min(minIvl, maxIvl)

        val fuzzed = random.nextDouble() * (maxIvl - minIvl + 1) + minIvl
        val fuzzedDays = min(round(fuzzed).toInt(), maximumInterval)
        return fuzzedDays * MILLIS_PER_DAY.toLong()
    }
}
