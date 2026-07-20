# Project Research Summary

**Project:** VocabMaster Refactor & Audit
**Domain:** Spaced Repetition Vocabulary App (Jetpack Compose / Clean Architecture / FSRS)
**Researched:** 2026-07-20
**Confidence:** HIGH

## Executive Summary

VocabMaster is an offline-first Android vocabulary learning application utilizing the Free Spaced Repetition Scheduler (FSRS) algorithm. The app's core architecture uses a multi-module Gradle layout split into `app`, `domain`, and `data`. However, the app suffers from severe technological debt: monolithic Compose screens exceeding 900 lines of code, widespread unsafe casting (`as`, `!!`), swallowed generic exceptions, and a critical bug in the FSRS scheduler causing negative stabilities and interval out-of-bounds anomalies.

The recommended approach to stabilizing and refactoring the application is a structured, bottom-up architectural cleanup. This begins by securing the data extraction pipeline and auditing the pure Kotlin FSRS implementation in the domain layer against mathematical golden vectors. Following database transactional and repository reinforcement, presentation logic must be isolated from the UI layer by extracting use cases and implementing strict Unidirectional Data Flow (UDF) with single immutable UI State objects. Finally, the monolithic views will be decomposed into stateless components utilizing safe Kotlin type casting and safe navigations.

Key risks include data corruption during scheduler updates and UI crashes from dynamic asset imports. These will be mitigated via comprehensive JUnit unit testing on the math module, defensive JSON deserialization using kotlinx.serialization with explicit exception handling, and runtime safety checks.

## Key Findings

### Recommended Stack

The project will utilize Jetpack Compose for declarative UI, structured under the Unidirectional Data Flow (UDF) pattern. The build will target Kotlin 2.3.20 using the K2 compiler for advanced type checking, with dependency injection handled via Hilt. Database persistence will be managed via Room 2.7.1, with coroutines and Flow handling asynchronous data retrieval and preventing UI blocking.

**Core technologies:**
- **Jetpack Compose (BOM 2026.03.01):** UI Framework — Decouples UI rendering from state management using a reactive declarative paradigm.
- **Kotlin 2.3.20 (K2 Compiler):** Programming Language — Provides K2 smart casting and type safety, eliminating unsafe casts.
- **Room 2.7.1:** Local Persistence — Offers compile-time query verification and seamless Kotlin Flow/suspend integration for offline-first capabilities.
- **DataStore 1.1.1:** Key-Value Storage — Modern, asynchronous, and type-safe replacement for SharedPreferences.
- **Hilt 2.60.1:** Dependency Injection — Standardized DI tool for Android, simplifying scoping and dependency management across architectural layers.
- **Kotlinx Serialization 1.7.3:** JSON Parsing — Highly efficient, type-safe compile-time JSON parser to replace error-prone Gson libraries.

### Expected Features

The user expectation centers around a highly robust, secure, and visually appealing spaced repetition learning loop.

**Must have (table stakes):**
- **FSRS Algorithm Integrity:** Absolute mathematical scheduling accuracy with no out-of-bounds intervals or negative stabilities.
- **Crash-Free Experience:** Zero crashes arising from navigation argument passing or parsing; elimination of force-unwraps (`!!`) and unsafe casts.
- **Smooth UI/UX:** Jank-free interface with smooth screen transitions and clear state updates.
- **Data Privacy:** Explicit cloud backup rules to prevent sensitive local database and key-value preferences from leaking via default backups.

**Should have (competitive):**
- **Clean Architecture UI:** Granular, testable Compose layouts isolated from ViewModel/domain logic.

**Defer (v2+):**
- **Multi-module Refactoring:** Postponed since the existing Gradle module boundaries (app, domain, data) are already validated.
- **Complete Backend Replacement:** Postponed; keep the FastAPI backend and limit fixes to synchronization contract adjustments.
- **New Study Modes:** Avoid adding new learning modes to maintain focus on stabilizing core features.

### Architecture Approach

The architecture relies on Clean Architecture principles, isolating the mathematical FSRS algorithm as a pure Kotlin module in the domain layer. The data layer acts as the offline-first repository, persisting changes in Room, while the presentation layer handles UI state transitions.

**Major components:**
1. **FSRS Engine (domain):** A pure math module that calculates stabilities, difficulties, intervals, and due dates. Knows nothing about Android or persistence.
2. **Study Use Cases (domain):** Use cases like `SubmitReviewUseCase` that coordinate fetching due cards, processing grades via the engine, and updating persistence.
3. **Card/Review Repository (data):** Implements domain repository interfaces, persisting card state and appending to immutable review logs via Room.
4. **Study ViewModels (app):** ViewModels that manage active session queues and UI-specific state, forwarding user input to domain use cases.
5. **Sync Manager (data):** Coordinates remote data syncing with the FastAPI backend without losing spaced repetition precision.

### Critical Pitfalls

