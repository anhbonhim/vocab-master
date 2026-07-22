# Requirements: VocabMaster

**Defined:** 2026-07-22
**Core Value:** Ensure absolute correctness of the spaced repetition scheduling logic and deliver a highly polished, intuitive, and modern user experience.

## v1.1 Requirements

### Content & Data Quality (New)

- [ ] **CONT-01**: Thiết kế API endpoints trên FastAPI backend để cấp phát dữ liệu Topic, Lesson và bài tập (hỗ trợ tải động theo nhu cầu).
- [ ] **CONT-02**: Tích hợp luồng sử dụng AI (LLM) ở backend để tự động sinh bài tập (nghe, điền từ, sắp xếp) từ bộ từ vựng, đi kèm script kiểm định chất lượng nội dung.
- [ ] **CONT-03**: Xây dựng Schema Validation (ví dụ bằng Pydantic trên backend) để đảm bảo dữ liệu sinh ra hoặc nhập vào luôn đúng cấu trúc và phân cấp độ khó.
- [ ] **CONT-04**: Thêm tính năng "Báo lỗi câu hỏi" (User Report) trong UI bài học để người dùng gửi phản hồi về nội dung sai lệch về backend.

### Architecture & Data

- [ ] **ARCH-01**: Implement Curriculum Data Models (Topic, Lesson, Exercise) in Room and Domain, cleanly separated from FSRS state.
- [ ] **ARCH-02**: Implement `QuizInteractionState` machine in `QuizViewModel` to handle Waiting, Answered (Instant Feedback), and Finished states.
- [ ] **ARCH-03**: Integrate `androidx.media3:media3-exoplayer` via a cleanly decoupled `AudioPlayerUseCase` for listening exercises.

### Gamified UI

- [ ] **UI-01**: Refactor `QuizScreen` to use a polymorphic UI pattern (delegating to separate Composables based on `QuestionType`).
- [ ] **UI-02**: Implement instant visual feedback using `DotLottie` animations for Correct/Incorrect states without causing unstable recomposition.
- [ ] **UI-03**: Ensure smooth transitions between questions when the user taps 'Tiếp tục' (Continue) after receiving feedback.

### Exercise Types

- [ ] **EXER-01**: Implement gamified UI and state handling for Multiple Choice exercises.
- [ ] **EXER-02**: Implement UI and audio playback for Listening (Audio) exercises.
- [ ] **EXER-03**: Implement UI and text input parsing for Fill-in-the-blanks exercises.
- [ ] **EXER-04**: Implement UI and interaction logic (tap-to-select chips or drag-and-drop) for Sentence Arrangement exercises.

## Future Requirements

Deferred to future release. Tracked but not in current roadmap.

### [Category]

- **[CAT]-01**: [Requirement description]

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Complex 3D Canvas Animations | Too high engineering effort; sticking to DotLottie for now. |
| Mixed FSRS rating during Gamified Quiz | Gamified UI uses Check -> Continue. FSRS ratings (Again/Hard/Good/Easy) must remain separate. |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CONT-01 | Phase 1 | Pending |
| CONT-02 | Phase 1 | Pending |
| CONT-03 | Phase 1 | Pending |
| CONT-04 | Phase 1 | Pending |
| ARCH-01 | Phase 2 | Pending |
| ARCH-02 | Phase 2 | Pending |
| ARCH-03 | Phase 2 | Pending |
| UI-01 | Phase 3 | Pending |
| UI-02 | Phase 3 | Pending |
| UI-03 | Phase 3 | Pending |
| EXER-01 | Phase 4 | Pending |
| EXER-02 | Phase 4 | Pending |
| EXER-03 | Phase 4 | Pending |
| EXER-04 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 14 total
- Mapped to phases: 0
- Unmapped: 14 ⚠️

---
*Requirements defined: 2026-07-22*
*Last updated: 2026-07-22 after adding Content Quality requirements*
