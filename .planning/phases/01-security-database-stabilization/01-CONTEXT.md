# Phase 1: Security & Database Stabilization - Context

**Gathered:** 2026-07-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers data safety + mathematical correctness for VocabMaster's spaced-repetition core. Specifically:

1. **FSRS scheduler audit + fix** — Port the canonical `py-fsrs` (FSRS-6) reference implementation to Kotlin, replacing the current buggy `FSRS.kt`. This includes the core scheduler **and the optimizer** (parameter training from review logs) — an explicit override of REQUIREMENTS.md's prior "Custom FSRS weights tuning" Out-of-Scope entry (now moved to v1 Active as **FSRS-05**). Existing user card scheduling data is **reset to New state** (cleanest math; viable pre-launch).
2. **Locale-safe math (FSRS-03)** — Remove `String.format` round-trips from FSRS math entirely; store raw `Double`/`Int`; use `Locale.US` only at the presentation layer for display.
3. **FSRS unit tests with golden vectors (FSRS-04)** — Generate golden vectors from `py-fsrs` itself (Python generator script), commit as JSON, assert the Kotlin port matches within tolerance.
4. **Secure backup rules (PERS-01)** — Configure `data_extraction_rules.xml` to exclude sensitive Room DBs and shared preferences from auto-backup.
5. **Room threading migration (PERS-02)** — Migrate **all** DAO operations: reads → `Flow`, one-shot reads + writes → `suspend`, multi-statement atomic ops → `@Transaction` / `db.withTransaction`. Remove `allowMainThreadQueries()` from the production DB builder so Room's main-thread guard enforces PERS-02.
6. **Transaction bounds (PERS-03)** — Atomic card + review log updates via a hybrid pattern: `@Transaction` on single-DAO ops, `db.withTransaction { }` at the repository for cross-DAO ops (CardDao + ReviewLogDao).
7. **JSON asset parsing error handling (PERS-04)** — Replace swallowed catches in **all repository implementations** with `runCatching + Result<T>` propagated to `UiState.Error`. No silent fallbacks.

**Out of scope for Phase 1:** UI refactors (Phase 2/3), ViewModel/UseCase extraction (Phase 2), sync verification (Phase 4). New study modes / curricula are v2.

</domain>

<decisions>
## Implementation Decisions

### FSRS Fix Scope & Strategy (FSRS-01, FSRS-02, FSRS-03, FSRS-04, FSRS-05)