1. **Swallowed Exceptions in Asset Parsing:** Generic try/catch blocks hide data parsing bugs. Avoid by catching specific `SerializationException` and mapping to a `UiState.Error` status.
2. **Unsafe Casts/Unwrapping in Compose UI:** Using `as` or `!!` causes crashes. Avoid by adopting Kotlin K2 compiler smart casts (`is`) and safe casts (`as?`) with fallback defaults.
3. **Monolithic Screen Composables:** Keeping 500-1000 line Composable files leads to performance jank and makes previewing/testing impossible. Avoid by splitting screens into top-level stateful "Screen" connectors and stateless "Content" components.
4. **Android Backup Data Leakage:** Automatic cloud backups expose local Room databases containing PII/learning history. Avoid by defining `data_extraction_rules.xml` to exclude databases and preferences.
5. **DAO Threading Violations:** Database access on the main thread throws `IllegalStateException`. Avoid by marking DAO write operations as `suspend` and utilizing `Flow` for reactive reads.

## Implications for Roadmap

Based on research, suggested phase structure for the refactoring process:

### Phase 1: Security & Database Stabilization
**Rationale:** Fixing the scheduler math and setting up secure local storage forms the foundation of the app. UI refactoring is useless if the underlying data layer is corrupted.
**Delivers:** A fully verified and unit-tested FSRS scheduler engine, Room DAOs utilizing suspend/Flow, atomic transaction logic for cards and review logs, and configured XML data extraction rules.
**Addresses:** FSRS Algorithm Integrity, Data Privacy, and DAO Threading Violations.
**Avoids:** Scheduling math errors (negative stability), main thread DB queries, and data backup leaks.

### Phase 2: Business Logic & ViewModel Refactoring
**Rationale:** ViewModels must be decoupled from business and scheduling logic before UI files are disassembled.
**Delivers:** Clean ViewModels, use case classes (e.g. `SubmitReviewUseCase`), and defined immutable `UiState` structures.
**Addresses:** Clean Architecture UI and Exception Swallowing.
**Avoids:** UI-Bound Algorithm State, God-Object ViewModels, and swallowed JSON parsing exceptions.

### Phase 3: Compose UI Refactoring & Polish
**Rationale:** Once ViewModels expose clean state flows, we can disassemble the massive screens and resolve casting issues in the UI.
**Delivers:** Decoupled, stateless Composable sub-elements, type-safe navigation parameters, and a polished design system implementation.
**Addresses:** Smooth UI/UX, Monolithic Screen Composables, and Unsafe Casts.
**Avoids:** Monolithic screens, `ClassCastException`/`NullPointerException` UI crashes, and UI jank.

### Phase 4: Sync & Integration Verification
**Rationale:** Backend sync must be audited and verified only after local database states, transactions, and UI loops have been fully stabilized.
**Delivers:** Robust `SyncManager` network handlers with retry logic and conflict resolution via immutable review logs.
**Addresses:** Data sync flow verification and FastAPI backend integration.
**Avoids:** Desynchronization between local and server states.

### Phase Ordering Rationale

- **Math and DB First:** The FSRS scheduler is the heart of the app. We verify it first with unit tests and secure local storage before touching presentation.
- **VM before View:** By extracting use cases and defining clean UI states first, we ensure that screen refactoring is limited to layout changes rather than changing business rules on the fly.
- **Sync Last:** A correct sync flow relies on correct local transaction and review log data. Consequently, syncing is deferred until database structures are final.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1 (Domain/Data):** Needs `/gsd-plan-phase --research-phase 1` to detail FSRS parameter specifications and confirm mathematical correctness.
- **Phase 3 (UI Refactor):** Needs `/gsd-plan-phase --research-phase 3` to verify Navigation Compose 3.0 type-safe argument passing APIs.
- **Phase 4 (Sync):** Needs `/gsd-plan-phase --research-phase 4` to audit network conflict resolution patterns and API payload schemas.

Phases with standard patterns (skip research-phase):
- **Phase 2 (ViewModels):** Standard, well-documented MVVM/UDF architecture patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | The selected technology versions are standard, modern choices for 2026 Android development. |
| Features | HIGH | Directly maps to the core requirements of VocabMaster. |
| Architecture | HIGH | Offline-first Clean Architecture with pure Kotlin domain layers is a proven industry standard. |
| Pitfalls | HIGH | The identified pitfalls (e.g., swallowed exceptions, unsafe casts) directly address the issues observed in the codebase. |

**Overall confidence:** HIGH

### Gaps to Address

- **FSRS Mathematical Parity:** The exact mathematical cause of the negative stabilities requires deep investigation of `FSRS.kt` implementation.
- **Navigation Compose 3.0 Integration:** The type-safe navigation contract configuration details must be detailed during Phase 3 planning.

## Sources

### Primary (HIGH confidence)
- Official Jetpack Compose Documentation — state hoisting, UDF patterns, and recomposition optimization.
- Android developer backup guide — `dataExtractionRules` configuration.
- Kotlin 2.3 Language Specification — K2 smart cast improvements.

### Secondary (MEDIUM confidence)
- [Flashcards Open Source App (GitHub)](https://github.com/kirill-markin/flashcards-open-source-app) — FSRS Implementation and parity testing.
- [VocabVault Architecture (GitHub)](https://github.com/alireza-malek/vocabvault) — Offline-first local database architectures.

### Tertiary (LOW confidence)
- [StudyBuddy Architecture (GitHub)](https://github.com/giovergos/study-buddy) — Core SM-2 repositories.

---
*Research completed: 2026-07-20*
*Ready for roadmap: yes*