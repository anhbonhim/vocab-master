<!-- refreshed: 2026-07-22 -->
# Architecture

**Analysis Date:** 2026-07-22

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                        Android App (app module)                         │
│  Jetpack Compose UI · Navigation 3 · Hilt DI · ExoPlayer · DataStore   │
├──────────────────────┬──────────────────────┬───────────────────────────┤
│   UI Layer           │   ViewModel Layer    │   Data Layer (data mod.)  │
│   `app/.../ui/`      │   `app/.../vm/`      │   `data/.../repository/`  │
│                      │                      │                           │
│   • Screens          │   • MainViewModel    │   • VocabularyRepoImpl    │
│   • NavGraph/Display │   • QuizViewModel    │   • ReviewRepoImpl        │
│   • Components/Quiz  │   • SettingsVM       │   • SettingsRepoImpl      │
│   • Theme            │   • StatsVM          │   • BackupRepoImpl        │
│                      │   • PlacementTestVM  │                           │
└──────────┬───────────┴──────────┬───────────┴─────────────┬─────────────┘
           │                     │                          │
           ▼                     ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Domain Layer (domain module)                     │
│               Pure Kotlin · No Android dependencies                       │
├──────────────────┬──────────────────┬───────────────────────────────────┤
│   Use Cases      │   Models         │   FSRS v6 Scheduler               │
│   • SubmitReview │   • Curriculum   │   • Card / State / ReviewLog      │
│   • EvaluateAns  │   • Question     │   • Scheduler (py-fsrs 6.3.1)     │
│   • LoadQuizSess │   • Repository   │   • Optimizer                     │
│   • CompleteQuiz │     Interfaces   │                                   │
│   • UpdateStreak │                  │                                   │
│   • MapRating    │                  │                                   │
└──────────────────┴──────────────────┴───────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Data Layer (data module)                             │
│   Room DB · Retrofit · Firebase Auth · DataStore · Coroutines            │
├──────────────────┬──────────────────┬───────────────────────────────────┤
│   Local Storage  │   Networking     │   Auth & Sync                     │
│   • Room DB (v8) │   • Retrofit     │   • Firebase Auth (Google)        │
│   • DataStore    │   • OkHttp       │   • Credential Manager            │
│   • Asset JSON   │   • ApiClient    │   • SyncManager (push/pull)       │
│     (curriculum) │   • API Services │   • AuthInterceptor               │
└──────────────────┴──────────────────┴───────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Backend Server (backend/)                           │
│               FastAPI · Python · SQLite · Firebase Admin                 │
├──────────────────┬──────────────────┬───────────────────────────────────┤
│   REST API       │   Services       │   Models                          │
│   • /api/v1/     │   • FSRS calc    │   • SQLAlchemy ORM                │
│   • vocabulary   │   • Placement    │   • Pydantic schemas              │
│   • placement    │     logic        │                                   │
│   • sync         │                  │                                   │
└──────────────────┴──────────────────┴───────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `MainActivity` | Activity lifecycle, DI entry, ViewModel wiring | `app/.../MainActivity.kt` |
| `VocabApplication` | `@HiltAndroidApp`, crash handler init, V6 migration | `app/.../VocabApplication.kt` |
| `VocabMasterApp` | Top-level Scaffold, theme, Navigation 3 NavDisplay, snackbar host | `app/.../ui/VocabMasterApp.kt` |
| `NavGraph` | Type-safe route definitions via `entryProvider` | `app/.../ui/navigation/NavGraph.kt` |
| `Screen` | Sealed class of `@Serializable` NavKey routes | `app/.../ui/navigation/Screen.kt` |
| `MainViewModel` | Navigation state, curriculum, badges, stats flows | `app/.../ui/viewmodel/MainViewModel.kt` |
| `QuizViewModel` | Quiz session lifecycle, process-death-safe via SavedStateHandle | `app/.../ui/viewmodel/QuizViewModel.kt` |
| `SettingsViewModel` | Theme, daily goal, retention preferences | `app/.../ui/viewmodel/SettingsViewModel.kt` |
| `StatisticsViewModel` | Review stats, mistake bank, badges | `app/.../ui/viewmodel/StatisticsViewModel.kt` |
| `PlacementTestViewModel` | Placement test flow state | `app/.../ui/viewmodel/PlacementTestViewModel.kt` |
| `DataModule` | Hilt DI bindings for DB, DAO, all repositories | `data/.../di/DataModule.kt` |
| `VocabDatabase` | Room database (v8), 10 entity types | `data/.../database/VocabDatabase.kt` |
| `VocabDao` | Room DAO — FSRS cards, curriculum, progress, review logs | `data/.../database/VocabDao.kt` |
| `VocabularyRepositoryImpl` | Implements `VocabularyRepository` — curriculum seeding, CRUD | `data/.../repository/VocabularyRepositoryImpl.kt` |
| `ReviewRepositoryImpl` | Implements `ReviewRepository` — card/log persistence | `data/.../repository/ReviewRepositoryImpl.kt` |
| `SettingsRepositoryImpl` | Implements `SettingsRepository` — DataStore-backed | `data/.../repository/SettingsRepositoryImpl.kt` |
| `BackupRepositoryImpl` | Implements `BackupRepository` | `data/.../repository/BackupRepositoryImpl.kt` |
| `SyncManager` | Push/pull sync to backend | `data/.../sync/SyncManager.kt` |
| `AuthManager` | Firebase Auth + Credential Manager (Google Sign-In) | `data/.../auth/AuthManager.kt` |
| `ApiClient` | Retrofit client setup with OkHttp interceptor chain | `data/.../remote/ApiClient.kt` |
| `Scheduler` | FSRS-6 algorithm port (py-fsrs 6.3.1) | `domain/.../fsrs/v6/Scheduler.kt` |
| `LoadQuizSessionUseCase` | Maps requests to quiz data for 6 quiz modes | `domain/.../usecase/LoadQuizSessionUseCase.kt` |
| `EvaluateAnswerUseCase` | Grading logic per question type | `domain/.../usecase/EvaluateAnswerUseCase.kt` |
| `SubmitReviewUseCase` | Records FSRS review, awards XP | `domain/.../usecase/SubmitReviewUseCase.kt` |
| `CompleteQuizSessionUseCase` | Mark node completion, update streak | `domain/.../usecase/CompleteQuizSessionUseCase.kt` |
| Backend FastAPI app | REST API — placement, sync, vocabulary endpoints | `backend/app/main.py` |

