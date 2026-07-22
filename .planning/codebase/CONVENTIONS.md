# Coding Conventions

**Analysis Date:** 2026-07-22

## Naming Patterns

**Files:**
- Kotlin: PascalCase matching class/interface name (e.g., `SubmitReviewUseCase.kt`, `QuizViewModel.kt`, `VocabDatabase.kt`, `Converters.kt`)
- Python: snake_case (e.g., `firebase_auth.py`, `irt_engine.py`, `seed_db.py`)
- Test files: `<ClassName>Test.kt` for JVM tests, `<ClassName>Test.kt` for instrumented tests in `androidTest/`
- Fake files: `Fake<InterfaceName>.kt` (e.g., `FakeReviewRepository.kt`, `FakeVocabularyRepository.kt`)

**Functions:**
- Kotlin: camelCase (e.g., `startNodeSession()`, `submitAnswer()`, `getDueCardsScoped()`, `restoreSession()`)
- Python: snake_case (e.g., `verify_token()`, `get_current_user_uid()`, `health_check()`)
- Compose functions: PascalCase (e.g., `VocabMasterApp()`, `VocabMasterNavScaffold()`, `LoadingSplash()`)
- Use case operator `invoke()` pattern (e.g., `SubmitReviewUseCase.invoke()` called as `useCase(...)`)

**Variables:**
- Kotlin: camelCase (e.g., `sessionStartTime`, `pendingRestoreIndex`, `updatedCorrectCount`)
- Python: snake_case (e.g., `daily_goal_xp`, `current_streak`)
- Constants: UPPER_SNAKE_CASE within `companion object` blocks (e.g., `private const val TAG = "QuizViewModel"`, `KEY_QUIZ_KIND`)

**Types:**
- Kotlin: PascalCase (e.g., `QuizUiState`, `AnswerResult`, `QuizType`, `Card`, `Scheduler`, `VocabDatabase`)
- Interfaces: PascalCase (e.g., `VocabularyRepository`, `ReviewRepository`, `SettingsRepository`)
- Enums: PascalCase, values in PascalCase (e.g., `State.New`, `State.Review`, `QuizType.Introduction`, `NodeType.LESSON`)
- Python classes: PascalCase (e.g., `Settings`, `Base` via SQLAlchemy)
- Python type aliases: PascalCase

## Code Style

**Formatting:**
- Tool: Detekt (`io.gitlab.arturbosch.detekt`) via Gradle
- Config: `config/detekt/detekt.yml` — build upon default config, allRules = false
- Detekt rules enabled: `ComplexCondition`, `ComplexInterface`, `ComplexMethod`, `CyclomaticComplexMethod`, `LabeledExpression`, `LargeClass`, `LongMethod`, `LongParameterList`, `MethodOverloading`, `NestedBlockDepth`, `ReplaceSafeCallChainWithRun`, `StringLiteralDuplication`, `TooManyFunctions`
- Baseline: `config/detekt/baseline.xml`
- Style guide: `kotlin.code.style=official` in `gradle.properties`

**Linting:**
- Kotlin: Detekt 1.23.6, applied to all projects via build.gradle.kts
- Python: No linting config detected (no `ruff.toml`, `.pylintrc`, or `.flake8` found)
- JS/TS: No linting config detected (no `.eslintrc` or `.prettierrc` found)

**Suppression patterns:**
- File-level: `@file:Suppress("MagicNumber", "NestedBlockDepth", ...)` used in the FSRS `Scheduler.kt` file
- Function-level: `@Suppress("TooManyFunctions", "LongMethod", ...)` on complex scheduler functions
- Detekt baseline is used to suppress existing violations without exhaustive inline suppressions

```kotlin
// File-level — Scheduler.kt
@file:Suppress("MagicNumber", "NestedBlockDepth", "ComplexCondition", "UseRequire", "UseCheckOrError")

// Class-level — Scheduler.kt
@Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "LongParameterList", "MagicNumber")
class Scheduler @Inject constructor(...)
```

## Import Organization