- **D-01:** Fix strategy = **Port `py-fsrs` to Kotlin** (highest correctness guarantee; aligns with the canonical FSRS-6 reference). Not a patch-in-place, not a library dependency, not guards-only.
- **D-02:** Existing user scheduling data migration = **Reset all cards to New state.** Viable because this is a pre-launch refactor (PROJECT.md confirms). No `reschedule_card` replay needed for the migration itself, but `reschedule_card` is still ported (see D-04).
- **D-03:** Port scope = **Full `py-fsrs` port including the optimizer** (parameter training from review logs). This is an explicit override of REQUIREMENTS.md's prior "Custom FSRS weights tuning by user" Out-of-Scope entry. REQUIREMENTS.md has been updated: the item is removed from Out-of-Scope and added as **FSRS-05** in v1 Active requirements, mapped to Phase 1 in the Traceability table. ROADMAP.md Phase 1 `Requirements:` line updated to include FSRS-05.
- **D-04:** Code placement = **Side-by-side in `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/`.** The new ported implementation lives alongside the legacy `FSRS.kt` (e.g., a new `FsrsV6.kt` or a package `domain/fsrs/v6/`). The legacy `FSRS.kt` is deleted only after the new impl passes all unit tests (golden vectors + py-fsrs parity suite). This gives a safer rollout — both implementations coexist during the port, reducing risk of a broken scheduler between commits.
- **D-05:** FSRS version target = **FSRS-6, 21 parameters.** Matches the current `py-fsrs` default and the FSRS-Kotlin audit's reference. Do NOT downgrade to FSRS-5.
- **D-06:** Default parameters = **`py-fsrs` `DEFAULT_PARAMETERS`** (the 21 FSRS-6 weights: `0.2172, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542`). The optimizer (when run) trains these from review logs; defaults still matter for fresh installs and tests.
- **D-07:** Fuzz strategy = **Fuzz OFF by default, opt-in via config.** Fixes FSRS-Kotlin audit Issue 8 (wall-clock-seeded fuzz → preview/commit mismatch). Tests are deterministic without a fuzz-seed fixture; user-facing scheduling can opt in later. `enable_fuzzing = false` is the port's default (note: `py-fsrs` defaults to `True`, but the port defaults to `false` for determinism — this is a deliberate deviation, document it in code).
- **D-08:** Scheduler modes = **Both short-term (learning/relearning steps, same-day reviews using `w[17..19]`) AND long-term (interval-based).** Full FSRS-6 correctness. Fixes audit Issue 10 (same-day Review/ReLearning handling). Port `learning_steps = (1min, 10min)`, `relearning_steps = (10min)`.
- **D-09:** DB schema = **Full schema: Card fields + ReviewLog entity.** Add all `py-fsrs` Card fields to the Room `Card` entity: `state` (New/Learning/Review/Relearning enum), `step` (Int?), `stability` (Double?), `difficulty` (Double?), `due` (Long millis), `last_review` (Long?), `card_id`. Add a new `ReviewLog` entity (rating, review_datetime, review_duration, card_id FK) — required by the optimizer. Bump DB version with a **destructive migration** that resets all cards to New (matches D-02). Add an index on `ReviewLog.card_id` for the optimizer's per-card training queries.
- **D-10:** Scheduler config defaults = **`py-fsrs` defaults:** `desired_retention = 0.9`, `maximum_interval = 36500` days (~100 years), `learning_steps = (1min, 10min)`, `relearning_steps = (10min)`. Hardcode these as Kotlin constants in the port (matching `py-fsrs` `Scheduler.__init__` defaults); do not make them runtime-configurable in Phase 1 (YAGNI; the optimizer writes parameters, not config).
- **D-11:** Locale fix (FSRS-03, audit Issue 7) = **No strings in math, `Locale.US` for display only.** Remove `String.format(...).toDouble()` from FSRS math code entirely. Store `stability`/`difficulty`/`interval` as raw `Double`/`Int` — never round-trip through strings. For user-facing display (e.g., "next review in 3 days"), use `String.format(Locale.US, ...)` or `NumberFormat` with explicit US locale, at the presentation layer only. The `domain` module's FSRS code must be pure numeric.

### FSRS Golden Vector Source (FSRS-04)

- **D-12:** Golden vector source = **Generate from `py-fsrs`, commit JSON.** Write a Python script that uses the official `pip install fsrs` package, feeds review sequences through `py-fsrs.Scheduler`, and dumps `(state, step, stability, difficulty, interval, due, last_review)` tuples to JSON. The Kotlin port's JUnit tests assert against these JSON vectors. The script runs at dev time only — it is NOT shipped in the APK.
- **D-13:** Coverage = **Full matrix (~30-50 test cases).** Every `Rating` (Again/Hard/Good/Easy) × every `State` (New/Learning/Review/Relearning), plus sequences of 1, 3, 10, 30 consecutive reviews, plus lapse paths (Review → Again → Relearning → Good), plus same-day reviews (Learning → Good within 1m step), plus edge cases (stability at `STABILITY_MIN=0.001`, difficulty at `MAX_DIFFICULTY=10`, interval at `maximum_interval=36500`). Matches FSRS-04's "comprehensive unit tests" wording.
- **D-14:** Generator tool = **Python script using `pip py-fsrs`.** Location: `domain/scripts/generate_fsrs_golden_vectors.py` (or `tools/fsrs-golden-vectors/`). Uses `pip install fsrs`, iterates the coverage matrix, dumps JSON. Document the regen command in a comment at the top of the script.
- **D-15:** Assertion precision = **Tolerance `1e-6` for `Double` fields (stability, difficulty), exact equality for `Int` fields (interval, step) and enum fields (state).** Handles cross-language IEEE 754 last-ULP differences in `exp`/`pow` (the FSRS-Kotlin audit and `ts-fsrs` both note "minor differences in decimal tail numbers"). Use JUnit `assertEquals(expected, actual, delta=1e-6)` for doubles.
- **D-16:** JSON schema = **Per-review snapshots.** Each vector is a JSON object: `{"id": "new_again_1", "initial_card": {...}, "reviews": [{"rating": "Again", "datetime": "..."}], "expected_after_each": [{state, step, stability, difficulty, due, last_review}, ...]}`. Timestamps as ISO 8601 strings. The Kotlin test parses the file, replays reviews, asserts after each review — catches bugs in intermediate steps, not just the final state.
- **D-17:** Vector file location = **Single JSON file: `domain/src/test/resources/fsrs/golden_vectors.json`.** Kotlin test reads via `ClassLoader.getResourceAsStream()`. Matches JUnit convention; one file to regenerate.
- **D-18:** Py-fsrs parity suite = **Yes, port `py-fsrs`'s own pytest cases to Kotlin JUnit as a separate `PyFsrsParityTest.kt`.** Golden vectors cover the comprehensive matrix; parity tests cover `py-fsrs`'s chosen edge cases (parameter validation, `reschedule_card`, optimizer tests). Double coverage; catches drift if the port later diverges from `py-fsrs`.
- **D-19:** Regen cadence = **One-time generation in Phase 1; manual regen thereafter.** Commit `golden_vectors.json` and the Python generator script. Regenerate manually only when bumping the `py-fsrs` version or changing `DEFAULT_PARAMETERS`. Document the regen command in the generator script header and in this CONTEXT.md. Do NOT wire into Gradle or CI for Phase 1.