## Pattern Overview

**Overall:** Android Clean Architecture (multi-module) — `domain` → `data` → `app` dependency chain.

**Key Characteristics:**
- **3-module structure:** `:domain` (pure Kotlin), `:data` (Android Library), `:app` (Android Application)
- **Dependency injection:** Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Singleton`, `@Binds`)
- **Repository pattern:** Interfaces in `domain/.../model/`, implementations in `data/.../repository/`
- **Use Case pattern:** Business logic in `domain/.../usecase/` — single-responsibility `operator fun invoke()`
- **Unidirectional data flow:** UI → ViewModel → UseCase → Repository → DB/API → Flow → ViewModel → UI
- **Type-safe Navigation:** Navigation 3 (1.0.1) with `@Serializable` sealed class routes, no string-based routing
- **MVVM with Compose:** `StateFlow` in ViewModels, `collectAsState()` in Composables
- **FSRS-6 spaced repetition:** Local client-side scheduler ported from py-fsrs, with optional server sync

## Layers

**Domain Layer (`domain/` module):**
- Purpose: Pure business logic and enterprise rules with zero Android dependencies
- Location: `domain/src/main/java/com/nhimz/vocabmaster/domain/`
- Contains:
  - `model/` — Repository interfaces (`VocabularyRepository`, `ReviewRepository`, `SettingsRepository`, `BackupRepository`) plus domain data classes (`Section`, `Unit`, `Node`, `Question`, `QuestionWithCard`, `QuizSessionModels`)
  - `usecase/` — `EvaluateAnswerUseCase`, `SubmitReviewUseCase`, `LoadQuizSessionUseCase`, `CompleteQuizSessionUseCase`, `UpdateStreakUseCase`, `MapRatingUseCase`, `PlacementTestUseCase`
  - `fsrs/v6/` — FSRS-6 scheduler port: `Scheduler.kt`, `Card.kt`, `State.kt`, `ReviewLog.kt`, `Optimizer.kt`
- Depends on: Nothing (pure Kotlin, only kotlinx.serialization and coroutines)
- Used by: `data` module, `app` module

**Data Layer (`data/` module):**
- Purpose: Data persistence, API communication, auth, sync — implements domain repository interfaces
- Location: `data/src/main/java/com/nhimz/vocabmaster/data/`
- Contains:
  - `database/` — Room DB v8 (`VocabDatabase`, `VocabDao`), 12 entity types, `Converters`
  - `repository/` — `VocabularyRepositoryImpl`, `ReviewRepositoryImpl`, `SettingsRepositoryImpl`, `BackupRepositoryImpl`
  - `remote/` — Retrofit `ApiClient`, `AuthInterceptor`, API service interfaces, DTOs
  - `auth/` — `AuthManager` (Firebase Auth + Credential Manager Google Sign-In)
  - `sync/` — `SyncManager` (bidirectional push/pull sync with backend)
  - `di/` — `DataModule` (Hilt @Binds + @Provides)
  - `model/` — DTO models for backup
- Depends on: `:domain` module

**App Layer (`app/` module):**
- Purpose: Android application entry point, UI layer, DI wiring
- Location: `app/src/main/java/com/nhimz/vocabmaster/`
- Contains:
  - `ui/` — All Jetpack Compose UI: screens, navigation, components, theme, viewmodels
  - `audio/` — `CDNAudioPlayer` (ExoPlayer-based with local caching)
  - `notification/` — `NotificationScheduler`, `NotificationReceiver`
  - `util/` — `LocalLogger`
- Depends on: `:data` and `:domain` modules

**Backend (`backend/` directory):**
- Purpose: Python FastAPI server providing REST API + sync endpoint
- Location: `backend/app/`
- Contains:
  - `main.py` — FastAPI app entry point, router inclusion
  - `routers/` — `vocabulary.py`, `placement.py`, `sync.py`
  - `models/` — SQLAlchemy ORM models
  - `schemas/` — Pydantic request/response schemas
  - `services/` — Business logic services
  - `utils/` — Firebase auth middleware
  - `database.py` — SQLAlchemy engine + session

## Data Flow

### Primary Quiz Flow

1. User taps a node on the Home screen path (`HomeScreen.kt`)
2. HomeScreen calls `mainViewModel.navigateTo(Screen.Quiz(...))` or `quizViewModel.startNodeSession(nodeId, index)`
3. `QuizViewModel` calls `LoadQuizSessionUseCase` which calls `VocabularyRepository`
4. Repository loads from Room DB (or seeds from asset JSON on first launch)
5. Use case returns `QuizSessionData` with `List<QuizQuestion>` mapped from domain `Question` + FSRS `Card`
6. `QuizViewModel` updates `_uiState: StateFlow<QuizUiState>` to `Active` state
7. `QuizScreen` composable observes `uiState` and renders the appropriate quiz card
8. User submits answer → `QuizViewModel.submitAnswer()` → `EvaluateAnswerUseCase` → then `SubmitReviewUseCase`
9. `SubmitReviewUseCase` runs FSRS scheduler, records card + log via `ReviewRepository`, awards XP via `SettingsRepository`
10. On completion, `CompleteQuizSessionUseCase` marks node progress, updates streak
11. Navigate to `Screen.Result` with session stats

### Backend Sync Flow

1. `SyncManager.sync()` collects all local settings, active cards, and review logs
2. Posts `SyncPayload` to `POST /api/v1/sync/push` via Retrofit
3. Gets server response from `GET /api/v1/sync/pull` with merged data
4. Applies pulled settings, cards, and review logs to local Room DB + DataStore

### Curriculum Loading Flow

1. `VocabularyRepositoryImpl.ensureCurriculumAndFsrsSeeded()` called on first data access
2. Reads `lessons_v3.json` from Android assets
3. Parses into `LessonsV2Asset` via `kotlinx.serialization`
4. Inserts all sections → units → guidebooks → nodes → sessions → questions → FSRS cards into Room DB
5. Subsequent reads go straight to Room

**State Management:**
- ViewModel state: `StateFlow` / `MutableStateFlow` in each ViewModel, collected as Compose state via `collectAsState()`
- Navigation state: `SnapshotStateList<NavKey>` owned by `MainViewModel` — survives config changes
- Persistence across process death: `SavedStateHandle` in `QuizViewModel` with whitelisted keys
- Theme/streak/XP/retention: `DataStore Preferences` via `SettingsRepository`

## Key Abstractions

**Repository Interfaces (domain layer):**
- Purpose: Define data contracts without Android dependencies
- Examples: `VocabularyRepository` (`domain/.../model/VocabularyRepository.kt`), `ReviewRepository` (`domain/.../model/ReviewRepository.kt`), `SettingsRepository` (`domain/.../model/SettingsRepository.kt`), `BackupRepository` (`domain/.../model/BackupRepository.kt`)
- Pattern: Interface in `:domain`, implementation in `:data`, bound via Hilt `@Binds` in `DataModule`

**Use Cases:**
- Purpose: Single-responsibility business operations wrapping domain logic
- Pattern: Class with `operator fun invoke()` returning `Result<T>`, injected via `@Inject constructor`
- Examples: `EvaluateAnswerUseCase`, `SubmitReviewUseCase`, `LoadQuizSessionUseCase`, `CompleteQuizSessionUseCase`

**FSRS v6 Scheduler:**
- Purpose: Port of py-fsrs 6.3.1 algorithm for spaced repetition
- Files: `domain/.../fsrs/v6/Scheduler.kt` (516 lines), `Card.kt`, `State.kt`, `ReviewLog.kt`, `Optimizer.kt`
- Key features: 21 default parameters, learning/relearning steps, fuzzing, difficulty/stability/retrievability math

**Quiz UI State Machine:**
- `QuizUiState` sealed interface (`app/.../ui/viewmodel/QuizUiState.kt`)
- States: `Loading` → `Active` (questions, answers, progress) → `Completed` (results, XP) | `Error`
- Transitions: `submitAnswer()` → `nextQuestion()` loop → final completion

## Entry Points

**Application:**
- `VocabApplication.onCreate()` — Hilt init, V6 DB migration reset, debug crash handler
- `MainActivity.onCreate()` — `setContent { VocabMasterApp(...) }`, default reminder scheduling

**Backend:**
- `backend/app/main.py` — FastAPI with `uvicorn`, 3 routers (vocabulary, placement, sync)

## Architectural Constraints

- **Threading:** All Room/IO operations use `Dispatchers.IO` via `withContext(Dispatchers.IO)`. UI composables run on main thread. ViewModel coroutines use `viewModelScope`. Room DB builder omits `allowMainThreadQueries()` to enforce main-thread safety.
- **Global state:** `SnapshotStateList<NavKey>` in `MainViewModel` as source of truth for navigation. `MutableStateFlow`/`SharedFlow` for ViewModel state. `MutableStateFlow<AppUser?>` in `AuthManager` for auth state.
- **Circular imports:** None detected — dependency graph is strictly `domain` → `data` → `app` with `data` depending only on `domain` and `app` depending on both.
- **Process death:** `QuizViewModel` uses `SavedStateHandle` with whitelisted keys (`PERSISTENCE_KEYS`) to survive both config changes and low-memory kill. Cumulative progress (correctCount, xpGained, incorrectCardIds) and per-question answer state are persisted.
- **Destructive migrations:** Room DB v8 uses `fallbackToDestructiveMigration(dropAllTables = true)` — curriculum is re-seeded from assets on schema bump.

## Anti-Patterns

### Repository Fallback Stubs

**What happens:** Several methods in `VocabularyRepositoryImpl` return hard-coded stubs (e.g. `getLearnedCountByTopic()` returns `0`, `getWordCountByTopicAndLevel()` returns `0`, `getCompletedLessons()` returns `emptyFlow()`).
**Why it's wrong:** Callers get silent incorrect data instead of a meaningful error or actual implementation. Topic-scoped queries are not implemented but the interface defines them.
**Do this instead:** Either implement proper topic-scoped queries via Room joins, or remove unused interface methods to keep the API honest.

### Large ViewModel with Mixed Concerns

**What happens:** `MainViewModel` (428 lines) owns navigation state, curriculum status computation (heavy flow combining sections/units/nodes with conditional logic), streak management, badge monitoring, snackbar messages, and counts refresh.
**Why it's wrong:** Violates single-responsibility. Makes the ViewModel harder to test and maintain. The curriculum status flow is particularly complex (nested loops over sections/units/nodes with lock computation).
**Do this instead:** Extract curriculum status computation into a specialized use case or separate ViewModel. Keep `MainViewModel` focused on navigation and top-level UI state.

### Long Repository Implementation

**What happens:** `VocabularyRepositoryImpl` is 644 lines with `@Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")` — signals a class trying to do too much.
**Why it's wrong:** Large classes are harder to understand, test, and modify. The curriculum seeding logic (a single 130-line method) alone handles 10 entity types.
**Do this instead:** Split curriculum seeding into a dedicated `CurriculumSeeder` class. Keep the repository focused on data access composition.

