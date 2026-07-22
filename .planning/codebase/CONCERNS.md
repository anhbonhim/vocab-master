# Codebase Concerns

**Analysis Date:** 2026-07-22

## Tech Debt

### Destructive Database Migrations — All user progress wiped on every schema bump

- **Issue:** `VocabDatabase.kt` (`data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDatabase.kt`) has `exportSchema = false` and the production builder in `DataModule.kt` (`data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt:69`) uses `fallbackToDestructiveMigration(dropAllTables = true)`. Every Room version bump blows away all FSRS card state, review logs, flag data, and progress.
- **Impact:** Any user with a released app that has accumulated weeks of FSRS state loses everything on upgrade. The only recovery is re-seeding curriculum from `assets/`.
- **Fix approach:** Implement proper migration paths (Room migration objects) for the v7→v8 transition and all future bumps. Keep destructive only for early dev builds.

### Stale migration code path persists in VocabApplication

- **Issue:** `VocabApplication.onCreate()` (`app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt:27-35`) still runs `resetForMigrationV6()` based on a SharedPreferences key `db_version`. This is a one-shot cleanup intended for the v5→v6 FSRS port transition. After the v7→v8 destructive migration, any user still below version 6 in their prefs will trigger this stale code path unnecessarily (though the destructive migration already wiped those settings).
- **Impact:** Spurious settings reset if a user's `migration_state` prefs survive a reinstall or data restore, resetting daily goal, streak, XP, etc.
- **Fix approach:** Remove the migration V6 block and the `settingsRepositoryImpl` injection from `VocabApplication` entirely, since all migrations now go through Room's destructive path.

### Huge baseline of suppressed Detekt issues

- **Issue:** `config/detekt/baseline.xml` contains **631 suppressed issues** across the codebase — `ComplexCondition`, `CyclomaticComplexMethod`, `LongMethod`, `FunctionNaming`, `ConstructorParameterNaming`, `ComplexInterface`, etc. This means nearly every non-trivial file has its complexities suppressed rather than addressed.
- **Impact:** Detekt effectively provides no active quality feedback. New violations are invisible because the baseline is so comprehensive that the delta is rarely checked.
- **Fix approach:** Set a realistic target: clear 50-100 baseline entries per cycle by refactoring the worst offenders (see Large File Complexity below).

### Overuse of @Suppress annotations in source files

- **Issue:** Multiple files carry `@Suppress` at the class or file level, bypassing lint and static analysis:
  - `SyncManager.kt`: `@Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth", "StringLiteralDuplication", "LabeledExpression", "TooGenericExceptionCaught", "VariableNaming")`
  - `Scheduler.kt`: `@Suppress("MagicNumber", "NestedBlockDepth", "ComplexCondition", "UseRequire", "UseCheckOrError", "TooManyFunctions", "LongMethod", "CyclomaticComplexMethod", "LongParameterList")`
  - `BackupRepositoryImpl.kt`: `@Suppress("LongMethod", "LabeledExpression", "TooGenericExceptionCaught")`
  - `DataIntegrityTests.kt`: `@Suppress("LongMethod", "CyclomaticComplexMethod")`
  - `FeedbackHelper.kt`: `@Suppress("DEPRECATION")` (×2)
- **Impact:** Suppressions hide real complexity from both static analysis and code review.
- **Fix approach:** Refactor suppressed methods into smaller units — extract domain logic from ViewModel orchestration, split large composables into sub-composables, and remove `@Suppress` entries as the code improves.

### Test fake stubs with TODO implementations

- **Issue:** Fake repositories in both `app/src/test/` and `domain/src/test/` contain numerous `TODO("not needed for these tests")` stubs for interface methods. Affected files:
  - `app/src/test/java/.../fakes/FakeVocabularyRepository.kt` — 15 TODO stubs (e.g. `getCardById`, `updateCard`, `insertAll`, `getCount`, `getDueCount`, curriculum methods)
  - `app/src/test/java/.../fakes/FakeSettingsRepository.kt` — 7 TODO stubs (e.g. `updateDailyGoal`, `addStudySeconds`, `addBadge`)
  - `app/src/test/java/.../fakes/FakeReviewRepository.kt` — 4 TODO stubs (`getDueCards`, `getAllReviewLogs`, etc.)
  - `domain/src/test/java/.../fakes/FakeVocabularyRepository.kt` — 17 TODO stubs
  - `domain/src/test/java/.../fakes/FakeSettingsRepository.kt` — 7 TODO stubs
  - `domain/src/test/java/.../fakes/FakeReviewRepository.kt` — 4 TODO stubs
