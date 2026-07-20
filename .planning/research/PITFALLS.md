# Domain Pitfalls

**Domain:** Spaced Repetition Vocabulary App
**Researched:** 2026-07-20

## Critical Pitfalls

Mistakes that cause rewrites or major issues.

### Pitfall 1: Swallowing Exceptions During JSON/Asset Parsing
**What goes wrong:** Malformed JSON data or unexpected edge cases fail silently, resulting in empty lists or null data being presented to the user.
**Why it happens:** Broad `try { ... } catch (e: Exception) { // do nothing }` blocks hide underlying data integrity issues.
**Consequences:** The app appears to work but fails to load vocabulary, breaking the learning loop without providing diagnostic information to developers.
**Prevention:** Catch specific exceptions (e.g., `SerializationException`). If catching generic exceptions, use `runCatching` and propagate errors to the UI state (`UiState.Error`) or log them prominently.
**Detection:** Empty lists in UI when data should be present, or `Logcat` showing no errors despite missing data.

### Pitfall 2: Unsafe Casts and Forced Unwrapping in Compose UI
**What goes wrong:** App crashes with `ClassCastException` or `NullPointerException`.
**Why it happens:** Developers use `as` or `!!` to bypass compiler checks, often when casting payload data from generic navigation routes or generic state containers.
**Consequences:** Instant app crash, severe degradation of user experience.
**Prevention:** Rely on Kotlin 2.3's K2 compiler smart casting (`is` checks). When explicit casting is necessary, use the safe cast operator `as?` paired with the Elvis operator `?:` to provide a safe fallback or trigger an error UI state.
**Detection:** Crashlytics/Logcat showing `ClassCastException` or `NullPointerException`.

### Pitfall 3: Monolithic Screen Composables
**What goes wrong:** `HomeScreen`, `QuizScreen`, and `SettingsScreen` are too large (500-1000 lines), mixing UI layout, business logic, and state management.
**Why it happens:** Rapid iteration without extracting reusable components or adhering to Unidirectional Data Flow (UDF).
**Consequences:** Impossible to preview in Android Studio, extremely difficult to test, and highly prone to unnecessary recomposition bugs (UI jank).
**Prevention:** Split screens into a top-level "Screen" composable (handles `ViewModel` and UDF state/events) and a "Content" composable (stateless, pure rendering). Hoist state appropriately using `StateFlow`.
**Detection:** Long scrolling files, sluggish UI performance, Android Studio preview timeouts.

## Moderate Pitfalls

### Pitfall 1: Leaking Sensitive Data via Android Backup
**What goes wrong:** Learning history (Room DB) and user preferences are automatically backed up to Google Drive.
**Prevention:** Implement `android:dataExtractionRules` in Android 12+ (and `fullBackupContent` for older versions) pointing to an XML file that explicitly excludes databases and shared preferences from `<cloud-backup>` and `<device-transfer>`.

### Pitfall 2: DAO Threading Violations
**What goes wrong:** Calling Room DAO methods on the main thread causes an `IllegalStateException`.
**Prevention:** Ensure all DAO write operations and one-shot reads are marked as `suspend` functions. Return `Flow` for reactive reads. Use `@Transaction` for multi-statement operations.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| FSRS Algorithm Audit | Corrupting existing user scheduling data during fixes. | Write extensive unit tests for `FSRS.kt` bounds checking before altering the algorithm. Ensure database migrations don't lose data. |
| Compose Refactor | Breaking navigation arguments when splitting screens. | Clearly define the UI State data classes and use type-safe Navigation Compose 3.0 APIs. |

## Sources

- [Android Developer Guidelines (Compose UDF, Room, Backup Rules)]
- [Kotlin 2.3 Language Specification (Smart Casts)]