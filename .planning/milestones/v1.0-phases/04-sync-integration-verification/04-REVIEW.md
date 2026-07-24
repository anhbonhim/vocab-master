---
phase: 04-sync-integration-verification
reviewed: 2026-07-22T08:30:00Z
depth: deep
files_reviewed: 7
files_reviewed_list:
  - data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt
  - data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDao.kt
  - data/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt
  - app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/SettingsViewModel.kt
  - app/src/main/java/com/nhimz/vocabmaster/ui/components/SnackbarMessage.kt
  - app/src/main/java/com/nhimz/vocabmaster/ui/components/DuoSnackbar.kt
  - data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt
findings:
  critical: 2
  warning: 3
  info: 3
  total: 8
status: issues_found
---

# Phase 04: Code Review Report (Deep)

**Reviewed:** 2026-07-22T08:30:00Z
**Depth:** deep
**Files Reviewed:** 7
**Status:** issues_found

## Summary

Phase 04 implements sync network resilience (SYNC-01) and time-based merge logic (SYNC-02). The production source code (SyncManager, VocabDao `mergePulledCards`, SettingsViewModel, SnackbarMessage, DuoSnackbar) is structurally sound and the core D-03 invariant (skip stale server payloads) is correctly implemented in VocabDao.

However, **the test file is non-functional**: it contains a Kotlin compile error (suspend-from-non-suspend call in FakeVocabDao) and test data that would throw `DateTimeParseException` at runtime (epoch millis strings passed where ISO_LOCAL_DATE_TIME is expected). These two issues together mean none of the 6 test methods can be executed — even after removing the `@Ignore` annotation — so the merge logic has **zero verified test coverage**.

Additionally, the SettingsViewModel lacks a concurrency guard on `triggerSync()`, allowing duplicate concurrent syncs. ApiClient ships with a hardcoded localhost URL and unconditional BODY-level HTTP logging, both of which prevent the sync feature from functioning outside dev and leak sensitive data in production builds.

## Narrative Findings (AI reviewer)

## Critical Issues

### CR-01: FakeVocabDao.getReviewLogsFlow calls suspend function from non-suspend context — compile error

**File:** `data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt:469-470`
**Issue:** `getReviewLogsFlow` is a non-`suspend` function (per VocabDao interface: `fun getReviewLogsFlow(cardId: String): Flow<List<ReviewLogEntity>>`), but its FakeVocabDao implementation at line 470 calls `getReviewLogs(cardId)` which is declared as `override suspend fun` at line 391. Kotlin forbids calling suspend functions from non-suspend contexts:

```
Suspend function 'getReviewLogs' should be called only from a coroutine or another suspend function
```

The `@Ignore` annotation on the class only skips test *execution* at JUnit runtime — the file must still compile during the `compileTestKotlin` Gradle task. This will fail the entire `:data:compileTestKotlin` task on any CI environment, blocking all tests in the `:data` test source set.

**Cross-file trace:** `VocabDao.getReviewLogsFlow` (VocabDao.kt:104) → non-suspend interface method → `FakeVocabDao.getReviewLogsFlow` (SyncManagerTest.kt:469) → calls `FakeVocabDao.getReviewLogs` (SyncManagerTest.kt:391) which is `suspend`.

**Fix:**
```kotlin
override fun getReviewLogsFlow(cardId: String): Flow<List<ReviewLogEntity>> =
    MutableStateFlow(reviewLogs.filter { it.cardId == cardId })
```

Inline the filter logic directly instead of delegating to the suspend function.

### CR-02: Test VocabularyCardDto uses epoch millis strings where ISO_LOCAL_DATE_TIME is required — runtime DateTimeParseException

**File:** `data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt:155-166,218-229`
**Issue:** The `testTimeBasedMerging_*` tests construct `VocabularyCardDto` with:
- `due = now.toString()` (e.g., `"1721630400000"`) at lines 157, 220
- `lastReview = 100L.toString()` (i.e., `"100"`) at line 164
- `lastReview = 300L.toString()` (i.e., `"300"`) at line 227

