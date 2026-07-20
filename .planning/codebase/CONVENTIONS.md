# Coding Conventions

**Analysis Date:** 2026-07-20

## Naming Patterns

**Files:**
- Kotlin classes and interfaces use PascalCase (e.g., `VocabApplication.kt`, `MainViewModel.kt`, `VocabularyRepositoryImpl.kt`).
- Jetpack Compose screen files use PascalCase and end with `Screen` (e.g., `HomeScreen.kt`, `QuizScreen.kt`).

**Functions:**
- Standard functions use camelCase (e.g., `initializeAppDefaultSettings`, `showNotification`, `scheduleDailyNotification`).
- Jetpack Compose composable functions use PascalCase and are annotated with `@Composable` (e.g., `FirstWinScreen`, `UnitHeaderItem`, `MultipleChoiceCard`).
- ViewModel functions are typically camelCase, often reflecting user actions or data fetching.

**Variables:**
- Variables use camelCase (e.g., `settingsRepositoryImpl`, `lastKnownVersion`).
- Private backing properties in StateFlows or similar often use an underscore prefix (e.g., `_logs`, `_uiState`).
- Constants use UPPER_SNAKE_CASE (e.g., `MAX_LOG_COUNT`).

**Types:**
- Classes and Interfaces use PascalCase.

## Code Style

**Formatting:**
- General Kotlin styling conventions appear to be followed, likely relying on standard Android Studio defaults or Detekt rules.

**Linting:**
- **Tool:** Detekt (`io.gitlab.arturbosch.detekt`).
- **Configuration:** Stored in `config/detekt/detekt.yml` and `config/detekt/baseline.xml`.
- **Key Rules Enabled (Complexity):**
    - `ComplexCondition`
    - `ComplexInterface`
    - `ComplexMethod`
    - `CyclomaticComplexMethod`
    - `LabeledExpression`
    - `LargeClass`
    - `LongMethod`
    - `LongParameterList`
    - `MethodOverloading`
    - `NestedBlockDepth`
    - `ReplaceSafeCallChainWithRun`
    - `StringLiteralDuplication`
    - `TooManyFunctions`

## Architecture and Patterns

**Overall Architecture:**
- Standard Android MVVM (Model-View-ViewModel) architecture with Clean Architecture principles (App, Domain, Data modules).
- **App Module:** Contains UI (Compose), ViewModels, and Application class.
- **Domain Module:** Contains Repository interfaces (e.g., `domain/src/main/java/com/nhimz/vocabmaster/domain/model/VocabularyRepository.kt`) and business logic models.
- **Data Module:** Contains Repository implementations (e.g., `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt`) and data sources (Room, etc.).
- **Dependency Injection:** Hilt is used heavily for injecting repositories into ViewModels and other components.

**UI Framework:**
- Jetpack Compose is the primary UI toolkit.

## Error Handling

**Patterns:**
- Try-catch blocks are used for handling exceptions, especially around data operations (parsing JSON, DB interactions, Coroutines).
- A custom `LocalLogger` is implemented to centralize logging, including a `setupCrashHandler()` method to catch uncaught exceptions and log them before delegating to the default handler.

## Logging

**Framework:** Custom `LocalLogger` object (`app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`), which wraps `android.util.Log` and maintains an in-memory buffer of logs exposed via a `StateFlow`.

**Patterns:**
- Includes standard levels: `d` (debug), `i` (info), `w` (warning), `e` (error).
- Includes formatting helpers like `section()` for prominent log boundaries.
- The `VocabApplication` initializes specific logging (like crash handling) only in `BuildConfig.DEBUG` mode.
- Has functionality to export logs (`getExportString()`).

## Concurrency

**Framework:** Kotlin Coroutines.
**Patterns:**
- ViewModels use `viewModelScope.launch` for async operations.
- Background tasks in `Application` class use `CoroutineScope(Dispatchers.IO).launch`.

---

*Convention analysis: 2026-07-20*