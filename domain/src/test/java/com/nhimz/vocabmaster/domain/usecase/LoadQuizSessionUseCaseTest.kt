package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.model.MatchPair
import com.nhimz.vocabmaster.domain.model.Node
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.Question
import com.nhimz.vocabmaster.domain.model.QuestionType
import com.nhimz.vocabmaster.domain.model.QuestionWithCard
import com.nhimz.vocabmaster.domain.model.Session
import com.nhimz.vocabmaster.domain.model.Unit
import com.nhimz.vocabmaster.domain.model.quiz.QuestionDirection
import com.nhimz.vocabmaster.domain.model.quiz.QuizType
import com.nhimz.vocabmaster.domain.usecase.QuizSessionRequest
import com.nhimz.vocabmaster.domain.usecase.fakes.FakeVocabularyRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadQuizSessionUseCaseTest {
    private val vocabularyRepository = FakeVocabularyRepository()
    private val useCase = LoadQuizSessionUseCase(vocabularyRepository)

    private fun card(questionId: String) = Card(cardId = questionId)
    private fun question(
        id: String,
        type: QuestionType,
        prompt: String = "prompt-$id",
        options: List<String>? = null,
        correctIndex: Int? = null,
        correctSentence: String? = null,
        scrambledWords: List<String>? = null,
        matchingPairs: List<MatchPair>? = null,
        audioUrl: String? = null,
        audioUrlSlow: String? = null
    ) = Question(
        id = id,
        sessionId = "session-1",
        word = "word-$id",
        type = type,
        prompt = prompt,
        options = options,
        correctIndex = correctIndex,
        correctSentence = correctSentence,
        scrambledWords = scrambledWords,
        translation = "translation-$id",
        audioUrl = audioUrl,
        audioUrlSlow = audioUrlSlow,
        matchingPairs = matchingPairs,
        imagePath = null
    )

    private fun questionWithCard(id: String, type: QuestionType) = QuestionWithCard(
        question = question(id, type),
        card = card(id)
    )

    @Test
    fun `node session maps all question types`() = runTest {
        val questions = listOf(
            question("q-intro", QuestionType.INTRODUCTION),
            question("q-fill", QuestionType.FILL_IN_BLANK, options = listOf("a", "b"), correctIndex = 0),
            question("q-mc", QuestionType.MULTIPLE_CHOICE, options = listOf("a", "b"), correctIndex = 1),
            question("q-scrambled", QuestionType.SCRAMBLED, scrambledWords = listOf("hello", "world"), correctSentence = "hello world"),
            question("q-listening", QuestionType.LISTENING, options = listOf("a", "b"), correctIndex = 0),
            question("q-matching", QuestionType.MATCHING, matchingPairs = listOf(MatchPair("left", "right"))),
            question("q-typing", QuestionType.TYPING, correctSentence = "type this")
        )
        vocabularyRepository.getSessionsByNodeResult = Result.success(listOf(Session(id = "session-1", nodeId = "node-1", index = 0, title = "S1", durationMinutes = 5, questionIds = questions.map { it.id })))
        vocabularyRepository.getQuestionsBySessionResult = Result.success(questions)
        vocabularyRepository.getCardByQuestionIdResult = card("card-1")

        val result = useCase(QuizSessionRequest.NodeSession("node-1", 0))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(7, data.questions.size)
        assertTrue(data.questions[0].type is QuizType.Introduction)
        assertTrue(data.questions[1].type is QuizType.FillInBlank)
        val fill = data.questions[1].type as QuizType.FillInBlank
        assertEquals(0, fill.correctIndex)
        assertEquals("a", fill.options[0])
        assertEquals("b", fill.options[1])
        assertEquals("word-q-fill", fill.word)
        assertTrue(data.questions[2].type is QuizType.MultipleChoice)
        assertTrue(data.questions[3].type is QuizType.ScrambledSentence)
        assertTrue(data.questions[4].type is QuizType.Listening)
        assertTrue(data.questions[5].type is QuizType.Matching)
        assertTrue(data.questions[6].type is QuizType.Typing)
        val mc = data.questions[2].type as QuizType.MultipleChoice
        assertEquals(QuestionDirection.EN_TO_VI, mc.direction)
        assertEquals(1, mc.correctIndex)
    }

    @Test
    fun `getSessionsByNode failure propagates same cause`() = runTest {
        val cause = IllegalStateException("sessions error")
        vocabularyRepository.failure = cause
        vocabularyRepository.getSessionsByNodeResult = Result.failure(cause)

        val result = useCase(QuizSessionRequest.NodeSession("node-1", 0))

        assertTrue(result.isFailure)
        assertSame(cause, result.exceptionOrNull())
    }

    @Test
    fun `getQuestionsBySession failure propagates same cause`() = runTest {
        val cause = IllegalStateException("questions error")
        vocabularyRepository.failure = cause
        vocabularyRepository.getSessionsByNodeResult = Result.success(listOf(Session(id = "session-1", nodeId = "node-1", index = 0, title = "S1", durationMinutes = 5, questionIds = emptyList())))
        vocabularyRepository.getQuestionsBySessionResult = Result.failure(cause)

        val result = useCase(QuizSessionRequest.NodeSession("node-1", 0))

        assertTrue(result.isFailure)
        assertSame(cause, result.exceptionOrNull())
    }

    @Test
    fun `session index out of range returns empty questions`() = runTest {
        vocabularyRepository.getSessionsByNodeResult = Result.success(listOf(Session(id = "session-1", nodeId = "node-1", index = 0, title = "S1", durationMinutes = 5, questionIds = emptyList())))
        vocabularyRepository.getQuestionsBySessionResult = Result.success(emptyList())

        val result = useCase(QuizSessionRequest.NodeSession("node-1", 5))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().questions.size)
    }

    @Test
    fun `empty question list returns empty questions`() = runTest {
        vocabularyRepository.getSessionsByNodeResult = Result.success(listOf(Session(id = "session-1", nodeId = "node-1", index = 0, title = "S1", durationMinutes = 5, questionIds = emptyList())))
        vocabularyRepository.getQuestionsBySessionResult = Result.success(emptyList())

        val result = useCase(QuizSessionRequest.NodeSession("node-1", 0))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().questions.size)
    }

    @Test
    fun `review node maps due cards to FSRSTailFlashcard`() = runTest {
        val dueCards = listOf(
            questionWithCard("card-1", QuestionType.TYPING),
            questionWithCard("card-2", QuestionType.TYPING)
        )
        vocabularyRepository.getDueCardsScopedResult = dueCards

        val result = useCase(QuizSessionRequest.ReviewNode("node-1", "unit-1", "section-1"))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(2, data.questions.size)
        assertTrue(data.questions.all { it.type is QuizType.FSRSTailFlashcard })
    }

    @Test
    fun `unit checkpoint aggregates LESSON and REVIEW nodes and caps at 16`() = runTest {
        val lessonQuestions = listOf(question("q1", QuestionType.MULTIPLE_CHOICE, options = listOf("a", "b"), correctIndex = 0))
        val reviewQuestions = listOf(
            question("q2", QuestionType.TYPING, correctSentence = "x"),
            question("q3", QuestionType.TYPING, correctSentence = "y"),
            question("q4", QuestionType.TYPING, correctSentence = "z")
        )
        val unitCheckpointQuestions = listOf(question("q5", QuestionType.TYPING, correctSentence = "z"))
        val nodes = listOf(
            Node(id = "node-lesson", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
            Node(id = "node-review", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = ""),
            Node(id = "node-check", unitId = "unit-1", index = 2, type = NodeType.UNIT_CHECKPOINT, title = "C", scenarioContext = "", icon = "")
        )
        vocabularyRepository.getNodesByUnitResult = flowOf(nodes)
        vocabularyRepository.getSessionsByNodeResult = Result.success(listOf(Session(id = "session-1", nodeId = "node-1", index = 0, title = "S", durationMinutes = 5, questionIds = emptyList())))
        vocabularyRepository.getQuestionsBySessionResult = Result.success(emptyList())
        var questionsBySession: Map<String, List<Question>> = emptyMap()

        // Override per-session behavior by mapping session id to questions
        // Note: FakeVocabularyRepository returns a single fixed result; for this test we use a single session.
        // We simulate two sessions by reusing the same fake result.
        // Instead, use a custom fake that maps node id to questions.
        val customFake = object : FakeVocabularyRepository() {
            override fun getNodesByUnit(unitId: String) = flowOf(nodes)
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(
                when (sessionId) {
                    "session-node-lesson" -> lessonQuestions
                    "session-node-review" -> reviewQuestions
                    "session-node-check" -> unitCheckpointQuestions
                    else -> emptyList()
                }
            )
            override suspend fun getCardByQuestionId(questionId: String): Card? = card(questionId)
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.UnitCheckpoint("unit-1"))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(4, data.questions.size)
        assertTrue(data.isUnitCheckpoint)
        assertEquals("unit-1", data.unitIdForUnitCheckpoint)
    }

    @Test
    fun `mistake review produces introduction and typing for each card`() = runTest {
        val mistakes = listOf(
            questionWithCard("card-1", QuestionType.TYPING),
            questionWithCard("card-2", QuestionType.TYPING)
        )
        vocabularyRepository.getMistakesResult = mistakes

        val result = useCase(QuizSessionRequest.MistakeReview(null))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(4, data.questions.size)
        assertTrue(data.questions[0].type is QuizType.Introduction)
        assertTrue(data.questions[1].type is QuizType.Typing)
        assertTrue(data.questions[2].type is QuizType.Introduction)
        assertTrue(data.questions[3].type is QuizType.Typing)
    }

    @Test
    fun `jump test aggregates LESSON and REVIEW nodes excluding checkpoints`() = runTest {
        val lessonQuestions = List(5) { i -> question("jq-lesson-$i", QuestionType.TYPING, correctSentence = "lesson-$i") }
        val reviewQuestions = List(5) { i -> question("jq-review-$i", QuestionType.TYPING, correctSentence = "review-$i") }
        val checkpointQuestions = List(5) { i -> question("jq-check-$i", QuestionType.TYPING, correctSentence = "check-$i") }
        val nodes = listOf(
            Node(id = "node-lesson", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
            Node(id = "node-review", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = ""),
            Node(id = "node-check", unitId = "unit-1", index = 2, type = NodeType.UNIT_CHECKPOINT, title = "C", scenarioContext = "", icon = "")
        )
        val questionsBySession = mapOf(
            "session-node-lesson" to lessonQuestions,
            "session-node-review" to reviewQuestions,
            "session-node-check" to checkpointQuestions
        )
        val customFake = object : FakeVocabularyRepository() {
            override fun getNodesByUnit(unitId: String) = flowOf(nodes)
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(
                questionsBySession[sessionId] ?: emptyList()
            )
            override suspend fun getCardByQuestionId(questionId: String): Card? = card(questionId)
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.JumpTest("unit-1"))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        // 5 lesson + 5 review (checkpoint excluded), no 20-cap truncation
        assertEquals(10, data.questions.size)
        assertTrue(data.isJumpTest)
        assertEquals("unit-1", data.unitIdForJumpTest)
    }

    @Test
    fun `jump test caps at 20 questions`() = runTest {
        val lessonQuestions = List(20) { i -> question("jq-cap-lesson-$i", QuestionType.TYPING, correctSentence = "l$i") }
        val reviewQuestions = List(20) { i -> question("jq-cap-review-$i", QuestionType.TYPING, correctSentence = "r$i") }
        val nodes = listOf(
            Node(id = "node-lesson", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
            Node(id = "node-review", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = "")
        )
        val questionsBySession = mapOf(
            "session-node-lesson" to lessonQuestions,
            "session-node-review" to reviewQuestions
        )
        val customFake = object : FakeVocabularyRepository() {
            override fun getNodesByUnit(unitId: String) = flowOf(nodes)
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(
                questionsBySession[sessionId] ?: emptyList()
            )
            override suspend fun getCardByQuestionId(questionId: String): Card? = card(questionId)
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.JumpTest("unit-1"))

        assertTrue(result.isSuccess)
        assertEquals(20, result.getOrThrow().questions.size)
    }

    @Test
    fun `jump test empty scope returns success with empty questions`() = runTest {
        // Only a unit checkpoint node exists; no LESSON/REVIEW -> nothing gathered.
        val nodes = listOf(
            Node(id = "node-check", unitId = "unit-1", index = 0, type = NodeType.UNIT_CHECKPOINT, title = "C", scenarioContext = "", icon = "")
        )
        val customFake = object : FakeVocabularyRepository() {
            override fun getNodesByUnit(unitId: String) = flowOf(nodes)
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(emptyList())
            override suspend fun getCardByQuestionId(questionId: String): Card? = null
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.JumpTest("unit-1"))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().questions.size)
        assertTrue(result.getOrThrow().isJumpTest)
    }

    @Test
    fun `section checkpoint aggregates LESSON and REVIEW nodes across units excluding checkpoints`() = runTest {
        val unit1Lesson = listOf(question("sq1", QuestionType.TYPING, correctSentence = "a"), question("sq2", QuestionType.TYPING, correctSentence = "b"))
        val unit1Review = listOf(question("sq3", QuestionType.TYPING, correctSentence = "c"), question("sq4", QuestionType.TYPING, correctSentence = "d"))
        val unit2Lesson = listOf(question("sq5", QuestionType.TYPING, correctSentence = "e"), question("sq6", QuestionType.TYPING, correctSentence = "f"))
        val unit2Review = listOf(question("sq7", QuestionType.TYPING, correctSentence = "g"), question("sq8", QuestionType.TYPING, correctSentence = "h"))
        val units = listOf(
            Unit(id = "unit-1", sectionId = "section-1", index = 0, topic = "T1", title = "U1", storySummary = "", icon = "", guidebookId = ""),
            Unit(id = "unit-2", sectionId = "section-1", index = 1, topic = "T2", title = "U2", storySummary = "", icon = "", guidebookId = "")
        )
        val nodesByUnit = mapOf(
            "unit-1" to listOf(
                Node(id = "u1-lesson", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
                Node(id = "u1-review", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = ""),
                Node(id = "u1-check", unitId = "unit-1", index = 2, type = NodeType.UNIT_CHECKPOINT, title = "C", scenarioContext = "", icon = "")
            ),
            "unit-2" to listOf(
                Node(id = "u2-lesson", unitId = "unit-2", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
                Node(id = "u2-review", unitId = "unit-2", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = ""),
                Node(id = "u2-check", unitId = "unit-2", index = 2, type = NodeType.UNIT_CHECKPOINT, title = "C", scenarioContext = "", icon = "")
            )
        )
        val questionsBySession = mapOf(
            "session-u1-lesson" to unit1Lesson,
            "session-u1-review" to unit1Review,
            "session-u2-lesson" to unit2Lesson,
            "session-u2-review" to unit2Review
        )
        val customFake = object : FakeVocabularyRepository() {
            override fun getUnitsBySection(sectionId: String) = flowOf(units)
            override fun getNodesByUnit(unitId: String) = flowOf(nodesByUnit[unitId] ?: emptyList())
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(
                questionsBySession[sessionId] ?: emptyList()
            )
            override suspend fun getCardByQuestionId(questionId: String): Card? = card(questionId)
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.SectionCheckpoint("section-1", "B1"))

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        // 2 lessons + 2 reviews per unit across 2 units (checkpoints excluded), no cap
        assertEquals(8, data.questions.size)
        assertTrue(data.isSectionCheckpoint)
        assertEquals("B1", data.nextSectionCefr)
    }

    @Test
    fun `section checkpoint caps at 20 questions`() = runTest {
        val unit1Lesson = List(20) { i -> question("sc-cap-1l-$i", QuestionType.TYPING, correctSentence = "a$i") }
        val unit1Review = List(20) { i -> question("sc-cap-1r-$i", QuestionType.TYPING, correctSentence = "b$i") }
        val unit2Lesson = List(20) { i -> question("sc-cap-2l-$i", QuestionType.TYPING, correctSentence = "c$i") }
        val unit2Review = List(20) { i -> question("sc-cap-2r-$i", QuestionType.TYPING, correctSentence = "d$i") }
        val units = listOf(
            Unit(id = "unit-1", sectionId = "section-1", index = 0, topic = "T1", title = "U1", storySummary = "", icon = "", guidebookId = ""),
            Unit(id = "unit-2", sectionId = "section-1", index = 1, topic = "T2", title = "U2", storySummary = "", icon = "", guidebookId = "")
        )
        val nodesByUnit = mapOf(
            "unit-1" to listOf(
                Node(id = "u1-lesson", unitId = "unit-1", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
                Node(id = "u1-review", unitId = "unit-1", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = "")
            ),
            "unit-2" to listOf(
                Node(id = "u2-lesson", unitId = "unit-2", index = 0, type = NodeType.LESSON, title = "L", scenarioContext = "", icon = ""),
                Node(id = "u2-review", unitId = "unit-2", index = 1, type = NodeType.REVIEW, title = "R", scenarioContext = "", icon = "")
            )
        )
        val questionsBySession = mapOf(
            "session-u1-lesson" to unit1Lesson,
            "session-u1-review" to unit1Review,
            "session-u2-lesson" to unit2Lesson,
            "session-u2-review" to unit2Review
        )
        val customFake = object : FakeVocabularyRepository() {
            override fun getUnitsBySection(sectionId: String) = flowOf(units)
            override fun getNodesByUnit(unitId: String) = flowOf(nodesByUnit[unitId] ?: emptyList())
            override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> = Result.success(
                listOf(Session(id = "session-$nodeId", nodeId = nodeId, index = 0, title = "S", durationMinutes = 5, questionIds = emptyList()))
            )
            override suspend fun getQuestionsBySession(sessionId: String): Result<List<Question>> = Result.success(
                questionsBySession[sessionId] ?: emptyList()
            )
            override suspend fun getCardByQuestionId(questionId: String): Card? = card(questionId)
        }
        val useCaseWithCustomFake = LoadQuizSessionUseCase(customFake)

        val result = useCaseWithCustomFake(QuizSessionRequest.SectionCheckpoint("section-1", "B1"))

        assertTrue(result.isSuccess)
        assertEquals(20, result.getOrThrow().questions.size)
    }

    @Test
    fun `section checkpoint repository failure propagates same cause`() = runTest {
        val cause = IllegalStateException("units error")
        vocabularyRepository.getUnitsBySectionFailure = cause

        val result = useCase(QuizSessionRequest.SectionCheckpoint("section-1", "B1"))

        assertTrue(result.isFailure)
        assertSame(cause, result.exceptionOrNull())
    }

    @Test
    fun `section checkpoint empty section returns success with empty questions`() = runTest {
        vocabularyRepository.getUnitsBySectionResult = flowOf(emptyList())

        val result = useCase(QuizSessionRequest.SectionCheckpoint("section-1", "B1"))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().questions.size)
        assertTrue(result.getOrThrow().isSectionCheckpoint)
        assertEquals("B1", result.getOrThrow().nextSectionCefr)
    }
}