- **Impact:** When new tests exercise these stubbed methods, they will throw `NotImplementedError` at runtime. This creates a false sense of test coverage — the fakes look complete but most methods are non-functional.
- **Fix approach:** Replace every `TODO("not needed for these tests")` with a sensible default implementation. Stored collections backed by `mutableListOf` / `mutableMapOf` are cheap and make fakes resilient.

### SyncManager suppressed complexity — 205 lines with 7 catch blocks

- **Issue:** `SyncManager.sync()` (`data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:54-204`) is a single 150-line method that reads 9+ settings properties, maps cards to DTOs, maps logs to DTOs, pushes, pulls, and merges the result — all in one `try/catch` with 4 distinct catch clauses. It is suppressed for `LongMethod`, `CyclomaticComplexMethod`, `NestedBlockDepth`, and `TooGenericExceptionCaught`.
- **Impact:** Hard to test, hard to reason about failure modes. The `catch (e: Exception)` at line 200 catches anything that fell through, making debugging sync failures difficult.
- **Fix approach:** Split into `pushSync()` / `pullSync()` / `applyPulledPayload()` phases. Catch specific exception types at the phase level, not one monolithic handler.

### Duplicated fake repositories (app module vs domain module)

- **Issue:** Nearly identical fake implementations exist in two modules:
  - `app/src/test/java/.../fakes/FakeVocabularyRepository.kt`
  - `domain/src/test/java/.../fakes/FakeVocabularyRepository.kt`
  - Same pattern for `FakeReviewRepository` and `FakeSettingsRepository`.
- **Impact:** Changes to repository interfaces must be duplicated across both sets of fakes. The domain module already depends on the domain interfaces, and the app module fakes could reuse them.
- **Fix approach:** Move canonical fakes into the `domain/src/test/` module or a shared test fixture module (`test-utils`). The `app` module tests should depend on the domain fakes.

### `DomainPlaceholder.kt` — empty class

- **Issue:** `domain/src/main/java/com/nhimz/vocabmaster/domain/DomainPlaceholder.kt` contains only `class DomainPlaceholder`. This was likely a workaround for a Kotlin compile issue with an empty module and is no longer needed.
- **Impact:** Litter — unused code that confuses new developers.
- **Fix approach:** Verify the domain module compiles without it, then delete.

### Field name mismatch between Android client and backend schema

- **Issue:** The Android `UserSettingsDto` uses field `dailyGoalXp` but the backend `UserSettingsSchema` uses field `dailyGoalMinutes`. The sync router at `backend/app/routers/sync.py:37` maps `s.dailyGoalMinutes` to `settings.daily_goal_min`, while the DTO field from the Android client is named `dailyGoalXp`.
- **Impact:** On first sync, settings could map incorrectly, causing daily goal to be read from the wrong field.
- **Fix approach:** Align naming between `UserSettingsDto` (Android, `data/src/main/java/.../remote/SyncPayload.kt`), `UserSettingsSchema` (backend, `backend/app/schemas/sync.py`), and `UserSettings` (backend DB model, `backend/app/models/user.py`). Use the same field name throughout the pipeline.

## Known Bugs

### Review log telemetry data zeroed during sync

