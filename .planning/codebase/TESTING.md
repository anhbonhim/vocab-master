# Testing Patterns

**Analysis Date:** 2026-07-22

## Test Framework

**Runner:**
- **JVM unit tests:** JUnit 4 (`junit:junit:4.13.2`)
- **Kotlin coroutines test:** `kotlinx-coroutines-test:1.10.2`
- **Android instrumented tests:** AndroidX Test runner, Espresso, Compose UI Test JUnit4
- **Robolectric:** Used for `SyncManager` unit tests that need Android SDK but not a device

**Config files:**
- Gradle: `app/build.gradle.kts` declares `testImplementation` and `androidTestImplementation` dependencies
- Domain module: `domain/build.gradle.kts` declares `testImplementation(libs.junit)` + `testImplementation(libs.kotlinx.coroutines.test)`
- Data module: `data/build.gradle.kts` (via version catalog)

**Run Commands (inferred from Gradle):**
```bash
./gradlew test                           # Run all JVM unit tests
./gradlew testDebugUnitTest              # Run debug unit tests only
./gradlew connectedAndroidTest           # Run instrumented tests on device/emulator
```

## Test File Organization

**Location:**
- JVM unit tests: Co-located in `src/test/java/` mirroring the production package structure
- Android instrumented tests: `app/src/androidTest/java/`
- Test doubles: Placed in a `fakes/` subdirectory under the package being tested

**Naming:**
- `<ClassName>Test.kt` — All test classes use the `Test` suffix
- Fake classes: `Fake<InterfaceName>.kt` (e.g., `FakeReviewRepository.kt`, `FakeVocabularyRepository.kt`)

**Structure:**
```
domain/src/test/java/com/nhimz/vocabmaster/domain/
├── usecase/
│   ├── SubmitReviewUseCaseTest.kt
│   ├── CompleteQuizSessionUseCaseTest.kt
│   ├── LoadQuizSessionUseCaseTest.kt
│   ├── EvaluateAnswerUseCaseTest.kt
│   ├── UseCasesTest.kt
│   └── fakes/
│       ├── FakeVocabularyRepository.kt
│       ├── FakeReviewRepository.kt
│       └── FakeSettingsRepository.kt
└── fsrs/v6/
    ├── GoldenVectorTest.kt
    ├── OptimizerTest.kt
    └── PyFsrsParityTest.kt

data/src/test/java/com/nhimz/vocabmaster/data/
├── database/
│   ├── VocabDaoTest.kt
│   └── VocabDatabaseSmokeTest.kt
├── repository/
│   └── VocabularyRepositoryImplTest.kt
└── sync/
    └── SyncManagerTest.kt

app/src/test/java/com/nhimz/vocabmaster/
├── ui/viewmodel/
│   ├── QuizViewModelTest.kt
│   ├── QuizViewModelPersistenceTest.kt
│   ├── fakes/
│   │   ├── FakeVocabularyRepository.kt
│   │   ├── FakeReviewRepository.kt
│   │   └── FakeSettingsRepository.kt
│   └── MainDispatcherRule.kt
├── ui/screens/
│   └── Plan0301ContainerContentTest.kt
└── ui/components/quiz/
    └── ScrambledWordMapperTest.kt

app/src/androidTest/java/com/nhimz/vocabmaster/
└── navigation/
    └── NavGraphTest.kt
```

## Test Structure

**Suite Organization:**
```kotlin
// Domain use case test — plain JUnit, no runner needed
class SubmitReviewUseCaseTest {
    private val reviewRepository = FakeReviewRepository()
    private val settingsRepository = FakeSettingsRepository()
    private val mapRatingUseCase = MapRatingUseCase()
    private val useCase = SubmitReviewUseCase(reviewRepository, settingsRepository, mapRatingUseCase)

    @Test
    fun `flashcard happy path schedules card and awards XP`() = runTest {
        val question = QuizQuestion(flashcardQuestion(Rating.Good))
        val result = useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10, explicitRating = Rating.Good)
        assertTrue(result.isSuccess)
        assertEquals(1, reviewRepository.recordReviewCalls)
        assertEquals(10, settingsRepository.lastAddedXp)
    }
}
```

**App ViewModel test — requires `MainDispatcherRule`:**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vocabRepo: FakeVocabularyRepository
    private lateinit var reviewRepo: FakeReviewRepository

    @Before
    fun setUp() {
        vocabRepo = FakeVocabularyRepository()
        reviewRepo = FakeReviewRepository()
        mapRatingUseCase = MapRatingUseCase()
        submitReviewUseCase = SubmitReviewUseCase(reviewRepo, settingsRepo, mapRatingUseCase)
    }

    @Test
    fun `startNodeSession with fake repo failure emits QuizUiState Error`() = runTest {
        vocabRepo.failure = RuntimeException("Database error")
        val viewModel = createViewModel()
        viewModel.startNodeSession("node_1", 0)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is QuizUiState.Error)
        assertEquals("Database error", (state as QuizUiState.Error).message)
    }
}
```

**Android instrumented test — uses `@RunWith`:**
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")
class SyncManagerTest {
    @Test
    fun testSyncNetworkFailure_pushThrowsIoException_returnsFalse() = runTest { ... }
}
```

