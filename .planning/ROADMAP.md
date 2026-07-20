# Roadmap: VocabMaster Refactor & Audit

## Overview

VocabMaster is being refactored and audited to build a robust local persistence layer, guarantee 100% mathematical correctness of its FSRS Spaced Repetition engine, establish type-safe Compose architectures with modern visual feedback, and secure local backups. This roadmap structures the transition from the current monolithic and bug-prone codebase into a highly stable and polished production-ready application.

## Phases

- [ ] **Phase 1: Security & Database Stabilization** - Verify FSRS scheduler calculations with unit tests, configure secure backups, and migrate Room DAOs to Flow/suspend.
- [ ] **Phase 2: Business Logic & ViewModel Refactoring** - Decouple ViewModels by moving domain logic to UseCases and implementing strict Unidirectional Data Flow (UDF).
- [ ] **Phase 3: Compose UI Refactoring & Polish** - Disassemble monolithic screen layouts, adopt safe Kotlin casts, and implement a modern design system with rich UX transitions.
- [ ] **Phase 4: Sync & Integration Verification** - Audit Remote Sync payload logic and verify conflict resolution flow with FastAPI.

---

## Phase Details

### Phase 1: Security & Database Stabilization
**Goal**: Establish data safety, secure user database contents from default backups, and ensure mathematical correctness of the FSRS scheduling engine.
**Depends on**: Nothing (Initial Phase)
**Requirements**: FSRS-01, FSRS-02, FSRS-03, FSRS-04, FSRS-05, PERS-01, PERS-02, PERS-03, PERS-04
**Success Criteria** (what must be TRUE):
  1. The app excludes sensitive databases and shared preferences from auto-backup according to XML configurations.
  2. All FSRS tests run successfully without producing negative stability or interval values.
  3. String/Locale formats do not cause NumberFormatExceptions in non-US locale runtime tests.
  4. Database operations utilize Kotlin Coroutines/Flows, executing off the main thread.
**Plans**: 2 plans

Plans:
- [ ] 01-01: FSRS Scheduler Audit and Parity Unit Tests
- [ ] 01-02: Secure Backup Rule Setup and Room Threading/Transaction Enforcement

### Phase 2: Business Logic & ViewModel Refactoring
**Goal**: Hoist logic out of presentation files, separating database queries and scheduling decisions from UI ViewModels.
**Depends on**: Phase 1
**Requirements**: ARCH-03, ARCH-04
**Success Criteria** (what must be TRUE):
  1. ViewModels do not contain raw SQL/Room query queries and use domain UseCases instead.
  2. Presentation states are modeled as single, immutable UiState data classes exposed as StateFlow.
  3. Dynamic asset import errors are safely propagated to UI states instead of swallowing exceptions.
**Plans**: 2 plans

Plans:
- [ ] 02-01: Extract Domain UseCases for Study Sessions
- [ ] 02-02: Refactor ViewModels to Unidirectional Data Flow (UDF) Patterns

### Phase 3: Compose UI Refactoring & Polish
**Goal**: Rebuild the UI with a modern design system, decompose monolithic screens, and resolve runtime casting safety issues.
**Depends on**: Phase 2
**Requirements**: ARCH-01, ARCH-02, UX-01, UX-02, UX-03, UI-01
**Success Criteria** (what must be TRUE):
  1. Screen Composables are split into stateful Container and stateless Content components.
  2. All unsafe forced unwraps (`!!`) and unsafe `as` casts are eliminated in the presentation code.
  3. Screen navigation handles argument passing in a type-safe manner.
  4. Quiz screens survive orientation changes without losing current question session states.
  5. UI components use cohesive typography, padding, color palette, and modern response feedback animations.
**Plans**: 3 plans

Plans:
- [ ] 03-01: Decouple Monolithic Screen Composables (HomeScreen & SettingsScreen)
- [ ] 03-02: Refactor QuizScreen and Add UX Feedback/State Transition Animations
- [ ] 03-03: Type-safe Navigation Compose Migration and Theme Standardization

### Phase 4: Sync & Integration Verification
**Goal**: Verify synchronization reliability and API contract correctness.
**Depends on**: Phase 3
**Requirements**: SYNC-01, SYNC-02
**Success Criteria** (what must be TRUE):
  1. SyncManager runs without crashing on network failures, applying retries gracefully.
  2. Client-server data sync does not downgrade or overwrite card scheduling precision.
**Plans**: 1 plan

Plans:
- [ ] 04-01: Network Request Resilience and Sync Payload Contract Verification

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Security & Database Stabilization | 0/2 | Not started | - |
| 2. Business Logic & ViewModel Refactoring | 0/2 | Not started | - |
| 3. Compose UI Refactoring & Polish | 0/3 | Not started | - |
| 4. Sync & Integration Verification | 0/1 | Not started | - |
