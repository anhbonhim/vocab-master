# Codebase Structure

**Analysis Date:** 2026-07-22

## Directory Layout

```
vocab-master/
├── app/                          # Android application module (Gradle)
│   ├── build.gradle.kts          # Dependencies: Compose, Hilt, Navigation3, Room
│   └── src/
│       ├── androidTest/          # Instrumented tests
│       │   └── java/com/nhimz/vocabmaster/
│       │       └── navigation/NavGraphTest.kt
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/nhimz/vocabmaster/
│       │       ├── MainActivity.kt            # Single activity entry point
│       │       ├── VocabApplication.kt        # @HiltAndroidApp
│       │       ├── audio/
│       │       │   └── CDNAudioPlayer.kt      # TTS/audio playback
│       │       ├── auth/                      # (empty) — auth lives in :data
│       │       ├── notification/
│       │       │   ├── NotificationReceiver.kt
│       │       │   └── NotificationScheduler.kt
│       │       ├── ui/
│       │       │   ├── VocabMasterApp.kt      # Top-level Composable
│       │       │   ├── components/
│       │       │   │   ├── Duo3DCard.kt
│       │       │   │   ├── DuoSnackbar.kt
│       │       │   │   ├── SnackbarMessage.kt
│       │       │   │   └── quiz/
│       │       │   │       ├── IntroductionCard.kt
│       │       │   │       ├── ListeningQuestionCard.kt
│       │       │   │       ├── MatchingQuestionCard.kt
│       │       │   │       └── TypingQuestionCard.kt
│       │       │   ├── navigation/
│       │       │   │   ├── NavGraph.kt        # entryProvider — route→Composable mapping
│       │       │   │   └── Screen.kt          # sealed class with 16 type-safe routes
│       │       │   ├── screens/
│       │       │   │   ├── DebugPanelScreen.kt
│       │       │   │   ├── FirstWinScreen.kt
│       │       │   │   ├── GoalPickerScreen.kt
│       │       │   │   ├── HomeScreen.kt
│       │       │   │   ├── HomeScreenContent.kt
│       │       │   │   ├── JumpTestScreen.kt
│       │       │   │   ├── LoginScreen.kt
│       │       │   │   ├── PlacementTestScreen.kt
│       │       │   │   ├── QuizScreen.kt
│       │       │   │   ├── QuizScreenContent.kt
│       │       │   │   ├── ResultScreen.kt
│       │       │   │   ├── ResultScreenContent.kt
│       │       │   │   ├── SectionCheckpointScreen.kt
│       │       │   │   ├── SettingsScreen.kt
│       │       │   │   ├── SettingsScreenContent.kt
│       │       │   │   ├── StatisticsScreen.kt
│       │       │   │   ├── UnitCheckpointScreen.kt
│       │       │   │   ├── UnitGuidebookScreen.kt
│       │       │   │   ├── WelcomeScreen.kt
│       │       │   │   ├── debug_components/
│       │       │   │   │   └── DataIntegrityTests.kt
│       │       │   │   └── statistics_components/
│       │       │   │       ├── MistakeBankTab.kt
│       │       │   │       └── OverviewTab.kt
│       │       │   ├── theme/                  # Material3 theme
│       │       │   └── viewmodel/
│       │       │       ├── FirstWinViewModel.kt
│       │       │       ├── LoginViewModel.kt
│       │       │       ├── MainViewModel.kt    # Navigation, curriculum, badges
│       │       │       ├── PlacementTestViewModel.kt
│       │       │       ├── QuizUiState.kt
│       │       │       ├── QuizViewModel.kt    # Quiz state machine
│       │       │       ├── SettingsViewModel.kt
│       │       │       └── StatisticsViewModel.kt
│       │       └── util/
│       │           └── LocalLogger.kt
│       └── test/                              # Unit tests
│           └── java/com/nhimz/vocabmaster/
│               ├── ui/screens/Plan0301ContainerContentTest.kt
│               ├── ui/viewmodel/
│               │   ├── MainDispatcherRule.kt
│               │   ├── QuizViewModelPersistenceTest.kt
│               │   ├── QuizViewModelTest.kt
│               │   └── fakes/
│               │       ├── FakeReviewRepository.kt
│               │       ├── FakeSettingsRepository.kt
│               │       └── FakeVocabularyRepository.kt
│               └── ...
├── domain/                       # Pure Kotlin domain module
│   ├── build.gradle.kts          # Only kotlinx + javax.inject
│   └── src/
│       ├── main/java/com/nhimz/vocabmaster/domain/
│       │   ├── DomainPlaceholder.kt
│       │   ├── fsrs/v6/
│       │   │   ├── Card.kt                    # FSRS card model + JSON/Map serde
│       │   │   ├── Optimizer.kt               # FSRS parameter optimizer
│       │   │   ├── ReviewLog.kt               # Review log model
│       │   │   ├── Scheduler.kt               # Full FSRS-6 algorithm (516 lines)
│       │   │   └── State.kt                   # New/Learning/Review/Relearning enum
│       │   ├── model/
│       │   │   ├── BackupRepository.kt        # Interface for JSON backup
│       │   │   ├── Curriculum.kt              # Section/Unit/Node/Session/Question models
│       │   │   ├── QuestionExtensions.kt
│       │   │   ├── QuestionWithCard.kt
│       │   │   ├── ReviewRepository.kt        # Interface for review logs
│       │   │   ├── SettingsRepository.kt      # Interface for user settings
│       │   │   ├── VocabDataException.kt
│       │   │   ├── VocabularyRepository.kt    # Interface for cards & curriculum
│       │   │   └── quiz/
│       │   │       └── QuizSessionModels.kt   # QuizQuestion, QuizType sealed class
│       │   └── usecase/
│       │       ├── CompleteQuizSessionUseCase.kt
│       │       ├── EvaluateAnswerUseCase.kt
│       │       ├── LoadQuizSessionUseCase.kt
│       │       ├── MapRatingUseCase.kt
│       │       ├── PlacementTestUseCase.kt
│       │       ├── SubmitReviewUseCase.kt
│       │       └── UpdateStreakUseCase.kt
│       └── test/                              # Pure JVM tests (no Android)
│           └── java/com/nhimz/vocabmaster/domain/
│               ├── fsrs/v6/
│               │   ├── GoldenVectorTest.kt
│               │   ├── OptimizerTest.kt
│               │   └── PyFsrsParityTest.kt
│               ├── usecase/
│               │   ├── CompleteQuizSessionUseCaseTest.kt
│               │   ├── EvaluateAnswerUseCaseTest.kt
│               │   ├── LoadQuizSessionUseCaseTest.kt
│               │   ├── SubmitReviewUseCaseTest.kt
│               │   ├── UseCasesTest.kt
│               │   └── fakes/
│               │       ├── FakeReviewRepository.kt
│               │       ├── FakeSettingsRepository.kt
│               │       └── FakeVocabularyRepository.kt
│               └── ...
├── data/                         # Android data module
│   ├── build.gradle.kts          # Room, Hilt, Retrofit, Firebase Auth
│   └── src/
│       ├── main/java/com/nhimz/vocabmaster/data/
│       │   ├── auth/
│       │   │   └── AuthManager.kt             # Firebase Authentication
│       │   ├── database/
│       │   │   ├── Converters.kt              # Room type converters
│       │   │   ├── VocabDao.kt                # All SQL queries (336 lines)
│       │   │   ├── VocabDatabase.kt           # Room DB (v8, 10 entities)
│       │   │   └── entity/
│       │   │       ├── FlaggedItemEntity.kt
│       │   │       ├── FsrsCardEntity.kt
│       │   │       ├── NodeEntity.kt
│       │   │       ├── NodeProgressEntity.kt
│       │   │       ├── QuestionAndFsrsCard.kt  # POJO for JOIN query results
│       │   │       ├── QuestionEntity.kt
│       │   │       ├── ReviewLogEntity.kt
│       │   │       ├── SectionEntity.kt
│       │   │       ├── SessionEntity.kt
│       │   │       ├── SessionProgressEntity.kt
│       │   │       ├── UnitEntity.kt
│       │   │       └── UnitGuidebookEntity.kt
│       │   ├── di/
│       │   │   └── DataModule.kt              # Hilt @Module — provides DB, DAO, binds repos
│       │   ├── model/
│       │   │   └── BackupModels.kt            # JSON backup serialization models
│       │   ├── remote/
│       │   │   ├── ApiClient.kt               # Retrofit client + auth interceptor
│       │   │   ├── AuthInterceptor.kt
│       │   │   ├── PlacementApiService.kt
│       │   │   ├── SyncPayload.kt             # Sync DTOs
│       │   │   └── ...
│       │   ├── repository/
│       │   │   ├── BackupRepositoryImpl.kt
│       │   │   ├── ReviewRepositoryImpl.kt
│       │   │   ├── SettingsRepositoryImpl.kt
│       │   │   └── VocabularyRepositoryImpl.kt  # (644 lines)
│       │   └── sync/
│       │       └── SyncManager.kt             # Bidirectional push/pull sync
│       └── test/                              # Android-roboelectric tests
│           └── java/com/nhimz/vocabmaster/data/
│               ├── database/
│               │   ├── VocabDaoTest.kt
│               │   └── VocabDatabaseSmokeTest.kt
│               ├── repository/
│               │   └── VocabularyRepositoryImplTest.kt
│               └── sync/
│                   └── SyncManagerTest.kt
├── backend/                      # Python FastAPI backend
│   ├── requirements.txt
│   ├── vocab.db                  # SQLite database (server-side)
│   ├── firebase-service-account.json
│   ├── run.sh
│   ├── setup.sh
│   └── app/
│       ├── __pycache__/
│       ├── config.py             # Pydantic settings
│       ├── database.py           # SQLAlchemy engine + session
│       ├── main.py               # FastAPI app + routers
│       ├── models/
│       │   ├── placement_session.py
│       │   ├── user.py
│       │   ├── user_progress.py
│       │   └── vocabulary.py
│       ├── routers/
│       │   ├── placement.py
│       │   ├── sync.py           # Push/Pull sync endpoints
│       │   └── vocabulary.py
│       ├── schemas/              # Pydantic request/response models
│       ├── services/
│       │   └── irt_engine.py     # Item Response Theory for placement
│       └── utils/
│           └── firebase_auth.py  # Firebase token verification middleware
├── docs/                         # Documentation
├── config/
│   └── detekt/
│       ├── detekt.yml
│       └── baseline.xml
├── .planning/                    # GSD planning artifacts
│   └── codebase/                 # (this directory)
├── build.gradle.kts              # Root build config (detekt plugin)
├── settings.gradle.kts           # Includes :app, :data, :domain
├── gradle.properties
├── gradlew / gradlew.bat
└── package.json                  # Node scripts for planning tooling
```

