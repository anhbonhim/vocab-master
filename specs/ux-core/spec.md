# UX Core Specification

## Purpose
Đặc tả các yêu cầu và hành vi tương tác cốt lõi của giao diện (UI) và trải nghiệm người dùng (UX) trong hệ thống làm bài tập (Quiz, Placement Test, First Lesson) của Vocab Master.

## Requirements

### Requirement: Shared Components (Anti-Dark Border Compliant)
- **Scenario: Clean Option Cards**
  Given a UI presenting multiple choice options,
  When the `DuolingoOptionCard` is rendered,
  Then it MUST have `0.dp` elevation, its `containerColor` MUST be a solid color (no alpha), and state changes MUST be indicated by `BorderStroke` thickness and solid colors.

- **Scenario: Bottom Feedback Banner**
  Given the user submits an answer,
  When the answer is evaluated,
  Then a `FeedbackBanner` MUST slide up from the bottom (Solid Green for Correct, Solid Red containing the correct answer text for Wrong), obscuring the original check button and presenting a "Continue" button.

### Requirement: Ephemeral Wrong Answer Queue
- **Scenario: Spaced Repetition within Session**
  Given a user answers incorrectly in a non-test quiz (`FirstWinScreen` or `QuizScreen`),
  When they dismiss the feedback banner,
  Then that specific question MUST be appended to the end of the session's queue, forcing them to answer it correctly before completing the session.

### Requirement: Dynamic First Win Experience
- **Scenario: Level-Matched Onboarding**
  Given a user enters `FirstWinScreen`,
  When the session starts,
  Then it MUST load exactly 7 questions matched to their saved placement level.

- **Scenario: Celebration Phase**
  Given the user completes the 7-question queue,
  When transitioning to the success state,
  Then it MUST display a Lottie animation and an animated XP counter (0 to 50) indicating the entry bonus.

### Requirement: Placement Test Persistence
- **Scenario: Saving the Level**
  Given the user completes the placement test,
  When navigating away,
  Then the resulting `DifficultyLevel` MUST be saved to the `SettingsRepository`.

### Requirement: Placement Test UI
- **Scenario: Test Integrity**
  Given the user is taking the placement test,
  When they answer incorrectly,
  Then it MUST show the `FeedbackBanner` but MUST NOT append the question to the end of the queue.
