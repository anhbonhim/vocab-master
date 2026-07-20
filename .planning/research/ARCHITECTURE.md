# Architecture Patterns: Spaced Repetition Apps

**Domain:** Android Spaced Repetition (Jetpack Compose / Clean Architecture / FSRS)
**Researched:** 2026-07-20

## Recommended Architecture

Modern spaced repetition applications heavily favor **Clean Architecture with offline-first local persistence** (Room/SQLite), where the complex scheduling algorithm (like FSRS or SM-2) is isolated as a pure mathematical module within the Domain layer. 

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Presentation Layer (app)                        │
│                                (Jetpack Compose)                            │
├─────────────────────┬─────────────────────────┬─────────────────────────────┤
│  StudySession Flow  │  Deck/List Management   │   Algorithm Tuning / Debug  │
│ (QuizScreen + VM)   │ (HomeScreen + VM)       │  (SettingsScreen + VM)      │
└────────┬────────────┴────────────┬────────────┴────────────┬────────────────┘
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               Domain Layer (domain)                         │
│                                  (Pure Kotlin)                              │
├─────────────────────┬─────────────────────────┬─────────────────────────────┤
│  Study UseCases     │   Data Interfaces       │       FSRS Engine           │
│ (SubmitReviewUseCase) (VocabularyRepository)  │ (calculate(card, grade))    │
└────────┬────────────┴────────────┬────────────┴────────────┬────────────────┘
         │                         │                         │
         ▼                         ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               Data Layer (data)                             │
│                                (Room + Retrofit)                            │
├──────────────────────────────────┬──────────────────────────────────────────┤
│           Local DB (Room)        │             Sync / Remote API            │
│  (Cards, Decks, ReviewHistory)   │       (FastAPI SyncManager)              │
└──────────────────────────────────┴──────────────────────────────────────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **Spaced Repetition Engine (FSRS)** | Pure math module. Calculates new stability, difficulty, interval, and due dates based on the previous state and user grade (`Again`, `Hard`, `Good`, `Easy`). | Called by `SubmitReviewUseCase`. Knows nothing about Android, Room, or UI. |
| **Study UseCases** | Orchestrates a review. Fetches due cards, passes user responses to the FSRS engine, and tells the Repository to persist the updated card and review log. | `FSRS Engine`, `ReviewRepository`, `CardRepository`. |
| **Review / Card Repository (Room)** | The Source of Truth. Stores card data, current FSRS state (stability, difficulty, elapsed days), and an append-only log of every review event. | Implements interfaces from Domain. Backed by Room DAOs. |
| **Study ViewModel (QuizViewModel)** | Holds the active session queue, manages the reveal state (front/back), and buffers UI feedback. Should NOT calculate spaced repetition logic. | Observes `CardRepository` flows, calls `Study UseCases`. |
| **Sync Manager** | Handles bidirectional or server-authoritative sync. Spaced repetition data (intervals, review logs) must sync reliably without losing precision. | `Backend API`, `Local DB (Room)`. |

### Data Flow: The Review Loop

The core flow of any spaced repetition app is the study loop. 

1. **Load Due Cards:** `QuizViewModel` requests due cards from `CardRepository` (via a UseCase) based on current timestamp vs card `due_date`.
2. **Display & Grade:** UI shows the front, user reveals the back, user selects a grade (e.g., `Good`).
3. **Calculate:** `ViewModel` dispatches the grade to `SubmitReviewUseCase`. The UseCase passes the card's current FSRS metrics and the grade to the `FSRS Engine`.
4. **Persist:** The `FSRS Engine` returns a *new* card state (new interval, new stability, new due date) and a *Review Log* entry. The UseCase saves both atomically in a Room Transaction via the Repository.
5. **Next Card:** The UI reacts to the updated queue or state flow, animating to the next card.

## Patterns to Follow

