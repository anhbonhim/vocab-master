# Phase 1: Security & Database Stabilization
> Nyquist Validation Strategy & Implementation Map

## Phase Objectives

1. **Security (PERS-01)**: Prevent accidental exposure of user data via ADB backup.
2. **Architecture (PERS-02, PERS-03)**: Guarantee non-blocking, atomic database operations.
3. **Logic (FSRS-01..05)**: Mathematically verify FSRS v6 scheduler implementation against canonical reference outputs, ensuring identical repetition intervals and states.

## Validation Dimensions

### Dimension 1: Feature Complete (UAT)
- **Goal:** All stated requirements are satisfied.
- **Implementation Map:**
  - `PERS-01`: Android manifest modification in Plan 04.
  - `PERS-02`, `PERS-03`: Room DAO modifications in Plan 04; Verified via Robolectric test in Plan 06.
  - `FSRS-01`..`FSRS-05`: PyFsrs parity tests in Plan 02, Optimizer tests in Plan 03.
- **Testing:** Automated unit tests covering both the algorithms and the DAO wrapper.

### Dimension 2: Security & Privacy
- **Goal:** User databases must not be extractable.
- **Implementation Map:**
  - `data_extraction_rules.xml` and `backup_rules.xml` specifically exclude `vocab_database`, `-wal`, `-shm` and `sharedpref`.
- **Testing:** Add assertions in `VocabDatabaseSmokeTest` (Plan 04) to load the rules XML and assert the exclusion of `vocab_database`.

### Dimension 3: Resilience & Edge Cases
- **Goal:** Negative stability or interval values never crash the app.
- **Implementation Map:**
  - Strict nullability/bounds checking inside FsrsV6.
- **Testing:** Boundary conditions supplied by Golden Vector generation script (Plan 01), consumed by unit tests (Plan 01, Plan 02).

### Dimension 4: State Consistency
- **Goal:** The database must never contain half-written review records.
- **Implementation Map:**
  - Room `@Transaction` over `recordReview` function.
- **Testing:** DAO suite atomicity test (Plan 06) throws artificial exceptions and asserts rollback.

### Dimension 5: Performance & Scale
- **Goal:** DB operations must not block the main thread.
- **Implementation Map:**
  - Kotlin Coroutines/Flows applied to all DAO methods.
- **Testing:** Compiler enforces `suspend`/`Flow` on all `VocabDao` functions (Plan 04).

### Dimension 8: Nyquist Compliance
- **Goal:** Automated verification for every single requirement.
- **Implementation Map:**
  - `FSRS-01..FSRS-05`: Automated by `PyFsrsParityTest` and `OptimizerTest`.
  - `PERS-01`: Verified by `VocabDatabaseSmokeTest` parsing XML.
  - `PERS-02, PERS-03, PERS-04`: Verified by `VocabDaoTest` and `VocabularyRepositoryImplTest`.