### Room Threading Migration (PERS-02, PERS-03)

- **D-20:** Migration cadence = **All-at-once.** Migrate every DAO in the `data` module during Phase 1: every read → `Flow`, every write/one-shot read → `suspend`, every multi-statement atomic op → `@Transaction` / `db.withTransaction`. Matches the "Database operations utilize Kotlin Coroutines/Flows" success criterion as a single coherent change. Verify the whole surface is consistent at the end of the phase.
- **D-21:** Transaction pattern = **Hybrid.** Use `@Transaction` on abstract DAO class methods for atomic operations that stay within a single DAO (e.g., `CardDao.upsertCards(cards)` batch, `CardDao.insertCardAndReviewLog` if both tables are in one DAO). Use `db.withTransaction { ... }` at the repository layer for atomic operations that span multiple DAOs (Card update + ReviewLog insert — the canonical PERS-03 case). Compile-time checked where possible; `db.withTransaction` handles the coroutine-safe single-thread dispatch.
- **D-22:** DAO/transaction test strategy = **In-memory Room + `runTest`.** `Room.inMemoryDatabaseBuilder(context, VocabDatabase::class.java).allowMainThreadQueries().build()` with `runTest` from `kotlinx-coroutines-test`. Tests call suspend DAO functions and assert DB state. Real SQLite semantics, no mocking. The `kotlinx.coroutines.test` dependency is already present.
- **D-23:** `allowMainThreadQueries()` = **Remove from the production DB builder; keep ONLY in the in-memory test builder.** This restores Room's built-in main-thread guard — any DAO call not marked `suspend`/`Flow` throws `IllegalStateException` at runtime, enforcing PERS-02 automatically. The researcher/planner must verify the production `Room.databaseBuilder(...)` call in `VocabDatabase.kt` (or wherever the production DB is constructed) does NOT call `allowMainThreadQueries()`; if it does, remove it.

### JSON Asset Parsing Error Handling (PERS-04)

- **D-24:** Error pattern = **`runCatching + Result<T>` propagated to `UiState`.** Repository functions return `Result<T>` (or fold into `UiState`). Catch **specific** exceptions (`SerializationException`, `JsonDecodingException`, `IOException`) and map them to typed failures; let generic exceptions bubble. ViewModel receives `Result<T>` and folds it into `UiState.Success` / `UiState.Error(message)`. Every catch is logged via `LocalLogger`. This sets the error-handling convention for Phase 2 (ViewModel refactor) and Phase 3 (UI).
- **D-25:** Parser library = **Researcher decides based on the current parser.** The researcher reads `VocabularyRepositoryImpl.kt` and `build.gradle.kts` to identify the current JSON parser (likely Gson or kotlinx.serialization). If the current parser is Gson with raw `Type`/`Class` reflection (the fragile pattern CONCERNS.md flags), the researcher recommends migration to `kotlinx.serialization` (type-safe, compile-time checked) — but that recommendation flows through research → planning, not decided here. If the current parser is already `kotlinx.serialization`, fix only the error handling. Phase 1's intent is "audit and fix", not "migrate the parser" — migration is only if the parser itself is the fragility source.
- **D-26:** Fallback policy = **No silent fallback.** If parsing fails, the repository returns `Result.failure(...)`; the ViewModel renders `UiState.Error` with a user-visible message and a retry action. Empty list / null is NOT a valid fallback for a parse failure — it hides data integrity issues (per PITFALLS.md). Forces the team to fix bad data, not hide it.
- **D-27:** Scope of fix = **All repository implementations in the `data` module this phase.** Apply the `runCatching + Result<T>` convention to every repository impl (VocabularyRepositoryImpl, ReviewRepository impl, SettingsRepository impl, etc.). PERS-04 says "in repositories" (plural); CONCERNS.md flags VocabularyRepositoryImpl specifically, but the convention must be uniform to set up Phase 2/3. The planner may sequence this as a separate plan from the FSRS port to keep PRs reviewable.

