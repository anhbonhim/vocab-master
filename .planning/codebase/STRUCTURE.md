# Codebase Structure

**Analysis Date:** 2026-07-22

## Directory Layout

```
vocab-master/
├── app/                          # Android Application module (Compose UI + DI)
│   ├── build.gradle.kts
│   └── src/main/java/com/nhimz/vocabmaster/
│       ├── MainActivity.kt       # Single Activity entry point
│       ├── VocabApplication.kt   # @HiltAndroidApp, crash handler, V6 migration
│       ├── audio/
│       │   └── CDNAudioPlayer.kt # ExoPlayer CDN audio with disk cache
│       ├── notification/
│       │   ├── NotificationReceiver.kt
│       │   └── NotificationScheduler.kt  # AlarmManager daily reminders
│       ├── ui/
│       │   ├── VocabMasterApp.kt         # Top-level Scaffold + NavDisplay
│       │   ├── navigation/
│       │   │   ├── NavGraph.kt           # Entry provider (all route declarations)
│       │   │   └── Screen.kt             # Type-safe @Serializable sealed routes
│       │   ├── screens/
│       │   │   ├── WelcomeScreen.kt
│       │   │   ├── LoginScreen.kt
│       │   │   ├── GoalPickerScreen.kt
│       │   │   ├── PlacementTestScreen.kt
│       │   │   ├── FirstWinScreen.kt
│       │   │   ├── HomeScreen.kt
│       │   │   ├── HomeScreenContent.kt
│       │   │   ├── QuizScreen.kt
│       │   │   ├── QuizScreenContent.kt
│       │   │   ├── ResultScreen.kt
│       │   │   ├── ResultScreenContent.kt
│       │   │   ├── StatisticsScreen.kt
│       │   │   ├── SettingsScreen.kt
│       │   │   ├── SettingsScreenContent.kt
│       │   │   ├── UnitGuidebookScreen.kt
│       │   │   ├── JumpTestScreen.kt
│       │   │   ├── SectionCheckpointScreen.kt
│       │   │   ├── UnitCheckpointScreen.kt
│       │   │   ├── DebugPanelScreen.kt
│       │   │   ├── GoalPickerScreen.kt
│       │   │   ├── statistics_components/
│       │   │   │   ├── OverviewTab.kt
│       │   │   │   ├── MistakeBankTab.kt
│       │   │   │   └── BadgesTab.kt
│       │   │   └── debug_components/
│       │   │       ├── DataIntegrityTests.kt
│       │   │       ├── SystemSettingsTests.kt
│       │   │       └── TestRunner.kt
│       │   ├── components/
│       │   │   ├── DuoSnackbar.kt
│       │   │   ├── Duo3DCard.kt
│       │   │   ├── SnackbarMessage.kt
│       │   │   └── quiz/
│       │   │       ├── DuolingoOptionCard.kt
│       │   │       ├── DuolingoProgressBar.kt
│       │   │       ├── FSRSTreeProgressBar.kt
│       │   │       ├── FeedbackBanner.kt
│       │   │       ├── IntroductionCard.kt
│       │   │       ├── ListeningQuestionCard.kt
│       │   │       ├── MatchingQuestionCard.kt
│       │   │       ├── ScrambledQuizCard.kt
│       │   │       ├── ScrambledWordMapper.kt
│       │   │       └── TypingQuestionCard.kt
│       │   ├── viewmodel/
│       │   │   ├── MainViewModel.kt
│       │   │   ├── QuizViewModel.kt
│       │   │   ├── QuizUiState.kt
│       │   │   ├── SettingsViewModel.kt
│       │   │   ├── StatisticsViewModel.kt
│       │   │   ├── PlacementTestViewModel.kt
│       │   │   ├── FirstWinViewModel.kt
│       │   │   └── LoginViewModel.kt
│       │   ├── theme/
│       │   │   ├── Theme.kt
│       │   │   ├── Color.kt
│       │   │   ├── Type.kt
│       │   │   └── AppIcons.kt
│       │   └── util/
│       │       └── FeedbackHelper.kt
│       └── util/
│           └── LocalLogger.kt
│
├── data/                          # Android Library module (data layer)
│   ├── build.gradle.kts
│   └── src/main/java/com/nhimz/vocabmaster/data/
│       ├── auth/
│       │   └── AuthManager.kt          # Firebase Auth + Google Sign-In
│       ├── database/
│       │   ├── VocabDatabase.kt        # Room DB v8, 10 entities
│       │   ├── VocabDao.kt             # All DAO queries
│       │   ├── Converters.kt           # Room type converters
│       │   └── entity/
│       │       ├── FsrsCardEntity.kt
│       │       ├── ReviewLogEntity.kt
│       │       ├── FlaggedItemEntity.kt
│       │       ├── SectionEntity.kt
│       │       ├── UnitEntity.kt
│       │       ├── UnitGuidebookEntity.kt
│       │       ├── NodeEntity.kt
│       │       ├── SessionEntity.kt
│       │       ├── QuestionEntity.kt
│       │       ├── NodeProgressEntity.kt
│       │       ├── SessionProgressEntity.kt
│       │       └── QuestionAndFsrsCard.kt
│       ├── di/
│       │   └── DataModule.kt           # Hilt DI bindings
│       ├── model/
│       │   └── BackupModels.kt         # Backup DTOs
│       ├── remote/
│       │   ├── ApiClient.kt            # Retrofit client setup
│       │   ├── AuthInterceptor.kt      # OkHttp auth interceptor
│       │   ├── PlacementApiService.kt
│       │   ├── VocabularyApiService.kt
│       │   └── SyncPayload.kt          # Sync DTOs
│       ├── repository/
│       │   ├── VocabularyRepositoryImpl.kt
│       │   ├── ReviewRepositoryImpl.kt
│       │   ├── SettingsRepositoryImpl.kt
│       │   └── BackupRepositoryImpl.kt
│       └── sync/
│           └── SyncManager.kt          # Push/pull sync with backend
│
├── domain/                        # Pure Kotlin module (business logic)
│   ├── build.gradle.kts
│   └── src/main/java/com/nhimz/vocabmaster/domain/
│       ├── DomainPlaceholder.kt
│       ├── fsrs/
│       │   └── v6/
│       │       ├── Card.kt             # FSRS card model
│       │       ├── State.kt            # New/Learning/Review/Relearning
│       │       ├── ReviewLog.kt        # Review log model
│       │       ├── Scheduler.kt        # FSRS-6 algorithm (516 lines)
│       │       └── Optimizer.kt        # Parameter optimization
│       ├── model/
│       │   ├── VocabularyRepository.kt # Interface
│       │   ├── ReviewRepository.kt     # Interface
│       │   ├── SettingsRepository.kt   # Interface
│       │   ├── BackupRepository.kt     # Interface
│       │   ├── VocabDataException.kt
│       │   ├── Curriculum.kt           # Section/Unit/Node/Question models
│       │   ├── QuestionWithCard.kt
│       │   ├── QuestionExtensions.kt
│       │   └── quiz/
│       │       └── QuizSessionModels.kt # QuizType, QuizQuestion, etc.
│       └── usecase/
│           ├── EvaluateAnswerUseCase.kt
│           ├── SubmitReviewUseCase.kt
│           ├── LoadQuizSessionUseCase.kt
│           ├── CompleteQuizSessionUseCase.kt
│           ├── UpdateStreakUseCase.kt
│           ├── MapRatingUseCase.kt
│           └── PlacementTestUseCase.kt
│
├── backend/                       # Python FastAPI backend (separate server)
│   ├── requirements.txt
│   ├── main.py                    # FastAPI entry point
│   ├── database.py                # SQLAlchemy setup
│   ├── config.py                  # Environment config
│   ├── seed_db.py
│   ├── setup.sh / run.sh
│   └── app/
│       ├── routers/
│       │   ├── vocabulary.py
│       │   ├── placement.py
│       │   └── sync.py
│       ├── models/
│       ├── schemas/
│       ├── services/
│       └── utils/
│           └── firebase_auth.py
│
├── build.gradle.kts               # Root Gradle build — detekt config, plugins
├── settings.gradle.kts            # Multi-module: :app, :data, :domain
├── gradle.properties
├── gradle/                        # Gradle wrapper + version catalog
├── config/
│   └── detekt/
│       ├── detekt.yml
│       └── baseline.xml
├── gradlew / gradlew.bat
├── package.json                    # Node tooling (prompt format scripts)
├── PROJECT_CONTEXT.md
├── PROJECT_WIKI.md
├── AGENTS.md
└── .planning/                     # GSD planning artifacts
```

