---
phase: 01
slug: security-database-stabilization
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: 2026-07-22
---

# Phase 01 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| pip registry → dev machine | Golden vectors depend on the `fsrs` PyPI package's integrity |
| py-fsrs → Kotlin port | Mathematical fidelity boundary; drift here corrupts scheduling silently |
| GitHub raw → dev machine | test_basic.py fetched at execution time as the parity spec |
| User review logs → optimizer | Training data is user-generated; malformed logs must not crash or hang training |
| App → Android Backup Transport | Unencrypted app data leaves the device trust zone |
| Maven Central → build | New test dependency (Robolectric) |
| UI → repository | QuizViewModel scheduling writes are the app's core data mutation |
| DB v7 → v8 | Destructive migration boundary — user scheduling data is intentionally wiped |
| SyncManager → backend DTO | Placeholder telemetry fields cross to the server until Phase 4 |
| Test harness → production DAO | In-memory Room must faithfully represent production SQLite semantics |
| assets/DB JSON columns → domain models | Untrusted-by-corruption data crosses the parse boundary |
| User-supplied backup file → importBackup | External file content enters the app |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-01-01 | Denial of Service | v6 Scheduler math | medium | mitigate | Port py-fsrs clamps exactly; GoldenVectorTest edge vectors assert | closed |
| T-01-02 | Tampering | Ported formulas diverge | high | mitigate | Golden vectors generated from pip fsrs 6.3.1 | closed |
| T-01-SC | Tampering | pip install fsrs | high | mitigate | Package Legitimacy Audit in 01-RESEARCH.md | closed |
| T-01-03 | Tampering | Parity spec fetch (test_basic.py) | low | mitigate | Fetch over HTTPS from official repo; golden values asserted in tests | closed |
| T-01-04 | Denial of Service | 1000-iteration bounds test | low | accept | JVM executes < 1s | closed |
| T-01-05 | Denial of Service | Optimizer CPU cost | medium | mitigate | 512-review guard + MAX_SEQ_LEN 64 cap | closed |
| T-01-06 | Tampering | Trained parameters escape bounds | high | mitigate | Hard clamp to LOWER/UPPER_BOUNDS_PARAMETERS | closed |
| T-01-07 | Information Disclosure | Review logs | low | accept | Local-only computation; backup exclusion | closed |
| T-01-08 | Information Disclosure | Room DB backup leak | high | mitigate | Explicit excludes for database, sharedpref, datastore | closed |
| T-01-09 | Information Disclosure | Backup exclusions reference wrong DB | medium | mitigate | Acceptance greps pin real name vocab_database | closed |
| T-01-10 | Denial of Service | non-suspend DAO write | medium | mitigate | Room main-thread guard active | closed |
| T-01-SC2 | Tampering | Robolectric test dependency | high | mitigate | Maven Central, testImplementation scope only | closed |
| T-01-11 | Tampering | Card update crash | high | mitigate | recordReview wraps both writes in db.withTransaction | closed |
| T-01-12 | Information Disclosure | Destructive v8 migration | medium | accept | Documented, curriculum auto re-seeds | closed |
| T-01-13 | Tampering | SyncManager placeholder telemetry | medium | accept | Placeholder explicit TODO | closed |
| T-01-14 | Denial of Service | Seconds/millis unit mismatch | medium | mitigate | All call sites converted to millis | closed |
| T-01-15 | Denial of Service | Malformed JSON crash | high | mitigate | Specific catches -> Result.failure; zero generic catch-alls | closed |
| T-01-16 | Tampering | Silent null/empty fallbacks | medium | mitigate | Decode failure logged + typed | closed |
| T-01-17 | Information Disclosure | Parse-error logs leak | low | mitigate | Log exception type/message only | closed |
| T-01-18 | Tampering | Legacy buggy scheduler survives | medium | mitigate | Delete FSRS.kt/Models.kt, v6 is only importable scheduler | closed |
| T-01-19 | Tampering | recordReview partially commits | high | mitigate | Atomicity test forces mid-transaction exception | closed |
| T-01-20 | Denial of Service | Test-harness main-thread allowance | low | accept | Allowance ONLY in in-memory test builders | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| R-01-01 | T-01-04 | JVM executes 1000 scheduler steps in < 1s; acceptable cost for a hard floor guarantee | Team | 2026-07-22 |
| R-01-02 | T-01-07 | Local-only computation; no export path added in this phase; backup exclusion rules (Plan 04) keep logs out of cloud backup | Team | 2026-07-22 |
| R-01-03 | T-01-12 | Explicit user decision D-02 (pre-launch, no production users); documented in VocabDatabase KDoc; curriculum auto re-seeds | Team | 2026-07-22 |
| R-01-04 | T-01-13 | Phase 4 owns the sync contract (SYNC-02); placeholders are explicit TODO(SYNC-02) comments and recorded in SUMMARY Next Steps; local destructive reset already zeroes this data class | Team | 2026-07-22 |
| R-01-05 | T-01-20 | D-22/D-23 explicitly allow the allowance ONLY in in-memory test builders; production guard verified in Plan 04 Task 2 | Team | 2026-07-22 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-07-22 | 22 | 22 | 0 | gsd-security-auditor |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-07-22