## Directory Purposes

**`app/` (Android Application Module):**
- Purpose: UI layer — all Compose screens, ViewModels, navigation, and Android-specific infrastructure (audio, notifications)
- Contains: Jetpack Compose UI files, ViewModels, navigation sealed class + entryProvider, `NotificationScheduler`/`Receiver`, `CDNAudioPlayer`
- Key files: `MainActivity.kt` (single activity entry point), `VocabApplication.kt` (@HiltAndroidApp), `VocabMasterApp.kt` (top-level Composable composable tree root)

**`domain/` (Pure Kotlin Module):**
- Purpose: Business logic layer — zero Android dependencies, pure JVM
- Contains: FSRS-6 scheduler port, repository interfaces, domain models (Curriculum, Question, Card, QuizSession), 7 use cases
- Key files: `Scheduler.kt` (FSRS-6 algorithm), `VocabularyRepository.kt` (main repository interface), `LoadQuizSessionUseCase.kt` (quiz question loading with 5 request types)

**`data/` (Android Data Module):**
- Purpose: Data persistence and networking — Room database, Retrofit HTTP client, Firebase auth, sync
- Contains: Room entities (12), `VocabDao` (DAOs), repository implementations (4), `ApiClient`, `SyncManager`, `DataModule` (Hilt DI)
- Key files: `VocabDatabase.kt` (DB v8), `VocabDao.kt` (all SQL + `mergePulledCards` sync logic), `DataModule.kt` (DI bindings), `SyncManager.kt`

