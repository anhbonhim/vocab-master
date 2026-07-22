# Coding Conventions

**Analysis Date:** 2026-07-22

## Naming Patterns

**Files:**
- Kotlin source files use PascalCase matching the primary class name (e.g., `QuizViewModel.kt`, `VocabApplication.kt`).
- Compose screen files end with `Screen` or `ScreenContent` (e.g., `HomeScreen.kt`, `HomeScreenContent.kt`, `SettingsScreen.kt`).
- Compose component files use descriptive names (e.g., `MatchingQuestionCard.kt`, `Duo3DCard.kt`, `FeedbackBanner.kt`).
- Repository interface files are in `domain/model/` and end with `Repository` (e.g., `VocabularyRepository.kt`, `ReviewRepository.kt`).
- Repository implementation files are in `data/repository/` and end with `Impl` (e.g., `VocabularyRepositoryImpl.kt`, `ReviewRepositoryImpl.kt`).
- Database entity files are in `data/database/entity/` and end with `Entity` (e.g., `FsrsCardEntity.kt`, `QuestionEntity.kt`).
- Use case files are in `domain/usecase/` and use descriptive verb-noun names (e.g., `EvaluateAnswerUseCase.kt`, `SubmitReviewUseCase.kt`, `CompleteQuizSessionUseCase.kt`).
- Test files mirror the source package structure with `Test` suffix (e.g., `QuizViewModelTest.kt`, `ScrambledWordMapperTest.kt`).
- Fake files use `Fake` prefix (e.g., `FakeVocabularyRepository.kt`, `FakeReviewRepository.kt`).

**Functions:**
- Standard Kotlin functions use camelCase (e.g., `startNodeSession`, `submitAnswer`, `setupCrashHandler`).
- Jetpack Compose composable functions use PascalCase with `@Composable` annotation (e.g., `HomeScreenContent`, `MatchingQuestionCard`, `SettingsActions`).
- ViewModel functions are camelCase, often named after user actions (e.g., `submitAnswer`, `startNodeSession`).
- Use case classes implement `operator fun invoke()` for clean call-site syntax.
- Use case functions internally use `execute()` naming (e.g., `MapRatingUseCase.execute()`).
- Private utility functions in tests use camelCase (e.g., `createViewModel`, `flashcardQuestion`, `qWithCard`).

**Variables:**
- Standard Kotlin camelCase (e.g., `settingsRepositoryImpl`, `lastKnownVersion`, `dailyGoalXp`).
- Private backing properties for `MutableStateFlow` use underscore prefix (e.g., `_logs`, `_uiState`).
- Constants use `UPPER_SNAKE_CASE` marked `const val` in `companion object` (e.g., `MAX_LOG_COUNT`, `DEFAULT_LOAD_ERROR`, `KEY_QUIZ_KIND`).
- Composable lambdas use PascalCase for factory functions or descriptive names (e.g., `onDailyGoalChange`, `onSync`, `onRestore`).

**Types:**
- Classes and interfaces use PascalCase.
- Sealed interfaces use PascalCase (e.g., `QuizUiState`, `QuizType`).
- Data classes nested inside sealed interfaces (e.g., `QuizUiState.Active`, `QuizUiState.Completed`, `QuizUiState.Error`).
- Enum classes use PascalCase (e.g., `QuestionType`, `Rating`, `State`, `NodeType`).

## Code Style

**Formatting:**
- Standard Kotlin conventions consistent with IntelliJ/Android Studio defaults.
- Indentation: 4 spaces.

**Linting:**
- **Tool:** Detekt (`io.gitlab.arturbosch.detekt`).
- **Configuration:** `config/detekt/detekt.yml` + `config/detekt/baseline.xml`.
- **Key rules enabled under `complexity` active=true:**
  - `ComplexCondition`, `ComplexInterface`, `ComplexMethod`
  - `CyclomaticComplexMethod`, `LabeledExpression`
  - `LargeClass`, `LongMethod`, `LongParameterList`
  - `MethodOverloading`, `NestedBlockDepth`
  - `ReplaceSafeCallChainWithRun`, `StringLiteralDuplication`, `TooManyFunctions`
