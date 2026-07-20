<!-- refreshed: 2026-07-20 -->
# Architecture

**Analysis Date:** 2026-07-20

## System Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer (app)                 │
├──────────────────┬──────────────────┬───────────────────────┤
│    Activities    │   UI (Compose)   │      ViewModels       │
│`app/src/main/`   │`app/src/main/`   │`app/src/main/`          │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer (domain)                    │
│                `domain/src/main/java/`                      │
├─────────────────────────────────────────────────────────────┤
│   Use Cases      │     Models       │    FSRS Logic         │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer (data)                        │
│                 `data/src/main/java/`                       │
├─────────────────────────────────────────────────────────────┤
│  Repositories    │   Room DB        │    API / Remote       │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                       Backend (FastAPI)                       │
│                         `backend/app/`                        │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `MainActivity` | Entry point for Android App, Sets up Compose UI | `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt` |
| `VocabApplication` | Application class, Hilt setup, global state init | `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt` |
| `ViewModels` | Manage UI state, interact with Domain layer | `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/` |
| `UseCases` | Encapsulate business logic, orchestrate repositories | `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/` |
| `Repositories` (Interfaces) | Define data access contracts | `domain/src/main/java/com/nhimz/vocabmaster/domain/model/` |
| `Repositories` (Impls) | Implement data access (Room, API, etc.) | `data/src/main/java/com/nhimz/vocabmaster/data/repository/` |
| `FSRS` | Spaced Repetition logic | `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/` |
| `Backend Routers` | FastAPI endpoints | `backend/app/routers/` |
| `Backend Services`| Backend business logic | `backend/app/services/` |

## Pattern Overview

**Overall:** Clean Architecture / MVVM (Android) + REST API (Backend)

**Key Characteristics:**
- **Separation of Concerns:** Clear boundary between UI (app), Business Logic (domain), and Data source (data) via Gradle modules.
- **Dependency Inversion:** UI depends on Domain, Data depends on Domain (implements interfaces defined in Domain).
- **Reactive UI:** Compose UI reacts to StateFlows emitted by ViewModels.
- **Dependency Injection:** Hilt is used extensively to manage dependencies.

## Layers

**Presentation Layer (app):**
- Purpose: Display UI, handle user input, manage UI state.
- Location: `app/src/main/java/com/nhimz/vocabmaster/`
- Contains: Activities, Compose UI components, ViewModels, notifications, audio playback.
- Depends on: `domain`, `data` (indirectly via DI)
- Used by: User

**Domain Layer (domain):**
- Purpose: Core business logic, models, and repository interfaces. Independent of Android framework.
- Location: `domain/src/main/java/com/nhimz/vocabmaster/domain/`
- Contains: Use cases, business models, FSRS algorithm, repository interfaces.
- Depends on: None (pure Kotlin)
- Used by: `app`, `data`

**Data Layer (data):**
- Purpose: Data retrieval and persistence (Local DB, Remote API).
- Location: `data/src/main/java/com/nhimz/vocabmaster/data/`
- Contains: Room Database, DAOs, Repository implementations, API clients, sync logic.
- Depends on: `domain`
- Used by: `app` (via DI providing implementations to ViewModels/UseCases)

**Backend Layer:**
- Purpose: Central server for sync, auth, etc.
- Location: `backend/app/`
- Contains: FastAPI application, routers, services, schemas, models.
- Depends on: External DB (likely PostgreSQL/SQLite), third-party services.
- Used by: `data` layer (Remote API calls)

## Data Flow

### Primary Request Path (e.g., Fetching Vocabulary)

1. **User Action:** User opens Quiz screen in Compose (`app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt`)
2. **ViewModel Intent:** Compose calls a function on `QuizViewModel` (`app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt`)
3. **Use Case Execution:** ViewModel invokes a UseCase or Repository method (e.g., `vocabularyRepository.getWordsForReview()`)
4. **Data Retrieval:** Repository Implementation in `data` layer fetches data from Room DB (`VocabDatabase`) or Remote API.
5. **State Update:** Data is returned as a Flow or suspend function result, ViewModel updates its `StateFlow`.
6. **UI Recomposition:** Compose observes the `StateFlow` and recomposes to display the new data.

## Key Abstractions

**Repositories:**
- Purpose: Abstract data sources from the rest of the application.
- Examples: `VocabularyRepository`, `ReviewRepository`, `SettingsRepository` in `domain/src/main/java/com/nhimz/vocabmaster/domain/model/`
- Pattern: Repository Pattern

**Use Cases (Interactors):**
- Purpose: Encapsulate specific business rules.
- Examples: Found in `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/`
- Pattern: Command Pattern / Use Case

## Entry Points

**Android App:**
- Location: `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt`
- Triggers: User launching the app.
- Responsibilities: Initialize UI, Setup Navigation.

**Backend API:**
- Location: `backend/app/main.py` (assumed based on standard FastAPI structure, specific entry point might vary but routers are in `backend/app/routers/`)
- Triggers: HTTP Requests from the Android app.
- Responsibilities: Route requests, handle business logic, interact with DB.

## Architectural Constraints

- **Dependency Rule:** Source code dependencies must point inwards towards the Domain layer. `app` and `data` know about `domain`, but `domain` knows nothing about them.
- **Framework Independence:** The `domain` module should contain pure Kotlin code, no Android specific dependencies (like `android.content.Context` or Room annotations).
- **Asynchrony:** Heavy operations (DB access, network) must be offloaded from the main thread using Kotlin Coroutines and Flows.

## Error Handling

**Strategy:** Expected to use a `Result` wrapper class (e.g., `kotlin.Result` or a custom sealed class `Resource<T>`) to propagate success/error states from the Data layer up to the Presentation layer, where ViewModels can translate them into user-friendly error messages or UI states.

## Cross-Cutting Concerns

**Dependency Injection:** Dagger Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject`)
**Concurrency:** Kotlin Coroutines and Flows
**Logging:** Custom `LocalLogger` in `app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt` and standard `Log`.

---

*Architecture analysis: 2026-07-20*
