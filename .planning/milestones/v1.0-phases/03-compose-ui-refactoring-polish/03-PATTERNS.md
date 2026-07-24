# Phase 3 Pattern Map

**Generated:** 2026-07-22

## UI Patterns

### 1. Route Container / Content Split
- **Source:** Compose Best Practices
- **Role:** Strict separation of DI/Navigation from pure UI rendering
- **Analog in Codebase:** Currently missing. Must be introduced.
- **Pattern:**
  ```kotlin
  @Composable
  fun ScreenRoute(
      viewModel: ScreenViewModel = hiltViewModel(),
      onNavigate: (Route) -> Unit
  ) {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      ScreenContent(
          state = state,
          onEvent = { event -> viewModel.handleEvent(event) }
      )
  }
  ```

### 2. Type-Safe Navigation
- **Source:** Navigation Compose 2.8.0
- **Role:** Route definitions
- **Analog in Codebase:** Replaces sealed class `Screen`.
- **Pattern:**
  ```kotlin
  @Serializable
  data class ScreenRoute(val id: String)
  ```
