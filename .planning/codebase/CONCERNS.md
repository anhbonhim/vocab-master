# Codebase Concerns

**Analysis Date:** 2026-07-22

## Tech Debt

### Destructive Database Migrations

- **Issue:** Room database version 8 uses `fallbackToDestructiveMigration(dropAllTables = true)` (`data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt:69`). Every schema version bump wipes all user data — FSRS scheduling state, review logs, XP, streaks — and re-seeds curriculum from assets. No migration paths exist.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt:69`, `data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDatabase.kt:43`
- **Impact:** Any database upgrade (even adding a new table/column) destroys months of user progress. Users who update the app lose all spaced-repetition data.
- **Fix approach:** Implement proper Room migrations (`Migration` objects with `ALTER TABLE`/`CREATE TABLE` for each version bump). Remove `fallbackToDestructiveMigration`. Enable `exportSchema = true` in `VocabDatabase.kt` to generate migration history.

### Detekt Baseline With 600+ Suppressed Issues

- **Issue:** `config/detekt/baseline.xml` contains 631 lines of suppressed detekt findings. The baseline was generated to pass CI and has not been systematically reduced. Analysis rules are actively suppressed at the file level via `@Suppress` annotations (13 `@Suppress` annotations across 10 files).
- **Files:** `config/detekt/baseline.xml`, various `@Suppress` annotations in source files
- **Impact:** Real issues are buried under the suppression pile. New code has no automated quality guard because detekt effectively passes everything.
- **Fix approach:** Audit the baseline quarterly. Remove suppressions gradually, fix issues, and re-baseline with a smaller set.

### Hardcoded Google Web Client ID

- **Issue:** `AuthManager.kt:48` contains a hardcoded Google OAuth web client ID (`170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com`). If this value is rotated (e.g., credential leak), the entire auth system breaks and requires a code release to fix.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt:48`
- **Impact:** Security rotation impossible without app update. Client secret exposure risk.
- **Fix approach:** Move to `google-services.json` (already configured via `libs.plugins.google.services`) or a server-driven configuration endpoint.

### V6 Settings Migration in Application.onCreate

- **Issue:** `VocabApplication.kt:26-35` runs a migration check in `onCreate()` using raw `SharedPreferences` key `db_version` to detect if a V6 settings wipe is needed. This is fragile — if the SharedPreferences file is cleared (e.g., app data reset), the migration re-runs on a fresh install and resets settings for no reason.
- **Files:** `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt:26-35`
- **Impact:** Race condition risk (launched in IO dispatcher but not awaited), unnecessary data loss on fresh installs.
- **Fix approach:** Use Room's callback-based migration instead. Remove the `VocabApplication` migration hack after the next database version bump.

### Duplicate Fake Test Implementations

- **Issue:** Both `app/src/test/` and `domain/src/test/` maintain separate, identical copies of `FakeVocabularyRepository.kt`, `FakeSettingsRepository.kt`, and `FakeReviewRepository.kt`. These are out of sync — each duplicates ~30-90 lines of stubs with `TODO("not needed for these tests")`.
- **Files:**
  - `app/src/test/java/com/nhimz/vocabmaster/ui/viewmodel/fakes/FakeVocabularyRepository.kt`
  - `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeVocabularyRepository.kt`
  - `app/src/test/java/com/nhimz/vocabmaster/ui/viewmodel/fakes/FakeSettingsRepository.kt`
  - `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeSettingsRepository.kt`
  - `app/src/test/java/com/nhimz/vocabmaster/ui/viewmodel/fakes/FakeReviewRepository.kt`
  - `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeReviewRepository.kt`
- **Impact:** When the repository interface grows (e.g., adding a new method), both fakes must be updated manually. Tests silently get `NotImplementedError` at runtime for unimplemented methods.
- **Fix approach:** Extract a shared test-fixtures module or use a mocking library (e.g., MockK) instead of hand-rolled fakes.

### Plain HTTP API Communication

- **Issue:** `ApiClient.kt:17` hardcodes `BASE_URL = "http://127.0.0.1:8000/"` (plain HTTP). `AndroidManifest.xml:18` enables `android:usesCleartextTraffic="true"` to allow this. The OkHttp logging interceptor is set to `Level.BODY` (`ApiClient.kt:27`), which logs full request/response bodies including auth tokens and user data.
- **Files:**
  - `data/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt:17,27`
  - `app/src/main/AndroidManifest.xml:18`
