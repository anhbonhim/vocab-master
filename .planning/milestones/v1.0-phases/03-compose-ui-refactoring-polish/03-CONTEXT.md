# Phase 3: Compose UI Refactoring & Polish - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 delivers a complete refactoring and polish of the presentation layer of VocabMaster. Specifically:
1. **Compose Screen Refactoring (ARCH-01):** Deconstruct large monolithic Compose files (`HomeScreen.kt`, `SettingsScreen.kt`, and `QuizScreen.kt`) into clean Screen Container (stateful, managing event callbacks and ViewModels) and Screen Content (stateless, pure UI layout) patterns.
2. **Elimination of Unsafe Casts & Unwraps (ARCH-02):** Eliminate all unsafe forced unwraps (`!!`) and raw unsafe casts (`as`) in the presentation code, replacing them with safe casting (`as?`), smart casts via sealed states, and Elvis operators (`?:`).
3. **Type-Safe Compose Navigation (UX-01):** Migrate the navigation from the current custom sealed class `Screen` structure in `VocabMasterApp.kt` to the official Jetpack Compose Navigation Type-Safe APIs utilizing Kotlin Serialization.
4. **Quiz State Survival (UX-02):** Re-architect `QuizViewModel` to preserve quiz session states (e.g. current question index, history, user input selections) across configuration changes (screen rotations) and process deaths utilizing `SavedStateHandle`.
5. **Rich Feedback & Animations (UX-03):** Enhance user feedback inside the Quiz flow with high-fidelity visual indicators, shake animations on incorrect answers, scale/Lottie animations on correct answers, and smooth transitions.
6. **Design System Standardization (UI-01):** Define and integrate a standardized Duolingo-style design system with custom 3D buttons, 3D cards, proper typography, unified spacing, and adaptive color schemes supporting both Light and Dark modes.

**Out of scope for this phase:**
- Database modifications or local persistence logic changes (already stabilized in Phase 1).
- ViewModel domain/logic separation (already refactored into Use Cases in Phase 2).
- Sync conflict resolution or remote FastAPI schema verification (belongs to Phase 4).
- Adding new curricula or study features beyond existing ones.
</domain>

<decisions>
## Implementation Decisions

### 1. Architectural Pattern & Deconstruction (ARCH-01)
- **D-01:** **Create components from scratch.** Instead of trying to bend existing components (like `DuolingoProgressBar`, `DuolingoOptionCard`), build entirely new stateless UI components from scratch that strictly adhere to the 3D Duolingo design system. This ensures maximum consistency and avoids inheriting legacy technical debt or incorrect padding/sizing from older iterations.

### 2. Type-Safe Navigation (UX-01)
- **D-02:** **Convert everything at once.** Replace the entire legacy `Screen` sealed class routing in `VocabMasterApp.kt` and all dependent screens (Home, Quiz, Settings) with Kotlin Serialization-based type-safe routes in a single cohesive update.

### 3. Design System & Theme (UI-01)
- **D-03:** **Shared color palette for Light/Dark mode.** Maintain the core Duolingo colors (Green, Gold, Gray). For Dark Mode, simply darken the background surfaces, keeping the brand colors consistent rather than introducing entirely new neon or pastel palettes.

### 4. Null-Safety & Error Handling (ARCH-02)
- **D-04:** **Log and display errors gracefully.** When safe casts (`as?`) fail (e.g., parsing a payload on card click), do not silently swallow the error. Push the state to `UiState.Error` with a user-friendly message, log the detailed stack trace via `LocalLogger`, and display a Snackbar to the user.

### 5. Error & Success Notifications (UX-03)
- **D-05:** **Prioritize Snackbar.** Use non-blocking Snackbars at the bottom of the screen for routine errors or invalid actions. Reserve center-screen Dialogs only for critical, destructive actions requiring explicit user confirmation.

### the agent's Discretion
- **Quiz State Survival (UX-02):** The agent will determine the most optimal balance of what state properties (index, history, answers) to store in `SavedStateHandle` vs what to re-query from the DB, prioritizing memory efficiency and correct rotation survival.
- **Feedback Animations (UX-03):** The agent is given full creative freedom to design and implement the exact choreographies (e.g., using `Animatable` or transitions) for correct/incorrect answers, aiming for the most fluid, native-feeling feedback possible.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Navigation & Compose Architecture
- `https://developer.android.com/guide/navigation/design/type-safety` — Type safety in Kotlin DSL and Navigation Compose.
- `https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate` — Save UI state with SavedStateHandle.

### Project-Internal Files
- `.planning/REQUIREMENTS.md` — Defines ARCH-01, ARCH-02, UX-01, UX-02, UX-03, UI-01 requirements.
- `.planning/ROADMAP.md` — Lists Phase 3 goals and success criteria.
- `.planning/codebase/CONCERNS.md` — Details monolithic screen issues and unsafe cast warnings.
- `app/src/main/java/com/nhimz/vocabmaster/ui/navigation/Screen.kt` — Legacy custom navigation structure.
- `app/src/main/java/com/nhimz/vocabmaster/ui/VocabMasterApp.kt` — Navigation graph entry point.
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/HomeScreen.kt` — Monolithic screen (995 lines) to be split.
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt` — Monolithic screen to be split.
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt` — Monolithic screen to be split.
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt` — ViewModel handling Quiz state and SavedStateHandle candidate.
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`LocalLogger`** (`app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`) — Core logging utility to capture failed safe casts (D-04).

### Established Patterns
- **Jetpack Compose MVVM & Clean Architecture:** App module holds presentation.
- **Sealed Interfaces for State:** `QuizUiState.kt` exposes UDF states. Standardize other viewmodels to expose immutable sealed state interfaces.

### Integration Points
- **`VocabMasterApp` navigation host:** NavHost and route navigation hooks must be completely overhauled for Kotlin Serialization (D-02).
</code_context>

<specifics>
## Specific Ideas
- All UI component creation must start from scratch for Phase 3 (D-01) to ensure perfect Duolingo 3D styling without inheriting old padding/margin quirks.
</specifics>

<deferred>
## Deferred Ideas
- **Additional custom study screens:** Defer layout for stats charts, customizable cards, or v2 mnemonics to milestone 2.
- **Sync status visual badges:** Interactive network retry animations or local sync conflict UI cards belong to Phase 4 (Sync Verification).
</deferred>

---

*Phase: 03-compose-ui-refactoring-polish*
*Context gathered: 2026-07-22*