### Pattern 1: Pure Mathematical Domain Model
**What:** The spaced repetition algorithm (FSRS) must be entirely decoupled from Android frameworks and persistence layers. 
**When:** Always.
**Example:**
```kotlin
// In domain module - Pure Kotlin, easily unit testable with golden vectors
fun calculate(cardState: FsrsState, grade: Grade): FsrsResult {
    // Pure math operations
    return FsrsResult(newStability, newDifficulty, newIntervalDays)
}
```

### Pattern 2: Immutable Review Logs
**What:** Every time a user grades a card, append a record to a `review_logs` table (Timestamp, CardID, Grade, previous State, new State) in addition to updating the card's current state.
**Why:** Critical for analytics, undo functionality, algorithm training/tuning in the future, and resolving sync conflicts.

### Pattern 3: Granular Compose State Management
**What:** Split monolithic ViewModels. A `QuizViewModel` should manage the UI state of the session (card queue, current side, animation triggers) and delegate the actual business logic (processing the grade) to UseCases.

## Anti-Patterns to Avoid

### Anti-Pattern 1: UI-Bound Algorithm State
**What:** Calculating intervals, stability, or difficulty directly inside `QuizViewModel` or, worse, inside a Compose screen based on UI events.
**Why bad:** Makes the core value of the app (the algorithm) untestable in isolation, prone to lifecycle bugs, and impossible to run in background sync tasks.
**Instead:** Keep FSRS logic strictly in the Domain layer as pure functions.

### Anti-Pattern 2: Destructive Updates (No Logs)
**What:** Updating a card's interval and due date without saving a history of the review event.
**Why bad:** If the algorithm has a bug (like the negative stability anomaly in VocabMaster), you have no historical data to reconstruct the correct state or debug *how* it reached that state.
**Instead:** Use Pattern 2 (Immutable Review Logs) and atomic transactions.

### Anti-Pattern 3: God-Object ViewModels & Screens
**What:** A `HomeScreen.kt` with 900+ lines doing data parsing, UI layout, and state manipulation.
**Why bad:** Compose recomposition becomes unpredictable, performance suffers, and null-safety issues (`!!`, raw `as` casts) proliferate to handle complex local state.
**Instead:** Break screens down into layout components (`DeckList`, `StatsCard`) and hoist business state to scoped ViewModels.

## Scalability Considerations

| Concern | At 100 Cards | At 10K Cards | At 1M Reviews |
|---------|--------------|--------------|-------------|
| **Initial Load** | Parse JSON asset entirely. | Chunked JSON parsing or direct SQLite prepopulation (avoid blocking main thread). | Must use paginated DB queries; cannot hold all cards in memory. |
| **Due Card Queries** | `SELECT * WHERE due < now` is fine. | Requires index on `due_date` and `deck_id` in Room. | Requires index on `due_date`; fetch in batches of ~50 for study sessions. |
| **Syncing** | Full JSON payload sync. | Incremental sync (timestamp-based) of updated cards and review logs only. | Background worker sync, server-authoritative conflict resolution based on review logs. |

## Application to VocabMaster (Build Order Implications)

To refactor VocabMaster safely, the dependency graph dictates this build order:

1. **Domain (FSRS Engine):** Audit and fix the FSRS math first. Create unit tests against golden vectors. If the engine is wrong, everything else is wrong.
2. **Data (Room):** Ensure atomic transactions exist for updating a Card and writing a Review Log simultaneously.
3. **Presentation (ViewModels):** Extract logic from the 647-line `QuizViewModel` into UseCases that call the now-stable FSRS engine.
4. **Presentation (UI):** Finally, break apart the 900-line Compose screens, replacing unsafe unwraps (`!!`) with proper state observing from the cleaned-up ViewModels.

## Sources
- [Flashcards Open Source App (GitHub) - FSRS Implementation & Parity Testing Architecture](https://github.com/kirill-markin/flashcards-open-source-app)
- [VocabVault Architecture (GitHub) - Offline First & Clean Arch](https://github.com/alireza-malek/vocabvault)
- [StudyBuddy Architecture (GitHub) - SM-2 & Repository Pattern](https://github.com/giovergos/study-buddy)