- Some long test files suppress certain Detekt rules with `@Suppress("LabeledExpression")` (e.g., `VocabularyRepositoryImplTest.kt`).

## Package Organization

**App module** (`app/src/main/java/com/nhimz/vocabmaster/`):
- `ui/screens/` — Top-level Screen composables and `*ScreenContent.kt` files
- `ui/screens/statistics_components/` — Statistics tab content (OverviewTab, MistakeBankTab, BadgesTab)
- `ui/screens/debug_components/` — Debug test runner components (DataIntegrityTests, SystemSettingsTests, TestRunner)
- `ui/components/` — Reusable Compose components (Duo3DCard, DuoSnackbar, SnackbarMessage)
- `ui/components/quiz/` — Quiz-specific components (MatchingQuestionCard, TypingQuestionCard, ScrambledWordMapper, etc.)
- `ui/viewmodel/` — ViewModel classes and UI state models
- `ui/navigation/` — NavGraph, Screen sealed class
- `ui/theme/` — Theme files (Color, Theme, Type, AppIcons)
- `ui/util/` — UI utilities (FeedbackHelper)
- `util/` — App utilities (LocalLogger)
- `notification/` — Notification scheduling and receiver
- `audio/` — CDN audio player

**Domain module** (`domain/src/main/java/com/nhimz/vocabmaster/domain/`):
- `model/` — Domain model interfaces (VocabularyRepository, ReviewRepository, SettingsRepository) and data classes (Question, Session, Node, Curriculum, VocabDataException)
- `model/quiz/` — Quiz domain types (QuizType, QuizSessionModels, QuestionDirection)
- `usecase/` — Business logic use cases (EvaluateAnswerUseCase, SubmitReviewUseCase, LoadQuizSessionUseCase, CompleteQuizSessionUseCase, etc.)
- `fsrs/v6/` — FSRS spaced repetition algorithm (Scheduler, Optimizer, State, Card, ReviewLog)

**Data module** (`data/src/main/java/com/nhimz/vocabmaster/data/`):
- `database/` — Room database (VocabDatabase, VocabDao, Converters)
- `database/entity/` — Room entity classes (FsrsCardEntity, QuestionEntity, ReviewLogEntity, etc.)
- `repository/` — Repository implementations (VocabularyRepositoryImpl, ReviewRepositoryImpl, etc.)
- `remote/` — API clients and network models (ApiClient, AuthManager, SyncPayload, etc.)
- `di/` — Hilt DI module (DataModule)
- `sync/` — Sync management (SyncManager)
- `auth/` — Authentication (AuthManager)
- `model/` — Data-layer models (BackupModels)

## Import Organization

**Order:**
1. `package` declaration
2. Blank line
3. Android/Kotlin standard library imports (`androidx.*`, `kotlinx.*`, `java.*`)
4. Project imports (`com.nhimz.vocabmaster.*`)
5. Blank line
6. Third-party library imports (`org.junit.*`, `dagger.hilt.*`)

**Wildcard imports:**
- Not used; explicit imports are preferred.

## Error Handling

**Patterns:**
- Use `Result<T>` return type and `runCatching {}` at use case boundaries for predictable error propagation.
  ```kotlin
  // domain/src/main/java/.../usecase/EvaluateAnswerUseCase.kt
  class EvaluateAnswerUseCase @Inject constructor() {
      operator fun invoke(...): Result<AnswerResult> = runCatching { ... }
  }
  ```
- Custom typed exception `VocabDataException` in `domain/src/main/java/com/nhimz/vocabmaster/domain/model/VocabDataException.kt` for data-layer parse failures — carries a human-readable message and the original `cause`.
- Implementations propagate errors using `Result.failure(...)` from `VocabularyRepositoryImpl`.
- ViewModels catch errors from use cases and map them to UI state (e.g., `QuizUiState.Error`).
- `try-catch` blocks used for platform-specific operations that may throw `UnsatisfiedLinkError` (Robolectric/Termux CI environment fallback).
- `TODO("not needed for these tests")` used as stub implementation in fake repositories for methods not exercised by current tests.

