# Phase 1: Security & Database Stabilization - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-20
**Phase:** 1-Security & Database Stabilization
**Areas discussed:** FSRS fix scope & strategy, FSRS golden vector source, Room threading migration approach, JSON asset parsing error pattern

---

## FSRS Fix Scope & Strategy

### Q1: Fix strategy for FSRS.kt bugs

| Option | Description | Selected |
|--------|-------------|----------|
| Patch in place per audit | Keep current FSRS.kt; apply 11 targeted fixes in audit's order. Lowest risk to existing data. | |
| Port py-fsrs to Kotlin | Replace FSRS.kt with fresh Kotlin port of py-fsrs scheduler.py. Highest correctness; aligns with canonical reference. | ✓ |
| Adopt FSRS-Kotlin lib | Depend on open-spaced-repetition/FSRS-Kotlin library. Removes maintenance; loses version control. | |
| Guards only, no algorithm change | Only add bounds-checking/clamping. Cheapest; leaves formula bugs. | |

**User's choice:** Port py-fsrs to Kotlin
**Notes:** Highest correctness guarantee; aligns with canonical FSRS-6 reference.

### Q2: Existing user scheduling data migration

| Option | Description | Selected |
|--------|-------------|----------|
| Recompute from review logs | Replay each card's review history through new scheduler. Matches py-fsrs `reschedule_card`. | |
| Keep existing S/D, port forward only | Preserve current values; use new scheduler for future reviews. No migration. | |
| Reset all cards to New state | Wipe S/D/interval; treat every card as new. Cleanest math; terrible UX unless pre-launch. | ✓ |
| Agent's discretion | Researcher/planner decides based on persisted review-log data. | |

**User's choice:** Reset all cards to New state
**Notes:** Viable because this is a pre-launch refactor (PROJECT.md confirms).

### Q3: Port scope (optimizer included?)

| Option | Description | Selected |
|--------|-------------|----------|
| Core scheduler only | Scheduler + Card + ReviewLog + math. Skip optimizer (v2 per REQUIREMENTS). | |
| Scheduler + reschedule_card helper | Core + migration helper. Slightly larger; sets up Phase 4. | |
| Full py-fsrs port | Everything including optimizer. Out of scope per REQUIREMENTS; would balloon effort. | ✓ |

**User's choice:** Full py-fsrs port
**Notes:** Conflict with REQUIREMENTS.md Out-of-Scope flagged — see Q4.

### Q4: Resolve REQUIREMENTS.md Out-of-Scope conflict

| Option | Description | Selected |
|--------|-------------|----------|
| Override — port optimizer now | Move "Custom FSRS weights tuning" from Out-of-Scope to v1 Active. Update REQUIREMENTS.md. | ✓ |
| Port optimizer, no user-facing tuning UI | Optimizer available programmatically; no UI. Keeps REQUIREMENTS.md intact. | |
| Reconsider — core scheduler only | Revert to Recommended; optimizer stays for v2. | |

**User's choice:** Override — port optimizer now
**Notes:** REQUIREMENTS.md and ROADMAP.md updated to add FSRS-05 (see "Executed Updates" below).

### Q5: Code placement

| Option | Description | Selected |
|--------|-------------|----------|
| domain module | Port lives in domain/fsrs/ (where FSRS.kt is). Pure Kotlin. | |
| New fsrs Gradle module | Isolated module; publishable later. Adds gradle overhead; out of scope. | |
| Side-by-side in domain, delete old after tests pass | New impl alongside legacy FSRS.kt; delete old once tests pass. Safer rollout. | ✓ |

**User's choice:** Side-by-side in domain, delete old after tests pass

### Q6: FSRS version target

| Option | Description | Selected |
|--------|-------------|----------|
| FSRS-6, 21 params | Current py-fsrs default. Matches audit's reference. | ✓ |
| FSRS-5, 19 params | Simpler; but current code already attempts FSRS-6 — downgrade is a regression. | |
| Configurable, default FSRS-6 | Future version bumps without code changes. YAGNI risk. | |

**User's choice:** FSRS-6, 21 params

### Q7: Default parameters