- **Symptoms:** The sync push path in `SyncManager.kt:105-119` maps local `ReviewLogEntity` records to `ReviewLogDto` with placeholder zero values: `elapsed_days = 0`, `scheduled_days = 0`, `stability = 0.0`, `difficulty = 0.0`, `state = 0`. This is acknowledged with `// TODO(SYNC-02, Phase 4)`. After sync, the server stores zeroed telemetry, and when pulled back by another device, the merged review logs contain only rating + timestamp — all FSRS telemetry is lost.
- **Trigger:** Any sync operation that pushes review logs to the server.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:110-118`
- **Workaround:** No workaround. Telemetry metadata is permanently lost once pushed.

### FSRS card `interval` field zeroed during sync

- **Symptoms:** In `SyncManager.kt:92`, `interval = 0` is hardcoded for every `VocabularyCardDto` pushed to the server, with `// TODO(SYNC-02, Phase 4): server contract for v3 card shape; interval removed from v8 schema.` The server stores `interval=0` for all cards, and the pull/merge path at `VocabDao.kt:286-332` never reads the `interval` field from the DTO (it rebuilds due from `stability`/`difficulty`). This is semantically correct per FSRS v8 but the zero value propagates through the wire.
- **Trigger:** Any sync push.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:92`
- **Workaround:** The interval isn't used by FSRS v8 scheduling on the server side, so this is purely a data pollution concern for any non-FSRS consumer of server data.

### Placement test fallback when vocabulary DB is empty throws 500

- **Symptoms:** In `backend/app/routers/placement.py:114-115` and line 263, if no vocabulary items exist in the DB, the endpoint raises `HTTPException(500, "Vocabulary DB is empty!")` — a 500 Internal Server Error for a condition that is really a 503 or should be handled at deploy time.
- **Trigger:** Server started without seeding vocabulary data via `seed_db.py`.
- **Files:** `backend/app/routers/placement.py:115`, `:264`

### `PlacementSession` foreign key constraint risk

- **Symptoms:** The `PlacementSession` model (`backend/app/models/placement_session.py`) references `user.id` but the `submit_answer` endpoint (`backend/app/routers/placement.py:146-151`) only queries the session for authenticated users. For unauthenticated users (`uid` is None), `session` stays `None` and `session.finished_at` check is skipped — but `responses_list` is still populated from `request.responses`. If an unauthenticated user's session somehow references a non-existent user, there's no FK validation at the application level (SQLite does not enforce FK by default).
- **Trigger:** Anonymous placement test flow with client-driven session state.
- **Workaround:** None needed for the happy path — the client manages its own responses.

## Security Considerations

### Firebase service-account JSON committed to repository

- **Risk:** `backend/firebase-service-account.json` is present in the git repository. This file contains the Firebase Admin SDK service account private key. Anyone with repo access can mint Firebase custom tokens, access Firebase Authentication, and potentially read/write Firebase project resources depending on the SA's IAM roles.
- **File:** `backend/firebase-service-account.json` (file exists — contents not read per rules)
- **Current mitigation:** The file is in `backend/.gitignore` but is tracked in git (already committed). Its existence in the repo at all is the vulnerability.
- **Recommendations:**
  1. Immediately rotate the service account key in Firebase Console.
  2. Remove the file from git history using `git filter-branch` or `bfg-repo-cleaner`.
  3. Use environment variables or a secrets manager (e.g., Google Secret Manager) instead.
  4. Add `firebase-service-account.json` to `.gitignore` at the repo root.

### Hardcoded Google OAuth Web Client ID in source

- **Risk:** `AuthManager.kt:48` contains a hardcoded `webClientId` string (`170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com`). While OAuth client IDs are not strictly secret (they are embedded in Android apps by necessity), any extracted ID can be used to attempt OAuth phishing against the registered OAuth consent screen.
- **File:** `data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt:48`
- **Current mitigation:** The ID is tied to the Firebase project's authorized redirect URIs, limiting misuse.
- **Recommendations:** Move to `BuildConfig` (build config field) so it can vary per build flavor (debug/release with different Firebase projects).

### `runBlocking` in OkHttp interceptor blocks the dispatcher thread

- **Risk:** `AuthInterceptor.kt:16` uses `runBlocking { authManager.getIdToken() }` inside an OkHttp `Interceptor`. OkHttp calls interceptors on its dispatcher thread pool. While `runBlocking` is tolerable in this context (background thread, token retrieval is fast), a slow network call in `getIdToken()` (e.g., token refresh triggering a network request) would block the dispatcher thread, potentially starving other HTTP calls in the pool.
- **File:** `data/src/main/java/com/nhimz/vocabmaster/data/remote/AuthInterceptor.kt:16`
- **Current mitigation:** `getIdToken(false)` does not force a token refresh, so it returns a cached token almost instantly.
- **Recommendations:** Document the assumption or switch to OkHttp's `java.util.concurrent.CompletableFuture`-based async interceptor API if `getIdToken` ever starts making network calls.

### Firebase Auth init errors silently swallowed in backend

- **Risk:** `backend/app/utils/firebase_auth.py:9-16` wraps Firebase Admin initialization in a `try/except` that logs the error and continues with `pass`. If the service-account file is missing or invalid, the app starts without authentication. The `verify_token` function will fail on every request when it tries to call `auth.verify_id_token(token)`.
- **File:** `backend/app/utils/firebase_auth.py:14-16`
- **Current mitigation:** The error is printed to stdout, but in production this is easily missed during deployment.
- **Recommendations:** Raise on initialization failure so the app fails fast and the deployment pipeline catches it. Alternatively, use a health check endpoint that verifies Firebase connectivity.

### Missing CORS and rate limiting on backend

- **Risk:** `backend/app/main.py` configures no CORS middleware and no rate limiting. Without CORS, any web origin could potentially call the API if the backend is ever exposed to the web (though currently it's a mobile app backend). Without rate limiting, the placement test endpoint is vulnerable to brute-force answer probing.
- **File:** `backend/app/main.py`
- **Recommendations:** Add `CORSMiddleware` with explicit allowed origins, and a rate limiter (e.g., `slowapi` for FastAPI) on sensitive endpoints like `/api/v1/placement/`.

## Performance Bottlenecks

### Gradient descent IRT estimator in synchronous HTTP handler

- **Problem:** The IRT engine's `estimate_theta()` (`backend/app/services/irt_engine.py:23-55`) runs gradient descent synchronously inside the FastAPI request handler (`backend/app/routers/placement.py`). Each answer submission triggers 5 Newton-Raphson iterations over all accumulated responses (up to 15 questions × N users). For a single user this is negligible, but under concurrent load it blocks the SQLAlchemy session thread pool.
- **Files:** `backend/app/services/irt_engine.py:23-55`, `backend/app/routers/placement.py:195`
- **Cause:** In-process synchronous computation on the main async thread pool.
- **Improvement path:** Offload theta estimation to a background thread via `run_in_executor`, or precompute IRT parameters in the seeding script and use a lookup table approach.

### No caching for quiz session data

- **Problem:** `LoadQuizSessionUseCase` (`domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCase.kt` — 255 lines) queries the database for curriculum structure (units, nodes, sessions, questions, FSRS cards) on every quiz start. For the REVIEW node type, it must query due cards across all sessions in a node. The curriculum data is static after seeding but is loaded fresh each time.
- **Files:** `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCase.kt`
- **Cause:** No in-memory caching layer between use cases and DAO.
- **Improvement path:** Add a lightweight in-memory cache (e.g., Caffeine) for the static curriculum structure (sections, units, nodes, sessions, questions). Invalidate only on curriculum re-seed.

### Room database accessed from multiple coroutine contexts without pooling

- **Problem:** Multiple repositories (`VocabularyRepositoryImpl`, `ReviewRepositoryImpl`, `SettingsRepositoryImpl`) each independently inject Room DAO instances. Room manages its own internal connection pool, but the DAO passthrough pattern means Room must serialize access from multiple coroutine dispatchers. This is generally fine for small datasets but could become a bottleneck as card counts grow.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt`, `data/src/main/java/com/nhimz/vocabmaster/data/repository/*`
- **Improvement path:** Monitor with Room's `setQueryCallback` in debug builds to detect long-running queries. Add indexes on frequently-queried columns (`fsrs_cards.due`, `fsrs_cards.state`, `questions.sessionId`).