## Error Handling

**Strategy:** Use `Result<T>` return types in use cases and repository calls. ViewModels fold on success/failure and emit error states or snackbar messages.

**Patterns:**
- Domain use cases return `Result<T>` — `runCatching { ... }.getOrElse { Result.failure(it) }`
- Repository methods use `runCatching` or try/catch with custom `VocabDataException`
- ViewModels fold results: `.fold(onSuccess = { ... }, onFailure = { error -> ... })`
- UI-level errors surface via snackbar through `SharedFlow<SnackbarMessage>` piped to `SnackbarHostState`
- `CDNAudioPlayer` uses silent fallback on playback errors (logs without crashing)

## Cross-Cutting Concerns

**Logging:** `LocalLogger` (`app/.../util/LocalLogger.kt`) — wraps Android `Log` with tag prefix, optional crash handler setup. Debug-only crash reporting via `Thread.setDefaultUncaughtExceptionHandler`.

**Validation:** Inline checks in `VocabularyRepositoryImpl` during curriculum seeding (`check()` calls for MULTIPLE_CHOICE options, MATCHING pairs size, SCRAMBLED words count, etc.). These are runtime assertions that throw on malformed asset data.

**Authentication:** Firebase Auth via `AuthManager` — Google Sign-In using Credential Manager API. Auth state exposed as `StateFlow<AppUser?>`. Backend validates via Firebase Admin SDK token verification.

**Audio:** `CDNAudioPlayer` (`@Singleton`) uses ExoPlayer with 90MB LRU disk cache for OGG audio files from CDN. Supports local dev server override for offline testing. Bound to activity lifecycle via `DefaultLifecycleObserver`.

---

*Architecture analysis: 2026-07-22*
