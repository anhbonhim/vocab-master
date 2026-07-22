# Testing Patterns

**Analysis Date:** 2026-07-22

## Test Framework

**Runner:**
- JUnit 4 (`org.junit.Test`) for both unit tests and instrumented tests.
- Robolectric (`RobolectricTestRunner`) for Room database tests.
- AndroidX Test Runner for instrumented tests.

**Assertion Library:**
- JUnit's `org.junit.Assert.*` (assertEquals, assertTrue, assertFalse, assertNotNull, assertNull).
- Standard assertions only — no third-party assertion libraries (Hamcrest, Truth) are used.

**Mocking:**
- **No mocking framework** (Mockito, MockK) is used anywhere in the codebase.
- All test dependencies are replaced via **hand-written Fake implementations** that implement the same interface.

**Run Commands:**
```bash
./gradlew testDebugUnitTest    # Run all unit tests
./gradlew test                 # Run all unit tests (all variants)
./gradlew connectedDebugAndroidTest  # Run instrumented tests (requires device/emulator)
```

## Test File Organization

**Location:**
- Unit tests mirror the source package structure under `app/src/test/java/`, `domain/src/test/java/`, `data/src/test/java/`.
- Instrumented tests (if any) would be in `app/src/androidTest/java/`.

**Naming:**
- Test classes: `{ClassName}Test` (e.g., `QuizViewModelTest.kt`, `EvaluateAnswerUseCaseTest.kt`).
- Fake classes: `Fake{InterfaceName}` (e.g., `FakeVocabularyRepository.kt`, `FakeReviewRepository.kt`).
- Test rule classes: descriptive + `Rule` (e.g., `MainDispatcherRule.kt`).

**Test data helper files:**
- `domain/src/test/resources/fsrs/golden_vectors.json` — golden vector fixture for FSRS parity tests.

**Structure:**
```
app/src/test/java/com/nhimz/vocabmaster/
├── ui/
│   ├── viewmodel/
│   │   ├── QuizViewModelTest.kt
│   │   ├── QuizViewModelPersistenceTest.kt
│   │   ├── MainDispatcherRule.kt
│   │   └── fakes/
│   │       ├── FakeVocabularyRepository.kt
│   │       ├── FakeReviewRepository.kt
│   │       └── FakeSettingsRepository.kt
│   ├── screens/
│   │   └── Plan0301ContainerContentTest.kt
│   └── components/
│       └── quiz/
│           └── ScrambledWordMapperTest.kt
app/src/androidTest/java/com/nhimz/vocabmaster/
└── navigation/
    └── NavGraphTest.kt
domain/src/test/java/com/nhimz/vocabmaster/
├── usecase/
│   ├── EvaluateAnswerUseCaseTest.kt
│   ├── SubmitReviewUseCaseTest.kt
│   ├── LoadQuizSessionUseCaseTest.kt
│   ├── CompleteQuizSessionUseCaseTest.kt
│   ├── UseCasesTest.kt
│   └── fakes/
│       ├── FakeVocabularyRepository.kt
│       ├── FakeReviewRepository.kt
│       └── FakeSettingsRepository.kt
└── fsrs/
    └── v6/
        ├── GoldenVectorTest.kt
        ├── PyFsrsParityTest.kt
        └── OptimizerTest.kt
data/src/test/java/com/nhimz/vocabmaster/
├── database/
│   ├── VocabDaoTest.kt
│   └── VocabDatabaseSmokeTest.kt
└── repository/
    └── VocabularyRepositoryImplTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
// Unit test example — domain use case
package com.nhimz.vocabmaster.domain.usecase

class EvaluateAnswerUseCaseTest {
    private val useCase = EvaluateAnswerUseCase()

    @Test
    fun `Introduction always correct with 1 XP`() {
        val result = useCase(QuizType.Introduction(null, "prompt", null))
        assertTrue(result.isSuccess)
        assertEquals(AnswerResult(isCorrect = true, xpEarned = 1, rating = null), result.getOrThrow())
    }
}
```

**Patterns:**
- Backtick-delimited test function names describing behavior (e.g., `` `startNodeSession happy path transitions to Active state` ``).
- Standard Arrange-Act-Assert (AAA) within each test.
- Branch coverage per question type / rating combination.
- Pure function tests do not need `runTest`; coroutine tests use `runTest`.

