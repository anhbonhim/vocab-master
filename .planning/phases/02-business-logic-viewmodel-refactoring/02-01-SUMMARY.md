---
phase: 02-business-logic-viewmodel-refactoring
plan: 01
subsystem: domain
tags: [kotlin, usecase, quiz, fsrs, tdd, junit]

requires:
  - phase: 01-security-database-stabilization
    provides: FSRS v6 scheduler, Card/ReviewLog models, repository interfaces

provides:
  - Package com.nhimz.vocabmaster.domain.model.quiz with relocated QuizType/QuizQuestion/QuestionDirection
  - LoadQuizSessionUseCase supporting all 6 quiz entry kinds
  - EvaluateAnswerUseCase covering all 7 QuizType variants
  - SubmitReviewUseCase wiring FSRS scheduling, review-log persistence, and XP
  - CompleteQuizSessionUseCase applying completion/pass thresholds
  - Hand-rolled fakes for repository interfaces enabling JVM unit tests

affects:
  - 02-business-logic-viewmodel-refactoring/02-01-PLAN.md
  - app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt
  - app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt
  - app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/*QuestionCard.kt

tech-stack:
  added:
    - kotlinx-coroutines-test (domain test classpath)
  patterns:
    - Pure-Kotlin domain UseCases returning Result
    - Hand-rolled fakes over mocking frameworks
    - TDD red-green cycle

key-files:
  created:
    - domain/src/main/java/com/nhimz/vocabmaster/domain/model/quiz/QuizSessionModels.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCase.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/EvaluateAnswerUseCase.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/SubmitReviewUseCase.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/CompleteQuizSessionUseCase.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeVocabularyRepository.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeReviewRepository.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeSettingsRepository.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCaseTest.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/EvaluateAnswerUseCaseTest.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/SubmitReviewUseCaseTest.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/CompleteQuizSessionUseCaseTest.kt
  modified:
    - domain/build.gradle.kts
    - app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt
    - app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt
    - app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/ListeningQuestionCard.kt
    - app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/MatchingQuestionCard.kt
    - app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/TypingQuestionCard.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/UpdateStreakUseCase.kt

key-decisions:
  - "Relocated quiz session types to domain.model.quiz so the domain module owns the business vocabulary."
  - "Used Result<T> for all cross-boundary outcomes so failures propagate with original causes."
  - "Made UpdateStreakUseCase.execute() open to allow spy subclassing in tests."
  - "Pinned behavior: on recordReview failure SubmitReviewUseCase returns Result.failure and does NOT award XP."

patterns-established:
  - "Pure-Kotlin UseCases: no Android imports, no Context, no resources."
  - "Failure propagation: runCatching + Result.failure with original cause."
  - "Hand-rolled fakes with mutable backing fields and invocation counters."

requirements-completed:
  - ARCH-04

# Coverage metadata (#1602)
coverage:
  - id: D1
    description: "Quiz type symbols relocated to domain.model.quiz and consumed by app module"
    requirement: ARCH-04
    verification:
      - kind: unit
        ref: "domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCaseTest.kt#node session maps all question types"
        status: pass
    human_judgment: false
  - id: D2
    description: "LoadQuizSessionUseCase loads all six quiz entry kinds and propagates repository failures"
    requirement: ARCH-04
    verification:
      - kind: unit
        ref: "domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCaseTest.kt"
        status: pass
    human_judgment: false
  - id: D3
    description: "EvaluateAnswerUseCase computes correctness and XP for all seven QuizType variants"
    requirement: ARCH-04
    verification:
      - kind: unit
        ref: "domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/EvaluateAnswerUseCaseTest.kt"
        status: pass
    human_judgment: false
  - id: D4
    description: "SubmitReviewUseCase schedules FSRS reviews, persists logs, and awards XP"
    requirement: ARCH-04
    verification:
      - kind: unit
        ref: "domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/SubmitReviewUseCaseTest.kt"
        status: pass
    human_judgment: false
  - id: D5
    description: "CompleteQuizSessionUseCase applies pass thresholds and completion side effects"
    requirement: ARCH-04
    verification:
      - kind: unit
        ref: "domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/CompleteQuizSessionUseCaseTest.kt"
        status: pass
    human_judgment: false

duration: 22 min
completed: 2026-07-21
status: complete
---

# Phase 02 Plan 01: Domain Quiz Business Logic Extraction Summary

**Extracted quiz session loading, answer evaluation, FSRS review submission, and completion rules from `QuizViewModel` into pure-Kotlin domain UseCases with full unit-test coverage.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-07-21T14:45:11Z
- **Completed:** 2026-07-21T15:07:26Z
- **Tasks:** 3
- **Files modified:** 13

## Accomplishments
- Relocated `QuestionDirection`, `QuizType`, and `QuizQuestion` to `domain.model.quiz`.
- Created four pure-Kotlin UseCases: `LoadQuizSessionUseCase`, `EvaluateAnswerUseCase`, `SubmitReviewUseCase`, `CompleteQuizSessionUseCase`.
- Created three hand-rolled fakes for repository interfaces to support JVM tests.
- Added `kotlinx-coroutines-test` to the domain module.
- Repointed app-module imports to the new domain package without changing behavior.
- All 43 domain module tests pass (including Phase-1 FSRS golden vectors and parity tests).

## Task Commits

Each task was committed atomically:

1. **Task 1: Wave-0 test harness + migrate quiz types** - `f45a783` (feat)
2. **Task 2 (RED): Add tests for LoadQuizSessionUseCase and EvaluateAnswerUseCase** - `641d554` (test)
3. **Task 2 (GREEN): Implement LoadQuizSessionUseCase and EvaluateAnswerUseCase** - `7ee3bb7` (feat)
4. **Task 3 (RED): Add tests for SubmitReviewUseCase and CompleteQuizSessionUseCase** - `e94ec7f` (test)
5. **Task 3 (GREEN): Implement SubmitReviewUseCase and CompleteQuizSessionUseCase** - `6555e8f` (feat)

## Files Created/Modified
- `domain/build.gradle.kts` — added `kotlinx-coroutines-test`
- `domain/src/main/java/com/nhimz/vocabmaster/domain/model/quiz/QuizSessionModels.kt` — relocated quiz types
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/LoadQuizSessionUseCase.kt` — session loading for all 6 entry kinds
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/EvaluateAnswerUseCase.kt` — answer correctness + XP
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/SubmitReviewUseCase.kt` — FSRS scheduling + persistence
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/CompleteQuizSessionUseCase.kt` — completion rules
- `domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/UpdateStreakUseCase.kt` — made `execute()` open for test spies
- `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/*` — hand-rolled fakes
- `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/*Test.kt` — 4 unit-test classes
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt` — removed relocated declarations, added imports
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt` — repointed imports
- `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/ListeningQuestionCard.kt` — repointed imports
- `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/MatchingQuestionCard.kt` — repointed imports
- `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/TypingQuestionCard.kt` — repointed imports

## Decisions Made
- **Moved quiz types to domain module.** Keeps the domain module as the single source of truth for business vocabulary.
- **Used `Result<T>` instead of exceptions/emitting error states.** Aligns with Clean Architecture and makes failures testable without Android context.
- **Made `UpdateStreakUseCase.execute()` open.** Minor testability concession; avoids introducing a separate interface for a single-method UseCase.
- **Pinned XP behavior on recordReview failure.** `SubmitReviewUseCase` returns `Result.failure` immediately and does not award XP, so a failed review never silently grants points.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed test expectations for regular-node completion and jump-test node marking**
- **Found during:** Task 3 (CompleteQuizSessionUseCaseTest)
- **Issue:** Tests asserted `isPassed = true` for regular nodes, but the original ViewModel logic only set `isPassed` for checkpoint/jump-test sessions. Jump-test test also asserted the first marked node instead of the total count.
- **Fix:** Removed the incorrect `isPassed` assertion for regular-node pass; changed jump-test test to verify total `markNodeCompleted` calls (2).
- **Files modified:** `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/CompleteQuizSessionUseCaseTest.kt`
- **Verification:** Full `:domain:test` passes
- **Committed in:** `6555e8f` (Task 3 GREEN commit)

**2. [Rule 3 - Blocking] Android SDK unavailable on Termux prevents `:app:compileDebugKotlin`**
- **Found during:** Task 1 verification
- **Issue:** `local.properties` points to `/data/data/com.termux/files/home/android-sdk`, but the SDK is not installed in this environment, so `:app:compileDebugKotlin` fails with "SDK location not found".
- **Fix:** Verified the domain module (`:domain:compileKotlin`, `:domain:compileTestKotlin`, `:domain:test`) compiles and passes. Verified app-module imports are structurally correct via grep. The app-only compile check is deferred to an environment with Android SDK.
- **Files modified:** None — environment limitation, not code.
- **Verification:** `grep` confirms app files import `com.nhimz.vocabmaster.domain.model.quiz.*` and no `QuizType`/`QuizQuestion`/`QuestionDirection` declarations remain in `app/src/main/java`.
- **Committed in:** Documented in `f45a783` commit message

**3. [Rule 1 - Bug] Fixed fake to support throwing `getNodesByUnit` for failure-path test**
- **Found during:** Task 3 (CompleteQuizSessionUseCaseTest)
- **Issue:** `FakeVocabularyRepository.failure` only applied to `Result`-returning methods, so the "repository throw returns Result failure" test could not make `getNodesByUnit` throw.
- **Fix:** Added `getNodesByUnitFailure` backing field to the fake and made `getNodesByUnit` throw when it is set.
- **Files modified:** `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/fakes/FakeVocabularyRepository.kt`
- **Verification:** `repository throw returns Result failure` test passes
- **Committed in:** `6555e8f` (Task 3 GREEN commit)

---

**Total deviations:** 3 auto-fixed (1 test-expectation bug, 1 environment blocker, 1 fake-capability bug)
**Impact on plan:** All fixes were required for correctness or verification. No scope creep.

## Issues Encountered
- Android SDK not installed in the Termux execution environment, so `:app:compileDebugKotlin` could not be run. Domain module and all unit tests are green; app imports were verified structurally.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 02 (ViewModel rewrite consuming these UseCases) is ready to begin.
- `QuizViewModel` no longer declares quiz business types; it only holds `QuizSessionState` UI state.
- All four UseCases are covered by unit tests and the full `:domain:test` suite is green.

## Self-Check: PASSED
- All created files exist on disk.
- All commits (`f45a783`, `641d554`, `7ee3bb7`, `e94ec7f`, `6555e8f`) are in `git log`.
- `:domain:test` passes with 43 tests.

---
*Phase: 02-business-logic-viewmodel-refactoring*
*Completed: 2026-07-21*