- **Impact:** All network traffic is unencrypted — auth tokens, FSRS card data, user settings sent in cleartext on the local network. Body-level logging leaks sensitive data in debug builds and could accidentally remain in release builds.
- **Fix approach:** Support HTTPS in production (server-side change). Make `BASE_URL` configurable via BuildConfig. Set logging level to `Headers` or `NONE` in release builds. Remove `usesCleartextTraffic` from manifest for release variant.

### SyncManager Monolith With Placeholder Data

- **Issue:** `SyncManager.kt` is a 213-line monolithic method that handles push, pull, merge, and conflict resolution in a single function. It sends placeholder values for sync payload fields `interval = 0`, `elapsed_days = 0`, `scheduled_days = 0`, `stability = 0.0`, `difficulty = 0.0`, `state = 0` as documented in TODO(SYNC-02, Phase 4). Missing server contract for v3 card/log shapes.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt`
- **Impact:** Server receives meaningless placeholder values for critical FSRS fields. Sync does NOT actually synchronize FSRS state — it pushes and pulls card data but the FSRS scheduling math happens independently on each client. Duplicate review logs can be created on pull.
- **Fix approach:** Refactor into separate `SyncPushUseCase` and `SyncPullUseCase`. Finalize the v3 server contract. Remove placeholder values. Add deduplication logic for review log merging.

### runBlocking in AuthInterceptor

- **Issue:** `AuthInterceptor.kt:16` uses `runBlocking { authManager.getIdToken() }` inside an OkHttp `Interceptor.intercept()`. While OkHttp runs interceptors on background threads, `runBlocking` still blocks that OkHttp thread until the Firebase token refresh completes (which could take hundreds of milliseconds on a slow network).
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthInterceptor.kt:16`
- **Impact:** Under network congestion, UI-visible delays occur because OkHttp's dispatcher thread pool is occupied. Blocking can cause ANR-like symptoms if multiple requests queue up.
- **Fix approach:** Cache the ID token with a refresh window (e.g., refresh 5 min before expiry) to avoid blocking on every request. Use `NonCancellable` coroutine context.

### Large Composable Screen Files

- **Issue:** Several screen content files exceed healthy component boundaries:
  - `QuizScreenContent.kt` — 916 lines
  - `HomeScreenContent.kt` — 892 lines
  - `SettingsScreenContent.kt` — 675 lines
  - `ResultScreenContent.kt` — 420 lines
  - `FirstWinScreen.kt` — 471 lines
- **Files:**
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreenContent.kt`
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/HomeScreenContent.kt`
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreenContent.kt`
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/ResultScreenContent.kt`
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/FirstWinScreen.kt`
- **Impact:** Difficult to test individual sections. High risk of merge conflicts. Poor reusability — small UI changes require touching massive files.
- **Fix approach:** Extract reusable composable components into `ui/components/`. Split screens by logical section (e.g., QuizContent → QuizQuestionArea, QuizProgressBar, QuizFeedback).

### `@Suppress` Abuse on Complex Methods

- **Issue:** 13 `@Suppress` annotations suppress 30+ distinct detekt warnings across 10 files. Suppressed rules include `LongMethod`, `CyclomaticComplexMethod`, `NestedBlockDepth`, `TooManyFunctions`, `MagicNumber`, `ComplexCondition`, `TooGenericExceptionCaught`, `VariableNaming`, `SwallowedException`, and `LabeledExpression`.
- **Files:** `SyncManager.kt`, `VocabularyRepositoryImpl.kt`, `Scheduler.kt`, `Optimizer.kt`, `BackupRepositoryImpl.kt`, `Card.kt`, `ReviewLog.kt`, `FeedbackHelper.kt`, `DataIntegrityTests.kt`, `VocabularyRepositoryImplTest.kt`
- **Impact:** Structural complexity is normalized. Refactoring is harder because there's no lint pressure to simplify.
- **Fix approach:** Remove `@Suppress` annotations one file at a time, refactor to reduce complexity, then re-enable checks.

### FSRS Placeholder Data Values

- **Issue:** `ReviewLogDto` (`data/src/main/java/com/nhimz/vocabmaster/data/remote/SyncPayload.kt`) sends `elapsed_days = 0`, `scheduled_days = 0`, `stability = 0.0`, `difficulty = 0.0`, `state = 0` as hardcoded placeholders in `SyncManager.kt:103-106`. These are real FSRS fields that are being zeroed out because the local schema no longer stores them (v8 removed interval, v3 removed telemetry fields).
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/remote/SyncPayload.kt:35-44`, `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:101-108`
- **Impact:** Server-side FSRS analytics are corrupted by zero-valued telemetry. Cannot compute average stability or track review history accuracy server-side.
- **Fix approach:** Remove telemetry fields from the DTO entirely (breaking API change) or recalculate them from local FSRS scheduler state before sync.

