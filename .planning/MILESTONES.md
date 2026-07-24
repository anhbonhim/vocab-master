# Milestones

## v1.0 VocabMaster Refactor & Audit (Shipped: 2026-07-22)

**Phases completed:** 4 phases, 14 plans, 27 tasks

**Key accomplishments:**

- Ported the py-fsrs `tests/test_basic.py` regression suite to Kotlin JUnit as `PyFsrsParityTest.kt`, covering scheduling semantics, learning/relearning steps, parameter validation, serde round-trips, reschedule behavior, and deterministic fuzz properties.
- Ported the py-fsrs 6.3.1 Optimizer to pure Kotlin with finite-difference Adam training and optimal-retention simulation, satisfying FSRS-05 with zero new dependencies.
- Cut the production scheduling path over to FSRS-6 v6.Scheduler, persist card updates and review logs atomically in Room v8, and standardize all due-date comparisons on epoch milliseconds.
- Typed parse failures via `VocabDataException`, `Result<T>` repository signatures, folded app call sites, and a Robolectric failure-injection suite.
- Extracted quiz session loading, answer evaluation, FSRS review submission, and completion rules from `QuizViewModel` into pure-Kotlin domain UseCases with full unit-test coverage.
- Refactored `QuizViewModel` to adopt Unidirectional Data Flow (UDF) pattern, delegate quiz business logic to UseCases, implement lightweight SavedStateHandle state persistence, and guard against rapid double submissions.
- Refactor HomeScreen and SettingsScreen to Container/Content pattern, introduce Duo3DCard + DestructiveDialog per 03-UI-SPEC.md, all 3 plan tasks committed atomically.
- Split QuizScreen into thin Container + stateless Content with UX-03 3D rotationY flip + shake animations, and harden QuizViewModel SavedStateHandle persistence to cover every Active field (kind, IDs, per-question state, cumulative progress) so a process death at any point is fully recoverable.
- Type-Safe Navigation 3 + global DuoSnackbar error pipeline + ResultScreen Container/Content with safe casting
- Eliminated 5 !! forced unwraps in 4 pre-existing UI files (ARCH-02 fully complete) and wired SharedFlow<SnackbarMessage> error pipeline into MainViewModel / QuizViewModel / SettingsViewModel + the 3 top-level Container screens.
- Hardened SyncManager (IOException/HttpException handled, CancellationException rethrown), DAO-level time-based merge (D-03), review log preservation on push failure (D-04), and a Snackbar retry action wired end-to-end through SettingsViewModel → SnackbarMessage.action → DuoSnackbar.

---
