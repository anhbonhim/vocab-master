# Vocab Master Technical Wiki

## 1. Project Overview
Vocab Master is an Android application dedicated to vocabulary learning and spaced repetition. The project is built using modern Android development practices, leveraging a reactive architecture to offer a responsive and robust user experience.

### Core Technologies
*   **Language:** Kotlin (JVM 17)
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture/Dependency Injection:** Hilt (Dagger)
*   **Database:** Room (SQLite)
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Serialization:** Kotlinx Serialization JSON
*   **Navigation:** Custom State-based Navigation
*   **Storage:** DataStore Preferences

### High-Level Architecture
The project strictly follows Clean Architecture principles, divided into three main modules:
*   **`:app` (UI/Presentation):** Contains Jetpack Compose UI code, ViewModels, and navigation logic.
*   **`:domain` (Business Logic):** Houses core business entities, repository interfaces, and use cases. It acts as the core and has zero dependencies on Android frameworks.
*   **`:data` (Data Layer):** Implements repository interfaces, containing Room DAOs, Entities, and DataStore preferences logic. It acts as the single source of truth for the app's state.

---

## 2. System Architecture

The architecture is built upon a unidirectional data flow pattern, using a multi-module setup to enforce boundary rules.

### Module Breakdown & Interdependencies
1.  **Domain Module (`:domain`)**: The innermost layer. It defines data contracts (`Repository` interfaces) and contains core business models (`VocabularyItem`, `Card`, `ReviewLog`). It does not depend on `:app` or `:data`.
2.  **Data Module (`:data`)**: Depends on `:domain`. It implements the repository interfaces defined in the domain layer. It handles SQLite database interactions using Room and preference storage using DataStore.
3.  **App Module (`:app`)**: Depends on both `:domain` and `:data`. It handles dependency injection using Hilt, injecting concrete repository implementations (from `:data`) and use cases (from `:domain`) into the ViewModels. The ViewModels manage UI state and expose it to Jetpack Compose screens.

---

## 3. Data Flow & Logic

The application follows a local single-source-of-truth strategy, entirely reliant on on-device storage.

### Spaced Repetition System (FSRS)
The core business logic revolves around the **Free Spaced Repetition Scheduler (FSRS)** algorithm, implemented in the domain layer (`com.nhimz.vocabmaster.domain.fsrs`).
*   **Process:** When a user reviews a flashcard, their response time and accuracy are converted into a `Rating` (`Again`, `Hard`, `Good`, `Easy`) via the `MapRatingUseCase`. The FSRS algorithm calculates a new `CardReviewResult`, containing the next `due` date, adjusted `stability`, and `difficulty` parameters.
*   **Data Updates:** The updated `Card` is saved back to the Room database via `VocabularyRepository`, and the review action is logged via `ReviewRepository`.

### Initial Data Seeding
There is no active remote server connection. On the very first launch, if the local Room database is empty, the `VocabularyRepositoryImpl` parses a static `assets/vocabulary.json` file. It maps these raw items into `VocabularyCardEntity` records, initializing their learning states as `State.New` before performing a batch insertion into Room.

### Reactive State Flow
Data structures are exposed as Kotlin `Flow`s from the Data layer up to the UI. For instance, `SettingsRepositoryImpl` exposes a stream of DataStore preferences. ViewModels consume these flows, transform them into `StateFlow`s, which Jetpack Compose screens observe (`collectAsState()`). This ensures UI updates are synchronized automatically when the underlying database or preferences change.

---

## 4. API & Interface Specifications

The application uses Repository interfaces as contracts between the domain and data layers.

### Internal APIs (Repository Interfaces)
Located in `com.nhimz.vocabmaster.domain.model`:

*   **`VocabularyRepository`**
    *   `getDueCards(limit: Int): Flow<List<VocabularyItemWithCard>>`: Returns cards ready for review (`State.New` or `due <= now`).
    *   `getCardsByLevel(level: DifficultyLevel): Flow<List<VocabularyItemWithCard>>`: Fetches vocabulary categorized by CEFR level.
    *   `updateCard(cardId: String, newCardState: Card): suspend Unit`: Updates scheduling metadata for a specific card.
