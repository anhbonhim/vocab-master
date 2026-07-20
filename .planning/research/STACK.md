# Technology Stack

**Project:** VocabMaster Refactor & Audit
**Researched:** 2026-07-20

## Recommended Stack

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Jetpack Compose | 2026.03.01 (BOM) | UI Framework | Standard declarative UI toolkit for Android. Must be used with Unidirectional Data Flow (UDF). Massive screens should be split into "Screen" (state connector) and "Content" (stateless renderer) composables. |
| Kotlin | 2.3.20 | Language | Standard for Android. K2 compiler offers advanced smart casting (e.g. across `or` checks and inline closures), eliminating the need for many unsafe `as` casts. |
| Hilt | 2.60.1 | Dependency Injection | Official Google recommendation. Reduces boilerplate over Dagger. Provides easy scoping (`@Singleton`, `@HiltViewModel`) for components like Room DBs and ViewModels. |

### Database
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Room | 2.7.1 | Local Persistence | Standard abstraction over SQLite. Seamlessly integrates with Coroutines (`Flow` for reactive reads, `suspend` for one-shot reads/writes). Provides compile-time query verification. |
| DataStore | 1.1.1 | Key-Value Storage | Modern replacement for SharedPreferences. Type-safe and async (using Coroutines/Flow). |

### Infrastructure
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Retrofit / OkHttp | 2.11.0 / 4.12.0 | Networking | Industry standard for type-safe HTTP clients communicating with the FastAPI backend. |
| Kotlinx Coroutines | 1.10.2 | Concurrency | Preferred async mechanism. Standard for Room/Retrofit operations and managing background threads. |
| Kotlinx Serialization | 1.7.3 | JSON parsing | Fast, compile-time safe, multiplatform-ready JSON parser. Better integration with Kotlin features than Gson. |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| UI State Management | `StateFlow` + single `UiState` data class | Multiple `MutableState` variables in Composable | Spreading state across multiple `remember` variables in a UI leads to recomposition bugs, unpredictable UI states, and monolithic composables. A single immutable data class in a ViewModel is the standard UDF pattern. |
| Casting & Type Checks | `as?` (safe cast) or `is` (smart cast) | `as` (unsafe cast) or `!!` (force unwrap) | `as` and `!!` throw `ClassCastException` and `NullPointerException` respectively, leading to app crashes. Kotlin 2.3's improved K2 compiler makes `is` smart casting very powerful; fallback to `as?` with Elvis operators when needed. |
| Backup Strategy | Explicit XML rules via `dataExtractionRules` | Default Auto Backup | Default backup includes sensitive app data (DBs, SharedPreferences). This can leak user credentials or FSRS learning states to Google Drive or during D2D transfers. Must define `data_extraction_rules.xml` to exclude these domains. |
| Exception Handling | Explicit `try/catch` or `runCatching` with specific types | Catching generic `Exception` and swallowing | Swallowing generic exceptions hides bugs (especially in dynamic JSON parsing or FSRS state updates). Must explicitly handle or log specific failures and update UI state (e.g., `UiState.Error`). |

## Installation

```bash
# Handled via gradle/libs.versions.toml
# Ensure KSP is enabled for Room and Hilt instead of KAPT
```

## Sources

- [Context7/official sources] (Medium Confidence based on latest Android Developer Documentation & Community Best Practices 2026)