### the agent's Discretion

The following were not explicitly decided and are left to the researcher/planner:
- **Backup rule granularity (PERS-01):** Which exact artifacts to exclude (Room DB files, shared preferences, internal storage caches) and whether to use `dataExtractionRules` only (Android 12+) or also `fullBackupContent` for older versions. The standard Android guidance + the existing TODO in `data_extraction_rules.xml` are sufficient — researcher applies the canonical pattern (exclude Room DB + shared prefs from both `<cloud-backup>` and `<device-transfer>`).
- **Kotlin idioms for the port:** Whether `Card`/`ReviewLog`/`Scheduler` are `data class`es (recommended for `Card`/`ReviewLog`) vs plain classes, whether `Scheduler` is a `class` with `@Inject` constructor (Hilt) or an `object`. Researcher/planner follows `py-fsrs` structure and existing `domain` module conventions (PascalCase, Hilt `@Inject` where appropriate).
- **Destructive migration specifics:** The exact Room `Migration` object implementation (e.g., `Migration(N, N+1)` that drops and recreates the Card table, or `fallbackToDestructiveMigration()`). Planner decides based on whether user data preservation matters (it doesn't — D-02 resets cards) — `fallbackToDestructiveMigration` is acceptable.
- **`ReviewLog` schema fields:** Exact column names/types beyond the `py-fsrs` `ReviewLog` attributes (rating, review_datetime, review_duration, card_id). Planner aligns with `py-fsrs` `ReviewLog` dataclass.
- **LocalLogger log level/format for parse failures:** `LocalLogger.e(tag, message, throwable)` is the existing convention — researcher/planner uses it.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### FSRS-6 Reference Implementation (port target)
- `https://github.com/open-spaced-repetition/py-fsrs` — Official Python FSRS-6 implementation. **This is the port target.** The Kotlin port must match its math exactly.
- `https://github.com/open-spaced-repetition/py-fsrs/blob/main/fsrs/scheduler.py` — `Scheduler` class, parameter validation (`LOWER_BOUNDS_PARAMETERS`, `UPPER_BOUNDS_PARAMETERS`), `_clamp_stability` (min only), `_clamp_difficulty` ([1,10]), `_next_interval`, `_next_stability`, `_next_forget_stability`, `_next_recall_stability`, `_get_fuzzed_interval`, `reschedule_card`.
- `https://open-spaced-repetition.github.io/py-fsrs/fsrs.html` — py-fsrs API docs: `Scheduler`, `Card`, `ReviewLog`, `Rating`, `State`, `reschedule_card`.
- `https://github.com/open-spaced-repetition/py-fsrs/blob/main/fsrs/models.py` — `Card` and `ReviewLog` dataclass definitions (fields to mirror in Room schema).

### FSRS-Kotlin Audit (defect list + fix order)
- `https://github.com/open-spaced-repetition/FSRS-Kotlin/issues/1` — **11 confirmed defects** in an FSRS.kt matching VocabMaster's symptoms, with severity ratings and a recommended fix order. Even though we're porting (not patching), this audit lists exactly what the current code gets wrong — useful for the planner to enumerate what the port must fix and for the test suite to cover.

### FSRS-6 Algorithm Spec
- `https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm` — FSRS-6 formulas (forgetting curve, stability after recall/forget, difficulty, mean reversion, short-term stability). The port's math must match these.

### Sister Implementations (cross-reference for tests)
- `https://github.com/open-spaced-repetition/ts-fsrs` — TypeScript FSRS. Maintainers treat its test intervals as "ground truth" — useful for sanity-checking golden vectors.
- `https://github.com/open-spaced-repetition/ts-fsrs/blob/v4.1.2/__tests__/algorithm.test.ts` — ts-fsrs algorithm tests (cross-reference for golden-vector values).
- `https://github.com/open-spaced-repetition/fsrs-rs` — Rust FSRS. Uses `clamp(S_MIN, S_MAX=36500)` pattern (both bounds on stability) — slightly differs from py-fsrs (min only). The port follows **py-fsrs** (min-only clamp on stability) since py-fsrs is the port target.

### Android Room + Coroutines (threading migration)
- `https://developer.android.com/training/data-storage/room/async-queries` — Official Room async query guide: `suspend` for one-shot, `Flow` for observable, `@Transaction` for atomic.
- `https://developer.android.com/reference/kotlin/androidx/room3/Transaction` — `@Transaction` annotation reference (abstract DAO class pattern).
- `https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90` — Room's `withTransaction` API: coroutine-safe single-thread dispatch, nested transaction handling. **Required reading for D-21's `db.withTransaction` repository pattern.**

### Project-Internal Files (read before planning)
- `.planning/REQUIREMENTS.md` — Requirements FSRS-01..05, PERS-01..04 (FSRS-05 added 2026-07-20 during this discuss-phase).
- `.planning/ROADMAP.md` — Phase 1 goal, success criteria, requirements list (updated to include FSRS-05).
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture layers: `domain` (pure Kotlin, FSRS lives here), `data` (Room, repositories), `app` (ViewModels, Compose).
- `.planning/codebase/CONCERNS.md` — Known bugs: `data_extraction_rules.xml` TODO, FSRS anomalies in `DataIntegrityTests.kt`, swallowed JSON catches in `VocabularyRepositoryImpl.kt`.
- `.planning/codebase/TESTING.md` — JUnit 4, `kotlinx.coroutines.test`/`runTest`, AAA pattern, test file org (`app/src/test/java/...`).
- `.planning/codebase/CONVENTIONS.md` — Detekt config, Hilt DI, PascalCase files, `LocalLogger` for logging.
- `.planning/research/PITFALLS.md` — Domain pitfalls: swallowed JSON exceptions, unsafe casts, monolithic screens, backup rules, DAO threading.
- `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/FSRS.kt` — **The current buggy FSRS implementation to be replaced.** Read to understand current API surface and call sites.
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/debug_components/DataIntegrityTests.kt` — Existing data integrity tests that detected negative stability / difficulty OOB / negative intervals. Useful as a regression baseline.
- `app/src/main/res/xml/data_extraction_rules.xml` — Existing backup rules file with a TODO. Modify for PERS-01.
- `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt` — Swallowed JSON catches. Modify for PERS-04.
- `app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt` — Logging utility (`d`, `i`, `w`, `e`). Use for parse-failure logging (D-24).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`LocalLogger`** (`app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`) — existing logging utility with `d`/`i`/`w`/`e` levels and an in-memory log buffer exposed via `StateFlow`. Use for parse-failure logging (D-24) and FSRS audit diagnostics.
- **`DataIntegrityTests.kt`** (`app/src/main/java/com/nhimz/vocabmaster/ui/screens/debug_components/DataIntegrityTests.kt`) — existing tests that detected the FSRS anomalies (negative stability, difficulty OOB, negative intervals). Useful as a regression baseline; the new golden-vector + parity suite should supersede but not delete these (they live in the debug panel, useful for runtime diagnosis).
- **`kotlinx.coroutines.test`** dependency — already present in `build.gradle.kts`. Provides `runTest` for DAO/transaction tests (D-22) and for any ViewModel tests in later phases.
- **Hilt DI** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`) — already configured. The ported `Scheduler` and optimizer should use `@Inject` constructor injection, matching existing `domain`/`data` conventions.
- **`VocabDatabase`** — the existing Room database. The port adds a `ReviewLog` entity and new fields to `Card`; bump the DB version and use a destructive migration (D-09, D-02).