## Fragile Areas

### `SyncManager` — single method orchestrating push + pull + merge

- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt`
- **Why fragile:** The entire sync lifecycle is a single `suspend fun sync(): Boolean` that reads local state, serializes it, pushes, pulls, merges settings, merges cards via `VocabDao.mergePulledCards`, and reconciles review logs. Any failure midway leaves the system in an inconsistent state — the server may have received the push but the pull fell over, or the local DB is partially merged. The function catches `Exception` broadly and returns `false`, giving the caller no diagnostic about which phase failed.
- **Test coverage:** `data/src/test/java/.../sync/SyncManagerTest.kt` (567 lines) exists but tests are integration-level, mocking Retrofit rather than the network boundary.
- **Safe modification:** Never modify `sync()` without adding a phase identifier to the log output and a specific exception type per phase. Consider extracting to `pushSync()`, `pullSync()`, `mergePulledSettings()`, `mergePulledCards()`, `mergePulledLogs()`.

### `VocabularyRepositoryImpl` — 644 lines, 30+ injected dependencies

- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt`
- **Why fragile:** This single class implements the entire `VocabularyRepository` interface for an app with structured curriculum (sections, units, nodes, sessions, questions), FSRS card management, quiz session loading, badge logic, streak calculation, audio URL management, and vocabulary loading from assets. It uses `@Suppress("LongMethod", "CyclomaticComplexMethod")` and mixes JSON asset parsing (`QuestionAssetItem`, `SessionAssetItem`, etc.) with DAO queries and domain model mapping.
- **Test coverage:** `data/src/test/java/.../repository/VocabularyRepositoryImplTest.kt` (331 lines) exists.
- **Safe modification:** Extract asset parsing into a dedicated `CurriculumAssetParser` class. Move quiz/question loading into a dedicated use case or service. Keep `VocabularyRepositoryImpl` as a thin facade over specialized services.

