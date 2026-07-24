# Domain Pitfalls

**Domain:** Android Jetpack Compose Clean Architecture with FSRS (Spaced Repetition)
**Researched:** 2026-07-22

## Critical Pitfalls

Mistakes that cause rewrites, data corruption, or massive performance regressions when adding gamified features and lesson hierarchies.

### Pitfall 1: Mixing Manual Lesson Scheduling with FSRS
**What goes wrong:** Developers try to use manual overrides or >1 day learning steps to force cards to fit a "static lesson" structure (e.g., "Review this topic tomorrow").
**Why it happens:** Attempting to force traditional fixed-interval lessons into a spaced repetition model. FSRS doesn't know about user-defined intervals, and user-defined steps don't know about FSRS.
**Consequences:** "Out of order" reviews, broken memory models, and users getting stuck in "Ease Hell" where intervals never grow. FSRS loses its predictive accuracy entirely.
**Prevention:** 
- **NEVER** use learning steps longer than 1 day in FSRS.
- **NEVER** manually override intervals via "Set Due Date" for regular reviews.
- Instead, use distinct FSRS **Presets** for different exercise types (e.g., Audio vs Multiple Choice). An audio question has a different intrinsic difficulty than a multiple-choice question; mixing them in one preset ruins the optimization.
**Detection:** Users report seeing the exact same cards every day despite pressing "Good"; average stability drops; RMSE (Root Mean Square Error) in FSRS optimization spikes.

### Pitfall 2: Unstable Lambdas in Compose Animation Loops
**What goes wrong:** The UI stutters or drops frames when the user provides an answer and the "Gamified UI" plays an instant feedback animation (like a card flip or color pulse).
**Why it happens:** Injecting viewmodel method calls directly into Composables like `onClick = { viewModel.submitAnswer(answer) }` creates a new lambda instance on every frame of the animation.
**Consequences:** Compose sees a new lambda, assumes inputs changed, and recomposes the *entire* screen 60 times a second during the animation.
**Prevention:**
- Use **Stable Function References**: `onClick = viewModel::submitAnswer`
- If you must use a lambda, remember it: `val onClick = remember { { viewModel.submitAnswer(answer) } }`
- Read scroll and animation state inside the layout/draw phases using lambda modifiers (e.g., `Modifier.offset { IntOffset(...) }`), NEVER in the composition phase.
**Detection:** Profile the app during an answer submission; if the recomposition count for parent containers spikes, you have unstable lambdas.

### Pitfall 3: Storing Complex Hierarchies as JSON Blobs in Room
**What goes wrong:** To quickly implement "Lessons contain Topics which contain Exercises", developers add a `@TypeConverter` to serialize the whole nested structure into a single JSON string column.
**Why it happens:** It seems like a fast way to bypass writing junction tables and mapping logic.
**Consequences:** 
- You can no longer query individual exercises.
- Migrating the database when you add a new exercise type requires pulling the JSON into memory, modifying strings, and writing them back — which is fragile, memory-intensive, and slow.
**Prevention:** 
- Strictly adhere to relational design: `TopicEntity`, `LessonEntity`, `ExerciseEntity`. 
- Use `@Relation` or Junction tables (e.g., `LessonExerciseCrossRef`) for queries.
**Detection:** Seeing `Gson` or `Moshi` inside a Room `@TypeConverter` for anything more complex than a simple list of primitives.

### Pitfall 4: Mixing @AutoMigration with Manual Data Migrations
**What goes wrong:** App crashes on startup for existing users after shipping the v1.1 update with new lesson structures.
**Why it happens:** Developers use `@AutoMigration` for simple column additions, but attach a manual `Migration` spec for data transformations on the *same version step*.
**Consequences:** Room runs both, causing `table not found` or `duplicate column` SQL exceptions, bricking the user's local database.
**Prevention:**
- `@AutoMigration` is for pure structural diffs (adding/dropping columns).
- If you need to transform data (e.g., moving existing loose flashcards into a default "Uncategorized" Lesson), write a purely manual `Migration(old, new)`. Do NOT register an `@AutoMigration` for that specific version jump.
- Never use `fallbackToDestructiveMigration()` in production; it permanently deletes user progress.
**Detection:** Migrations pass `runMigrationsAndValidate` (which only checks structure) but fail in real-world scenarios. Always test migrations against an actual populated SQLite database file.

## Moderate Pitfalls

### Pitfall 5: Fat Composables Violating Open-Closed Principle (OCP)
**What goes wrong:** A single `QuizScreen.kt` has a massive `when(exerciseType)` block that renders completely different UI for Audio, Fill-in-the-blank, and Drag-and-drop.
**Why it happens:** Lack of polymorphic UI design.
**Prevention:** 
- Define a sealed interface in Domain: `sealed interface Exercise`.
- Map to a UI sealed interface: `sealed interface ExerciseUiState`.
- Use a Factory/Strategy pattern in Compose: Create distinct, isolated Composables (`AudioExerciseContent`, `FillBlankContent`) and delegate to them based on the sealed type.

### Pitfall 6: Direct Media Dependencies in ViewModels
**What goes wrong:** The ViewModel directly instantiates Android `MediaPlayer` or `ExoPlayer` for the new Audio exercises.
**Prevention:** Abstract audio playback behind a pure Kotlin domain interface (e.g., `AudioPlayerUseCase`). Implement it in the `data` or `app` module and inject it. This keeps the ViewModel unit-testable and adheres to Clean Architecture.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Room Database Migration | Destroying user data via `fallbackToDestructiveMigration` or bad JSON TypeConverters. | Write explicit manual migrations. Create proper junction tables for Lessons/Topics. Test with real data. |
| Gamified UI Implementation | Mass recomposition from unstable lambdas during feedback animations. | Use method references (`viewModel::action`), derived state, and lambda modifiers for layout offsets. |
| Exercise Type Architecture | Massive switch-statements breaking OCP in `QuizScreen`. | Model exercises as Sealed Interfaces in Domain and UI. Delegate rendering to specialized child Composables. |
| FSRS Integration | Breaking the memory model by manually overriding intervals for static lessons. | Use separate FSRS Presets for different exercise formats (Audio vs Text). Let FSRS handle the intervals. |

## Sources

- [FSRS - Anki Forums: Struggles with learning steps](https://forums.ankiweb.net/t/in-what-ways-does-fsrs-struggle-with-lots-of-learning-steps-or-longer-learning-steps/69848) (HIGH confidence)
- [FSRS - Anki Forums: Separate presets](https://forums.ankiweb.net/t/when-how-to-separate-presets-for-fsrs/45317) (HIGH confidence)
- [Android Developers: Compose Performance Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) (HIGH confidence)
- [Medium: Jetpack Compose Anti-Patterns](https://aditlal.dev/compose-bottleneck-antipatterns-performance/) (HIGH confidence)
- [Medium: Room Database Migrations That Won't Crash Production](https://medium.com/@sivavishnu0705/part-3-database-migrations-that-wont-crash-production-from-automigrations-to-programmatic-632473e4f3c1) (HIGH confidence)
- [ProAndroidDev: SOLID Principles in Android](https://proandroiddev.com/top-3-android-use-cases-for-every-solid-principle-with-code-960eedcdbc3f) (HIGH confidence)