| Option | Description | Selected |
|--------|-------------|----------|
| py-fsrs DEFAULT_PARAMETERS | The 21 FSRS-6 weights. Matches reference; deterministic. | ✓ |
| Current VocabMaster values | May be mis-transcribed; audit flags missing validation. | |
| Runtime-configurable, default py-fsrs | Optimizer writes trained params; defaults still py-fsrs. Required for optimizer. | |

**User's choice:** py-fsrs DEFAULT_PARAMETERS
**Notes:** Optimizer (when run) trains these; defaults matter for fresh installs and tests.

### Q8: Fuzz strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Fuzz off by default, opt-in | Deterministic tests; user can opt in later. Fixes audit Issue 8. | ✓ |
| Fuzz on, deterministic seed | Hash of card_id + review_date. Fixes Issue 8; tests pin seed. | |
| Fuzz on, random seed | py-fsrs default. Breaks preview/commit determinism; tests disable fuzz locally. | |

**User's choice:** Fuzz off by default, opt-in
**Notes:** Deliberate deviation from py-fsrs default (True) — port defaults to false for determinism.

### Q9: Scheduler modes

| Option | Description | Selected |
|--------|-------------|----------|
| Both short + long term | Full FSRS-6 correctness. Fixes audit Issue 10. | ✓ |
| Long-term only | Simpler; learning steps and same-day reviews won't match py-fsrs. | |
| Long-term default, short-term opt-in | Middle ground; risk of hiding Issue 10. | |

**User's choice:** Both short + long term

### Q10: DB schema changes

| Option | Description | Selected |
|--------|-------------|----------|
| Full schema: Card fields + ReviewLog | All py-fsrs Card fields + ReviewLog entity. Destructive migration resets cards. | ✓ |
| Card fields only, no ReviewLog | Skip ReviewLog — but then optimizer has no training data. Contradicts full-port. | |
| Minimal schema delta, map onto existing | Cheapest; risk of losing py-fsrs state fidelity. | |

**User's choice:** Full schema: Card fields + ReviewLog

### Q11: Scheduler config defaults

| Option | Description | Selected |
|--------|-------------|----------|
| py-fsrs defaults | desired_retention=0.9, max_interval=36500, learning_steps=1m/10m, relearning_steps=10m. | ✓ |
| Current VocabMaster values | May not match py-fsrs; tests diverge. | |
| Hardcode core, configurable steps | Middle ground. | |

**User's choice:** py-fsrs defaults

### Q12: Locale fix (FSRS-03, audit Issue 7)

| Option | Description | Selected |
|--------|-------------|----------|
| No strings in math, Locale.US for display | Remove String.format from math; store raw Double/Int; Locale.US only at presentation. | ✓ |
| Keep String.format with Locale.US | Minimal change; keeps string round-trips (precision drift). | |
| Kotlin native Double.toString() | No locale dep; slightly different from py-fsrs style. | |

**User's choice:** No strings in math, Locale.US for display only

### Q13: Update REQUIREMENTS.md to reflect override

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, update REQUIREMENTS.md now | Move "Custom FSRS weights tuning" to v1 Active as FSRS-05; update Traceability. | ✓ |
| No, record override in CONTEXT.md only | User updates REQUIREMENTS.md later. | |

**User's choice:** Yes, update REQUIREMENTS.md now
**Notes:** Executed — see "Executed Updates" below.

---

## FSRS Golden Vector Source

### Q1: Golden vector source

| Option | Description | Selected |
|--------|-------------|----------|
| Generate from py-fsrs, commit JSON | Python script dumps (state, S, D, interval) tuples to JSON. Highest fidelity. | ✓ |
| Translate py-fsrs pytest cases to JUnit | Reuse reference's test suite; may miss VocabMaster edge cases. | |
| Use ts-fsrs test intervals as JSON fixtures | Cross-language; minor decimal-tail differences may fail exact equality. | |
| Hand-compute from spec formulas | Smallest effort; error-prone for FSRS-6 exponentials. | |

**User's choice:** Generate from py-fsrs, commit JSON

### Q2: Coverage

| Option | Description | Selected |
|--------|-------------|----------|
| Full matrix (~30-50 cases) | Rating × State, sequences of 1/3/10/30, lapses, same-day, edge cases. | ✓ |
| Smoke set (~8-12 cases) | Fast; may miss formula branch bugs. | |
| Property-based tests (invariants only) | Catches OOB but not formula drift. | |

**User's choice:** Full matrix