**Pure navigation test — no runner, no Android deps:**
```kotlin
class NavGraphTest {
    @Test
    fun all_routes_implement_NavKey() {
        val welcome: NavKey = Screen.Welcome
        assertNotNull(welcome)
    }
}
```

**Patterns:**
- **Arrange-Act-Assert** structure with blank line separation between phases
- **`runTest`** from `kotlinx-coroutines-test` wraps every coroutine test
- **`advanceUntilIdle()`** used to flush coroutine dispatchers in ViewModel tests
- **Descriptive backtick test names** (Kotlin) documenting the scenario being tested
- **Underscore test names** in Robolectric/SyncManager tests (`testSyncNetworkFailure_...`)
- **Data model contract tests** in `Plan0301ContainerContentTest` for UI state data classes

## Mocking

**Framework:** No mocking library (MockK, Mockito) detected. The project exclusively uses hand-written fake implementations.

**Patterns:**
```kotlin
// Fake class implementing Repository interface
class FakeReviewRepository : ReviewRepository {
    var recordReviewCalls: Int = 0
    var lastRecordedCard: Card? = null
    var lastRecordedLog: ReviewLog? = null
    var recordReviewFailure: Throwable? = null

    override suspend fun recordReview(card: Card, log: ReviewLog) {
        recordReviewFailure?.let { throw it }
        recordReviewCalls++
        lastRecordedCard = card
        lastRecordedLog = log
    }

    // Unused methods get TODO
    override suspend fun insertReviewLog(cardId: String, log: ReviewLog) =
        TODO("not needed for these tests")
}
```

**What to Mock:**
- Repository interfaces (`VocabularyRepository`, `ReviewRepository`, `SettingsRepository`)
- DAOs (`VocabDao` — in `SyncManagerTest` an inline `FakeVocabDao` is used)
- API services (`SyncApiService` — lambda-based stubs in `SyncManagerTest`)
- Android `Context` — via Robolectric `ApplicationProvider.getApplicationContext()`

**What NOT to Mock:**
- Pure use cases with no side effects (e.g., `MapRatingUseCase`, `EvaluateAnswerUseCase`)
- Data model classes (`Card`, `Question`, `AnswerResult`)
- `Scheduler` — validated through `GoldenVectorTest` with real JSON golden vectors

**Stub pattern (used in SyncManagerTest):**
```kotlin
private fun stubApiService(
    pushResult: () -> Response<Unit>,
    pullResult: () -> Response<SyncPayload>
): SyncApiService = object : SyncApiService {
    override suspend fun pushSync(payload: SyncPayload): Response<Unit> = pushResult()
    override suspend fun pullSync(since: Long): Response<SyncPayload> = pullResult()
}
```

## Fixtures and Factories

**Test Data:**
- Inline factory functions within test files
- Custom `QuestionWithCard`/`QuizQuestion` builders per test case

```kotlin
// Domain test — helper function inside test class
private fun flashcardQuestion(rating: Rating? = null): QuizType.FSRSTailFlashcard {
    val item = QuestionWithCard(
        Question(id = "q1", sessionId = "s1", word = "w", type = QuestionType.TYPING, prompt = "p", ...),
        Card(cardId = "q1", state = State.New)
    )
    return QuizType.FSRSTailFlashcard(item)
}

// ViewModel test — inline question building
val question = Question(
    id = "q1",
    type = QuestionType.MULTIPLE_CHOICE,
    prompt = "Hello",
    options = listOf("Xin chào", "Tạm biệt"),
    correctIndex = 0
)
```

**Location:**
- No shared test fixtures directory detected
- Each test file contains its own inline helpers

## Coverage

**Requirements:** Not enforced as a build step (no JaCoCo/Kover configuration detected in Gradle files)

**View Coverage (inferred):**
```bash
./gradlew testDebugUnitTest jacocoTestReport   # If configured, but not currently set up
```

## Test Types

**Unit Tests:**
- **Domain use case tests** (`domain/src/test/java/...`): Pure JVM, run with `runTest`, test business logic in isolation via fake repositories
- **FSRS algorithm tests** (`domain/src/test/java/.../fsrs/v6/`): Golden vector tests, optimizer tests, parity tests against Python py-fsrs
- **ViewModel tests** (`app/src/test/java/.../ui/viewmodel/`): Test state transitions by injecting fake repositories and using `MainDispatcherRule`
- **Data model contract tests** (`app/src/test/java/.../ui/screens/`): Test data class default values and field contracts without Android dependencies