**Kotlin imports order** (observed pattern, not enforced by tool):
1. `kotlin.*` / `java.*` / `javax.*` standard library imports
2. Android framework imports (`androidx.*`, `android.*`)
3. Project-internal imports (`com.nhimz.vocabmaster.*`)
4. Third-party imports (`dagger.*`, `kotlinx.*`, `retrofit2.*`, `okhttp3.*`)
5. Blank line between groups

**Python imports order** (observed pattern):
1. Standard library (`os`, `from typing import ...`)
2. Third-party (`fastapi`, `firebase_admin`, `pydantic_settings`)
3. Local app modules (`from app.config import settings`)

**Wildcard imports usage:**
- Single-type wildcards are common: `import com.nhimz.vocabmaster.domain.usecase.*` for use case groupings in ViewModels
- No star imports observed in domain module

**Path Aliases:**
- Standard Gradle module paths: `:app`, `:domain`, `:data` — imported via `project(":domain")` / `project(":data")`
- No additional path aliases configured

## Error Handling

**Patterns:**
- **Kotlin Result type** — Use cases return `Result<T>` using `runCatching {}`
  ```kotlin
  // SubmitReviewUseCase.kt
  suspend operator fun invoke(...): Result<Card?> = runCatching {
      ...
      Result.success(updatedCard)
  }.getOrElse { Result.failure(it) }
  ```

- **ViewModel `.fold(onSuccess, onFailure)`** — Standard pattern for consuming use case results
  ```kotlin
  loadQuizSessionUseCase(request).fold(
      onSuccess = { data -> handleLoadedSession(data) },
      onFailure = { error ->
          _uiState.value = QuizUiState.Error(msg)
          emitSnackbar(SnackbarMessage(text = msg, isError = true))
      }
  )
  ```

- **Error state in UI state** — `QuizUiState.Error(message)` sealed class variant for UI error display
- **SnackbarMessage emission** — ViewModels emit error messages via `SharedFlow<SnackbarMessage>` for the UI
- **Python exceptions** — Exception handling with HTTPException for API errors:
  ```python
  raise HTTPException(status_code=401, detail=f"Invalid authentication credentials: {str(e)}")
  ```

**Illegal state handling:**
- `IllegalArgumentException` thrown in use cases for invalid input combinations:
  ```kotlin
  if (optionIndex == null) {
      Result.failure(IllegalArgumentException("optionIndex must be provided for MultipleChoice"))
  }
  ```

**Firebase auth initialization:**
- Exception caught and logged, app continues without crashing for test environments:
  ```python
  try:
      cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
      firebase_admin.initialize_app(cred)
  except Exception as e:
      print(f"Error initializing Firebase Admin: {e}")
      pass
  ```

## Logging

**Framework:** Custom `LocalLogger` singleton — `app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`

**Patterns:**
- Methods: `d()`, `i()`, `w()`, `e()`, `section()`
- Signature: `fun d(tag: String, message: String)`, `fun e(tag: String, message: String, throwable: Throwable? = null)`
- Tags are static strings from `companion object` (e.g., `private const val TAG = "QuizViewModel"`)
- In-memory ring buffer of 500 entries exposed as `StateFlow<List<LogEvent>>`
- Wraps Android `Log.d/i/w/e` while also storing in memory for debug panel

```kotlin
LocalLogger.e(TAG, "Failed to load node session", error)
LocalLogger.d(TAG, "Session loaded successfully")
```

**Python logging:** Primarily `print()` statements — no structured logging framework detected

## Comments

**When to Comment:**
- KDoc (`/** ... */`) on public classes, interfaces, and significant functions
- Inline `//` comments for non-obvious business logic, edge cases, and architectural decisions
- TODO/FIXME comments reference plan IDs (e.g., `// Plan 03-02 hardening:`)
- Section comments use `// ===== Section title =====` patterns

**KDoc/TSDoc:**
- KDoc extensively used on domain model classes explaining FSRS deviations from py-fsrs:
  ```kotlin
  /**
   * FSRS-6 card model.
   *
   * Deviation from py-fsrs:
   * - [due] and [lastReview] are stored as UTC epoch milliseconds ([Long]) instead of
   *   ISO-8601 strings. ...
   */
  ```
- KDoc on ViewModel classes explaining intent, plan references, and design decisions
- Python docstrings used for function documentation