### Established Patterns
- **Clean Architecture module boundary** — `domain` is pure Kotlin (no Android deps); FSRS code must stay in `domain`. `data` holds Room + repository impls. `app` holds ViewModels + Compose. The port respects this: `Scheduler`/`Card`/`ReviewLog`/`Rating`/`State` in `domain`; Room entities (annotated `@Entity`) in `data` with mappers to/from domain models.
- **Repository pattern** — `VocabularyRepository`, `ReviewRepository`, `SettingsRepository` interfaces in `domain`; impls in `data`. The `runCatching + Result<T>` convention (D-24) applies at the impl boundary.
- **`StateFlow` for UI state** — ViewModels expose `StateFlow<UiState>`; the `Result<T>` from repositories folds into `UiState.Success` / `UiState.Error` (D-24).
- **Detekt** — configured in `config/detekt/detekt.yml`. The port must pass Detekt (complexity rules: `LongMethod`, `LargeClass`, `CyclomaticComplexMethod`, etc.). The `Scheduler` class may need `@Suppress("TooManyFunctions")` or splitting to satisfy `TooManyFunctions`.
- **JUnit 4 + AAA** — existing test convention. Golden-vector tests and parity tests follow AAA: Arrange (load vector), Act (replay reviews through port), Assert (compare to expected).