*   **`ReviewRepository`**
    *   `insertReviewLog(log: ReviewLog): suspend Unit`: Records a historical review.
    *   `getStats(): Flow<ReviewStats>`: Aggregates learning states and difficulty levels into statistical data.
*   **`SettingsRepository`**
    *   Manages streams for user preferences, streak counts, XP, UI theme, etc. Example: `val theme: Flow<ThemeOption>`
*   **`BackupRepository`**
    *   `exportBackup(): suspend String`: Serializes all local data into a JSON string payload.
    *   `importBackup(jsonString: String): suspend Boolean`: Parses a JSON payload and reconstructs the database and preferences within a database transaction.

### Data Schemas (Room Entities)
Located in the Data Layer:
*   **`VocabularyCardEntity`**: Represents a flashcard. Contains vocabulary data (`word`, `definition`, `ipa`) and FSRS scheduling data (`due`, `stability`, `difficulty`, `state`).
*   **`ReviewLogEntity`**: Records review history. Stores the given `rating`, `elapsed_days`, `scheduled_days`, and a foreign key to the `VocabularyCardEntity`.

---

## 5. Dependency Map

The project manages dependencies via a Gradle Version Catalog (`gradle/libs.versions.toml`).

### Major Libraries & Frameworks
*   **AndroidX Core & UI:** `androidx.core:core-ktx`, `androidx.activity:activity-compose`
*   **Jetpack Compose:** `androidx.compose.ui:ui`, `androidx.compose.material3:material3`, `androidx.compose.ui:ui-tooling`
*   **Dependency Injection:** Dagger Hilt (`com.google.dagger:hilt-android`)
*   **Database:** Room (`androidx.room:room-runtime`, `androidx.room:room-ktx`)
*   **Preferences Storage:** DataStore (`androidx.datastore:datastore-preferences`)
*   **Coroutines:** `org.jetbrains.kotlinx:kotlinx-coroutines-android`
*   **Serialization:** Kotlinx Serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json`)

---

## 6. Setup & Deployment

### Environment Configuration
1.  **Prerequisites:** Install Android Studio (Ladybug or newer recommended) and JDK 17.
2.  **Clone:** Clone the repository to the local machine.
3.  **Sync:** Open the project in Android Studio. Gradle will automatically sync dependencies based on the `libs.versions.toml` catalog.
4.  **Run:** Select an Android Emulator (API 24+) or connect a physical device. Click the "Run" button to deploy the `app` module.

### Deployment Process
*   The application is configured in `app/build.gradle.kts`.
*   Build variants can be configured. The standard process involves building a signed APK or Android App Bundle (AAB) via Android Studio's Build menu.

---

## 7. Code Patterns & Conventions

Future autonomous agents must adhere to the following established patterns when making modifications:

### UI & Navigation Patterns
*   **Jetpack Compose Exclusivity:** All new UI must be built using Compose and Material 3. XML layouts are strictly forbidden.
*   **Custom State-based Navigation:** Do not introduce the Jetpack Navigation Component. Use the existing custom approach: define screens in the `Screen` sealed class, update the `MainViewModel.currentScreen` state, and map the UI conditionally in `MainActivity.kt`.
*   **State Hoisting:** Composable functions must observe state from ViewModels using `collectAsState()` and delegate events upwards via lambda parameters. Composables should remain stateless where possible.

### Architecture Patterns
*   **Strict Clean Architecture:** The `:domain` module must never contain Android dependencies (`android.*` imports are prohibited).
*   **Repository Pattern:** ViewModels must never query the database directly. All data access must occur through injected Repository interfaces.
*   **Use Case Encapsulation:** Complex business logic or algorithms (like `GenerateDistractorsUseCase`) must be encapsulated within UseCase classes in the `:domain` layer, rather than bloating ViewModels.

### Concurrency & Data Observation
*   **Coroutines & Flow:** Use Kotlin Coroutines for asynchronous work. Use `Flow` (specifically `StateFlow` in ViewModels) to expose data streams.
*   **Safe Execution:** All background operations in ViewModels must be executed within `viewModelScope.launch`. Data layer operations should switch to `Dispatchers.IO` when performing disk/network I/O.