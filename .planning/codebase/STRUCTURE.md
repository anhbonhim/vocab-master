# Codebase Structure

**Analysis Date:** 2026-07-20

## Directory Layout

```
[project-root]/
├── app/               # Android App Module (Presentation)
│   └── src/main/java/com/nhimz/vocabmaster/
├── backend/           # Python FastAPI Backend
│   └── app/
├── data/              # Data Module (Repositories Impl, DB, API)
│   └── src/main/java/com/nhimz/vocabmaster/data/
├── domain/            # Domain Module (Use Cases, Models, Interfaces)
│   └── src/main/java/com/nhimz/vocabmaster/domain/
├── config/            # Configuration files (e.g., Detekt)
├── docs/              # Project documentation
├── gradle/            # Gradle wrapper and configuration
├── specs/             # Product and technical specifications
└── tools/             # Helper scripts
```

## Directory Purposes

**`app/src/main/java/com/nhimz/vocabmaster/`:**
- Purpose: Presentation layer logic and UI.
- Contains: Activities, Compose screens, ViewModels, Notifications.
- Key files: `MainActivity.kt`, `VocabApplication.kt`

**`app/src/main/java/com/nhimz/vocabmaster/ui/`:**
- Purpose: All UI related code.
- Contains: `components`, `navigation`, `screens`, `theme`, `viewmodel`.

**`domain/src/main/java/com/nhimz/vocabmaster/domain/`:**
- Purpose: Core business logic and definitions.
- Contains: `fsrs` (algorithm), `model` (interfaces and entities), `usecase`.

**`data/src/main/java/com/nhimz/vocabmaster/data/`:**
- Purpose: Data access implementations.
- Contains: `auth`, `database` (Room), `di` (Hilt modules), `model` (DTOs), `remote` (API), `repository` (Impls), `sync`.

**`backend/app/`:**
- Purpose: FastAPI backend server.
- Contains: `models` (DB), `routers` (Endpoints), `schemas` (Pydantic), `services` (Business logic), `utils`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt`: Android App main screen.
- `app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt`: Application class.

**Configuration:**
- `build.gradle.kts` (Project and Module level): Dependency and build configuration.
- `config/detekt/detekt.yml`: Static analysis configuration.

**Core Logic:**
- `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/`: Core spaced repetition algorithm.
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/`: Business logic orchestrators.

## Naming Conventions

**Files/Classes (Kotlin):**
- Pattern: PascalCase
- Example: `VocabularyRepository.kt`, `QuizViewModel.kt`

**Files/Modules (Python):**
- Pattern: snake_case
- Example: `main.py`, `auth_service.py`

**Interfaces:**
- Pattern: PascalCase (typically no 'I' prefix in Kotlin).
- Example: `VocabularyRepository`

**Implementations:**
- Pattern: InterfaceName + `Impl`.
- Example: `VocabularyRepositoryImpl`

**Compose Functions:**
- Pattern: PascalCase, annotated with `@Composable`.
- Example: `VocabMasterApp()`, `QuizScreen()`

## Where to Add New Code

**New Android Feature (UI):**
- Screen: `app/src/main/java/com/nhimz/vocabmaster/ui/screens/`
- ViewModel: `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/`
- Components: `app/src/main/java/com/nhimz/vocabmaster/ui/components/`

**New Business Logic / Domain Concept:**
- Model/Interface: `domain/src/main/java/com/nhimz/vocabmaster/domain/model/`
- Use Case: `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/`

**New Data Source (API/DB):**
- Implementation: `data/src/main/java/com/nhimz/vocabmaster/data/repository/`
- Room Entity/DAO: `data/src/main/java/com/nhimz/vocabmaster/data/database/`

**New Backend Endpoint:**
- Router: `backend/app/routers/`
- Service Logic: `backend/app/services/`
- Schema: `backend/app/schemas/`

## Special Directories

**`specs/`:**
- Purpose: Contains detailed product specifications and change proposals.
- Generated: No
- Committed: Yes

**`backend/venv/`:**
- Purpose: Python virtual environment.
- Generated: Yes
- Committed: No

---

*Structure analysis: 2026-07-20*