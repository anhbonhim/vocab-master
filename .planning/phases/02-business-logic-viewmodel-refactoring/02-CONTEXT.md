# Phase 02: Business Logic & ViewModel Refactoring - Context

**Gathered:** 2026-07-21
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase delivers architectural improvements to the Presentation layer (ViewModels and Compose UI) to ensure maintainability, safe state handling, and robustness. Specifically:
1.  **Extracting Business Logic (ARCH-04):** Moving complex quiz and scheduling logic from `QuizViewModel` into pure Domain Use Cases (e.g., `SubmitReviewUseCase`).
2.  **Standardizing UI State (ARCH-03):** Implementing a strict `Sealed Interface` pattern for UI State (Loading, Success, Error) across ViewModels to eliminate partial state bugs.
3.  **State Survival (UX-02):** Using `SavedStateHandle` in ViewModels to ensure Quiz session progress survives configuration changes (like screen rotation).
4.  **Compose File Splitting (ARCH-01):** Breaking down monolithic Compose files (e.g., `HomeScreen`, `QuizScreen`) into highly granular, reusable Design System components.

**Out of Scope for this phase:** Backend changes, Data layer (Room/API) modifications, FSRS algorithm logic changes (completed in Phase 1).
</domain>

<decisions>
## Implementation Decisions

### Quiz Logic Extraction (ARCH-04)
- **Decision:** Extract business logic into dedicated Use Cases (e.g., `SubmitReviewUseCase`).
- **Rationale:** Aligns perfectly with Clean Architecture, keeps ViewModels lean, isolates testing without Android context, and promotes reusability.

### UI State Management (ARCH-03)
- **Decision:** Utilize a `Sealed Interface/Class` pattern for root UI state (e.g., `UiState.Loading`, `UiState.Success`, `UiState.Error`).
- **Rationale:** Enforces exhaustive state handling at compile time, eliminating bugs associated with unhandled "loading" or "error" edge cases in the UI.

### Quiz State Survival (UX-02)
- **Decision:** Store critical Quiz session state using `SavedStateHandle` within the ViewModel.
- **Rationale:** Ensures session progress is retained automatically across configuration changes (screen rotations) and system-initiated process death, without the overhead of immediate DB persistence.

### Compose File Splitting (ARCH-01)
- **Decision:** Break down monolithic screens into extremely granular Design System components (e.g., specialized buttons, typography wrappers, atomic cards) before composing the larger screen.
- **Rationale:** Prioritizes strict component reusability and atomic design principles over feature-specific chunking, aiming for a robust foundational UI library for the app.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/REQUIREMENTS.md` — Defines active requirements ARCH-01, ARCH-03, ARCH-04, UX-02.
- `.planning/codebase/CONCERNS.md` — Highlights the monolithic screen sizes and the ViewModel complexity issue.
- `.planning/codebase/ARCHITECTURE.md` — Details the intended Clean Architecture flow.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- ViewModels: `QuizViewModel` (currently monolithic), `HomeViewModel`, `SettingsViewModel`.
- UI: Monolithic screens needing breakdown (`HomeScreen.kt`, `QuizScreen.kt`, `SettingsScreen.kt`).

### Established Patterns
- Clean Architecture (app/domain/data separation).
- Hilt for Dependency Injection.
- Coroutines & Flow for asynchronous operations.
</code_context>