## Directory Purposes

**`app/` — Android Application Module:**
- Purpose: Entry point, Compose UI, ViewModels, audio, notifications, debug tools
- Contains: `MainActivity`, `VocabApplication`, 8 ViewModels, 18 screens, 10 quiz components, navigation (type-safe routes)
- Key files: `MainActivity.kt` (DI wiring), `VocabMasterApp.kt` (top-level Scaffold), `NavGraph.kt` (route registration)

**`data/` — Android Library Data Module:**
- Purpose: Room DB, Retrofit API, Firebase Auth, DataStore preferences, cloud sync
- Contains: 4 repositories, 12 Room entities, 3 API service interfaces, sync manager
- Key files: `DataModule.kt` (DI bindings), `VocabDatabase.kt` (Room v8), `ApiClient.kt` (Retrofit setup), `SyncManager.kt`

**`domain/` — Pure Kotlin Domain Module:**
- Purpose: Business logic, FSRS algorithm, domain models, repository interfaces
- Contains: 4 repository interfaces, 7 use cases, 5 FSRS v6 classes, curriculum/quiz models
- Key files: `Scheduler.kt` (FSRS-6 core, 516 lines), `LoadQuizSessionUseCase.kt` (quiz orchestration)

**`backend/` — Python FastAPI Server:**
- Purpose: REST API for sync, placement testing, vocabulary management
- Contains: FastAPI app with 3 routers, SQLAlchemy models, Firebase auth middleware

## Naming Conventions

