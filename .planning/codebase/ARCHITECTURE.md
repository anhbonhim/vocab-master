<!-- refreshed: 2026-07-22 -->
# Architecture

**Analysis Date:** 2026-07-22

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────────┐
│                          Presentation Layer (app module)                  │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────────────────┐  │
│  │   Screens     │  │   ViewModels      │  │  Navigation (NavGraph)    │  │
│  │  (Composable) │──│  (State Holders)  │──│  Type-safe sealed class   │  │
│  │  `app/.../ui/ │  │  `app/.../ui/    │  │  `app/.../ui/navigation/` │  │
│  │  screens/`    │  │  viewmodel/`     │  │                           │  │
│  └──────┬───────┘  └────────┬─────────┘  └───────────────────────────┘  │
│         │                    │                                           │
│  ┌──────┴────────────────────┴──────────────────────────────────────┐  │
│  │              VocabMasterApp.kt (Top-level Composable)             │  │
│  │              MainActivity.kt (Entry Point)                        │  │
│  └────────────────────────────┬──────────────────────────────────────┘  │
└───────────────────────────────┼──────────────────────────────────────────┘
                                │ injects repositories & use cases
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         Domain Layer (domain module)                      │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────────────────────┐   │
│  │  Use Cases    │  │  Repositories  │  │  FSRS-6 Scheduler          │   │
│  │  (Pure Kotlin)│──│  (Interfaces)  │  │  (Pure alg. port w/ tests)  │   │
│  │  `domain/.../ │  │  `domain/.../  │  │  `domain/.../fsrs/v6/`     │   │
│  │  usecase/`    │  │  model/`       │  │                            │   │
│  └──────────────┘  └───────┬────────┘  └────────────────────────────┘   │
│                            │                                             │
│                   Domain Models (Curriculum, QuizSession, etc.)          │
└────────────────────────────┼──────────────────────────────────────────────┘
                             │ Implements
                             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           Data Layer (data module)                        │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────────────────────┐   │
│  │  Repository   │  │  Room Database  │  │  Remote / Sync             │   │
│  │  Impls        │──│  VocabDao +    │  │  ApiClient (Retrofit)      │   │
│  │  `data/.../   │  │  Entities      │  │  SyncManager               │   │
│  │  repository/` │  │  `data/.../db/`│  │  `data/.../remote/`        │   │
│  └──────────────┘  └────────────────┘  └───────────┬────────────────┘   │
│  ┌──────────────┐  ┌────────────────┐              │                     │
│  │  DI Module   │  │  AuthManager   │              │                     │
│  │  DataModule  │  │ (Firebase Auth) │              │                     │
│  └──────────────┘  └────────────────┘              │                     │
│                                                     ▼                     │
│                                          FastAPI Backend (Python)        │
│                                          `backend/app/`                  │
└──────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| VocabApplication | App lifecycle, Hilt entry point, V6 migration trigger | `app/src/main/java/.../VocabApplication.kt` |
| MainActivity | Single activity host, injects all repositories, sets content | `app/src/main/java/.../MainActivity.kt` |
| VocabMasterApp | Top-level Composable, NavDisplay, bottom bar, snackbar host | `app/src/main/java/.../ui/VocabMasterApp.kt` |
| Screen | Type-safe navigation sealed class (16 routes) | `app/src/main/java/.../ui/navigation/Screen.kt` |
| NavGraph (entryProvider) | Route-to-Composable mapping, async data loading per route | `app/src/main/java/.../ui/navigation/NavGraph.kt` |
| MainViewModel | Navigation back stack owner, curriculum status, badge tracking | `app/src/main/java/.../ui/viewmodel/MainViewModel.kt` |
| QuizViewModel | Quiz session state machine, question lifecycle, FSRS review flow | `app/src/main/java/.../ui/viewmodel/QuizViewModel.kt` |
| VocabularyRepository | Repository interface for card & curriculum data | `domain/src/main/java/.../model/VocabularyRepository.kt` |
| ReviewRepository | Repository interface for review logs | `domain/src/main/java/.../model/ReviewRepository.kt` |
| SettingsRepository | Repository interface for user settings, XP, streaks, badges | `domain/src/main/java/.../model/SettingsRepository.kt` |
| BackupRepository | Repository interface for JSON export/import | `domain/src/main/java/.../model/BackupRepository.kt` |
| VocabDao | Room DAO — all SQL queries for cards, questions, curriculum, sync | `data/src/main/java/.../database/VocabDao.kt` |
| VocabDatabase | Room database (version 8, 10 entities) | `data/src/main/java/.../database/VocabDatabase.kt` |
| Scheduler | FSRS-6 spaced repetition algorithm (pure Kotlin, ported from py-fsrs) | `domain/src/main/java/.../domain/fsrs/v6/Scheduler.kt` |
| DataModule | Dagger Hilt module — binds repository implementations, provides DB/DAO | `data/src/main/java/.../di/DataModule.kt` |
| SyncManager | Bidirectional push/pull sync with cloud backend | `data/src/main/java/.../sync/SyncManager.kt` |
| ApiClient | Retrofit HTTP client with Firebase auth interceptor | `data/src/main/java/.../remote/ApiClient.kt` |
| FastAPI Backend | Python REST API — vocabulary, placement, sync endpoints | `backend/app/main.py` |

