# VocabMaster Refactor & Audit

## What This Is

VocabMaster is an Android app built with Jetpack Compose using Clean Architecture and MVVM, powered by an FSRS (Free Spaced Repetition Scheduler) algorithm for smart vocabulary learning, synced with a FastAPI backend. This project focuses on auditing, refactoring, and polishing the app's core spaced repetition logic, user experience flows, and visual design to make it production-ready.

## Core Value

Ensure absolute correctness of the spaced repetition scheduling logic and deliver a highly polished, intuitive, and modern user experience.

## Requirements

### Validated

- ✓ Basic clean architecture structure (app, domain, data Gradle modules) — existing
- ✓ Local Room DB storage and basic repository implementations — existing
- ✓ Basic FastAPI backend structures for synchronization — existing
- ✓ FSRS algorithm auditing and UI refactoring (Milestone v1.0)

### Active

- [ ] **AUDIT-01**: Audit and repair FSRS algorithm calculation bugs and data integrity anomalies (negative stability, interval out of bounds)
- [ ] **AUDIT-02**: Audit and refactor monolithic screens (`HomeScreen.kt`, `QuizScreen.kt`, `SettingsScreen.kt`) to fix unsafe casts, `!!` assertions, and swallowed exceptions
- [ ] **UX-01**: Refactor Quiz flow & navigation transitions to support smooth user input validation, correct progress updating, and visual feedback
- [ ] **UI-01**: Redesign UI with a modern, clean, and polished design system, ensuring consistent theme, typography, and spacing
- [ ] **INTEG-01**: Verify data sync flow and resolve SyncManager state/network request robustness

### Out of Scope

- [ ] Adding new study modes or curriculum packages beyond the current structure (defer to v2)
- [ ] Complete replacement of the FastAPI backend framework (keep existing python structure, only fix endpoints if sync contracts break)

## Current Milestone: v1.1 Thiết kế lại bài học (Gamified Learning Experience)

**Goal:** Nâng cấp toàn diện trải nghiệm học tập bằng giao diện sinh động, phản hồi tức thì và bổ sung các loại bài tập tương tác đa dạng được tổ chức theo chủ đề với độ khó tăng dần.

**Target features:**
- Thiết kế cấu trúc dữ liệu mới (Data Model): Hỗ trợ bài học theo chủ đề (Topics/Lessons), phân cấp độ khó.
- Giao diện Sinh động (Gamified UI): Redesign hiển thị câu hỏi, đáp án với màu sắc, hình ảnh và hiệu ứng.
- Luồng học phản hồi tức thì (Instant Feedback UX): Hiển thị đúng/sai lập tức sau khi trả lời, yêu cầu người dùng bấm 'Tiếp tục' để qua câu mới.
- Đa dạng hóa bài tập: Bổ sung format mới (Nghe, Điền vào chỗ trống, Sắp xếp câu).

## Context

- The codebase has several known concerns including huge Compose screen files (>900 lines), unsafe unwraps, dynamic JSON asset imports, and data integrity errors detected in `DataIntegrityTests.kt`.
- Clean Architecture boundaries must be respected: the `domain` module must remain pure Kotlin, `data` handles Room & Retrofit, and `app` focuses on ViewModels and Compose UI.
- Milestone v1.1 requires ensuring the new gamified data structure integrates smoothly with the existing FSRS algorithm.

## Constraints

- **Architecture**: Keep Clean Architecture structure (app, domain, data gradle modules).
- **UI Framework**: Jetpack Compose only.
- **Language**: Pure Kotlin for domain/data modules.
- **Spaced Repetition**: Must strictly conform to FSRS scheduling specifications.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Focus on audit and refactor before introducing new screens | Existing screens are monolithic and prone to memory/recomposition bugs | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-07-22 after milestone v1.1 initialized*
