<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
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
- **Backup rule granularity (PERS-01):** Which exact artifacts to exclude (Room DB files, shared preferences, internal storage caches) and whether to use `dataExtractionRules` only (Android 12+) or also `fullBackupContent` for older versions. The standard Android guidance + the existing TODO in `data_extraction_rules.xml` are sufficient — researcher applies the canonical pattern (exclude Room DB + shared prefs from both `<cloud-backup>` and `<device-transfer>`).
- **Kotlin idioms for the port:** Whether `Card`/`ReviewLog`/`Scheduler` are `data class`es (recommended for `Card`/`ReviewLog`) vs plain classes, whether `Scheduler` is a `class` with `@Inject` constructor (Hilt) or an `object`. Researcher/planner follows `py-fsrs` structure and existing `domain` module conventions (PascalCase, Hilt `@Inject` where appropriate).
- **Destructive migration specifics:** The exact Room `Migration` object implementation (e.g., `Migration(N, N+1)` that drops and recreates the Card table, or `fallbackToDestructiveMigration()`). Planner decides based on whether user data preservation matters (it doesn't — D-02 resets cards) — `fallbackToDestructiveMigration` is acceptable.
- **`ReviewLog` schema fields:** Exact column names/types beyond the `py-fsrs` `ReviewLog` attributes (rating, review_datetime, review_duration, card_id). Planner aligns with `py-fsrs` `ReviewLog` dataclass.
- **LocalLogger log level/format for parse failures:** `LocalLogger.e(tag, message, throwable)` is the existing convention — researcher/planner uses it.

### Deferred Ideas (OUT OF SCOPE)
- **User-facing FSRS weights tuning UI** — the optimizer is ported (FSRS-05), but no UI for users to tune their own weights. That's a v2 feature (UI for triggering optimization, displaying trained parameters, opting in/out). Phase 1 only ports the programmatic optimizer.
- **CI regen-and-diff check for golden vectors** — D-19 chose one-time manual regen. A CI check that regenerates vectors and fails on diff is a future hardening step, deferred until VocabMaster has CI set up (not in Phase 1 scope).
- **Gradle task wrapping the Python generator** — D-19 chose manual regen. A `./gradlew generateFsrsGoldenVectors` task is future work (would require Python dev dep coupling).
- **Parser migration to `kotlinx.serialization`** — D-25 leaves this to the researcher. If the researcher recommends migration and the planner agrees, it may happen in Phase 1; otherwise it's a follow-up. Not pre-decided here.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FSRS-01 | mathematically correct FSRS scheduler | FSRS algorithm & math reference |
| FSRS-02 | prevent out-of-bounds intervals | FSRS bound clamping reference |
| FSRS-03 | fully localized math | `Locale.US` isolation in UI |
| FSRS-04 | golden vector tests | Py-fsrs generation scripts & Pytest Parity |
| FSRS-05 | FSRS weights tuning by user | Optimizer Port strategy |
| PERS-01 | explicit XML data extraction rules | Android 12+ Cloud/Device Transfer Exclusions |
| PERS-02 | background thread DAO operations | Kotlin Coroutines `suspend` and `Flow` |
| PERS-03 | atomic transactions | `@Transaction` and `db.withTransaction` patterns |
| PERS-04 | unsafe JSON asset parsing fallbacks | `kotlinx.serialization` error handling |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | (current) | Persistence | Android standard for typed SQLite access. Native coroutine support. |
| kotlinx.serialization | (current) | JSON parsing | Fast, type-safe serialization native to Kotlin. Prevents dynamic reflection pitfalls. |
| kotlinx-coroutines-android | (current) | Concurrency | Standard Kotlin library for `suspend` and `Flow` asynchronous programming on Android. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| fsrs (Python) | (v6 via pip) | Golden Vector Generation | Generating `expected` FSRS mathematically correct outputs for Kotlin testing. Used only during dev. |

**Version verification:**
*(Versions assumed from project's existing build.gradle.kts files; Python fsrs package to be installed dynamically during vector generation).*

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| fsrs | PyPI | N/A | N/A | open-spaced-repetition/py-fsrs | OK | Approved (Dev-only python script dependency) |

## Architecture Patterns

### Recommended Project Structure
```
domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/
├── Card.kt              # FSRS domain model for a single study card
├── ReviewLog.kt         # FSRS domain model tracking an individual review
├── Scheduler.kt         # The core FSRS mathematical engine (port target)
└── State.kt             # Enums for card state and review rating

domain/scripts/
└── generate_fsrs_golden_vectors.py # Python script for FSRS parity assertions

data/src/main/java/com/nhimz/vocabmaster/data/
├── database/VocabDatabase.kt    # Removes allowMainThreadQueries()
└── repository/VocabularyRepositoryImpl.kt # Safe JSON parsing with Result<T>
```

### Pattern 1: FSRS V6 Parallel Implementation
**What:** Writing `FsrsV6` inside a new package `domain/fsrs/v6/` alongside the legacy implementation.
**When to use:** When rewriting a deeply integrated and highly mathematical module that needs to pass full tests before replacing the current version.
**Example:**
```kotlin
// domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/v6/Scheduler.kt
package com.nhimz.vocabmaster.domain.fsrs.v6
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.pow

class Scheduler @Inject constructor() {
    // py-fsrs DEFAULT_PARAMETERS mapping
    private val defaultWeights = doubleArrayOf(0.2172, 1.2931, /* ... */)
    // ...
}
```

### Pattern 2: Result<T> with runCatching for Repository Boundaries
**What:** Catching specific serialization exceptions in the repository and wrapping the outcomes in `Result<T>` instead of swallowing them.
**When to use:** To prevent unhandled crashes or silent failures from corrupt JSON assets.
**Example:**
```kotlin
return runCatching {
    json.decodeFromString<LessonsV2Asset>(jsonString)
}.onFailure { e ->
    LocalLogger.e("Repository", "Failed to decode assets", e)
}
```

### Pattern 3: Room Database Transaction with db.withTransaction
**What:** Leveraging `withTransaction { ... }` block to execute atomic updates bridging multiple tables via repositories, safely suspending.
**When to use:** When updating a `Card` and inserting a `ReviewLog` from the repository layer simultaneously.

### Anti-Patterns to Avoid
- **String.format math:** Parsing strings via Locale configurations leads to NumberFormatExceptions in non-US locales. Store and calculate strictly as `Double`.
- **allowMainThreadQueries:** Using this Room builder feature covers up synchronous database calls that block the UI thread.
- **Silent Exception Catching:** Catching generic `Exception` during JSON parsing and hiding the failure by returning defaults (e.g. empty lists).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Algorithm correctness assertions | Hand-calculated test intervals | `py-fsrs` generated golden JSON vectors | Ensures 100% mathematical parity and detects IEEE 754 precision drifts. |
| Complex multi-table atomicity | Manual JDBC/SQLite transactions | `androidx.room.withTransaction` block | Provides coroutine safety and thread-confinement out of the box. |

**Key insight:** Replicating the canonical `py-fsrs` exactly and using golden vectors removes the guessing game for math stability.

## Runtime State Inventory

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | Existing `Card` entities and potential `ReviewLog` analogues | Destructive Room Migration to wipe DB. Existing User `Card` entities get reset to `State.New`. |
| Live service config | None — verified | No external service relies on local database schema changes |
| OS-registered state | None — verified | No OS tasks registered tying to FSRS data |
| Secrets/env vars | None — verified | The data_extraction_rules fix limits backup exposure |
| Build artifacts | `FSRS.kt` (legacy) | Phase 1 deletion of legacy FSRS file once V6 passes suite. |

## Common Pitfalls

### Pitfall 1: Coroutine Thread Confinement during `withTransaction`
**What goes wrong:** Calling non-suspending database calls or changing dispatchers inside the `db.withTransaction` block leading to `SQLiteException` or thread leaks.
**Why it happens:** Room confines the transaction strictly to a specific coroutine thread allocated for the transaction.
**How to avoid:** Ensure *all* DAO calls within `withTransaction` are marked as `suspend` and do NOT internally swap to `Dispatchers.IO` again using `withContext` inside the transaction.

### Pitfall 2: Double precision drift between Python and Kotlin
**What goes wrong:** `assertEquals` in unit tests fails by microscopic fractions due to how IEEE 754 float/double rounding behaves across language libraries for `exp` or `pow`.
**Why it happens:** Inherent mathematical discrepancies between standard libraries.
**How to avoid:** Use `assertEquals(expected, actual, delta=1e-6)` explicitly for `stability` and `difficulty`.

## Code Examples

### Exclude Room and Datastore from backups (Android 12+)
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="vocab_database"/>
        <exclude domain="database" path="vocab_database-wal"/>
        <exclude domain="database" path="vocab_database-shm"/>
        <exclude domain="sharedpref" path="."/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="vocab_database"/>
        <exclude domain="database" path="vocab_database-wal"/>
        <exclude domain="database" path="vocab_database-shm"/>
        <exclude domain="sharedpref" path="."/>
    </device-transfer>
</data-extraction-rules>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Synchronous DAO via allowMainThreadQueries | `Flow` reads and `suspend` writes | Since Coroutines native integration with Room | Avoids ANRs and conforms to modern Android architectures. |
| In-place string math (`String.format`) | Raw `Double`/`Int` variables | Localized environments | Eliminates number format crashes on locales using commas for decimals. |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `VocabDatabase` uses `fallbackToDestructiveMigration(dropAllTables = true)`. | Runtime State | If not, cards may not reset properly or schema mismatches will throw exceptions on app startup. |
| A2 | Py-fsrs uses a 21 parameter configuration by default for v6. | User Constraints | Planner might implement the older 17 parameter version or incorrectly train the algorithm. |

## Open Questions (RESOLVED)

1. **How should legacy `FSRS.kt` integration points adapt? (RESOLVED)**
   - What we know: `FSRS.kt` is currently tied to `QuizViewModel` and UseCases.
   - What's unclear: If `FsrsV6` drops in or if `QuizViewModel` undergoes a refactor here.
   - Recommendation: Keep integration minimal, update method calls to `review_card` or `reschedule_card`, but avoid major architecture restructuring of ViewModels (delegated to Phase 2/3).
   - Resolution: Plan 01-05 explicitly defines the adapter pattern to isolate these changes from the UI layer.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Python | FSRS Golden Vector Script | ✓ | Assumed | Requires local python installation for developers running `generate_fsrs_golden_vectors.py` |
| SQLite | Room DB | ✓ | Native | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + kotlinx.coroutines.test |
| Config file | `app/build.gradle.kts` (testImplementation) |
| Quick run command | `./gradlew :domain:testDebugUnitTest --tests "com.nhimz.vocabmaster.domain.fsrs.*"` |
| Full suite command | `./gradlew testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FSRS-01 | Math parity with Py-FSRS | unit | `./gradlew :domain:testDebugUnitTest --tests "*PyFsrsParityTest*"` | ❌ Wave 0 |
| FSRS-04 | Core math passes Golden Vector tests | unit | `./gradlew :domain:testDebugUnitTest --tests "*GoldenVectorTest*"` | ❌ Wave 0 |
| PERS-02 | DAO threading correctly uses coroutines | unit | `./gradlew :data:testDebugUnitTest --tests "*DatabaseTest*"` | ✅ Wave 0 (DataIntegrityTests exist but need migration) |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:testDebugUnitTest --tests "com.nhimz.vocabmaster.domain.fsrs.*"`
- **Per wave merge:** `./gradlew testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/GoldenVectorTest.kt` — covers FSRS-04
- [ ] `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/PyFsrsParityTest.kt` — covers FSRS-01
- [ ] `domain/src/test/resources/fsrs/golden_vectors.json` — required for assertions

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | kotlinx.serialization (with strict parsing / fallback Result wrapping) |
| V6 Cryptography | no | — |

### Known Threat Patterns for Android / Persistence

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Backup leak of SQLite databases | Information Disclosure | `data_extraction_rules.xml` excludes via `<exclude domain="database" ... />` |
| JSON format exception crashes (DoS) | Denial of Service | Safely wrapping asset deserialization in `runCatching` + `Result<T>` |

## Sources

### Primary (HIGH confidence)
- `.planning/phases/01-security-database-stabilization/01-CONTEXT.md` - Phase 1 constraints and goals.
- `app/src/main/res/xml/data_extraction_rules.xml` - Validated the current structure.
- Android Official Developer Reference - `androidx.room.RoomDatabase.withTransaction`.

### Secondary (MEDIUM confidence)
- `data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt` - Inspected JSON deserialization handling in the codebase.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Core Android architectural standards.
- Architecture: HIGH - Matches requested pattern from CONTEXT.md.
- Pitfalls: HIGH - Coroutines thread confinement is a well-documented hazard with `Room`.

**Research date:** 2026-07-20
**Valid until:** 30 days