## Logging

**Framework:** Custom `LocalLogger` singleton in `app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`.

**Patterns:**
- Wraps `android.util.Log` with in-memory buffer exposed via `StateFlow`.
- Levels: `d()` (debug), `i()` (info), `w()` (warning), `e()` (error with optional `Throwable`).
- `section(tag, title)` helper for prominent log boundaries.
- `setupCrashHandler()` — installs a global uncaught exception handler that logs via `LocalLogger.e(...)` before delegating to the default handler.
- In-memory buffer capped at `MAX_LOG_COUNT = 500` entries.
- `getExportString()` exports all buffered logs for debug purposes.
- Logging is initialized in `VocabApplication` only in `BuildConfig.DEBUG` mode.

**Tag convention:**
- Each file defines a `TAG` constant (e.g., `private const val TAG = "QuizViewModel"`).

## Dependency Injection

**Framework:** Hilt (`dagger.hilt`).

**Patterns:**
- ViewModels annotated with `@HiltViewModel` and constructor-injected.
  ```kotlin
  @HiltViewModel
  class QuizViewModel @Inject constructor(
      private val savedStateHandle: SavedStateHandle,
      private val loadQuizSessionUseCase: LoadQuizSessionUseCase,
      ...
  ) : ViewModel()
  ```
- Use case classes annotated with `@Inject constructor()`.
- Data module provides bindings via `DataModule` (`data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt`).
- Multi-module Hilt compiler workaround: `kspTest` and `kspAndroidTest` for `hilt.compiler`.

## Coroutines & Concurrency

**Patterns:**
- ViewModels use `viewModelScope.launch { }` for all async operations.
- Application-level background tasks use `CoroutineScope(Dispatchers.IO).launch { }`.
- Tests use `kotlinx.coroutines.test.runTest { }` with `advanceUntilIdle()` and `UnconfinedTestDispatcher`.
- `MainDispatcherRule` (JUnit `TestWatcher`) sets `Dispatchers.setMain` for ViewModel tests.
- State management via `StateFlow` + `MutableStateFlow` with `asStateFlow()` for read-only exposure.
- One-shot events via `SharedFlow` + `MutableSharedFlow` with `asSharedFlow()`.

## Compose UI Conventions

**Patterns:**
- Container/Content split pattern: Screens have a `*Screen.kt` (Container — ViewModel wiring) and `*ScreenContent.kt` (Content — pure Compose UI driven by state + action callbacks).
  - `HomeScreen.kt` + `HomeScreenContent.kt`
  - `SettingsScreen.kt` + `SettingsScreenContent.kt`
  - `QuizScreen.kt` + `QuizScreenContent.kt`
  - `ResultScreen.kt` + `ResultScreenContent.kt`
- Content composables accept typed state and action callbacks, not ViewModels.
- UI state modeled as data classes with sensible defaults (e.g., `HomeScreenUiState()`, `SettingsUiModel()`).
- Action callbacks grouped into action classes with no-op defaults (e.g., `SettingsActions`).
- `Duo*` prefix for Duolingo-style custom components (e.g., `Duo3DCard.kt`, `DuoSnackbar.kt`).
- Snackbar messages modeled as immutable data class `SnackbarMessage` with `text`, `actionLabel`, `duration`, `isError`.
- Theme files split across `Color.kt`, `Theme.kt`, `Type.kt`, `AppIcons.kt`.

## Module Design

**Exports:**
- `domain` module: exposes repository interfaces and domain models only (no Android dependencies).
- `data` module: contains repository implementations, Room DB, and remote API clients.
- `app` module: contains UI, ViewModels, DI wiring.

**Barrel files:** Not used; each class is imported directly.

**Companion objects:** Used for constants and factory methods within ViewModels and utility classes.

---

*Convention analysis: 2026-07-22*