Both the production `VocabDao.mergePulledCards()` (VocabDao.kt:293) and the FakeVocabDao's `mergePulledCards()` (SyncManagerTest.kt:403) parse these using `DateTimeFormatter.ISO_LOCAL_DATE_TIME`, which expects strings like `"2024-01-15T10:30:00"`. Passing epoch millis strings will throw `java.time.format.DateTimeParseException` before the merge logic is reached.

**Cross-file type mismatch:** `VocabularyCardDto.due` is typed `String` with comment `// ISO String` (SyncPayload.kt:23) and `lastReview` is `String?` with comment `// ISO String` (SyncPayload.kt:30). The production push path in SyncManager.kt:88 correctly formats these as ISO strings via `Instant.ofEpochMilli(...).atOffset(ZoneOffset.UTC).format(formatter)`. But the test bypasses this formatting and passes raw numeric strings. Even if CR-01 is fixed, these tests will crash before reaching any merge assertion.

**Fix:**
```kotlin
import java.time.Instant
import java.time.LocalDateTime

val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

// In VocabularyCardDto construction, replace:
//   due = now.toString(),
//   lastReview = 100L.toString(),
// With:
due = Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC).format(formatter),
lastReview = Instant.ofEpochMilli(100L).atOffset(ZoneOffset.UTC).format(formatter),
```

Apply this pattern to all `VocabularyCardDto` constructions in the test file (lines 155-166 and 218-229).

## Warnings

### WR-01: triggerSync() has no concurrency guard — concurrent syncs possible

**File:** `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/SettingsViewModel.kt:103-126`
**Issue:** `triggerSync()` launches a new coroutine via `viewModelScope.launch {}` on every invocation. Although line 105 sets `isSyncing = true`, there is no early-return guard that checks this flag before launching. If the user taps the Retry action on the error snackbar (which directly calls `triggerSync()` at line 121) while a sync is still in-flight, multiple `SyncManager.sync()` coroutines run concurrently.

**Cross-file impact:** `SyncManager.sync()` reads `syncPrefs.getLong(LAST_SYNC_KEY, 0L)` at line 56 and writes it at line 181. Concurrent syncs will read the same `lastSync` value, push duplicate data, and the second write to `last_sync_timestamp` may overwrite with an incorrect value. Additionally, `vocabDao.mergePulledCards()` runs inside a `@Transaction` but concurrent merges from two pull responses can still interleave at the transaction boundary, leading to inconsistent card state.

**Fix:**
```kotlin
fun triggerSync() {
    if (_uiState.value.isSyncing) return
    viewModelScope.launch {
        _uiState.update { it.copy(isSyncing = true, syncSuccess = null, syncError = null) }
        // ... rest unchanged
    }
}
```

### WR-02: ApiClient hardcodes localhost BASE_URL — sync feature non-functional in non-dev builds

**File:** `data/src/main/java/com/nhimz/vocabmaster/data/remote/ApiClient.kt:17`
**Issue:** `BASE_URL` is hardcoded to `"http://127.0.0.1:8000/"`. This URL only resolves when a local dev server is running on the Android device or emulator. In release builds, staging environments, or any real device without a local server, every sync API call will fail with a connection refused error. The `useLocalDevServer` setting in `SettingsRepository` (used elsewhere, e.g., `CDNAudioPlayer.kt:93`) is not consulted by `ApiClient`, making the setting inconsistent.

**Security sub-issue (same file, line 26-28):** `HttpLoggingInterceptor.Level.BODY` is unconditionally enabled. This logs full request/response bodies — including user settings, FSRS card data, review logs, and potentially auth tokens from `AuthInterceptor` — to Logcat in all build variants. On rooted devices or via ADB, any process can read Logcat output.

**Fix:**
```kotlin
private val BASE_URL = BuildConfig.API_BASE_URL  // Set per build variant in build.gradle.kts

.addInterceptor(HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
})
```

Note: This issue predates Phase 04 — the only Phase 04 change to ApiClient was making `class` and `syncApi` `open`. Flagged here because the entire sync feature depends on this class routing to a real server.

