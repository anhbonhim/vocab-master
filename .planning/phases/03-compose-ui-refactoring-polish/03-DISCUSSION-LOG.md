# Phase 3: Compose UI Refactoring & Polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-21
**Phase:** 3-Compose UI Refactoring & Polish
**Areas discussed:** Design System, Screen Architecture, Navigation, Quiz Feedback, State Survival, Casting Safety

---

## Design System

| Option | Description | Selected |
|--------|-------------|----------|
| Duolingo-style | Focus on benched 3D styling ( ledge offsets, bold colors, haptic cues) | ✓ |
| Material 3 chuẩn | Standard Material 3 components and shapes | |

**User's choice:** Duolingo-style
**Notes:** Custom widgets (like `DuolingoButton` and `DuolingoCard`) will be created to render tactile press offsets. Bảng màu sẽ tích hợp trực tiếp vào ColorScheme để thích ứng với Dark/Light modes.

---

## Screen Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| Tách Container/Content | Stateful Screen (Container) and Stateless ScreenContent (pure layout) | ✓ |
| Chỉ tách Sub-composables | Keep presentation state logic in screens and extract UI sub-elements | |

**User's choice:** Tách Container/Content (Pattern chuẩn cho UI Refactoring)
**Notes:** HomeScreen, SettingsScreen, và QuizScreen sẽ được cấu trúc lại hoàn chỉnh để tách biệt logic sự kiện/state khỏi layout render.

---

## Navigation

| Option | Description | Selected |
|--------|-------------|----------|
| Navigation Compose Type-Safe | Official Jetpack Compose Navigation Type-Safe APIs via Kotlin Serialization | ✓ |
| Custom Screen sealed class | Maintain current custom Screen routing structure without unsafe casts | |

**User's choice:** Navigation Compose Type-Safe
**Notes:** Loại bỏ hoàn toàn lớp Route custom thủ công để đưa về mô hình serialization hiện đại của Android.

---

## Quiz Feedback

| Option | Description | Selected |
|--------|-------------|----------|
| Full Rich Feedback | Shake animation on wrong answer, scale/pop on correct answer, animated colors | ✓ |
| Tĩnh & Đơn giản | Simple color swaps to save recomposition overhead | |

**User's choice:** Full Rich Feedback
**Notes:** Nhắm tới việc mang lại trải nghiệm học tập sinh động và phản hồi trực quan sắc nét.

---

## State Survival

| Option | Description | Selected |
|--------|-------------|----------|
| SavedStateHandle tích hợp | Store active session indices and selections in SavedStateHandle | ✓ |
| Reload từ DB | Fetch state again from local Room DB using session ID upon rotation | |

**User's choice:** SavedStateHandle tích hợp
**Notes:** Đảm bảo Quiz không bị reset trạng thái khi thiết bị xoay màn hình đột ngột.

---

## Casting Safety

| Option | Description | Selected |
|--------|-------------|----------|
| Xử lý triệt để | Replace all `as` and `!!` with safe casting/Elvis options | ✓ |
| Chỉ xử lý nơi có nguy cơ cao | Fix only known instances flagged in concerns | |

**User's choice:** Xử lý triệt để
**Notes:** Bảo vệ ứng dụng khỏi nguy cơ NullPointerException và ClassCastException ở tầng presentation.

---

## the agent's Discretion

- Canvas operations for 3D tactile button animation logic.
- Exact spacing, padding, and layout optimizations for stateless ScreenContents.

## Deferred Ideas

- Advanced learning charts screen (v2 analytics).
- Sync error resolution visual flows (Phase 4).