## ViewModel Test Pattern

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vocabRepo: FakeVocabularyRepository
    // ... other fakes

    @Before
    fun setUp() {
        vocabRepo = FakeVocabularyRepository()
        // instantiate use cases with fakes
    }

    private fun createViewModel(...): QuizViewModel { ... }

    @Test
    fun `startNodeSession happy path transitions to Active state`() = runTest {
        // Arrange
        vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
        vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))
        val viewModel = createViewModel()

        // Act
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue("Expected Active state but got $state", state is QuizUiState.Active)
    }
}
```

**Key patterns:**
- `MainDispatcherRule` replaces `Dispatchers.Main` with `UnconfinedTestDispatcher`.
- `advanceUntilIdle()` used to flush coroutines launched by ViewModel.
- Fakes are configured before ViewModel creation by setting mutable result properties.
- `SavedStateHandle` is constructed manually with `mapOf(...)` for persistence tests.

## Mocking / Fakes

**Framework:** None. **Hand-written fakes** are used exclusively.

**Pattern:**
```kotlin
// app/src/test/java/.../fakes/FakeVocabularyRepository.kt
open class FakeVocabularyRepository : VocabularyRepository {
    var getSessionsByNodeResult: Result<List<Session>> = Result.success(emptyList())
    var getQuestionsBySessionResult: Result<List<Question>> = Result.success(emptyList())
    var failure: Throwable? = null

    override suspend fun getSessionsByNode(nodeId: String): Result<List<Session>> =
        failure?.let { Result.failure(it) } ?: getSessionsByNodeResult

    override suspend fun getCardsByTopic(topic: String, ...): Flow<List<QuestionWithCard>> =
        TODO("not needed for these tests")
}
```

**Fake design rules:**
- Mutable result properties (e.g., `var getSessionsByNodeResult`) that tests set before exercise.
- `var failure: Throwable?` controls error injection globally.
- Call counters (e.g., `var markNodeCompletedCalls: Int`) for interaction verification.
- Last-argument capture (e.g., `var lastMarkNodeCompletedArgs: Triple<...>?` ) for argument verification.
- Unused interface methods use `TODO("not needed for these tests")` — not mocked by default.
- Fakes are duplicated per module (`app/src/test/` and `domain/src/test/`) since they're in different Gradle modules.

## Fixtures and Factories

**Inline fixture helpers:**
```kotlin
// LoadQuizSessionUseCaseTest.kt
private fun card(questionId: String) = Card(cardId = questionId)
private fun question(id: String, type: QuestionType, ...) = Question(...)
private fun questionWithCard(id: String, type: QuestionType) = QuestionWithCard(...)
```

**Script-generated fixtures:**
- `domain/src/test/resources/fsrs/golden_vectors.json` — generated by `domain/scripts/generate_fsrs_golden_vectors.py`, consumed by `GoldenVectorTest`.

**No dedicated fixture/factory files** — helpers are kept in the test class they serve.

## Coverage

**Requirements:** Not enforced — no JaCoCo/Kover configuration found.

**View Coverage:**
```bash
# If Kover or JaCoCo is added later:
./gradlew koverHtmlReport
```

## Test Types

**Unit Tests (30+ test files, ~150+ test cases):**

| Module | Focus | Framework |
|--------|-------|-----------|
| `domain/` | Use case business logic, FSRS algorithm, quiz evaluation | JUnit 4, `runTest` |
| `app/` | ViewModel state transitions, persistence, Compose UI state, pure functions | JUnit 4, `runTest`, `MainDispatcherRule` |
| `data/` | Room DAO operations, Repository error handling | JUnit 4, Robolectric |

**Domain Use Case Tests:**
- `EvaluateAnswerUseCaseTest.kt` — Pure function tests covering all `QuizType` variants (Introduction, MultipleChoice, Listening, Typing, Matching, ScrambledSentence, FSRSTailFlashcard) with correct/incorrect branches and XP values.
- `SubmitReviewUseCaseTest.kt` — Tests FSRS scheduling and XP awarding; verifies explicit rating wins over automatic `MapRatingUseCase`; tests non-flashcard and card-less question paths.
- `LoadQuizSessionUseCaseTest.kt` — Tests all question type mappings (7 types) for node sessions; review session loading; unit checkpoint node counting.
- `CompleteQuizSessionUseCaseTest.kt` — Tests `UpdateStreakUseCase` always called; jump test/pass threshold; section checkpoint placement level; unit/section checkpoint node marking.
- `UseCasesTest.kt` — Tests `MapRatingUseCase` (rating by correctness + response time) and `PlacementTestUseCase` (level transitions).

**FSRS Algorithm Tests:**
- `PyFsrsParityTest.kt` (960 lines, 25+ tests) — Comprehensive Kotlin port of py-fsrs regression suite: review scheduling, learning steps, relearning states, interval bounds, serialization (dict/JSON), scheduler parameters, rescheduling, fuzz property verification.
- `GoldenVectorTest.kt` — Parameterized golden vector test against `golden_vectors.json` (30+ vectors generated by Python reference implementation).
- `OptimizerTest.kt` — Property-based optimizer test: default guard validation, loss decreases after training, deterministic retention candidates.

**ViewModel Tests:**
- `QuizViewModelTest.kt` — Tests state transitions (Loading → Active/Error/Completed), answer submission with correct/incorrect branching, FSRS rating flow, retry/review mode.
- `QuizViewModelPersistenceTest.kt` — Tests `SavedStateHandle` persistence across process death: restoring NODE/MISTAKE_REVIEW quiz types, progression tracking, multi-question session restoration.

**Data Layer Tests:**
- `VocabDaoTest.kt` (386 lines) — Room DAO: insert/read/update card v8 shape, FSRS v6 field persistence, order-by-due, session listing, question-by-session, section/unit/node queries. **All @Ignore'd** with note: "Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment."
- `VocabDatabaseSmokeTest.kt` — Room in-memory DB smoke test. **@Ignore'd** same reason.
- `VocabularyRepositoryImplTest.kt` — Tests malformed JSON handling (options, matchingPairs), missing fields, unknown question types — verifies `VocabDataException` propagation. **@Ignore'd** same reason.

**Compose UI Tests:**
- `NavGraphTest.kt` (in `androidTest/`, but pure JVM) — Route type-safety verification (all `Screen` subtypes implement `NavKey`); back-stack semantics (push/pop/top-level); carries correct arguments.
- `Plan0301ContainerContentTest.kt` — Tests data class contracts for Container/Content split: `HomeScreenUiState`, `SettingsUiModel`, `SettingsActions` default values and field carrying; `DestructiveDialog` enum states.

**UI Component Tests:**
- `ScrambledWordMapperTest.kt` — Tests pure function `calculateScrambledIndex` (no duplicates, duplicates) and `calculateSelectedIndices` (multi-occurrence tracking).

**Test data generation scripts:**
- `domain/scripts/generate_fsrs_golden_vectors.py` — Python script that calls py-fsrs to produce golden test vectors.

## Common Patterns

**Coroutine Testing:**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `test description`() = runTest {
        // Use advanceUntilIdle() to flush coroutines
        viewModel.someMethod()
        advanceUntilIdle()
        assertEquals(expected, viewModel.uiState.value)
    }
}
```