### `Scheduler.kt` — 516 lines of pure FSRS math

- **Files:** `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/Scheduler.kt`
- **Why fragile:** This is a literal port of py-fsrs 6.3.1 with 516 lines of deeply nested when-blocks (Learning / Review / Relearning states × 4 rating values). The state machine has 12+ distinct branching paths with subtle edge cases around `newStep` bounds, empty `learningSteps`/`relearningSteps`, and state transitions. An incorrect edge case here silently corrupts all scheduling data.
- **Test coverage:** `GoldenVectorTest.kt` (960 lines) provides py-fsrs parity coverage. `PyFsrsParityTest.kt` (960 lines) also exists. Coverage is robust.
- **Safe modification:** Always run `GoldenVectorTest.kt` against any change. Never modify the Scheduler without generating new golden vectors from py-fsrs 6.3.1.

### `QuizScreenContent.kt` — 916 lines of single-file composable UI

- **Files:** `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreenContent.kt`
- **Why fragile:** A single Composable file handles all quiz question types (multiple choice, listening, scrambled, matching, typing, FSRSTailFlashcard, introduction) plus the full quiz container layout. At 916 lines, it's difficult to reason about recomposition scope, state hoisting correctness, and accessibility.
- **Test coverage:** `app/src/test/java/.../screens/Plan0301ContainerContentTest.kt` exists but likely covers only a subset of states.
- **Safe modification:** Extract each question type card (already split into individual card files under `ui/components/quiz/`) into their own screenshot-testable composables. The container file should only orchestrate visibility and transitions.

## Scaling Limits

### Backend SQLite will bottleneck at modest concurrency

- **Current capacity:** SQLite with `check_same_thread=False` allows concurrent reads but serializes writes at the file-system level. FastAPI runs on uvicorn (async workers), so multiple requests can hit `db.commit()` simultaneously.
- **Limit:** ~10-50 concurrent write operations per second before WAL contention degrades latency noticeably. With a single-user or small-team deployment this is fine, but it will not scale beyond a few hundred daily active users.
- **Files:** `backend/app/database.py` — `engine = create_engine(settings.DATABASE_URL, connect_args=connect_args)` where `DATABASE_URL` defaults to `sqlite:///./vocab.db`
- **Scaling path:** Replace SQLite with PostgreSQL (or at least SQLite in WAL mode with connection pooling). The SQLAlchemy ORM makes this straightforward — swap the connection string.