**Architectural decision comments:**
- Plan references in comments: `// Plan 03-02 hardening: if we are restoring from a saved state...`
- Cross-reference to PLAN.md and design documents: `// Per D-03 / SYNC-02`

## Function Design

**Size:** Functions range from short (1-5 line helpers) to moderately long (up to ~50 lines for state management). Complex logic is broken into private helper methods (e.g., `persistActiveState()`, `clearPersistenceKeys()`, `setupPersistenceKeys()` in `QuizViewModel.kt`).

**Parameters:**
- Use case `invoke` operators use named parameters with sensible defaults:
  ```kotlin
  suspend operator fun invoke(
      question: QuizQuestion,
      isCorrect: Boolean,
      responseTimeMs: Long,
      xpEarned: Int,
      explicitRating: Rating? = null
  ): Result<Card?>
  ```
- ViewModel method parameters use default values for optional arguments:
  ```kotlin
  fun submitAnswer(optionIndex: Int? = null, textAnswer: String? = null, ...)
  ```

**Return Values:**
- Use cases return `Result<T>` (Kotlin standard library)
- Repository interfaces return `Flow<List<T>>` for reactive collections, `suspend fun` that returns `T?` or `Int` for one-shot queries
- ViewModel methods are void; state is exposed via `StateFlow`

## Module Design

**Exports:** Each module exposes public interfaces/classes in the top-level package of its concern:
- `:domain` — pure Kotlin module with no Android dependencies, exports `model/*Repository`, `usecase/*UseCase`, `fsrs/v6/*`
- `:data` — Android module with Room, Retrofit, DataStore; exports `database/*`, `remote/*`, `repository/*`, `sync/*`, `auth/*`
- `:app` — Android application module with Compose UI; depends on `:domain` and `:data`

**Barrel Files:** Not used. Imports reference specific packages within modules.

**Package by Feature + Layer:**
```
com.nhimz.vocabmaster.domain/
  ├── model/        (domain models, repository interfaces)
  ├── usecase/      (business logic use cases)
  ├── fsrs/v6/      (FSRS scheduling algorithm)
  └── model/quiz/   (quiz-specific models)

com.nhimz.vocabmaster.data/
  ├── database/       (Room DAO, entities, converters)
  ├── database/entity/ (Room entity classes)
  ├── repository/     (Repository implementations)
  ├── remote/         (Retrofit API services, DTOs)
  ├── auth/           (Firebase auth manager)
  ├── sync/           (Sync manager)
  └── di/             (Hilt DataModule)

app/src/main/java/com/nhimz/vocabmaster/
  ├── ui/
  │   ├── screens/      (Feature-specific screens)
  │   ├── components/   (Shared composables)
  │   ├── viewmodel/    (ViewModels per feature)
  │   ├── navigation/   (NavGraph, Screen routes)
  │   ├── theme/        (Color, Type, Theme, Icons)
  │   └── util/         (UI utilities)
  ├── audio/            (CDN audio player)
  ├── notification/     (Notification scheduling/receiving)
  └── util/             (LocalLogger)
```

## Dependency Injection

**Framework:** Hilt (Dagger) for all Android modules (`@HiltViewModel`, `@AndroidEntryPoint`, `@Inject`, `@Module`/`@Provides`)

**Pattern:**
- `@HiltViewModel` annotation on ViewModels with `@Inject constructor`
- `@AndroidEntryPoint` on `MainActivity` (the only entry point activity)
- `DataModule` Hilt module in `:data` module at `data/src/main/java/.../data/di/DataModule.kt`
- `javax.inject.Inject` for domain module constructors (no Hilt dependency in `:domain`)
- `ksp(hilt.compiler)` as the annotation processor (KSP, not kapt)

## Testing Conventions

- Test files mirror production structure with `Test` suffix: `SubmitReviewUseCaseTest`, `QuizViewModelTest`
- Fake classes implement repository interfaces for test isolation: `FakeReviewRepository`, `FakeVocabularyRepository`
- `TODO("not needed for these tests")` used for unimplemented fake methods
- `@Ignore` annotation used for flaky or platform-dependent tests (e.g., Robolectric on Termux)

---

*Convention analysis: 2026-07-22*
