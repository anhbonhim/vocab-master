# Requirements: VocabMaster Refactor & Audit

**Defined:** 2026-07-20
**Core Value:** Ensure absolute correctness of the spaced repetition scheduling logic and deliver a highly polished, intuitive, and modern user experience.

## v1 Requirements

Requirements for the refactored and audited application, mapped directly to roadmap phases.

### FSRS Algorithm & Math Engine

- [x] **FSRS-01**: The FSRS scheduler calculations must be mathematically correct, eliminating negative stability and difficulty values.
- [x] **FSRS-02**: All FSRS computations must prevent out-of-bounds intervals, ensuring computed intervals are positive integers.
- [x] **FSRS-03**: FSRS math must be fully localized, removing formatting calls (like `String.format`) that cause decimal separator crashes in non-US locales.
- [ ] **FSRS-04**: Core FSRS scheduling math must be covered by comprehensive unit tests with golden vectors.
- [ ] **FSRS-05**: Port the py-fsrs optimizer (parameter training from review logs) to Kotlin so custom FSRS weights can be trained from user review history. (Moved here from Out-of-Scope during Phase 1 discuss-phase on 2026-07-20 — full py-fsrs port was chosen as the FSRS fix strategy.)

### Database & Local Persistence

- [ ] **PERS-01**: Configure explicit XML data extraction rules (`data_extraction_rules.xml`) to prevent Room DB and Datastore keys from leaking through default cloud backups.
- [ ] **PERS-02**: Ensure all Room DAO operations run on appropriate background threads (using `suspend` for writes and `Flow` for reads) to prevent UI blocking.
- [x] **PERS-03**: Implement database transaction bounds for atomic cards and review logs updates to prevent desynchronization.
- [ ] **PERS-04**: Replace dynamic, unsafe JSON asset parsing fallbacks in repositories with robust error handling and specific exception catching.

### UI & Presentation Layer Architecture

- [ ] **ARCH-01**: Refactor large monolithic Compose screens (e.g., `HomeScreen.kt` and `QuizScreen.kt`) into Screen/Content patterns, separating state/events from pure UI layout.
- [ ] **ARCH-02**: Eliminate all unsafe forced unwraps (`!!`) and raw unsafe casts (`as`) in ViewModels and UI, replacing them with safe type casting (`as?`) and Elvis operators.
- [ ] **ARCH-03**: Expose UI state from ViewModels using structured, immutable `UiState` classes via Kotlin `StateFlow` (UDF pattern).
- [ ] **ARCH-04**: Hoist scheduling and quiz logic from ViewModels into clean domain Use Cases (e.g. `SubmitReviewUseCase`).

### UX Flow & Redesign

- [ ] **UX-01**: Implement smooth navigation transitions using type-safe argument passing APIs.
- [ ] **UX-02**: Refactor Quiz flow to handle screen orientation changes without losing active session progress.
- [ ] **UX-03**: Build user feedback states during quizzes (correct/incorrect answer highlights, card scheduling preview) with high-fidelity visual indicators.
- [ ] **UI-01**: Standardize spacing, typography, and theme across all Compose screens using a clean, modern design system layout.

### Sync & Networking

- [ ] **SYNC-01**: Verify data sync flow and ensure `SyncManager` handles backend request failures gracefully with retry mechanisms.
- [ ] **SYNC-02**: Ensure bidirectional data synchronization does not corrupt or downgrade FSRS card states.

## v2 Requirements

### Advanced AI Integrations

- **AI-01**: AI-generated context mnemonics and dynamic example sentences based on CEFR levels.
- **AI-02**: Extraction of vocabulary directly from PDF or YouTube link inputs.

### Analytics & Custom Decks

- **ANL-01**: Advanced learning progress charts and FSRS parameter tuning dashboard.
- **DK-01**: Custom user-generated vocabulary decks sharing and community reviews.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multi-module restructure | gradle module architecture is already modular (`app`, `domain`, `data`) and functional |
| Full backend rewrite | fastapi backend structure is locked; only adjust endpoints or sync payloads if contracts break |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| FSRS-01 | Phase 1 | Complete |
| FSRS-02 | Phase 1 | Complete |
| FSRS-03 | Phase 1 | Complete |
| FSRS-04 | Phase 1 | Pending |
| FSRS-05 | Phase 1 | Pending |
| PERS-01 | Phase 1 | Pending |
| PERS-02 | Phase 1 | Pending |
| PERS-03 | Phase 1 | Complete |
| PERS-04 | Phase 1 | Pending |
| ARCH-03 | Phase 2 | Pending |
| ARCH-04 | Phase 2 | Pending |
| ARCH-01 | Phase 3 | Pending |
| ARCH-02 | Phase 3 | Pending |
| UX-01 | Phase 3 | Pending |
| UX-02 | Phase 3 | Pending |
| UX-03 | Phase 3 | Pending |
| UI-01 | Phase 3 | Pending |
| SYNC-01 | Phase 4 | Pending |
| SYNC-02 | Phase 4 | Pending |

**Coverage:**

- v1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0 ✓

---
*Requirements defined: 2026-07-20*
*Last updated: 2026-07-20 after initial definition*
