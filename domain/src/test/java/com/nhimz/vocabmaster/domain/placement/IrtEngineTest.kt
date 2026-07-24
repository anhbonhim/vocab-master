package com.nhimz.vocabmaster.domain.placement

import com.nhimz.vocabmaster.domain.model.PlacementResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IrtEngineTest {

    @Test
    fun probability_isSigmoid() {
        // At theta == b, probability is 0.5 regardless of a.
        assertEquals(0.5, IrtEngine.probability(theta = 1.0, a = 1.0, b = 1.0), 1e-9)
        // Above difficulty -> high probability.
        assertTrue(IrtEngine.probability(theta = 3.0, a = 1.0, b = 0.0) > 0.9)
        // Below difficulty -> low probability.
        assertTrue(IrtEngine.probability(theta = -3.0, a = 1.0, b = 0.0) < 0.1)
    }

    @Test
    fun estimateTheta_increasesWithCorrectAnswers() {
        val correct = List(10) { PlacementResponse(isCorrect = true, a = 1.0, b = 0.5) }
        val wrong = List(10) { PlacementResponse(isCorrect = false, a = 1.0, b = 0.5) }

        val afterCorrect = IrtEngine.estimateTheta(correct)
        val afterWrong = IrtEngine.estimateTheta(wrong)

        assertTrue(afterCorrect.theta > 0.0)
        assertTrue(afterWrong.theta < 0.0)
        assertTrue(afterCorrect.theta > afterWrong.theta)
    }

    @Test
    fun estimateTheta_emptyReturnsDefault() {
        val estimate = IrtEngine.estimateTheta(emptyList())
        assertEquals(0.0, estimate.theta, 1e-9)
        assertEquals(9.0, estimate.standardError, 1e-9)
    }

    @Test
    fun mapThetaToCefr_boundaries() {
        assertEquals("A1", IrtEngine.mapThetaToCefr(-3.0))
        assertEquals("A2", IrtEngine.mapThetaToCefr(-1.0))
        assertEquals("B1", IrtEngine.mapThetaToCefr(0.0))
        assertEquals("B2", IrtEngine.mapThetaToCefr(1.0))
        assertEquals("C1", IrtEngine.mapThetaToCefr(2.0))
        assertEquals("C2", IrtEngine.mapThetaToCefr(3.0))
    }

    @Test
    fun cefrToTheta_roundTripsReasonably() {
        assertEquals(-2.5, IrtEngine.cefrToTheta("A1"), 1e-9)
        assertEquals(2.5, IrtEngine.cefrToTheta("C2"), 1e-9)
    }
}