**Integration Tests:**
- **DAO tests** (`data/src/test/java/.../database/`): `VocabDaoTest.kt`, `VocabDatabaseSmokeTest.kt` — test Room queries with in-memory databases
- **Repository implementation tests** (`data/src/test/java/.../repository/`): `VocabularyRepositoryImplTest.kt`
- **SyncManager tests** (`data/src/test/java/.../sync/`): Test network resilience, time-based merging, and log preservation — uses Robolectric + fake API

**E2E Tests:** Not used

**Android Instrumented Tests:**
- `NavGraphTest.kt` — Tests navigation routing on a real/compat Android runtime (runs in `androidTest/`, but is pure JVM-compatible navigation logic)

## Common Patterns

**Async Testing:**
```kotlin
@Test
fun `flashcard happy path schedules card and awards XP`() = runTest {
    val result = useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10)
    assertTrue(result.isSuccess)
}
```

**ViewModel coroutine testing with `advanceUntilIdle`:**
```kotlin
@Test
fun `startNodeSession happy path transitions to Active state`() = runTest {
    vocabRepo.getSessionsByNodeResult = Result.success(listOf(session))
    vocabRepo.getQuestionsBySessionResult = Result.success(listOf(question))

    val viewModel = createViewModel()
    viewModel.startNodeSession("node_1", 0)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state is QuizUiState.Active)
}
```

**Error Testing:**
```kotlin
@Test
fun `recordReview failure returns failure and does not award XP`() = runTest {
    reviewRepository.recordReviewFailure = IllegalStateException("db error")
    val result = useCase(question, isCorrect = true, responseTimeMs = 2000, xpEarned = 10)
    assertTrue(result.isFailure)
    assertEquals(0, settingsRepository.addXpCalls)
}
```

**State machine testing (rapid double tap guard):**
```kotlin
@Test
fun `rapid double submit in same frame calls SubmitReviewUseCase exactly once`() = runTest {
    // Submit twice synchronously without advancing dispatcher
    viewModel.submitAnswer(optionIndex = 0)
    viewModel.submitAnswer(optionIndex = 0)
    advanceUntilIdle()
    assertEquals(1, reviewRepo.recordReviewCalls)
}
```

**Golden vector / parameterized testing:**
```kotlin
@Test
fun testGoldenVectors() {
    val data = json.decodeFromString<GoldenData>(jsonStr)
    for (vector in data.vectors) {
        var card = Card(...)
        for (i in vector.reviews.indices) {
            val result = scheduler.reviewCard(card, mapRating(...), ...)
            card = result.first
            assertEquals(expected.stability, card.stability!!, 1e-6)
            assertEquals(expected.interval_days, intervalDays)
        }
    }
}
```

**SavedStateHandle persistence testing (process death simulation):**
```kotlin
@Test
fun `fresh ViewModel restores cumulative state from SavedStateHandle across process death`() = runTest {
    val handle = SavedStateHandle(mapOf(
        "quiz_kind" to "NODE",
        "quiz_correct_count" to 1,
        "quiz_xp_gained" to 10,
        ...
    ))
    val viewModel = createViewModel(handle)
    advanceUntilIdle()
    val state = viewModel.uiState.value
    val active = state as QuizUiState.Active
    assertEquals(1, active.correctAnswersCount)
    assertEquals(10, active.xpGained)
}
```

**Persistence key whitelist pattern:**
```kotlin
// In production ViewModel
val PERSISTENCE_KEYS = setOf(
    KEY_QUIZ_KIND, KEY_NODE_ID, KEY_CURRENT_INDEX, ...
)

// In test — verify all keys are whitelisted
@Test
fun `flaky test guard — all SavedStateHandle keys are registered in PERSISTENCE_KEYS`() = runTest {
    // Tests that no key is persisted without being in the whitelist
}
```

## ViewModel Test Infrastructure

**`MainDispatcherRule`:**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

Used as `@get:Rule` in all ViewModel tests to override `Dispatchers.Main` with `UnconfinedTestDispatcher`.

## Known Test Limitations

- **`@Ignore("Robolectric Conscrypt native library is unavailable on this Termux aarch64 environment.")`** — The `SyncManagerTest` is annotated with `@Ignore` because the Robolectric Conscrypt native library cannot load on Termux aarch64 architecture. These tests are verified by running on a standard JVM.
- **No connected device tests** — `connectedDebugAndroidTest` target cannot run on Termux aarch64 (no connected device/emulator). Compose UI tests (`ui-test-junit4`) are declared as dependencies but not actively run.
- **No mocking library** — The absence of MockK/Mockito means all fakes must be manually maintained. When repository interfaces change, fakes in both `domain/src/test/` and `app/src/test/` must be updated.
- **No code coverage tool configured** — JaCoCo or Kover not detected in build files.

---

*Testing analysis: 2026-07-22*
