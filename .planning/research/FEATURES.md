# Feature Landscape

**Domain:** Gamified Language Learning App
**Researched:** 2026-07-22

## Table Stakes

Features users expect. Missing = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Hierarchical Curriculum | Users need structured progression (Topics -> Lessons). | Medium | Requires robust Room relationships (`@Relation`) and domain mapping. |
| Instant Visual Feedback | Users need to know immediately if they were right/wrong before moving to the next question. | Low | Use Compose `AnimatedVisibility` and color transitions. |
| Basic FSRS Review Queue | Core value proposition of the app. | Medium | Must track S, D, R per item. Already partially built, needs integration with new curriculum. |
| Multiple Choice Exercises | Standard testing format. | Low | Simple Compose lists/radio buttons. |

## Differentiators

Features that set product apart. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Interactive Lottie Feedback | High polish, rewarding "game-like" feel (e.g., exploding confetti on correct answer). | Medium | Requires `DotLottieController` and state machine `.lottie` files. |
| Sentence Arrangement Exercises | Deeper grammar testing, interactive drag-and-drop or chip selection. | Medium-High | Drag-and-drop in Compose can be tricky; chip selection is a simpler MVP alternative. |
| Fill-in-the-blanks | Tests active recall rather than passive recognition. | Medium | Requires text parsing to generate input fields within a sentence layout. |
| Distinct "Learning" vs "Review" modes | Prevents users from being overwhelmed by FSRS scheduling brand new words immediately. | High | Requires architectural split in how queues are generated and handled. |

## Anti-Features

Features to explicitly NOT build.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Monolithic Quiz Screen | `QuizScreen.kt` is currently >900 lines. Adding new exercise types here will cause unmaintainable spaghetti code and recomposition bugs. | Use a modular approach: A host `GameScreen` that delegates to specific Composables (`MultipleChoiceLayout`, `FillInBlankLayout`) based on a polymorphic `PracticeType` domain model. |
| Denormalized FSRS + Lesson Data | Storing FSRS data inside the `Lesson` entity makes it impossible to review words across different contexts or after a lesson is "complete". | Keep FSRS tracking state linked to the `VocabularyItem`, separate from the `Lesson` curriculum structure. |
| Complex 3D/Custom Canvas Animations | Too high engineering effort for the value. | Use `DotLottie` for high-quality, pre-rendered vector animations controlled by state. |

## Feature Dependencies

```
Hierarchical Curriculum (Room Models) → Exercise Polymorphism (Domain Models)
Exercise Polymorphism → Modular UI Components (Compose)
Modular UI Components → Instant Visual Feedback (Compose + Lottie)
Instant Visual Feedback → FSRS Integration (Updating state on answer)
```

## MVP Recommendation

Prioritize:
1. Data modeling: `Topic` -> `Lesson` -> `Exercise` (Polymorphic) in Room/Domain.
2. Modular UI refactor: Break up `QuizScreen.kt` to handle a generic `PracticeType`.
3. Implement `MultipleChoice` and `FillInBlank` UI components with basic color-change instant feedback.
4. Integrate the result of these exercises back into the existing FSRS update logic.

Defer: 
- `Sentence Arrangement` with Drag-and-Drop (Use simple tap-to-select chips for v1.1 MVP).
- Complex multi-state Lottie interactive animations (Stick to simple success/fail Lottie clips first).

## Sources

- FSRS Integration logic: [Building an AI-Powered Driving Theory Exam Platform with the FSRS Algorithm](https://dev.to/ketisdev/building-an-ai-powered-driving-theory-exam-platform-with-the-fsrs-algorithm-2nei)
- Compose App Structure: [Building a Language Learning App with Compose — Part 5](https://proandroiddev.com/building-a-language-learning-app-with-compose-part-5-65ddad95a453)