package com.nhimz.vocabmaster.domain.placement

import com.nhimz.vocabmaster.domain.model.PlacementResponse
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Pure-Kotlin port of the backend 2PL Item Response Theory (IRT) engine
 * (backend/app/services/irt_engine.py). Used by the offline placement test to
 * estimate a learner's ability (theta) without any network call.
 */
object IrtEngine {

    /** P(theta) = 1 / (1 + exp(-a * (theta - b))) */
    fun probability(theta: Double, a: Double, b: Double): Double {
        val exponent = -a * (theta - b)
        return when {
            exponent > 700.0 -> 0.0
            exponent < -700.0 -> 1.0
            else -> 1.0 / (1.0 + exp(exponent))
        }
    }

    /**
     * Estimates ability (theta) and standard error via a Newton-Raphson style
     * update (EAP approximation).
     *
     * @param responses list of answered items: correctness + item params (a,b)
     * @param currentTheta starting ability estimate
     * @return clamped theta in [-3, 3] (A1 .. C2) and its standard error
     */
    fun estimateTheta(
        responses: List<PlacementResponse>,
        currentTheta: Double = 0.0
    ): ThetaEstimate {
        if (responses.isEmpty()) return ThetaEstimate(0.0, 9.0)

        var theta = currentTheta
        val learningRate = 0.5
        var info = 0.0

        repeat(5) {
            var gradient = 0.0
            info = 0.0
            for (r in responses) {
                val p = probability(theta, r.a, r.b)
                val q = 1.0 - p
                val u = if (r.isCorrect) 1.0 else 0.0
                gradient += r.a * (u - p)
                info += (r.a * r.a) * p * q
            }
            if (info > 0) {
                theta += (gradient / info) * learningRate
            }
        }

        val se = if (info > 0) 1.0 / sqrt(info) else 9.0
        return ThetaEstimate(theta.coerceIn(-3.0, 3.0), se)
    }

    /** Maps a theta estimate to a CEFR level string (A1 .. C2). */
    fun mapThetaToCefr(theta: Double): String {
        return when {
            theta < -1.5 -> "A1"
            theta < -0.5 -> "A2"
            theta < 0.5 -> "B1"
            theta < 1.5 -> "B2"
            theta < 2.5 -> "C1"
            else -> "C2"
        }
    }

    /** Maps a CEFR level to its representative theta (difficulty) value. */
    fun cefrToTheta(level: String): Double {
        return when (level) {
            "A1" -> -2.5
            "A2" -> -1.5
            "B1" -> -0.5
            "B2" -> 0.5
            "C1" -> 1.5
            "C2" -> 2.5
            else -> -0.5
        }
    }
}

data class ThetaEstimate(
    val theta: Double,
    val standardError: Double
)