### Integration Points
- **FSRS call sites** — `FSRS.kt` is called from `QuizViewModel` (`app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt`) and possibly use cases in `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/`. The port must preserve the public API surface OR update all call sites. Planner decides: keep the same function names (drop-in) or rename to match `py-fsrs` (`review_card`, `reschedule_card`) and update call sites.
- **Room `Card` entity** — the `Card` table gains new fields (state, step, stability, difficulty, due, last_review). Any code reading/writing `Card` rows must be updated for the new schema.
- **`data_extraction_rules.xml`** — referenced from `AndroidManifest.xml` (via `android:dataExtractionRules` attribute). PERS-01 modifies this XML in place.
- **`VocabDatabase` builder** — wherever the production `Room.databaseBuilder(...)` is constructed (likely `data/src/main/java/com/nhimz/vocabmaster/data/VocabDatabase.kt` or a Hilt module), D-23 removes `allowMainThreadQueries()` from it.

</code_context>

<specifics>
## Specific Ideas

- **FSRS-Kotlin Issue #1 audit is a ready-made defect list.** Even though the fix strategy is "port py-fsrs" (not "patch in place"), the audit's 11 defects enumerate exactly what the current code gets wrong. The planner should reference this audit when writing acceptance criteria — each defect should have a corresponding golden-vector or parity test that proves the port doesn't reproduce it.
- **`STABILITY_MIN = 0.001`, `MIN_DIFFICULTY = 1.0`, `MAX_DIFFICULTY = 10.0`** — these are the standard FSRS-6 clamp constants from `py-fsrs`. Use these exact values in the port (do not invent new constants).
- **`py-fsrs` `DEFAULT_PARAMETERS`** — the exact 21 weights are listed in D-06. Copy them verbatim into a Kotlin `doubleArrayOf(...)` constant.
- **Optimizer port** — even though we're porting the optimizer, it's not user-facing in Phase 1 (no UI for tuning). It's available programmatically for Phase 4 sync conflict resolution and for future server-side batch training. The planner should sequence the optimizer port AFTER the core scheduler + tests pass.
- **`reschedule_card(card, review_logs)`** — port this helper even though D-02 resets cards. It's useful for Phase 4 sync (re-importing a card's history) and for the optimizer's training pipeline.
- **Test file org** — golden-vector tests and parity tests live in `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/` (mirroring the source package, per TESTING.md convention). The JSON vector file lives in `domain/src/test/resources/fsrs/golden_vectors.json` (D-17).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 1 scope. The following were noted as future-phase work but were not promoted to Phase 1:
- **User-facing FSRS weights tuning UI** — the optimizer is ported (FSRS-05), but no UI for users to tune their own weights. That's a v2 feature (UI for triggering optimization, displaying trained parameters, opting in/out). Phase 1 only ports the programmatic optimizer.
- **CI regen-and-diff check for golden vectors** — D-19 chose one-time manual regen. A CI check that regenerates vectors and fails on diff is a future hardening step, deferred until VocabMaster has CI set up (not in Phase 1 scope).
- **Gradle task wrapping the Python generator** — D-19 chose manual regen. A `./gradlew generateFsrsGoldenVectors` task is future work (would require Python dev dep coupling).
- **Parser migration to `kotlinx.serialization`** — D-25 leaves this to the researcher. If the researcher recommends migration and the planner agrees, it may happen in Phase 1; otherwise it's a follow-up. Not pre-decided here.

</deferred>

---

*Phase: 1-Security & Database Stabilization*
*Context gathered: 2026-07-20*