**Error Testing:**
```kotlin
@Test
fun `malformed options Json fails loudly`() = runTest {
    val dao = vocabDao ?: return@runTest
    seedOneQuestion(dao, options = "{not valid json")
    val repo = VocabularyRepositoryImpl(dao, context)
    val result = repo.getQuestionsBySession("session_1")
    assertTrue("Malformed options must produce a failure", result.isFailure)
    assertTrue("Failure must be VocabDataException", result.exceptionOrNull() is VocabDataException)
}
```

**Fake-based interaction verification:**
```kotlin
@Test
fun `UpdateStreakUseCase is always invoked`() = runTest {
    useCase(QuizCompletionInput(correctCount = 5, totalQuestions = 10, ...))
    assertEquals(1, updateStreakUseCase.executeCalls)
}

@Test
fun `jump test pass marks all unit nodes completed`() = runTest {
    vocabularyRepository.getNodesByUnitResult = flowOf(nodes)
    val result = useCase(...)
    assertTrue(result.isSuccess)
    assertEquals(2, vocabularyRepository.markNodeCompletedCalls)
}
```

**FSRS algorithm delta assertions:**
```kotlin
assertTrue("first Good due ~10 min", firstDeltaMinutes in 9.5..10.5)
assertTrue("Again due ~1 min", deltaSeconds in 55.0..65.0)
assertTrue("interval >= 1 day", intervalDays >= 1)
assertEquals(53.62691, card.stability!!, 1e-4)
```

**`@Ignore` pattern for environment-incompatible tests:**
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
class VocabDaoTest { ... }
```

**Skipped test with try-catch fallback:**
```kotlin
@Before
fun setup() {
    try {
        database = Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()
    } catch (e: UnsatisfiedLinkError) {
        println("Skipping Room SQLite test on Termux due to UnsatisfiedLinkError: ${e.message}")
    }
}
```

## Unexplored / Gaps

- **No Compose UI screenshot tests** — Compose `createComposeRule` tests are not present despite dependencies being declared.
- **No E2E tests** — No instrumentation tests beyond `NavGraphTest`.
- **No performance benchmarks** — No `macrobenchmark` or `microbenchmark` modules.
- **No code coverage enforcement** — No JaCoCo, Kover, or coverage gate in CI.
- **Data layer tests are all `@Ignore`'d** — Room DB tests (`VocabDaoTest`, `VocabDatabaseSmokeTest`, `VocabularyRepositoryImplTest`) are disabled due to Termux Robolectric incompatibility, creating a blind spot for data-layer changes.
- **Dedicated fake directories per module** — `app/src/test/` and `domain/src/test/` each have their own copy of `FakeVocabularyRepository`, `FakeReviewRepository`, `FakeSettingsRepository`. These are not shared between modules.

---

*Testing analysis: 2026-07-22*
