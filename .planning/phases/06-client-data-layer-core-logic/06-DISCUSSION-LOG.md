# Phase 06: client-data-layer-core-logic - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-23
**Phase:** 06-client-data-layer-core-logic
**Areas discussed:** Thiết kế Model Domain, Tích hợp AudioPlayer, Luồng xử lý (Data Flow)

---

## Thiết kế Model Domain

| Option | Description | Selected |
|--------|-------------|----------|
| Domain Model độc lập (Recommended) | Khởi tạo các data class độc lập trong Domain (VD: Topic, Lesson, Exercise) tách biệt hoàn toàn với FSRS Card, sau đó ánh xạ (map) sang UI Models ở Presentation layer (Khuyến nghị). | ✓ |
| Tái sử dụng Entities | Tái sử dụng các Models từ Data layer (Room Entities) trực tiếp cho Domain và UI để giảm boilerplate. (Vi phạm Clean Architecture). | |
| Data Model Tổng hợp | Chỉ tạo một Data class tổng hợp và nhúng FSRS State vào bên trong. (Dễ gây rối logic FSRS). | |
| Other | Custom answer (type your own) | |

**User's choice:** Domain Model độc lập (Recommended)
**Notes:** 

---

## Tích hợp AudioPlayer

| Option | Description | Selected |
|--------|-------------|----------|
| AudioPlayerUseCase + Hilt DI (Recommended) | Tạo `AudioPlayerUseCase` trong Domain layer ẩn chi tiết ExoPlayer. Inject ExoPlayer thông qua DataModule của Hilt. UI chỉ gửi request qua ViewModel (Khuyến nghị). | ✓ |
| ExoPlayer trong ViewModel | Khởi tạo trực tiếp ExoPlayer bên trong ViewModel. | |
| ExoPlayer UI State | Khởi tạo ExoPlayer dưới dạng Composable state (rememberExoPlayer). | |
| Other | Custom answer (type your own) | |

**User's choice:** AudioPlayerUseCase + Hilt DI (Recommended)
**Notes:** 

---

## Luồng xử lý (Data Flow)

| Option | Description | Selected |
|--------|-------------|----------|
| Tách biệt Use Cases mới (Recommended) | Tạo các Use Cases mới chuyên biệt cho Curriculum (VD: LoadTopicUseCase, SubmitExerciseUseCase) kết nối với Repository. QuizViewModel chỉ tương tác với Use Cases. (Khuyến nghị). | ✓ |
| Mở rộng Use Cases hiện tại | Sửa đổi Use Cases hiện tại (VD: thêm logic Curriculum vào LoadQuizSessionUseCase). Dễ gây phình to class. | |
| Gọi trực tiếp Repository | Bỏ qua Use Cases, để QuizViewModel gọi trực tiếp CurriculumRepository. | |
| Other | Custom answer (type your own) | |

**User's choice:** Tách biệt Use Cases mới (Recommended)
**Notes:** 

---

## the agent's Discretion

None

## Deferred Ideas

None