## Dependencies at Risk

### `androidx.credentials` uses 1.5.0-rc01 (release candidate)

- **Risk:** `credentialsPlayServices = "1.5.0-rc01"` in `gradle/libs.versions.toml:5` is a release candidate version, not a stable release. RC APIs may change behavior in the stable release.
- **Impact:** Google Sign-In flow could break if the RC introduces incompatibilities with the Google Play Services version on the device.
- **Migration plan:** Monitor for stable release and update promptly. The current version is from 2024-08, so a stable version should be available.

### Detekt 1.23.6 — local JAR install

- **Risk:** Detekt CLI is installed as a raw JAR (`detekt-cli-1.23.6.zip`) committed to the repo root. This suggests manual tooling setup that may diverge from the Gradle plugin version (`1.23.6` in `libs.versions.toml:6`).
- **Files:** `detekt-cli-1.23.6.zip`, `detekt-cli/` directory at repo root.
- **Impact:** Inconsistent analysis results between local Detekt runs and CI/Gradle builds if the ZIP install and the Gradle plugin are configured differently.

## Missing Critical Features

### No offline-first sync conflict resolution strategy

- **Problem:** The sync protocol (`SyncManager.kt` + `sync.py`) uses last-modified-wins for cards and append-only for review logs. There is no mechanism to detect or resolve conflicts when the same card is reviewed on two offline devices before either syncs. The `VocabDao.mergePulledCards` D-03 invariant only prevents stale server data from overwriting newer local data, but it does not handle concurrent edits on two devices.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt`, `data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDao.kt:286-332`
- **Blocks:** Multi-device sync correctness. A user who reviews a card on their phone while offline, then reviews it on their tablet offline, will lose one device's review when the second device syncs.

### No analytics or crash reporting in production

- **Problem:** The app uses `LocalLogger` for debug logging only (guarded by `BuildConfig.DEBUG`). The only crash handler is `setupCrashHandler()` which logs to `LocalLogger` — never persisted to a remote service. There is no Firebase Crashlytics, Sentry, or any production telemetry.
- **Files:** `app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`, `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt:21-24`
- **Blocks:** Understanding production crash rates, ANRs, and user errors.

## Test Coverage Gaps

### No E2E or integration tests for the quiz flow

- **What's not tested:** The full user journey from HomeScreen → node/checkpoint selection → QuizScreen → answer submission → FSRS card update → ResultScreen is not covered by any automated test. The `NavGraphTest.kt` in `app/src/androidTest/` only verifies navigation routes exist.
- **Files:** `app/src/androidTest/java/com/nhimz/vocabmaster/navigation/NavGraphTest.kt`
- **Risk:** Regressions in state management across the quiz lifecycle (process death restoration, double-tap guard, session completion) are only covered by unit tests of `QuizViewModel`, not the integrated Compose UI.
- **Priority:** Medium

### Sync error handling under network degradation untested

- **What's not tested:** `SyncManagerTest.kt` tests the happy path and specific error responses but does not simulate partial failures (push succeeds / pull fails, timeout mid-sync, network drops during merge). The current `sync()` implementation returns `false` for all recoverable errors with no partial-state recovery.
- **Files:** `data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt`
- **Risk:** Users experiencing network degradation mid-sync get a silent `false` return and no indication of whether settings were partially saved on the server.
- **Priority:** Medium

### Debug panel integrity tests are not part of the CI test suite

- **What's not tested:** `DataIntegrityTests.kt` provides valuable in-app runtime assertions (stability floor, difficulty bounds, due/ordering, orphan review logs, backup roundtrip) but these are manual-only — they run from a debug UI screen, not from the Gradle test runner. There is no scheduled or CI-triggered execution.
- **Files:** `app/src/main/java/com/nhimz/vocabmaster/ui/screens/debug_components/DataIntegrityTests.kt`
- **Risk:** Data corruption in the field goes undetected until a user manually navigates to the debug panel and runs the tests.
- **Priority:** Low

---

*Concerns audit: 2026-07-22*