## Pattern Overview

**Overall:** Clean Architecture with 3 Gradle modules (app → domain ← data)

**Key Characteristics:**
- Strict layer dependency: `app` depends on `domain` and `data`; `data` depends on `domain`; `domain` has zero Android dependencies
- Each layer is a separate Gradle module with its own `build.gradle.kts`
- Repository pattern: interfaces live in `domain/model/`, implementations in `data/repository/`
- Use cases in `domain/usecase/` are stateless `@Inject` classes with `operator fun invoke()`
- ViewModels hold Compose-aware state (`StateFlow`, `SnapshotStateList`) and mediate between UI and domain
- Dagger Hilt for dependency injection (singleton components cross modules)
- Type-safe Navigation 3 (sealed class `Screen` hierarchy with `@Serializable` routes)
- FSRS-6 port is pure Kotlin with no external dependencies, tested against golden vectors

## Layers

**Presentation Layer (app module):**
- Purpose: Android UI — Jetpack Compose screens, ViewModels, navigation
- Location: `app/src/main/java/com/nhimz/vocabmaster/`
- Contains: Compose screens, ViewModels, navigation graph, audio player, notifications
- Depends on: `domain` module (models, use cases), `data` module (repositories, database)
- Used by: Android OS (via `AndroidManifest.xml`, `MainActivity` as launcher)

**Domain Layer (domain module):**
- Purpose: Pure business logic — no Android dependencies, testable in pure JVM
- Location: `domain/src/main/java/com/nhimz/vocabmaster/domain/`
- Contains: Repository interfaces, domain models (`Question`, `Card`, `Curriculum`), use cases, FSRS-6 scheduler
- Depends on: Nothing Android — only `kotlinx.coroutines`, `kotlinx.serialization`, `javax.inject`
- Used by: `app` module and `data` module

**Data Layer (data module):**
- Purpose: Data persistence, networking, synchronization
- Location: `data/src/main/java/com/nhimz/vocabmaster/data/`
- Contains: Room database (10 entities, VocabDao), repository implementations, Retrofit API client, sync manager, Firebase auth
- Depends on: `domain` module, Android SDK, Room, Retrofit, Firebase Auth
- Used by: `app` module

**Backend Layer (Python FastAPI):**
- Purpose: Cloud sync, user data persistence, placement test IRT engine
- Location: `backend/app/`
- Contains: FastAPI app, SQLAlchemy models, REST routers (vocabulary, placement, sync), Firebase auth middleware
- Depends on: Python 3, FastAPI, SQLAlchemy, Firebase Admin SDK
- Used by: Android app sync manager

