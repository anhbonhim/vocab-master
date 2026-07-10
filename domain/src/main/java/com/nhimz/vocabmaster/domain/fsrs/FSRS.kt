package com.nhimz.vocabmaster.domain.fsrs

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*
import kotlin.random.Random

class FSRS(
    private val requestRetention: Double = 0.9,
    private val params: List<Double> = defaultParams,
    private val isReview: Boolean = false,
    private val enableFuzz: Boolean = true,
    private val seed: Long? = null
) {
    companion object {
        val defaultParams = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
            1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
            1.8729, 0.5425, 0.0912, 0.0658, 0.1542
        )
    }

    private val decay = -params[20]
    private val factor = 0.9.pow(1.0 / decay) - 1
    private val random = seed?.let { Random(it) } ?: Random

    data class InitState(var difficulty: Double = 0.0, var stability: Double = 0.0)

    data class GradeInfo(
        val rating: Rating,
        val stability: Double,
        val difficulty: Double,
        val interval: Int,
        val durationMillis: Long,
        val nextState: State
    )

    fun calculate(card: Card): Map<Rating, GradeInfo> {
        val phase = when (card.state) {
            State.New -> 0
            State.Learning, State.Relearning -> 1
            State.Review -> 2
        }

        var stateAgain: InitState
        var stateHard: InitState
        var stateGood: InitState
        var stateEasy: InitState

        var durationHard = 5 * 60 * 1000L // 5min
        var durationGood: Long
        var durationEasy: Long

        var ivlHard = 0
        var ivlGood = 0
        var ivlEasy = 0

        val dayConvertor: Long = 24 * 60 * 60 * 1000L

        when (phase) {
            0 -> { // Added/New
                stateAgain = InitState()
                stateHard = InitState()
                stateGood = InitState()
                stateEasy = InitState()

                ivlEasy = 1
                durationGood = 10 * 60 * 1000L
                durationEasy = ivlEasy * dayConvertor
            }

            1 -> { // ReLearning/Learning
                if (card.difficulty == 0.0) {
                    stateAgain = initState(Rating.Again)
                    stateHard = initState(Rating.Hard)
                    stateGood = initState(Rating.Good)
                    stateEasy = initState(Rating.Easy)
                } else {
                    val lastD = card.difficulty
                    val lastS = card.stability

                    stateAgain = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Again),
                        stability = nextShortTermStability(lastS, Rating.Again)
                    )
                    stateHard = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Hard),
                        stability = nextShortTermStability(lastS, Rating.Hard)
                    )
                    stateGood = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Good),
                        stability = nextShortTermStability(lastS, Rating.Good)
                    )
                    stateEasy = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Easy),
                        stability = nextShortTermStability(lastS, Rating.Easy)
                    )
                }

                ivlGood = nextInterval(stateGood.stability, lastInterval = card.interval)
                ivlEasy = nextInterval(stateEasy.stability, lastInterval = card.interval)
                ivlEasy = max(ivlEasy, ivlGood + 1)

                durationGood = ivlGood * dayConvertor
                durationEasy = ivlEasy * dayConvertor
            }

            else -> { // Review
                val interval = card.interval
                val lastD = card.difficulty
                val lastS = card.stability

                val retrievability = forgettingCurve(interval.toDouble(), lastS)

                stateAgain = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Again),
                    stability = nextForgetStability(lastD, lastS, retrievability)
                )
                stateHard = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Hard),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Hard)
                )
                stateGood = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Good),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Good)
                )
                stateEasy = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Easy),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Easy)
                )

                ivlHard = nextInterval(stateHard.stability, lastInterval = card.interval)
                ivlGood = nextInterval(stateGood.stability, lastInterval = card.interval)
                ivlEasy = nextInterval(stateEasy.stability, lastInterval = card.interval)

                ivlHard = min(ivlHard, ivlGood)
                ivlGood = min(ivlGood, ivlHard + 1)
                ivlEasy = min(ivlEasy, ivlGood + 1)

                durationHard = ivlHard * dayConvertor
                durationGood = ivlGood * dayConvertor
                durationEasy = ivlEasy * dayConvertor
            }
        }

        val nextStateAgain = when (card.state) {
            State.New -> State.Learning
            State.Learning -> State.Learning
            State.Review -> State.Relearning
            State.Relearning -> State.Relearning
        }
        val nextStateHard = when (card.state) {
            State.New -> State.Learning
            State.Learning -> State.Learning
            State.Review -> State.Review
            State.Relearning -> State.Relearning
        }
        val nextStateGood = when (card.state) {
            State.New -> State.Learning
            State.Learning -> State.Review
            State.Review -> State.Review
            State.Relearning -> State.Review
        }
        val nextStateEasy = State.Review

        return mapOf(
            Rating.Easy to GradeInfo(
                rating = Rating.Easy,
                stability = stateEasy.stability,
                difficulty = stateEasy.difficulty,
                interval = ivlEasy,
                durationMillis = durationEasy,
                nextState = nextStateEasy
            ),
            Rating.Good to GradeInfo(
                rating = Rating.Good,
                stability = stateGood.stability,
                difficulty = stateGood.difficulty,
                interval = ivlGood,
                durationMillis = durationGood,
                nextState = nextStateGood
            ),
            Rating.Hard to GradeInfo(
                rating = Rating.Hard,
                stability = stateHard.stability,
                difficulty = stateHard.difficulty,
                interval = ivlHard,
                durationMillis = durationHard,
                nextState = nextStateHard
            ),
            Rating.Again to GradeInfo(
                rating = Rating.Again,
                stability = stateAgain.stability,
                difficulty = stateAgain.difficulty,
                interval = card.interval,
                durationMillis = 3 * 60 * 1000L,
                nextState = nextStateAgain
            )
        )
    }

    fun schedule(card: Card, rating: Rating, now: LocalDateTime): CardReviewResult {
        val elapsedDays = if (card.lastReview == null) {
            0
        } else {
            max(0, ChronoUnit.DAYS.between(card.lastReview, now).toInt())
        }

        val gradeInfos = calculate(card)
        val info = gradeInfos[rating] ?: throw IllegalArgumentException("Invalid rating: $rating")

        val nextReps = card.reps + 1
        val nextLapses = if (card.state == State.Review && rating == Rating.Again) {
            card.lapses + 1
        } else {
            card.lapses
        }

        val nextDue = if (info.interval > 0) {
            now.plusDays(info.interval.toLong())
        } else {
            now.plusMinutes(info.durationMillis / (60 * 1000L))
        }

        val updatedCard = card.copy(
            due = nextDue,
            stability = info.stability,
            difficulty = info.difficulty,
            interval = info.interval,
            reps = nextReps,
            lapses = nextLapses,
            state = info.nextState,
            lastReview = now
        )

        val log = ReviewLog(
            rating = rating,
            elapsed_days = elapsedDays,
            scheduled_days = card.interval,
            stability = info.stability,
            difficulty = info.difficulty,
            state = card.state,
            timestamp = now
        )

        return CardReviewResult(updatedCard, log)
    }

    private fun applyFuzz(
        interval: Double,
        fuzzFactor: Double,
        scheduledDays: Int = 0
    ): Double {
        if (!enableFuzz || interval < 2.5) return interval

        val ivl = interval.roundToInt()
        var minIvl = max(2, (ivl * 0.95 - 1).roundToInt())
        val maxIvl = (ivl * 1.05 + 1).roundToInt()

        if (isReview && ivl > scheduledDays)
            minIvl = max(minIvl, scheduledDays + 1)

        return floor(fuzzFactor * (maxIvl - minIvl + 1) + minIvl)
    }

    private fun forgettingCurve(interval: Double, stability: Double): Double {
        return exp(-interval / stability)
    }

    private fun generateFuzzFactor(): Double {
        return random.nextDouble()
    }

    private fun initDifficulty(rating: Rating): Double {
        val base = params[4]
        val exponent = params[5] * (rating.value - 1)
        val raw = base - exp(exponent) + 1
        return String.format(java.util.Locale.US, "%.2f", raw.coerceIn(1.0, 10.0)).toDouble()
    }

    private fun initStability(rating: Rating): Double {
        val index = rating.value - 1
        val value = params.getOrElse(index) { 0.1 }
        return String.format(java.util.Locale.US, "%.2f", value.coerceAtLeast(0.1)).toDouble()
    }

    private fun initState(rating: Rating): InitState {
        return InitState(
            difficulty = initDifficulty(rating),
            stability = initStability(rating)
        )
    }

    private fun linearDamping(delta: Double, oldD: Double): Double {
        return delta * (10 - oldD / 9)
    }

    private fun meanReversion(initD: Double, nextD: Double): Double {
        return params[7] * initD + (1 - params[7]) * nextD
    }

    private fun nextInterval(
        stability: Double,
        maxInterval: Int = 36500,
        lastInterval: Int = 0
    ): Int {
        val fuzzFactor = generateFuzzFactor()
        val rawInterval = stability / factor * (requestRetention.pow(1 / decay) - 1)
        val fuzzed = applyFuzz(rawInterval, fuzzFactor, scheduledDays = lastInterval)
        return fuzzed.roundToInt().coerceIn(1, maxInterval)
    }

    private fun nextDifficulty(currentD: Double, rating: Rating): Double {
        val deltaD = -params[6] * (rating.value - 3)
        val damped = linearDamping(deltaD, currentD)
        val nextD = currentD + damped
        val reverted = meanReversion(initDifficulty(Rating.Easy), nextD)
        return String.format(java.util.Locale.US, "%.2f", reverted.coerceIn(1.0, 10.0)).toDouble()
    }

    private fun nextShortTermStability(currentS: Double, rating: Rating): Double {
        var sinc = exp(params[17] * (rating.value - 3 + params[18])) * currentS.pow(-params[19])
        if (rating.value >= 3) {
            sinc = max(sinc, 1.0)
        }
        return String.format(java.util.Locale.US, "%.2f", abs(currentS * sinc)).toDouble()
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double
    ): Double {
        val sMin = stability / exp(params[17] * params[18])

        val result = params[11] *
                difficulty.pow(-params[12]) *
                ((stability + 1).pow(params[13]) - 1) *
                exp((1 - retrievability) * params[14])

        return String.format(java.util.Locale.US, "%.2f", min(result, sMin)).toDouble()
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.Hard) params[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) params[16] else 1.0

        val factor = exp(params[8]) *
                (11 - d) *
                s.pow(-params[9]) *
                (exp((1 - r) * params[10]) - 1) *
                hardPenalty *
                easyBonus

        val result = s * (1 + factor)
        return String.format(java.util.Locale.US, "%.2f", result).toDouble()
    }
}

data class CardReviewResult(
    val card: Card,
    val log: ReviewLog
)
