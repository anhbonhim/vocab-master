# Phase 06: client-data-layer-core-logic - Context

**Gathered:** 2026-07-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Xây dựng và tích hợp Data Model cho cấu trúc học tập mới (Topic -> Lesson -> Exercise) tại Client (Android), đảm bảo phân tách hoàn toàn với FSRS Card theo chuẩn Clean Architecture. Hỗ trợ Media3 ExoPlayer cho dạng bài tập Listening.
</domain>

<decisions>
## Implementation Decisions

### Thiết kế Model Domain
- **D-01:** Khởi tạo các data class độc lập trong Domain (VD: Topic, Lesson, Exercise) tách biệt hoàn toàn với FSRS Card, sau đó ánh xạ (map) sang UI Models ở Presentation layer. (Domain Model độc lập). — **Reversibility:** costly — Nếu sau này thay đổi cấu trúc dữ liệu từ server, cần map lại từ DB/Network sang Domain. Giúp giữ FSRS State an toàn.

### Tích hợp AudioPlayer
- **D-02:** Tạo `AudioPlayerUseCase` trong Domain layer ẩn chi tiết ExoPlayer. Inject ExoPlayer thông qua DataModule của Hilt. UI chỉ gửi request qua ViewModel. (AudioPlayerUseCase + Hilt DI). — **Reversibility:** costly — Nếu muốn thay đổi thư viện Audio (ExoPlayer sang MediaPlayer, ...), chỉ cần sửa Implementation cung cấp cho Hilt. Tránh leak memory trong ViewModel.

### Luồng xử lý (Data Flow)
- **D-03:** Tạo các Use Cases mới chuyên biệt cho Curriculum (VD: LoadTopicUseCase, SubmitExerciseUseCase) kết nối với Repository. QuizViewModel chỉ tương tác với Use Cases. (Tách biệt Use Cases mới). — **Reversibility:** reversible — Rõ ràng, dễ test.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Architecture & Specs
- `.planning/codebase/ARCHITECTURE.md` — The core architecture document (Clean Architecture rules, Domain/Data/Presentation separation).
- `.planning/codebase/STACK.md` — Defines libraries to use (Jetpack Compose, Room, Hilt, Media3 ExoPlayer).
- `.planning/codebase/CONVENTIONS.md` — Coding guidelines (Naming, Use Cases operator invoke).
- `.planning/REQUIREMENTS.md` — Requirements for v1.1 (ARCH-01, ARCH-02, ARCH-03).
- `.planning/phases/05-backend-infrastructure-content-api/05-CONTEXT.md` — Ensures alignment with the backend models (Topic, Lesson, Exercise stored as JSON).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/nhimz/vocabmaster/domain/usecase/` — Existing pattern for UseCases (e.g. `LoadQuizSessionUseCase`, `SubmitReviewUseCase`) that return `Result<T>` and catch exceptions.
- `data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt` — Dagger Hilt module to provide the implementation for the new `AudioPlayerUseCase` and `CurriculumRepository`.
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt` — Quiz session state machine. Should interact with the new Curriculum UseCases.

### Established Patterns
- **Clean Architecture:** `app` depends on `domain` & `data`. `data` depends on `domain`. `domain` is pure Kotlin.
- **Repository Pattern:** Interface in `domain/model/`, Implementation in `data/repository/`.
- **Error Handling:** Use Cases return `Result<T>`. ViewModel uses `.fold(onSuccess, onFailure)` to consume and update state.

### Integration Points
- **FSRS Core:** The new Curriculum Domain Models MUST be isolated from `domain/.../fsrs/v6/Card.kt`.
- **DI (Dependency Injection):** New `CurriculumRepository` and `AudioPlayerUseCase` must be injected via Hilt (`@Inject` / `@Binds`).
- **Media3:** Need to add `androidx.media3:media3-exoplayer` properly to handle Listening exercises.

</code_context>

<specifics>
## Specific Ideas

- Sử dụng Hilt để tiêm (inject) `AudioPlayerUseCase` (chứa ExoPlayer) vào ViewModel, đảm bảo UI (Compose) không trực tiếp khởi tạo ExoPlayer gây memory leak, và giữ cho Domain layer sạch (không phụ thuộc framework Android).
- Tách biệt FSRS Card (Algorithm) và Exercise (UI / Domain Data) thành 2 entity độc lập.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 06-client-data-layer-core-logic*
*Context gathered: 2026-07-23*