# Phase 3: Compose UI Refactoring & Polish - Context

**Gathered:** 2026-07-21
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
- **D-01: Container/Content Separation.** Each major screen (Home, Settings, Quiz) must be split into:
  - A stateful `*Screen` (Container) that handles ViewModel interactions, UI event callbacks, dynamic Route arguments, and Hilt injection.
  - A stateless `*Content` component that only receives state and event lambdas, making it previewable and unit-testable.
- **D-02: Granular Widget Hierarchy.** Deconstruct massive screen UI blocks into small reusable widgets inside `app/src/main/java/com/nhimz/vocabmaster/ui/components/`.

### 2. Type Safety & Casting (ARCH-02)
- **D-03: Zero Unsafe Operations.** No Kotlin `!!` assertions or raw `as` casts in the presentation layer. Use smart cast mechanisms, sealed interface states (`QuizUiState`), safe casts `as?`, and fallback values via Elvis operators `?:`.

### 3. Type-Safe Navigation (UX-01)
- **D-04: Serialization Routes.** Migrate navigation definitions to standard `@Serializable` objects/data classes. Replace the legacy custom `Screen` sealed class in `ui/navigation/Screen.kt` with modern Kotlin Serialization routes mapped by `NavHost`.

### 4. SavedStateHandle State Survival (UX-02)
- **D-05: SavedStateHandle Integration.** Store core quiz state properties (`currentIndex`, list of active questions, answered states) in `SavedStateHandle` to preserve user progress across device rotation.

### 5. Standardized Duolingo Design System (UI-01, UX-03)
- **D-06: Custom 3D Components.** Implement benched 3D styling (e.g., `DuolingoButton` and `DuolingoCard`) that features a solid bottom shadow (ledge) which offsets vertically on click/pressed state to mimic tactile interaction.
- **D-07: Light & Dark Schemes.** Standardize all brand colors (DuolingoGreen, DuolingoGold, DuolingoGray) in `ui/theme/Color.kt` and integrate them systematically into `ColorScheme` in `ui/theme/Theme.kt` to ensure seamless adaptiveness.
- **D-08: High-Fidelity Feedback Animations.** Provide active feedback in the quiz cards: shake animation on incorrect answer submission, pop/scale/success banner animations on correct answers, and smooth progress transitions.

### the agent's Discretion
- The exact layout arrangement of decomposed content (e.g. padding details, grid arrangements, specific icon styles) is delegated to the planning and implementation agents, provided it respects the Duolingo style guidelines.
- The choice of animation libraries or custom modifier canvas operations for the 3D button press depth is left to the planner.
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
- **`DuolingoProgressBar` & `DuolingoOptionCard`** (`app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/`) — existing custom UI assets carrying the target theme. Can be standardized and generalized.
- **`FeedbackHelper`** (`app/src/main/java/com/nhimz/vocabmaster/ui/util/FeedbackHelper.kt`) — helper utility for haptic and audio feedback. Ensure it is integrated cleanly without swallowing exceptions.

### Established Patterns
- **Jetpack Compose MVVM & Clean Architecture:** App module holds presentation. Domain handles use cases (already extracted for quiz sessions in Phase 2). Data module handles persistence.
- **Sealed Interfaces for State:** `QuizUiState.kt` exposes UDF states. Standardize other viewmodels to expose immutable sealed state interfaces.

### Integration Points
- **`VocabMasterApp` navigation host:** NavHost and route navigation hooks are implemented in `VocabMasterApp.kt` and `MainActivity.kt`.
- **Card and Question models:** Room database entities map directly to domain models. Compose screens must consume domain model types instead of casting raw payloads.
</code_context>

<specifics>
## Specific Ideas
- The shake animation can be implemented using a Compose `Animatable(0f)` with a coroutine triggering a series of keyframe offsets (e.g. `0f -> -20f -> 20f -> -10f -> 10f -> 0f`) on submission of an incorrect answer.
- Check and fix any implicit dependency conflicts in `libs.versions.toml` if adding navigation serialization features.
</specifics>

<deferred>
## Deferred Ideas
- **Additional custom study screens:** Defer layout for stats charts, customizable cards, or v2 mnemonics to milestone 2.
- **Sync status visual badges:** Interactive network retry animations or local sync conflict UI cards belong to Phase 4 (Sync Verification).
</deferred>

---
*Phase: 3-Compose UI Refactoring & Polish*
*Context gathered: 2026-07-21*
