package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.Node
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.usecase.fakes.FakeSettingsRepository
import com.nhimz.vocabmaster.domain.usecase.fakes.FakeVocabularyRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteQuizSessionUseCaseTest {
    private val vocabularyRepository = FakeVocabularyRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val updateStreakUseCase = SpyUpdateStreakUseCase(settingsRepository)
    private val useCase = CompleteQuizSessionUseCase(vocabularyRepository, settingsRepository, updateStreakUseCase)

    class SpyUpdateStreakUseCase(settingsRepository: FakeSettingsRepository) : UpdateStreakUseCase(settingsRepository) {
        var executeCalls: Int = 0
        override suspend fun execute() {
            executeCalls++
            super.execute()
        }
    }

    @Test
    fun `UpdateStreakUseCase is always invoked`() = runTest {
        useCase(QuizCompletionInput(correctCount = 5, totalQuestions = 10, xpGained = 50, nodeId = "node-1"))
        assertEquals(1, updateStreakUseCase.executeCalls)
    }

    @Test
    fun `jump test pass marks all unit nodes completed`() = runTest {
        val nodes = listOf(
            Node(id = "n1", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "", scenarioContext = "", icon = ""),
            Node(id = "n2", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "", scenarioContext = "", icon = "")
        )
        vocabularyRepository.getNodesByUnitResult = flowOf(nodes)

        val result = useCase(QuizCompletionInput(correctCount = 8, totalQuestions = 10, xpGained = 80, isJumpTest = true, unitIdForJumpTest = "unit-1"))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isPassed)
        assertEquals(2, vocabularyRepository.markNodeCompletedCalls)
        assertEquals("n1", vocabularyRepository.lastMarkNodeCompletedArgs?.first)
    }

    @Test
    fun `jump test below threshold does not mark nodes`() = runTest {
        vocabularyRepository.getNodesByUnitResult = flowOf(emptyList())

        val result = useCase(QuizCompletionInput(correctCount = 7, totalQuestions = 10, xpGained = 70, isJumpTest = true, unitIdForJumpTest = "unit-1"))

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isPassed)
        assertEquals(0, vocabularyRepository.markNodeCompletedCalls)
    }

    @Test
    fun `section checkpoint pass sets placement level`() = runTest {
        val result = useCase(QuizCompletionInput(correctCount = 8, totalQuestions = 10, xpGained = 80, isSectionCheckpoint = true, nextSectionCefr = "B1"))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isPassed)
        assertEquals("B1", settingsRepository.setPlacementLevelValue)
    }

    @Test
    fun `section checkpoint below 80 percent does not set placement`() = runTest {
        val result = useCase(QuizCompletionInput(correctCount = 7, totalQuestions = 10, xpGained = 70, isSectionCheckpoint = true, nextSectionCefr = "B1"))

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isPassed)
        assertEquals(null, settingsRepository.setPlacementLevelValue)
    }

    @Test
    fun `unit checkpoint pass marks only UNIT_CHECKPOINT node`() = runTest {
        val nodes = listOf(
            Node(id = "n1", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "", scenarioContext = "", icon = ""),
            Node(id = "n-check", unitId = "unit-1", index = 1, type = NodeType.UNIT_CHECKPOINT, title = "", scenarioContext = "", icon = ""),
            Node(id = "n2", unitId = "unit-1", index = 2, type = NodeType.REVIEW, title = "", scenarioContext = "", icon = "")
        )
        vocabularyRepository.getNodesByUnitResult = flowOf(nodes)

        val result = useCase(QuizCompletionInput(correctCount = 8, totalQuestions = 10, xpGained = 80, isUnitCheckpoint = true, unitIdForUnitCheckpoint = "unit-1"))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isPassed)
        assertEquals(1, vocabularyRepository.markNodeCompletedCalls)
        assertEquals("n-check", vocabularyRepository.lastMarkNodeCompletedArgs?.first)
    }

    @Test
    fun `regular node pass marks node completed`() = runTest {
        val result = useCase(QuizCompletionInput(correctCount = 7, totalQuestions = 10, xpGained = 70, nodeId = "node-1"))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isPassed)
        assertEquals(1, vocabularyRepository.markNodeCompletedCalls)
        assertEquals("node-1", vocabularyRepository.lastMarkNodeCompletedArgs?.first)
    }

    @Test
    fun `regular node below 70 percent does not mark completed`() = runTest {
        val result = useCase(QuizCompletionInput(correctCount = 6, totalQuestions = 10, xpGained = 60, nodeId = "node-1"))

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().isPassed)
        assertEquals(0, vocabularyRepository.markNodeCompletedCalls)
    }

    @Test
    fun `checkpoint boundary 79 percent fails`() = runTest {
        val result = useCase(QuizCompletionInput(correctCount = 79, totalQuestions = 100, xpGained = 790, isSectionCheckpoint = true, nextSectionCefr = "B1"))
        assertFalse(result.getOrThrow().isPassed)
    }

    @Test
    fun `repository throw returns Result failure`() = runTest {
        vocabularyRepository.getNodesByUnitResult = flowOf(emptyList())
        vocabularyRepository.failure = RuntimeException("boom")

        val result = useCase(QuizCompletionInput(correctCount = 8, totalQuestions = 10, xpGained = 80, isJumpTest = true, unitIdForJumpTest = "unit-1"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}