**Files:**
- **Kotlin:** PascalCase per class name, e.g. `QuizViewModel.kt`, `VocabularyRepository.kt`
- **Data entities:** `*Entity.kt` suffix, e.g. `FsrsCardEntity.kt`, `SectionEntity.kt`
- **Test files:** `*Test.kt` suffix, e.g. `QuizViewModelTest.kt`
- **Backend:** snake_case Python files, e.g. `firebase_auth.py`, `sync_payload.py`

**Directories:**
- **Kotlin packages:** lowercase dotted package convention, e.g. `com.nhimz.vocabmaster.ui.viewmodel`
- **Feature grouping:** Screens in `ui/screens/`, screen-specific subcomponents in `ui/screens/statistics_components/` and `ui/screens/debug_components/`
- **Shared UI components:** grouped by domain in `ui/components/quiz/`

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt`: Single Activity, Hilt entry, ViewModel wiring
- `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt`: `@HiltAndroidApp`, crash handler, migration
- `backend/app/main.py`: FastAPI app with `uvicorn`

**Configuration:**
- `build.gradle.kts`: Root build — version catalog aliases, detekt config
- `settings.gradle.kts`: Multi-module includes (`:app`, `:data`, `:domain`)
- `gradle.properties`: Android/Kotlin/Compose compiler properties
- `app/build.gradle.kts`: app module dependencies (Compose, Hilt, Room, Navigation 3)
- `data/build.gradle.kts`: Room, Hilt, Retrofit, Firebase, DataStore
- `domain/build.gradle.kts`: Pure Kotlin JVM, coroutines, serialization

**Core Logic:**
- `domain/.../fsrs/v6/Scheduler.kt`: FSRS-6 algorithm (516 lines)
- `domain/.../usecase/LoadQuizSessionUseCase.kt`: 6 quiz mode types → question loading
- `data/.../repository/VocabularyRepositoryImpl.kt`: 644 lines — curriculum seeding, CRUD, due card queries
- `data/.../sync/SyncManager.kt`: Bidirectional push/pull sync (213 lines)
- `app/.../ui/viewmodel/MainViewModel.kt`: Navigation state + curriculum computation (428 lines)
- `app/.../ui/viewmodel/QuizViewModel.kt`: Quiz lifecycle + SavedStateHandle persistence (589 lines)

**Testing:**
- `app/src/test/`: Unit tests for ViewModels, screens, components
- `app/src/androidTest/`: Instrumented tests (NavGraph)
- `data/src/test/`: DAO tests, repository tests, database smoke tests
- `domain/src/test/`: Use case tests, FSRS parity tests, golden vector tests

## Where to Add New Code

**New Feature (Quiz type):**
- Define new `QuizType` variant in `domain/.../model/quiz/QuizSessionModels.kt`
- Add grading logic in `domain/.../usecase/EvaluateAnswerUseCase.kt`
- Create UI card component in `app/.../ui/components/quiz/`
- Map question to quiz type in `domain/.../usecase/LoadQuizSessionUseCase.kt`

**New Screen/Route:**
- Add route to `Screen` sealed class in `app/.../ui/navigation/Screen.kt`
- Register entry in `app/.../ui/navigation/NavGraph.kt`
- Create screen composable in `app/.../ui/screens/`
- Add ViewModel if needed in `app/.../ui/viewmodel/`
- Wire ViewModel into `MainActivity` and `VocabMasterApp`

**New Data Entity:**
- Create entity class in `data/.../database/entity/`
- Add to `VocabDatabase.kt` entities list
- Add DAO methods in `data/.../database/VocabDao.kt`
- Add domain model in `domain/.../model/` if needed
- Add repository method in domain interface + data implementation
- Bind in `data/.../di/DataModule.kt` if new repository

**New API Endpoint (backend):**
- Add router in `backend/app/routers/`
- Add Pydantic schema in `backend/app/schemas/`
- Add API service interface on Android in `data/.../remote/`
- Wire into `ApiClient.kt` lazy services

**New Use Case:**
- Create file in `domain/.../usecase/`
- Implement `operator fun invoke()` returning `Result<T>`
- Inject via `@Inject constructor`
- Call from appropriate ViewModel

**New Utility:**
- Android-specific utilities: `app/.../util/`
- Domain utilities: `domain/.../model/`

## Special Directories

**`backend/`:**
- Purpose: Python FastAPI server, completely separate from Android app
- Generated: No
- Committed: Yes

**`config/detekt/`:**
- Purpose: Static analysis config — `detekt.yml` rules + `baseline.xml` for existing violations
- Generated: No
- Committed: Yes

**`node_modules/` (root):**
- Purpose: Node.js tooling for prompt formatting scripts (`format_prompt.js`, etc.)
- Generated: Yes (npm install)
- Committed: No

**`.planning/`:**
- Purpose: GSD workflow artifacts — plans, specs, decisions, codebase analysis
- Generated: Yes (by GSD system)
- Committed: Yes

**`build/` `app/build/` `data/build/`:**
- Purpose: Gradle build outputs
- Generated: Yes
- Committed: No

---

*Structure analysis: 2026-07-22*