## Data Flow

### Primary Request Path (Quiz Flow)

1. User taps a learning node on the curriculum path (`HomeScreen` → `NavGraph.kt:144`)
2. `mainViewModel.navigateTo(Screen.Quiz())` pushes route onto `SnapshotStateList` back stack (`MainViewModel.kt:359`)
3. `NavDisplay` detects new back stack entry, calls `entryProvider` which renders `QuizScreen` (`NavGraph.kt:199`)
4. `QuizViewModel` uses `LoadQuizSessionUseCase` to fetch questions (`domain/usecase/LoadQuizSessionUseCase.kt`)
5. Use case calls `VocabularyRepository.getSessionsByNode()` etc. — interface methods
6. `VocabularyRepositoryImpl` delegates to `VocabDao` Room queries, maps entities to domain models (`data/repository/VocabularyRepositoryImpl.kt:531`)
7. Room queries SQLite DB, returns `Flow<List<QuestionAndFsrsCard>>`
8. User answers each question; `QuizViewModel` calls `SubmitReviewUseCase` for each answer
9. `SubmitReviewUseCase` invokes `Scheduler.reviewCard()` (FSRS-6 algorithm), then `ReviewRepository.recordReview()` (`domain/usecase/SubmitReviewUseCase.kt`)
10. On quiz completion, `CompleteQuizSessionUseCase` marks node progress, updates streaks

### Sync Data Flow

1. `SyncManager.sync()` reads local Room DB state and `SettingsRepository` values (`data/sync/SyncManager.kt:54`)
2. Builds `SyncPayload` with user settings, FSRS cards, review logs
3. Calls `ApiClient.syncApi.pushSync(payload)` (Retrofit POST)
4. FastAPI backend (`backend/app/routers/sync.py`) processes push — last-write-wins for settings, timestamp-based merge for cards
5. Backend returns `pullSync` response with any newer server data
6. `SyncManager` applies pulled data: settings via `SettingsRepository`, cards via `VocabDao.mergePulledCards()` (D-03 server-wins-with-time-guard)
7. `syncPrefs` stores `last_sync_timestamp` for incremental next sync

**State Management:**
- Navigation: `SnapshotStateList<NavKey>` owned by `MainViewModel` — survives config changes, triggers Compose recomposition
- Screen state: `StateFlow` in each ViewModel (e.g., `QuizUiState`, curriculum `SectionStatus`)
- One-shot events: `SharedFlow` for snackbar messages, badge unlock events
- Persisted state: Room SQLite for curriculum/cards/review logs; `SharedPreferences` (wrapped by DataStore-backed `SettingsRepositoryImpl`) for user settings
- Pagination/loading: none — curriculum is fully loaded; quiz sessions fetch up to 20 items

### Secondary Flow (Onboarding)

1. Launch → `VocabApplication.onCreate()` → Hilt init + V6 migration check
2. `MainActivity.onCreate()` → `setContent { VocabMasterApp(...) }`
3. `VocabMasterApp` calls `MainViewModel.checkOnboardingStatus()` which checks badges for `onboarding_completed`
4. If not completed, backStack starts at `Screen.Welcome`
5. Sequential flow: Welcome → Login (Firebase) → GoalPicker → PlacementTest → FirstWin → Home
6. `completeOnboarding()` saves `onboarding_completed` badge, 50XP, redirects to Home

## Key Abstractions

**Repository Interfaces (domain layer):**
- Purpose: Contract between data and domain layers
- Examples: `VocabularyRepository` (`domain/src/main/java/.../model/VocabularyRepository.kt`), `ReviewRepository` (`domain/.../model/ReviewRepository.kt`), `SettingsRepository` (`domain/.../model/SettingsRepository.kt`), `BackupRepository` (`domain/.../model/BackupRepository.kt`)
- Pattern: Interface in domain, `@Singleton` implementation in data, bound via `DataModule` (`data/.../di/DataModule.kt`) using `@Binds`