### Q3: Generator tool

| Option | Description | Selected |
|--------|-------------|----------|
| Python script using pip py-fsrs | In domain/scripts/ or tools/. Not shipped in APK. | ✓ |
| Node.js script using ts-fsrs | TS may be more familiar; but py-fsrs is the port target. | |
| Both py-fsrs + ts-fsrs, cross-check | Highest confidence; double maintenance. | |

**User's choice:** Python script using pip py-fsrs

### Q4: Decimal precision

| Option | Description | Selected |
|--------|-------------|----------|
| Tolerance 1e-6 for doubles, exact for ints | Handles cross-language ULP differences. | ✓ |
| Exact equality on all fields | Strictest; cross-language ULP differences cause flaky failures. | |
| Round to 4 decimals before compare | Avoids ULP noise; loses 1e-5 drift signal. | |

**User's choice:** Tolerance 1e-6 for doubles, exact for ints

### Q5: JSON schema

| Option | Description | Selected |
|--------|-------------|----------|
| Per-review snapshots | expected_after_each array; catches intermediate-step bugs. | ✓ |
| Final-state only | Smaller files; masks intermediate bugs. | |
| Single-review table | Compact; can't test multi-review sequences. | |

**User's choice:** Per-review snapshots

### Q6: Vector location

| Option | Description | Selected |
|--------|-------------|----------|
| Single JSON in test resources | domain/src/test/resources/fsrs/golden_vectors.json. JUnit convention. | ✓ |
| One file per vector | Easier to diff; many small files. | |
| Inline Kotlin constants | Type-safe; huge file; hard to regenerate. | |

**User's choice:** Single JSON in domain/src/test/resources/fsrs/golden_vectors.json

### Q7: Port py-fsrs tests too?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, port py-fsrs tests as parity suite | PyFsrsParityTest.kt. Double coverage; catches drift. | ✓ |
| Skip, golden vectors are enough | Less work; risk missing py-fsrs edge cases. | |
| Port only uncovered py-fsrs tests | Complementary; needs coverage diff. | |

**User's choice:** Yes, port py-fsrs tests as parity suite

### Q8: Regen cadence

| Option | Description | Selected |
|--------|-------------|----------|
| One-time, manual regen | Commit JSON + script; regen only on py-fsrs version bump. | ✓ |
| Gradle task wrapping Python script | Reproducible; adds Python dev dep coupling. | |
| CI regen-and-diff check | Strongest; VocabMaster may not have CI yet. | |

**User's choice:** One-time, manual regen

---

## Room Threading Migration Approach

### Q1: Migration cadence

| Option | Description | Selected |
|--------|-------------|----------|
| All-at-once | Every DAO in Phase 1: reads→Flow, writes→suspend, atomic→@Transaction. | ✓ |
| Incremental, FSRS-touched DAOs first | Smaller PRs; mixed state mid-phase. | |
| FSRS-touched DAOs only | Smallest; PERS-02 says "all" — wouldn't meet requirement. | |

**User's choice:** All-at-once

### Q2: Transaction pattern

| Option | Description | Selected |
|--------|-------------|----------|
| @Transaction on DAO methods | Compile-time checked; keeps transaction in DAO layer. | |
| db.withTransaction at repository layer | Flexible; spans DAOs; moves transaction ownership to repo. | |
| Hybrid: @Transaction single-DAO, withTransaction cross-DAO | Pragmatic; uses each where it fits. | ✓ |

**User's choice:** Hybrid: @Transaction single-DAO, withTransaction cross-DAO

### Q3: Test strategy

| Option | Description | Selected |
|--------|-------------|----------|
| In-memory Room + runTest | Real SQLite semantics; no mocking; runTest already available. | ✓ |
| MockK mock DAOs | Fast; can't verify SQL or transaction atomicity. | |
| Robolectric + in-memory Room | Faster than connectedAndroidTest; adds Robolectric dep. | |

**User's choice:** In-memory Room + runTest

### Q4: allowMainThreadQueries

| Option | Description | Selected |
|--------|-------------|----------|
| Remove from production, keep in tests | Restores Room's main-thread guard; enforces PERS-02 automatically. | ✓ |
| Leave as-is | No enforcement; relies on developer discipline. | |
| Add DEBUG-only main-thread crash guard | Stronger than Room's default; extra code. | |