### WR-03: SnackbarMessage is data class with lambda field — breaks equals/hashCode

**File:** `app/src/main/java/com/nhimz/vocabmaster/ui/components/SnackbarMessage.kt:25-36`
**Issue:** `SnackbarMessage` is a `data class` with `val action: (() -> Unit)? = null` as a constructor property. Kotlin data class auto-generates `equals()`, `hashCode()`, `copy()`, and `toString()` using ALL constructor properties including the lambda. Lambda instances are compared by reference identity — two structurally identical `SnackbarMessage` instances with different lambda instances (even if functionally equivalent, e.g., two separate `{ triggerSync() }` captures) will never be `equals()`.

**Cross-file impact:** The `SettingsViewModel.triggerSync()` at line 116-122 creates a new `SnackbarMessage` with a new `{ triggerSync() }` lambda on every failure. If any downstream consumer (current or future) uses `distinctUntilChanged()`, `Set<SnackbarMessage>`, or any equality-based deduplication on the `snackbarMessages` SharedFlow, deduplication will silently fail.

**Fix:** Move `action` out of the primary constructor so it's excluded from the auto-generated `equals`/`hashCode`:
```kotlin
data class SnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val isError: Boolean = false,
) {
    var action: (() -> Unit)? = null
}
```
Or use a regular class with manual `equals`/`hashCode` for the non-lambda fields.

## Info

### IN-01: DuoSnackbarPalette is declared but never referenced

**File:** `app/src/main/java/com/nhimz/vocabmaster/ui/components/DuoSnackbar.kt:82-86`
**Issue:** The `DuoSnackbarPalette` object exposes `ErrorContainerLight`, `ErrorContainerDark`, and `ErrorAccent` colors re-exported from the theme package. However, no file in the codebase references `DuoSnackbarPalette`. The `DuoSnackbarHost` composable at line 34-49 doesn't use these colors — it always uses `MaterialTheme.colorScheme.inverseSurface` regardless of the `isError` flag on `SnackbarMessage`. This is dead code that also reveals an incomplete feature: error snackbars were intended to have a distinct red color but the rendering logic was never connected.
**Fix:** Either wire `isError` into the composable to use the error palette, or remove `DuoSnackbarPalette` and the unused `ErrorRedLight`/`ErrorRedLightDark` imports.

### IN-02: SyncManager uses System.currentTimeMillis() directly — prevents deterministic testing

**File:** `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt:99`
**Issue:** `lastModified = System.currentTimeMillis()` hard-couples the push timestamp to the system clock. This prevents writing deterministic tests for the time-based merge logic — the FakeVocabDao tests work around this by using raw epoch millis in the DTO, but that workaround itself is broken (see CR-02). Injecting a `java.time.Clock` or `() -> Long` provider would enable fully deterministic merge tests.
**Fix:** Add `private val clock: Clock = Clock.systemUTC()` as a constructor parameter (with default) and use `clock.millis()`.

### IN-03: SnackbarMessage.isError field is set but never consumed in rendering

**File:** `app/src/main/java/com/nhimz/vocabmaster/ui/components/SnackbarMessage.kt:29` → `app/src/main/java/com/nhimz/vocabmaster/ui/components/DuoSnackbar.kt:60-76`
**Issue:** `SettingsViewModel.triggerSync()` sets `isError = true` on error snackbar messages (line 120 of SettingsViewModel.kt). However, the `showSnackbar` extension function (DuoSnackbar.kt:60-76) never reads `message.isError`, and the `DuoSnackbarHost` composable always renders with `inverseSurface` colors. Error and success snackbars look visually identical. This contradicts the SnackbarMessage KDoc which says "UI có thể dùng cờ này để đổi màu nền nếu cần."
**Fix:** Either remove `isError` if the uniform style is intentional, or pass `isError` through the snackbar data and conditionally apply the `DuoSnackbarPalette.ErrorContainerLight`/`ErrorContainerDark` colors in `DuoSnackbarHost`.

---

_Reviewed: 2026-07-22T08:30:00Z_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: deep_
