<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
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

### Deferred Ideas (OUT OF SCOPE)
- **Additional custom study screens:** Defer layout for stats charts, customizable cards, or v2 mnemonics to milestone 2.
- **Sync status visual badges:** Interactive network retry animations or local sync conflict UI cards belong to Phase 4 (Sync Verification).
</user_constraints>
<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ARCH-01 | Refactor large monolithic Compose screens (e.g., `HomeScreen.kt` and `QuizScreen.kt`) into Screen/Content patterns, separating state/events from pure UI layout. | Defined Container/Content pattern boundaries, extracting logic into Route and pure UI to Content. |
| ARCH-02 | Eliminate all unsafe forced unwraps (`!!`) and raw unsafe casts (`as`) in ViewModels and UI, replacing them with safe type casting (`as?`) and Elvis operators. | Confirmed pattern to use `as?` combined with `LocalLogger` and UI state error handling (D-04). |
| UX-01 | Implement smooth navigation transitions using type-safe argument passing APIs. | Identified Kotlin Serialization setup and Navigation Compose 2.8.0 API usage (`toRoute<T>()`). |
| UX-02 | Refactor Quiz flow to handle screen orientation changes without losing active session progress. | Verified `SavedStateHandle` integration in ViewModels via `getMutableStateFlow` and `.saved` delegates. |
| UX-03 | Build user feedback states during quizzes (correct/incorrect answer highlights, card scheduling preview) with high-fidelity visual indicators. | Agent discretion for `Animatable` and transitions, relying on standard Compose animation tools. |
| UI-01 | Standardize spacing, typography, and theme across all Compose screens using a clean, modern design system layout. | D-01 dictates from-scratch 3D UI components; D-03 dictates shared light/dark brand colors. |
</phase_requirements>

# Phase 3: Compose UI Refactoring & Polish - Research

**Researched:** 2026-07-22
**Domain:** Compose UI Architecture, Navigation, State Management
**Confidence:** HIGH

## Summary

This phase focuses on structural modernization and UI polish for the presentation layer. The legacy string-based and custom-sealed class navigation must be completely replaced with Compose Navigation 2.8.0's type-safe APIs using Kotlin Serialization. The large monolithic screens (`HomeScreen.kt`, `QuizScreen.kt`, etc.) must be split into a Route/Screen Container (handling ViewModel and Navigation) and a Content Composable (handling pure, stateless UI). 

Additionally, the UI must be rebuilt from scratch using a Duolingo-style 3D design system, and the Quiz session state must survive configuration changes using `SavedStateHandle`. Finally, all raw type casting (`as`) must be replaced with safe casts (`as?`) and proper error propagation via `UiState.Error` and Snackbars.

**Primary recommendation:** Start by updating dependencies for Kotlin Serialization and Navigation 2.8.0, then migrate `VocabMasterApp.kt` routing, and systematically refactor each major screen into the Route/Content pattern while implementing the new from-scratch 3D UI components.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Navigation Routing | UI Layer (Compose) | — | Navigation graph definition using `NavHost` and Kotlin Serialization. |
| UI State & Event Handling | UI Layer (ViewModel) | — | ViewModels manage UI state flows, intercept events, and hold `SavedStateHandle`. |
| Pure UI Rendering | UI Layer (Compose) | — | Stateless Content composables react strictly to `UiState` and emit callback events. |
| Feedback & Animations | UI Layer (Compose) | — | Managed directly within Compose using `Animatable` and `updateTransition`. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.navigation:navigation-compose` | 2.8.0+ | Type-safe declarative navigation | Replaces legacy string/sealed-class routing with first-class type safety. |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 (verified) | Route payload serialization | Required by Navigation Compose 2.8.0 for type-safe arguments. |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.10.0 | State survival | Persists active quiz state across configuration changes/process deaths. |
| `androidx.compose.material3` | BOM | Core UI components | Provides standard scaffolding, snackbars, and typography base. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom sealed class router | Compose Navigation | Custom routers lack deep linking, proper backstack management, and standard ViewModel scoping. |
| Stateful UI Composables | Container/Content Pattern | Stateful UIs are difficult to preview and test. Container/Content perfectly isolates concerns. |

**Installation:**
```bash
# Ensure plugins are added to app/build.gradle.kts
alias(libs.plugins.kotlin.serialization)

# Ensure dependencies are added
implementation(libs.androidx.navigation.compose)
implementation(libs.kotlinx.serialization.json)
```

## Package Legitimacy Audit

> **Required** whenever this phase installs external packages. Run the Package Legitimacy Gate protocol before completing this section.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | npm (erroneous lookup, exists on Maven Central) | N/A | N/A | github.com/Kotlin/kotlinx.serialization | OK | Approved |
| `androidx.navigation:navigation-compose` | Maven Central | N/A | N/A | android.googlesource.com | OK | Approved |

*Note: The `package-legitimacy` tool queried npm for a Kotlin/Android package resulting in a SLOP verdict. These are official Google/JetBrains Maven dependencies and are inherently legitimate.*

## Architecture Patterns

### Component Deconstruction (Container/Content)

```
[VocabMasterApp (NavHost)]
       │
       ▼