**User's choice:** Remove from production, keep in tests

---

## JSON Asset Parsing Error Pattern

### Q1: Error pattern

| Option | Description | Selected |
|--------|-------------|----------|
| runCatching + Result<T> to UiState | Kotlin-idiomatic; sets convention for Phase 2/3; every catch logged. | ✓ |
| Sealed LoadError type | Most type-safe; most expressive; more boilerplate. | |
| Specific catches + logging, keep fallback | Minimal; UI still silently degrades. | |
| Hybrid: runCatching internal, sealed LoadResult external | Clearest contract; most boilerplate. | |

**User's choice:** runCatching + Result<T> to UiState

### Q2: Parser library

| Option | Description | Selected |
|--------|-------------|----------|
| Keep current parser, fix error handling | Lowest risk; matches "audit and fix" intent. | |
| Migrate to kotlinx.serialization | Type-safe; scope creep risk (parser migration). | |
| Researcher decides based on current parser | Investigate; recommend migration only if parser is the fragility source. | ✓ |

**User's choice:** Researcher decides based on current parser

### Q3: Fallback policy

| Option | Description | Selected |
|--------|-------------|----------|
| No silent fallback — propagate to UiState.Error | Forces fixing bad data; matches PITFALLS.md. | ✓ |
| Field-criticality-aware fallback | Pragmatic; requires classifying each field. | |
| Keep fallback + prominent logging | Cheapest; user still sees broken UI in release. | |

**User's choice:** No silent fallback — propagate to UiState.Error

### Q4: Scope of fix

| Option | Description | Selected |
|--------|-------------|----------|
| VocabularyRepositoryImpl only this phase | Focused; matches CONCERNS.md flag. | |
| All repositories this phase | Thorough; PERS-04 says "repositories" plural. | ✓ |
| VocabularyRepositoryImpl + shared helper for Phase 2 | Middle ground. | |

**User's choice:** All repositories this phase

---

## the agent's Discretion

The following were explicitly deferred to the researcher/planner:
- Backup rule granularity (PERS-01) — apply canonical Android pattern (exclude Room DB + shared prefs from cloud-backup and device-transfer).
- Kotlin idioms for the port (data class vs plain class, Hilt @Inject vs object) — follow py-fsrs structure and existing domain conventions.
- Destructive migration specifics (Migration object vs fallbackToDestructiveMigration) — planner decides; user data preservation doesn't matter (D-02 resets).
- ReviewLog schema fields beyond py-fsrs attributes — planner aligns with py-fsrs ReviewLog dataclass.
- LocalLogger log level/format for parse failures — use existing LocalLogger.e(tag, message, throwable).

## Deferred Ideas

- User-facing FSRS weights tuning UI (v2 — optimizer is ported but no UI in Phase 1).
- CI regen-and-diff check for golden vectors (future hardening; deferred until CI is set up).
- Gradle task wrapping the Python generator (future; would add Python dev dep coupling).
- Parser migration to kotlinx.serialization (D-25 leaves to researcher; may or may not happen in Phase 1).

---

## Executed Updates

During this discuss-phase, the following project files were edited to keep planning artifacts consistent with the override decision (D-03/D-04/Q4):

- **`.planning/REQUIREMENTS.md`**:
  - Added `FSRS-05` to "FSRS Algorithm & Math Engine" section: "Port the py-fsrs optimizer (parameter training from review logs) to Kotlin so custom FSRS weights can be trained from user review history. (Moved here from Out-of-Scope during Phase 1 discuss-phase on 2026-07-20 — full py-fsrs port was chosen as the FSRS fix strategy.)"
  - Removed the "Custom FSRS weights tuning by user" row from the Out-of-Scope table.
  - Added `FSRS-05 | Phase 1 | Pending` to the Traceability table.
  - Updated Coverage counts: v1 requirements 18 → 19, mapped 18 → 19.
- **`.planning/ROADMAP.md`**:
  - Phase 1 `Requirements:` line updated from `FSRS-01, FSRS-02, FSRS-03, FSRS-04, PERS-01, PERS-02, PERS-03, PERS-04` to `FSRS-01, FSRS-02, FSRS-03, FSRS-04, FSRS-05, PERS-01, PERS-02, PERS-03, PERS-04`.