## Known Bugs

### Review Log Deduplication Gap on Sync Pull

- **Symptoms:** `SyncManager.kt:185-186` checks `existingLogs.any { it.reviewDatetime == logTime }` to avoid duplicate review log insertion on pull. However, review logs can legitimately share the same timestamp (e.g., batch reviews processed at the same millisecond), causing false deduplication and dropped data.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:185-186`
- **Trigger:** Pull sync when two review logs were created within the same millisecond.
- **Workaround:** None — the log is silently dropped.

### Node Progress Marker Node Locking (Ordering)

- **Symptoms:** `MainViewModel.kt:219` locks nodes based on `isPreviousNodeCompleted` using sequential ordering from `vocabularyRepository.getNodesByUnit(unit.id).first()`. If nodes are reordered or types change (e.g., REVIEW nodes inserted after a unit is marked complete), the lock state becomes incorrect.
- **Files:** `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/MainViewModel.kt:216-230`
- **Trigger:** Adding or reordering nodes in the curriculum asset.
- **Workaround:** Manually clear node_progress via the debug panel.

### Section Completion Calculation Overcounting

- **Symptoms:** `MainViewModel.kt:269-274` computes `isSectionCompleted = totalNodesInSection > 0 && completedNodesCount == totalNodesInSection`. If a section has no nodes (edge case), `totalNodesInSection` is 0, so the section is never marked complete. Also, this counts synthetic REVIEW nodes in `nodes.size` via `getNodesByUnit`, overcounting.
- **Files:** `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/MainViewModel.kt:269-274`
- **Trigger:** Section with only a node newly seeded but no node_progress entries.
- **Workaround:** None — the section remains permanently uncompleted.

## Security Considerations

### Cleartext Traffic in Production Build

- **Risk:** `AndroidManifest.xml:18` has `android:usesCleartextTraffic="true"` globally. The API base URL `http://127.0.0.1:8000/` is unencrypted HTTP. Any device on the local network can MITM traffic.
- **Files:** `app/src/main/AndroidManifest.xml:18`, `data/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt:17`
- **Current mitigation:** None. Cleartext is unconditionally allowed.
- **Recommendations:** Use a `network_security_config.xml` that only allows cleartext for `127.0.0.1` during debug. Use HTTPS for production API endpoints. Remove `usesCleartextTraffic` from release manifest.

### Sensitive Data in Logs

- **Risk:** `ApiClient.kt:27` enables `HttpLoggingInterceptor.Level.BODY` which logs full request/response bodies including Firebase ID tokens (Bearer auth), card content, and user settings. In debug builds, this is visible in logcat which any app with `READ_LOGS` permission can read.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt:27`
- **Current mitigation:** `BuildConfig.DEBUG` is not checked — the interceptor runs at BODY level regardless.
- **Recommendations:** Gate `HttpLoggingInterceptor.Level.BODY` behind `BuildConfig.DEBUG`. Use `Headers` level as default.

### Hardcoded Firebase Auth Web Client ID

- **Risk:** The Google OAuth web client ID in `AuthManager.kt:48` is hardcoded in source. If rotated or leaked, a fake app can impersonate the real client ID.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt:48`
- **Current mitigation:** The ID is embedded in the open-source APK. Anyone can extract it via `apkanalyzer`.
- **Recommendations:** Move to `google-services.json` resource-based configuration. Implement app verification via Play Integrity API.

### Database Schema Exposed via `exportSchema = false`

- **Risk:** `VocabDatabase.kt:43` sets `exportSchema = false`, preventing Room from generating schema JSON files. Without schema export, migration history cannot be tracked in source control, and future migration authors have no reference for what changed.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDatabase.kt:43`
- **Current mitigation:** None — destructive migrations make schema history moot.
- **Recommendations:** Set `exportSchema = true` and add the schema output directory to version control. This is a prerequisite for non-destructive migrations.

## Performance Bottlenecks

### SyncManager Loads All Cards Into Memory

- **Problem:** `SyncManager.kt:75` calls `vocabDao.getAllCards()` which loads ALL FSRS card entities into memory at once. The number can grow to thousands of cards over extended use. The entire method holds a single long transaction.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:75,95,142,179`
- **Cause:** No pagination or streaming. The v1 sync implementation prioritizes correctness over memory efficiency.
- **Improvement path:** Implement cursor-based pagination for card/log queries. Use `PagingSource` or chunked queries.