**Use Cases (domain layer):**
- Purpose: Single-responsibility business operations
- Examples: `LoadQuizSessionUseCase`, `SubmitReviewUseCase`, `CompleteQuizSessionUseCase`, `EvaluateAnswerUseCase`, `UpdateStreakUseCase`, `MapRatingUseCase`
- Pattern: Stateless `@Inject` classes with `operator fun invoke()`, return `Result<T>`

**FSRS-6 Scheduler:**
- Purpose: Pure algorithmic implementation of the Free Spaced Repetition Scheduler v6
- Location: `domain/src/main/java/.../domain/fsrs/v6/`
- Files: `Card.kt` (card model with toDict/fromDict/JSON serde), `State.kt` (enum: New/Learning/Review/Relearning), `ReviewLog.kt`, `Scheduler.kt` (all math), `Optimizer.kt`
- Properties: 21 FSRS parameters, learning/relearning steps (millis), desired retention, fuzzing support
- Dependencies: Zero — pure Kotlin math (exp, pow, log), serializable to/from py-fsrs JSON/Map format

**Type-Safe Navigation:**
- Purpose: Compiler-checked routing instead of string-based routes
- Location: `app/src/main/java/.../ui/navigation/Screen.kt`
- Pattern: `sealed class Screen : NavKey` with `@Serializable` subtypes
- Parametric routes: `Quiz(val cardIds: List<String>?)`, `Result(val xpGained: Int, ...)`, `Guidebook(val unitId: String)`, `JumpTest(unitId)`, `SectionCheckpoint(sectionId)`, `UnitCheckpoint(unitId)`
- Back stack: `SnapshotStateList<NavKey>` in `MainViewModel`, rendered by `NavDisplay` (Navigation 3 library)

**Curriculum Domain Model:**
- Purpose: Hierarchical learning path structure — Section → Unit → Node → Session → Question
- Location: `domain/src/main/java/.../domain/model/Curriculum.kt`
- Key types: `Section` (CEFR level grouping), `Unit` (topic-based), `Node` (LESSON/REVIEW/CHECKPOINT/JUMP_TEST), `Session` (bundle of questions), `Question` (individual quiz item)
- Node types: LESSON, REVIEW, UNIT_CHECKPOINT, SECTION_CHECKPOINT, JUMP_TEST, GUIDEBOOK
- Question types: INTRODUCTION, FILL_IN_BLANK, MULTIPLE_CHOICE, SCRAMBLED, LISTENING, MATCHING, TYPING

## Entry Points

**Android App:**
- Location: `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt`
- Triggers: Android OS — app process start
- Responsibilities: Hilt initialization, V6 DB migration reset, crash handler setup in debug

**MainActivity:**
- Location: `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt`
- Triggers: Android launcher intent
- Responsibilities: Single activity host, injects all repositories (5), creates 5 ViewModels, binds TTS lifecycle, initializes default reminder settings, renders `VocabMasterApp` Composable

**Backend API:**
- Location: `backend/app/main.py`
- Triggers: Uvicorn server start
- Responsibilities: Creates DB tables via SQLAlchemy, includes 3 routers (vocabulary, placement, sync), provides health check endpoints
- Endpoints: `GET /` (welcome), `GET /api/v1/health`, `GET /api/v1/me` (Firebase auth check)

## Architectural Constraints

