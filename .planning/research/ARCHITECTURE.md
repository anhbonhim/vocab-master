# Architecture Patterns

**Domain:** Educational App / Language Learning
**Researched:** 2026-07-22

## Recommended Architecture

The architecture will remain Clean Architecture (App, Domain, Data) with additions specifically targeted at gamification, instant feedback, and the new data structures. The FSRS algorithm remains core and untouched in its mathematical operation, but the way sessions are generated and evaluated changes.

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `app:ui:screens:QuizScreen` | Displays the gamified quiz UI, manages immediate correct/incorrect feedback states, visual progress. | `app:ui:viewmodel:QuizViewModel` |
| `app:ui:components:quiz:*` | Renders specific question types (Scrambled, Listening, Fill-in-the-blank, etc.) and instant feedback banners. | `app:ui:screens:QuizScreenContent` |
| `app:ui:viewmodel:QuizViewModel` | Manages the state machine of the quiz (Question -> Answered (Feedback) -> Next Question). Processes immediate answer evaluations. | `domain:usecase:EvaluateAnswerUseCase`, `domain:usecase:CompleteQuizSessionUseCase` |
| `domain:usecase:EvaluateAnswerUseCase` | Evaluates raw user input (text, selections, scrambled order) against `Question` models to return `AnswerResult`. | `app:ui:viewmodel:QuizViewModel` |
| `domain:model:Curriculum` | Defines the new `Unit`, `Section`, `Node`, `Session`, and `Question` data classes. | Data mappers, UI ViewModels |
| `domain:fsrs:v6:Scheduler` | Schedules reviews based on FSRS. Untouched, but its outputs (Cards) might be interleaved with standard curriculum Questions. | `domain:usecase:LoadQuizSessionUseCase` |
| `data:database:entity:*` | Room entities corresponding to `Curriculum` models (e.g., `QuestionEntity`, `SessionEntity`, `UnitEntity`). | `data:database:VocabDao`, `data:repository:*` |
| `data:repository:VocabularyRepositoryImpl` | Maps Room entities to Domain models and provides them to use cases. | `data:database:VocabDao`, Domain UseCases |

### Data Flow

1. **Session Load:** User selects a `Node` (Lesson). `LoadQuizSessionUseCase` queries `VocabularyRepositoryImpl` to load a `Session` and its list of `Question`s. If it's a review session, it might interleave FSRS `Card`s by querying the FSRS scheduler.
2. **UI Rendering:** `QuizViewModel` exposes the first `Question` in its `UiState`. `QuizScreen` renders the appropriate component based on `QuestionType` (e.g., `ScrambledQuizCard`).
3. **User Action:** User inputs an answer and taps "Check".
4. **Instant Evaluation:** `QuizViewModel` calls `EvaluateAnswerUseCase`.
5. **Feedback Loop:** `EvaluateAnswerUseCase` returns true/false. `QuizViewModel` updates state to `Answered(isCorrect)`. The UI displays a `FeedbackBanner` (Green for correct, Red for incorrect) and changes the primary button to "Continue".
6. **Progression:** User taps "Continue". `QuizViewModel` advances to the next question.
7. **Session Completion:** When all questions are answered, `CompleteQuizSessionUseCase` is called to save progress, calculate XP, and update FSRS states for any interleaved review cards.

## Patterns to Follow

### Pattern 1: State Machine for Quiz Progression
**What:** The `QuizViewModel` should manage the quiz state explicitly using a sealed interface representing the phases of a single question interaction.
**When:** Managing the "Instant Feedback UX".
**Example:**
```kotlin
sealed interface QuizInteractionState {
    data class WaitingForInput(val question: Question) : QuizInteractionState
    data class AnswerEvaluated(val question: Question, val isCorrect: Boolean, val feedbackText: String) : QuizInteractionState
    object Finished : QuizInteractionState
}
```

### Pattern 2: Domain-Driven Question Types
**What:** Use the existing `QuizType` sealed class (or expand `QuestionType` enum in `Curriculum.kt`) in the domain layer to drive UI rendering. The UI should use exhaustive `when` statements to render the correct Compose component.
**When:** Building the "Đa dạng hóa bài tập" (Diverse Exercises) feature.

## Anti-Patterns to Avoid

### Anti-Pattern 1: Leaking FSRS logic into the Curriculum UI
**What:** Trying to mix FSRS rating buttons (Again, Hard, Good, Easy) directly into the new gamified `QuestionType`s (like Scrambled Sentence).
**Why bad:** Gamified learning relies on binary (Correct/Incorrect) or graded XP feedback. FSRS is for reviewing known items.
**Instead:** Keep them separate in the UI flow. A `Session` can contain both `QuizType` (gamified) and `FSRSTailFlashcard` types. When presenting a gamified question, use the "Check" -> "Continue" flow. When presenting an FSRS card (usually at the end or in a dedicated review node), show the 4 rating buttons.

### Anti-Pattern 2: Fat ViewModels handling business rules
**What:** Putting the logic that determines *if* a scrambled sentence is correct inside the ViewModel.
**Why bad:** Hard to test, violates Clean Architecture.
**Instead:** Rely heavily on `EvaluateAnswerUseCase` (which already exists and handles `QuizType` logic) to perform the validation.

## Scalability Considerations

| Concern | At 100 users | At 10K users | At 1M users |
|---------|--------------|--------------|-------------|
| **Database Size** | Local Room DB handles curriculum fine. | Optimize JSON serialization in `QuestionEntity` options/scrambledWords if tables get large. | Pre-package curriculum in SQLite asset instead of relying entirely on initial sync. |
| **Syncing Curriculum** | Simple REST calls work. | Delta syncing (only download updated Units/Lessons) becomes necessary to save bandwidth. | CDN distribution for static curriculum JSON/SQLite files. |

## Sources

- Project analysis of `/domain/src/main/java/com/nhimz/vocabmaster/domain/model/Curriculum.kt`
- Project analysis of `/domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/EvaluateAnswerUseCase.kt`
- Project analysis of `/data/src/main/java/com/nhimz/vocabmaster/data/database/entity/QuestionEntity.kt`