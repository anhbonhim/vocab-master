# Codebase Concerns

**Analysis Date:** 2026-07-20

## Tech Debt

**[UI/Screen Complexity]:**
- Issue: Several UI screens are extremely large and monolithic, mixing UI state, business logic, layout, and sometimes data transformations.
- Files: 
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/HomeScreen.kt` (995 lines)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/DebugPanelScreen.kt` (677 lines)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt` (561 lines)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt` (486 lines)
- Impact: Hard to read, test, and maintain. Increases the likelihood of recomposition bugs and state mismanagement in Compose.
- Fix approach: Break down these monolithic screens into smaller, reusable UI components in `app/src/main/java/com/nhimz/vocabmaster/ui/components/`. Extract state management and business logic more cleanly into the respective ViewModels.

**[ViewModel Complexity]:**
- Issue: Some ViewModels are becoming god objects, handling too many responsibilities and states.
- Files: 
  - `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt` (647 lines)
- Impact: Difficult to unit test all paths, prone to bugs when modifying session state, and hard to understand the full flow of a quiz.
- Fix approach: Refactor `QuizViewModel` by extracting sub-state managers or specific logic (like answer validation, FSRS scheduling updates, or session progression) into separate domain use cases or helper classes.

## Known Bugs

**[Exception Swallowing]:**
- Symptoms: There are numerous `try/catch` blocks that seem to catch broad `Exception`s without proper logging, error state handling, or re-throwing, which can hide critical failures.
- Files: 
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/DebugPanelScreen.kt` (Multiple empty/swallowed catches)
  - `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt` (JSON parsing exceptions are caught and swallowed, defaulting to null/empty)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/util/FeedbackHelper.kt` (Multiple swallowed try/catch)
- Trigger: Malformed JSON data in assets or database, or unexpected UI interactions.
- Workaround: The app silently degrades (e.g., returning empty lists or nulls), but this makes debugging data issues very difficult. Fix by adding proper logging and error propagation.

## Security Considerations

**[Data Backup Rules]:**
- Risk: The app might be backing up sensitive user data or large database files unnecessarily, which could be a privacy concern or consume excessive quota.
- Files: `app/src/main/res/xml/data_extraction_rules.xml` (Contains a TODO: Use <include> and <exclude> to control what is backed up)
- Current mitigation: Default Android backup rules apply.
- Recommendations: Explicitly define which databases, preferences, or files should be excluded from auto-backup to ensure user privacy and reduce backup size.

## Performance Bottlenecks

**[Unsafe Forced Unwrapping & Casting]:**
- Problem: Widespread use of `!!` (not-null assertion) and raw `as` casts in UI code can lead to `NullPointerException`s and `ClassCastException`s, crashing the app instantly.
- Files: 
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/HomeScreen.kt` (Lots of `as` casts for payload data)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/FirstWinScreen.kt` (`!!` used on selected options)
  - `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt`
- Cause: Treating nullable types or generic any types as non-null/specific types without safe checks (`as?` or `?.let`).
- Improvement path: Replace `!!` with safe calls (`?.`) or Elvis operators (`?:`). Replace raw `as` with safe casts (`as?`) and handle the null case appropriately to prevent crashes.

## Fragile Areas

**[JSON Asset Parsing]:**
- Files: `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt`
- Why fragile: Heavy reliance on parsing complex JSON structures dynamically (e.g., options, scrambled words, grammar tips) from database string fields or assets, with fallback to empty states on failure.
- Safe modification: Ensure the asset JSON schemas are strictly validated before importing.
- Test coverage: Needs robust unit tests covering all edge cases of malformed JSON strings in the database or asset files to ensure the app doesn't break silently.

## Scaling Limits

**[Database Sync/Init]:**
- Current capacity: Currently handles initial asset load.
- Limit: As the `vocab_structured.json` asset grows (currently at least 41k lines based on `XXX` search), the initial database population and parsing logic might block or take significantly longer.
- Scaling path: Move to a more chunked or stream-based approach for initial database population, or consider downloading the latest database file rather than parsing a massive JSON asset on first boot.

## Test Coverage Gaps

**[Data Integrity]:**
- What's not tested: Anomalies found in `DataIntegrityTests.kt` (negative stability, difficulty out of bounds, negative intervals) suggest the FSRS algorithm implementation or data persistence might occasionally produce invalid states.
- Files: `app/src/main/java/com/nhimz/vocabmaster/ui/screens/debug_components/DataIntegrityTests.kt`, `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/FSRS.kt`
- Risk: The spaced repetition system might schedule cards incorrectly, breaking the core learning loop.
- Priority: High - Ensure robust unit tests for `FSRS.kt` bounds checking and state transitions.

---

*Concerns audit: 2026-07-20*