### Curriculum Seeding Loads Entire JSON Into Memory

- **Problem:** `VocabularyRepositoryImpl.kt:184` calls `reader.readText()` on the entire `lessons_v3.json` asset file, which could be several megabytes. The entire seed operation blocks `Dispatchers.IO` and holds the mutex (`prepopulateCurriculumMutex`) for the duration.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt:182-184`
- **Cause:** The JSON decoder requires a complete string to parse. Asset file can grow as the curriculum expands.
- **Improvement path:** Stream-decode the top-level array using `Json.decodeToSequence` or chunk the asset into per-section files.

### `ensureCurriculumAndFsrsSeeded()` Called on Every Data Access

- **Problem:** Nearly every method in `VocabularyRepositoryImpl` starts by calling `ensureCurriculumAndFsrsSeeded()`, which checks `getQuestionCount() > 0` every time. While the mutex prevents re-seeding, the `getQuestionCount()` call still hits the DB on every flow collection.
- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt` (22 call sites across the file)
- **Cause:** Lazy-seeding pattern chosen over application-init or DataStore-backed flag.
- **Improvement path:** Seed once at application startup in `VocabApplication.onCreate()` or use a `StateFlow<Boolean>` to track seeding state.

## Fragile Areas

### FSRS Scheduler — Math Precision and Parity

- **Files:**
  - `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/Scheduler.kt` (516 lines)
  - `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/Optimizer.kt` (512 lines)
- **Why fragile:** Direct port of py-fsrs v6.3.1 floating-point math. Any deviation in rounding (`round()` in `nextInterval`) or expression evaluation causes schedule divergence from the reference Python implementation. Bankser's rounding (`round-half-to-even`) matches Python's `round()` but differs from Kotlin's default `roundToInt()` (ties-to-away).
- **Safe modification:** All scheduler changes must be validated against the `golden_vectors.json` test suite (`domain/src/test/resources/fsrs/golden_vectors.json`).
- **Test coverage:** Well-covered — `PyFsrsParityTest.kt` (960 lines), `GoldenVectorTest.kt`, `OptimizerTest.kt`.

### VocabularyRepositoryImpl Fallback Chain

- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt` (644 lines)
- **Why fragile:** The 3-tier fallback chain (`getDueCardsScoped`: unit → section → global) has non-obvious behavior. Several methods (`getCardsByLevel`, `getCardsByTopic`, `getNewCardsByTopicAndLevels`, `getNewCardsByLevels`) all fall back to the same `getDueAndNewCardsByTopicFallback` query with a fixed limit of 100, ignoring topic/level filtering entirely.
- **Safe modification:** Adding new query parameters must update ALL fallback paths. Removing unused interface methods (`getCompletedLessons`, `markLessonCompleted` returning empty/stub) should precede refactoring.
- **Test coverage:** `VocabularyRepositoryImplTest.kt` (331 lines) covers some paths but not the fallback chain.

### VocabDao Query Comments Exposing Schema Ambiguity

- **Files:** `data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDao.kt:27-35`
- **Why fragile:** The DAO file contains extensive inline commentary about missing `topic` and `difficultyLevel` columns on the `questions` table, noting that "questions table doesn't have topic or difficultyLevel!" and "I will remove getCardsByLevel, getCardsByTopic..." but these methods still exist in the interface as fallbacks that return unfiltered results. A future developer might be confused about which queries are authoritative.
- **Safe modification:** Remove dead fallback queries (`getDueAndNewCardsByTopicFallback`) and the interface methods that use them. Consolidate ownership of topic/level filtering in the service layer.

## Scaling Limits

### Local Learning Steps Array for All Cards

- **Current capacity:** `Scheduler.kt:31-33` uses fixed `longArrayOf(60_000L, 600_000L)` for learning steps and `longArrayOf(600_000L)` for relearning steps for ALL cards. These are hardcoded, not per-card.
- **Limit:** Cannot provide differentiated learning intervals per card or per user. All FSRS cards share the same scheduling parameters at the `Scheduler` class level.
- **Scaling path:** Store `learningSteps` and `relearningSteps` as per-user preferences (in DataStore) or per-card metadata.

### Single-Page Quiz Session for Large Question Sets

- **Current capacity:** `LoadQuizSessionUseCase` loads all questions for a quiz session into memory as `List<QuizQuestion>`. For a review session with hundreds of due cards, the entire list is held in ViewModel state.
- **Limit:** Session size grows linearly with accumulated due cards. Onboarding or curriculum-wide review sessions could exceed memory budgets.
- **Scaling path:** Implement paginated question loading with a sliding window of 10-20 questions ahead of the current index.

## Dependencies at Risk

### Navigation 3 (alpha-stage library)

- **Risk:** The project uses `androidx.navigation3.ui`, `androidx.navigation3.runtime`, and `androidx.lifecycle.viewmodel.navigation3` (version 1.0.1-alpha). Navigation 3 is an alpha-stage Jetpack library with an unstable API surface. Breaking changes are expected before stable release.
- **Impact:** API breakage on upgrade. Migration path to stable Navigation Compose is unclear. The project's entire navigation architecture (backStack, NavDisplay, entryProvider) is coupled to this alpha API.
- **Migration plan:** Consider migrating to standard Jetpack Navigation Compose (which is stable) if Navigation 3 is deprecated or significantly restructured.

### Robolectric (test dependency)

- **Risk:** `data/build.gradle.kts:77` pins `org.robolectric:robolectric:4.15.1`. Robolectric releases often break on new Android SDK levels. Upgrading `compileSdk` or `targetSdk` past 36 may require a newer Robolectric version.
- **Impact:** Database tests (`VocabDaoTest.kt`, `VocabularyRepositoryImplTest.kt`) will fail until Robolectric catches up with the SDK.
- **Mitigation:** Keep Robolectric version pinned and test the SDK upgrade in a branch first.

## Missing Critical Features

### No Offline Sync Conflict Resolution

- **Problem:** The SyncManager (`data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt`) implements "last-writer-wins" — the server's data completely overwrites local state on pull. There is no three-way merge, no timestamp-based conflict detection, and no user-facing conflict resolution UI.
- **Blocks:** Multiple-device users will lose progress when syncing from different devices if review sessions overlap. The server copy can overwrite local FSRS state that hasn't been pushed yet.

### No Data Export/Portability (Beyond Debug)

- **Problem:** Backup/restore (`BackupRepositoryImpl.kt`) exists for internal app restore but there is no user-facing data export (CSV, Anki-compatible APKG, etc.).
- **Blocks:** Users cannot migrate their spaced-repetition data to another app or create backups for archival purposes outside the app's restore mechanism.

### No Rate Limiting or Backpressure on Sync

- **Problem:** Manual sync calls (triggered from settings/debug) fire immediately regardless of network state. No exponential backoff, no queue, no retry limit.
- **Blocks:** A flaky server connection causes repeated full database scans (getAllCards, getAllReviewLogsList) which drain battery and block the IO dispatcher.

## Test Coverage Gaps

### Untested Source Files (No Associated Test)

The following modules have zero test coverage:

- `app/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt` — 107 lines
- `app/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt` — 50 lines
- `app/src/main/java/com/nhimz/vocabmaster/data/remote/AuthInterceptor.kt` — 28 lines
- `app/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt` — 213 lines
- `app/src/main/java/com/nhimz/vocabmaster/data/repository/SettingsRepositoryImpl.kt` — 330 lines
- `app/src/main/java/com/nhimz/vocabmaster/data/repository/BackupRepositoryImpl.kt` — 199 lines
- `app/src/main/java/com/nhimz/vocabmaster/notification/*.kt` — 167 lines total
- `app/src/main/java/com/nhimz/vocabmaster/audio/CDNAudioPlayer.kt` — 140 lines
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/MainViewModel.kt` — 428 lines (core navigation + curriculum logic)
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/SettingsViewModel.kt` — missing test
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/PlacementTestViewModel.kt` — missing test
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/StatisticsViewModel.kt` — missing test
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/LoginViewModel.kt` — missing test

**Risk:** These untested files contain critical logic: authentication (AuthManager, AuthInterceptor), data persistence (SyncManager), database operations (BackupRepositoryImpl), navigation (MainViewModel), and state management (all ViewModels). Regressions in these areas would affect core app functionality and may go unnoticed.

**Priority sources:**
1. **HIGH** — `SyncManager.kt` (sync logic, I/O, placeholder data), `MainViewModel.kt` (navigation, curriculum unlock logic)
2. **HIGH** — `AuthManager.kt`, `AuthInterceptor.kt` (security-sensitive, Firebase integration)
3. **MEDIUM** — `BackupRepositoryImpl.kt` (data loss risk), `SettingsRepositoryImpl.kt` (persistence layer)
4. **MEDIUM** — `SettingsViewModel.kt`, `PlacementTestViewModel.kt`, `LoginViewModel.kt`, `StatisticsViewModel.kt` (UI state)

---

*Concerns audit: 2026-07-22*