- **Threading:** Single-threaded UI via Compose main thread; all DB/network operations use `Dispatchers.IO` via `withContext` or `flowOn(Dispatchers.IO)` in repository implementations; coroutines structured in `viewModelScope`
- **Global state:** `SnapshotStateList<NavKey>` in `MainViewModel` (`backStack`) is the sole navigation state owner — survives config changes; `curriculumStatus: StateFlow<List<SectionStatus>>` is a `combine()` of 3 flows in `MainViewModel` — reactive curriculum tree
- **Circular imports:** Not detected — layer separation via Gradle modules prevents cycles; `data` → `domain`, `app` → both
- **Room main-thread guard:** `DataModule.provideVocabDatabase()` deliberately omits `allowMainThreadQueries()` — enforces that all DAO calls are `suspend` or `Flow` (PERS-02)
- **No production database migrations:** `fallbackToDestructiveMigration(dropAllTables = true)` configured — all schema changes are destructive; curriculum is re-seeded from `lessons_v3.json` asset on next access after DB wipe
- **FSRS-6 determinism:** `enableFuzzing` defaults to `false` for deterministic test output (py-fsrs defaults to `true`)
- **Curriculum seeding:** `ensureCurriculumAndFsrsSeeded()` guarded by a `Mutex` — runs once at first access via `questionCount == 0` check; reads `lessons_v3.json` from Android assets, deserializes into Room entities and FSRS cards

## Anti-Patterns

### Topic/Level Query Fallbacks

**What happens:** Several `VocabularyRepository` query methods (`getCardsByLevel`, `getDueCardsByTopic`, `getNewCardsByTopicAndLevels`) do not actually filter by topic or level — they fall back to `getDueAndNewCardsByTopicFallback` which is a simple "get all" query (`data/repository/VocabularyRepositoryImpl.kt:221`).
**Why it's wrong:** The `questions` table lacks direct topic/level columns (they are implied by Section → Unit hierarchy), so topic-scoped queries require multi-table joins that were never fully implemented.
**Do this instead:** Implement proper JOIN queries in `VocabDao` that walk the Section → Unit → Node → Session → Question chain, or denormalize topic/level onto the `questions` table.

### Obsolete Completed-Lessons API

**What happens:** `getCompletedLessons(stage, unitTopic)` returns `flow { emit(emptyList()) }` and `markLessonCompleted()` is a no-op (`VocabularyRepositoryImpl.kt:584-588`).
**Why it's wrong:** These legacy methods from an earlier curriculum model are never removed; dead code creates confusion about the API surface.
**Do this instead:** Remove these methods from the interface and all call sites (currently no callers exist in production code — only the interface contract remains).

## Error Handling

**Strategy:** `Result<T>` return type in use cases and repository suspend functions; exceptions are caught and wrapped with `runCatching` at the use case boundary.

**Patterns:**
- Use cases wrap bodies in `runCatching { ... }.getOrElse { Result.failure(it) }` — caller always gets `Result<T>` never an unhandled exception
- Repository implementations use `withContext(Dispatchers.IO)` + `runCatching` for individual operations
- `VocabDataException` custom exception type in domain layer for data integrity violations (malformed JSON, unknown enum values)
- `SyncManager` catches `IOException`, `HttpException`, `CancellationException`, and generic `Exception` separately with distinct logging
- ViewModels launch coroutines in `viewModelScope` — uncaught exceptions crash the coroutine but not the app (Hilt/ViewModel lifecycle handles cleanup)

## Cross-Cutting Concerns

**Logging:** `LocalLogger` utility in `app/src/main/java/.../util/LocalLogger.kt` — wraps Android `Log` with debug-only crash handler setup in `VocabApplication.onCreate()`
**Validation:** Runtime assertions in `VocabularyRepositoryImpl.ensureCurriculumAndFsrsSeeded()` — checks `MULTIPLE_CHOICE` has 4 options, `MATCHING` has >=3 pairs, etc. during curriculum seeding
**Authentication:** Firebase Auth via `AuthManager` (`data/src/main/java/.../data/auth/AuthManager.kt`) — `AuthInterceptor` attaches Firebase ID token to all Retrofit HTTP requests; backend validates via `get_current_user_uid` Firebase middleware
**Serialization:** `kotlinx.serialization` used throughout — JSON for Room entity fields (options, scrambledWords, matchingPairs), Retrofit body conversion, FSRS Card/Scheduler JSON serde, sync DTOs

---

*Architecture analysis: 2026-07-22*