[HomeScreen (Route Container)] ───────► (Injects ViewModel, collects state, handles Nav)
       │
       ▼
[HomeScreenContent (Stateless UI)] ───► (Receives State, emits Callbacks)
       │
       ├─► [HomeHeader]
       ├─► [HomeSectionList]
       └─► [HomeCard]
```

**What:** Separating the "Screen" (which talks to DI and Navigation) from the "Content" (which only draws UI).
**When to use:** For every screen in the app.
**Example:**
```kotlin
// Route Container
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        state = state,
        onAction = { action -> 
            when(action) {
                is ProfileAction.Back -> onNavigateBack()
                else -> viewModel.handleAction(action)
            }
        }
    )
}

// Stateless Content
@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    // Pure UI, highly previewable
}
```

### Anti-Patterns to Avoid
- **Passing ViewModels to Content:** Never pass a ViewModel down into the Content composable or its children. It ruins previewability and couples UI to the DI framework.
- **`!!` and `as`:** Never use forced unwrapping or unsafe casts. Always use `as?` and handle the null case.
- **Handling side-effects in Content:** Navigation and snackbar triggers must be hoisted to the Container.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Route Definitions | Sealed classes with custom `Screen` objects | `@Serializable` classes with `NavHost` | Type safety is handled by the compiler and plugin; integrates with `NavBackStackEntry`. |
| State persistence | Custom SharedPreferences saving for rotations | `SavedStateHandle` | Automatically tied to the ViewModel lifecycle and task stack. |

## Common Pitfalls

### Pitfall 1: Unsafe Cast Failures Crashing the App
**What goes wrong:** `val payload = item as SectionStatus` throws `ClassCastException` if the backend payload changes or the list is heterogeneous.
**Why it happens:** Assuming the type of a generic object without checking.
**How to avoid:** Use `val payload = item as? SectionStatus ?: return@forEach`. Log the failure via `LocalLogger` and emit a `UiState.Error`.

### Pitfall 2: State Loss on Rotation
**What goes wrong:** The user rotates the phone during a quiz, and the current question resets to index 0.
**Why it happens:** The ViewModel retains data, but the active UI state might be bound to the Activity lifecycle if not hoisted properly, or process death clears the ViewModel.
**How to avoid:** Use `SavedStateHandle` to back critical state fields (like `currentIndex`) in the `QuizViewModel`.

## Code Examples

### Type-Safe Navigation setup
```kotlin
// Define Route
@Serializable
data class QuizRoute(val cardIds: List<String>? = null)

// In NavHost
composable<QuizRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<QuizRoute>()
    QuizScreen(
        cardIds = route.cardIds,
        onNavigateBack = { navController.popBackStack() }
    )
}

// In ViewModel
class QuizViewModel(savedStateHandle: SavedStateHandle): ViewModel() {
    val route = savedStateHandle.toRoute<QuizRoute>()
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom Sealed `Screen` routing | Type-Safe Navigation Compose with Kotlinx Serialization | Navigation 2.8.0 | Completely eliminates manual argument passing and string route parsing. |
| Monolithic Compose Files | Container/Content Split | Compose Best Practices | vastly improves previewability and testability of UI components. |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Navigation Compose 2.8.0 is available and compatible with the project's Compose BOM. | Standard Stack | Build failures if versions clash. |

## Open Questions (RESOLVED)

1. **Quiz State Complexity** (RESOLVED)
   - What we know: `QuizUiState` contains complex objects like `List<QuizQuestion>`.
   - What's unclear: Storing a massive list of questions in `SavedStateHandle` might exceed Bundle size limits during process death.
   - Recommendation: Store only the `sessionId` or `nodeId` and the `currentIndex` in `SavedStateHandle`. On recreation, reload the questions from the DB using the ID, then jump to the saved index.

## Environment Availability

Step 2.6: SKIPPED (no external dependencies identified beyond Android SDK/Gradle)

## Sources

### Primary (HIGH confidence)
- [CITED: developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate] - ViewModel SavedStateHandle usage
- [CITED: developer.android.com/guide/navigation/design/type-safety] - Navigation Compose Type Safety

### Secondary (MEDIUM confidence)
- [VERIFIED: websearch/github] - Container/Content pattern best practices

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official Android Jetpack documentation.
- Architecture: HIGH - Industry standard Compose patterns.
- Pitfalls: HIGH - Directly aligned with CONCERNS.md and Phase requirements.

**Research date:** 2026-07-22
**Valid until:** 2026-08-22
<!-- gsd:write-continue -->