**`backend/` (Python API Server):**
- Purpose: Cloud sync backend — user data persistence, cross-device sync, placement testing
- Contains: FastAPI app, SQLAlchemy models for User/UserCard/ReviewLog/UserSettings, Firebase auth middleware, sync push/pull endpoints, IRT engine
- Key files: `main.py` (FastAPI entry), `sync.py` (sync router), `irt_engine.py` (placement test IRT)

**`.planning/`:**
- Purpose: GSD (Goal-driven Software Development) planning artifacts
- Contains: Roadmap, phase plans, specs, codebase analysis docs, phase manifests
- Committed: Yes

**`config/`:**
- Purpose: Static analysis tooling configuration
- Contains: detekt (Kotlin linter) YAML config + baseline

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt`: Android Application class, `@HiltAndroidApp`
- `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt`: Single activity, launcher intent receiver (`AndroidManifest.xml:24`)
- `backend/app/main.py`: FastAPI ASGI entry point (Uvicorn)
- `settings.gradle.kts`: Gradle multi-module root (includes `:app`, `:data`, `:domain`)

**Configuration:**
- `app/build.gradle.kts`: Compile SDK 36, Compose, Hilt, Navigation 3, Room (10 entities)
- `data/build.gradle.kts`: `API_BASE_URL = "http://127.0.0.1:8000/"` (dev server), Firebase Auth BOM, Room
- `domain/build.gradle.kts`: Pure Kotlin — only kotlinx-coroutines, kotlinx-serialization, javax.inject
- `backend/app/config.py`: `DATABASE_URL = "sqlite:///./vocab.db"`, Firebase credentials path
- `build.gradle.kts`: Root — detekt lint plugin applied to all projects
- `gradle.properties`: Gradle JVM properties

**Core Logic:**
- `domain/src/main/java/.../fsrs/v6/Scheduler.kt`: FSRS-6 algorithm (516 lines) — reviewCard, rescheduleCard, getCardRetrievability, 21 parameters
- `domain/src/main/java/.../usecase/LoadQuizSessionUseCase.kt`: Loads quiz questions by request type (NodeSession, ReviewNode, UnitCheckpoint, JumpTest, SectionCheckpoint, MistakeReview)
- `domain/src/main/java/.../usecase/SubmitReviewUseCase.kt`: Evaluates answer, calls FSRS scheduler, persists review log + XP
- `app/src/main/java/.../ui/viewmodel/MainViewModel.kt`: Navigation back stack owner, curriculum status builder (combine of 3 flows), badge monitoring
- `app/src/main/java/.../ui/viewmodel/QuizViewModel.kt`: Quiz session state machine
- `data/src/main/java/.../database/VocabDao.kt`: All Room queries (336 lines) including `mergePulledCards` sync merge
- `data/src/main/java/.../repository/VocabularyRepositoryImpl.kt`: Full repository implementation (644 lines) with curriculum seeding + domain mapping
- `data/src/main/java/.../sync/SyncManager.kt`: Push/pull sync orchestration (205 lines)
- `app/src/main/java/.../ui/navigation/Screen.kt`: 16 type-safe navigation routes
- `app/src/main/java/.../ui/navigation/NavGraph.kt`: Route-to-Composable mapping (297 lines)

**Testing:**
- `app/src/test/`: Unit tests for ViewModels (QuizViewModel, MainDispatcherRule), fakes
- `app/src/androidTest/`: Instrumented test for NavGraph navigation
- `domain/src/test/`: Pure JVM tests for FSRS (golden vector parity, optimizer) and use cases
- `data/src/test/`: Room database tests (Dao, smoke), repository tests, SyncManager tests (Robolectric)

## Naming Conventions

**Files:**
- **Kotlin source files:** PascalCase — `MainViewModel.kt`, `VocabMasterApp.kt`, `QuizScreen.kt`
- **Test files:** `{TestedClass}Test.kt` — `QuizViewModelTest.kt`, `VocabDaoTest.kt`
- **Android entities:** PascalCase with `Entity` suffix — `QuestionEntity.kt`, `FsrsCardEntity.kt`
- **Android DAO:** `{tableName}Dao` pattern — `VocabDao` (single DAO for all tables)
- **Repository interfaces:** PascalCase in `domain/model/` — `VocabularyRepository.kt`, `SettingsRepository.kt`
- **Repository implementations:** PascalCase with `Impl` suffix in `data/repository/` — `VocabularyRepositoryImpl.kt`
- **Python files:** snake_case — `main.py`, `firebase_auth.py`, `irt_engine.py`
- **Gradle files:** `build.gradle.kts`, `settings.gradle.kts`

**Directories:**
- **Kotlin packages:** dot-separated lowercase — `com.nhimz.vocabmaster.ui.viewmodel`, `com.nhimz.vocabmaster.domain.fsrs.v6`
- **Android source sets:** `src/main/java/`, `src/test/java/`, `src/androidTest/java/`
- **Feature subdirectories:** lowercase plural — `screens/`, `components/`, `viewmodel/`, `viewmodel/fakes/`
- **Backend Python:** `routers/`, `models/`, `schemas/`, `services/`, `utils/`

**Functions/Methods:**
- **Kotlin UI composables:** PascalCase — `VocabMasterApp()`, `HomeScreen()`, `VocabMasterBottomBar()`
- **Kotlin functions:** camelCase — `navigateTo()`, `goBack()`, `checkOnboardingStatus()`, `ensureCurriculumAndFsrsSeeded()`
- **Use case invocation:** `operator fun invoke()` — all use cases are callable as `loadQuizSessionUseCase(request)`
- **Python functions/routes:** snake_case — `sync_push()`, `sync_pull()`, `get_current_user_uid()`

**Types:**
- **Domain model classes:** PascalCase — `Section`, `Unit`, `Node`, `Session`, `Question`, `Card`
- **Repository interfaces:** PascalCase with `Repository` suffix
- **Use cases:** PascalCase with `UseCase` suffix — `SubmitReviewUseCase`
- **ViewModels:** PascalCase with `ViewModel` suffix — `MainViewModel`, `QuizViewModel`
- **Enums:** PascalCase — `State` (New/Learning/Review/Relearning), `NodeType`, `QuestionType`, `Rating`
- **Sealed classes/types:** PascalCase — `Screen`, `QuizType`, `QuizSessionRequest`
- **Data transfer objects:** PascalCase with `Dto` suffix — `VocabularyCardDto`, `SyncPayload`, `UserSettingsDto`
- **Room entities:** PascalCase with `Entity` suffix

## Where to Add New Code

**New Feature (e.g., new quiz type):**
1. Add domain model/type in `domain/src/main/java/.../domain/model/` — e.g., new `QuestionType` enum member or new `QuizType` sealed subclass
2. Add/update use case in `domain/src/main/java/.../domain/usecase/` if new business logic
3. Add entity mapping in `data/src/main/java/.../data/database/entity/` if persistence changes
4. Update `VocabDao` in `data/src/main/java/.../data/database/VocabDao.kt` if new queries needed
5. Add Compose UI card in `app/src/main/java/.../ui/components/quiz/`
6. Add screen in `app/src/main/java/.../ui/screens/` (separate `*Screen.kt` + `*ScreenContent.kt` pattern)
7. Add navigation route in `app/src/main/java/.../ui/navigation/Screen.kt`
8. Wire route in `app/src/main/java/.../ui/navigation/NavGraph.kt`
9. Add ViewModel state in `app/src/main/java/.../ui/viewmodel/`

**New Screen/Route:**
1. Add `data object` or `data class` subclass to `Screen` sealed class in `Screen.kt`
2. Create screen Composable files in `ui/screens/` (follow `*Screen.kt` entry + `*ScreenContent.kt` composables pattern)
3. Add `entry<Screen.NewRoute> { ... }` in `vocabMasterEntryProvider()` in `NavGraph.kt`
4. If screen has state, create or reuse ViewModel in `ui/viewmodel/`

**New Repository Implementation:**
1. Define interface in `domain/src/main/java/.../domain/model/`
2. Implement in `data/src/main/java/.../data/repository/`
3. Bind in `data/src/main/java/.../di/DataModule.kt` using `@Binds`

**New Use Case:**
1. Create file in `domain/src/main/java/.../domain/usecase/`
2. Implement as `@Inject` class with `operator fun invoke()`
3. Return `Result<T>` for fallible operations
4. Inject into ViewModel or other use case

**New Backend Endpoint:**
1. Add Pydantic schema in `backend/app/schemas/`
2. Add SQLAlchemy model in `backend/app/models/`
3. Create or extend router in `backend/app/routers/`
4. Register router in `backend/app/main.py`

**Tests:**
- **Unit tests (Fakes):** `app/src/test/java/.../ui/viewmodel/fakes/` (Android context needed via Robolectric)
- **Unit tests (Domain):** `domain/src/test/java/.../domain/usecase/fakes/` (pure JVM, no Android)
- **Database tests:** `data/src/test/java/.../data/database/` (Robolectric)
- **Repository tests:** `data/src/test/java/.../data/repository/`
- **Navigation tests:** `app/src/androidTest/` (instrumented)

## Special Directories

**`.gradle/`:** Gradle build cache — generated, not committed
**`build/`:** Build outputs — generated, not committed
**`node_modules/`:** Node.js dependencies for planning tooling scripts — generated, not committed
**`venv/`:** Python virtual environment for backend — generated, not committed (in `.gitignore`)
**`.planning/`:** GSD planning artifacts — committed (architectural decisions, specs, codebase maps)
**`*.db` files:** `vocab.db` (root) + `backend/vocab.db` — SQLite databases, not committed (in `.gitignore`)
**`firebase-service-account.json`:** Firebase admin credentials — not committed (in `.gitignore`)

---

*Structure analysis: 2026-07-22*
