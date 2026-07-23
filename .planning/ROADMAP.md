# Roadmap: VocabMaster Refactor & Audit

## Milestones

- ✅ **v1.0 VocabMaster Refactor & Audit** — Phases 1-4 (shipped 2026-07-22)
- 🚧 **v1.1 Thiết kế lại bài học (Gamified Learning Experience)** — Phases 5-8

## Phases

<details>
<summary>✅ v1.0 VocabMaster Refactor & Audit (Phases 1-4) — SHIPPED 2026-07-22</summary>

- [x] Phase 1: Security & Database Stabilization (7/7 plans) — completed 2026-07-21
- [x] Phase 2: Business Logic & ViewModel Refactoring (2/2 plans) — completed 2026-07-21
- [x] Phase 3: Compose UI Refactoring & Polish (4/4 plans) — completed 2026-07-22
- [x] Phase 4: Sync & Integration Verification (1/1 plans) — completed 2026-07-22

</details>

- [x] **Phase 5: Backend Infrastructure & Content API** - Backend có khả năng tự sinh, kiểm định và cung cấp dữ liệu bài học (completed 2026-07-23)
- [ ] **Phase 6: Client Data Layer & Core Logic** - Xây dựng model dữ liệu Room, state machine và audio use case cho app
- [ ] **Phase 7: Gamified Quiz UI Foundation & Feedback** - Xây dựng nền tảng UI đa hình, hệ thống phản hồi Lottie và Multiple Choice
- [ ] **Phase 8: Interactive Exercise Types** - Bổ sung các dạng bài tập tương tác (Listening, Fill-in-the-blank, Sentence Arrangement)

## Phase Details

### Phase 5: Backend Infrastructure & Content API

**Goal**: Backend có thể tự động tạo, kiểm tra tính hợp lệ và cung cấp dữ liệu Topic, Lesson, Exercise.
**Depends on**: Nothing
**Requirements**: CONT-01, CONT-02, CONT-03
**Success Criteria** (what must be TRUE):

  1. Client có thể gọi API để lấy dữ liệu Topics, Lessons và Exercises theo chủ đề.
  2. Hệ thống backend có kịch bản (script) sinh bài tập tự động từ bộ từ vựng thông qua LLM.
  3. Mọi dữ liệu bài học sinh ra hoặc nhập vào đều vượt qua kiểm định đúng cấu trúc Pydantic.

**Plans**: 3/3 plans executed

- [x] 05-01-PLAN.md — Khởi tạo Curriculum Data Models (Topic, Lesson) và User Report Model cùng các API endpoints
- [x] 05-02-PLAN.md — Kiểm định package cài đặt, cấu hình biến môi trường và xây dựng hệ thống Pydantic schemas LLM
- [x] 05-03-PLAN.md — Tích hợp LLM để tự động sinh bài tập từ bộ từ vựng thông qua Opencode API và kịch bản script

### Phase 6: Client Data Layer & Core Logic

**Goal**: Ứng dụng Android lưu trữ đúng cấu trúc bài học mới, sẵn sàng xử lý âm thanh và quản lý vòng đời trả lời.
**Depends on**: Phase 5
**Requirements**: ARCH-01, ARCH-02, ARCH-03
**Success Criteria** (what must be TRUE):

  1. Dữ liệu bài học (Topic, Lesson, Exercise) được lưu và truy vấn chính xác qua Room Database (tách biệt hoàn toàn FSRS).
  2. `AudioPlayerUseCase` có thể phát âm thanh cho bài tập nghe mà không gây lỗi hoặc rò rỉ bộ nhớ.
  3. `QuizViewModel` chuyển đổi đúng các trạng thái `Waiting`, `Answered`, và `Finished` khi người dùng thao tác.

**Plans**: TBD

### Phase 7: Gamified Quiz UI Foundation & Feedback

**Goal**: Người dùng trải nghiệm giao diện quiz hiện đại, có phản hồi tức thì với Lottie, báo lỗi câu hỏi và hỗ trợ dạng bài Trắc nghiệm cơ bản.
**Depends on**: Phase 6
**Requirements**: UI-01, UI-02, UI-03, EXER-01, CONT-04
**Success Criteria** (what must be TRUE):

  1. Người dùng thấy giao diện câu hỏi thay đổi tương ứng theo loại câu hỏi nhờ cơ chế đa hình (Polymorphic UI).
  2. Người dùng thấy hiệu ứng Lottie phản hồi đúng/sai ngay khi chọn đáp án mà không bị giật lag giao diện.
  3. Màn hình chỉ chuyển sang câu hỏi tiếp theo sau khi người dùng bấm nút "Tiếp tục".
  4. Người dùng có thể trả lời hoàn chỉnh các câu hỏi dạng Trắc nghiệm (Multiple Choice).
  5. Người dùng có thể bấm gửi "Báo lỗi" trên câu hỏi và hệ thống ghi nhận.

**Plans**: TBD
**UI hint**: yes

### Phase 8: Interactive Exercise Types

**Goal**: Người dùng có thể học tập với các định dạng bài tập nâng cao: Nghe, Điền từ và Sắp xếp câu.
**Depends on**: Phase 7
**Requirements**: EXER-02, EXER-03, EXER-04
**Success Criteria** (what must be TRUE):

  1. Người dùng có thể nghe phát âm audio và chọn/nhập đáp án cho bài tập Nghe (Listening).
  2. Người dùng có thể gõ văn bản vào ô trống và nộp bài để hoàn thành bài tập Điền từ.
  3. Người dùng có thể kéo thả hoặc chạm để sắp xếp các khối từ thành một câu hoàn chỉnh.

**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Security & Database Stabilization | v1.0 | 7/7 | Complete | 2026-07-21 |
| 2. Business Logic & ViewModel Refactoring | v1.0 | 2/2 | Complete | 2026-07-21 |
| 3. Compose UI Refactoring & Polish | v1.0 | 4/4 | Complete | 2026-07-22 |
| 4. Sync & Integration Verification | v1.0 | 1/1 | Complete | 2026-07-22 |
| 5. Backend Infrastructure & Content API | v1.1 | 4/3 | Complete    | 2026-07-23 |
| 6. Client Data Layer & Core Logic | v1.1 | 0/0 | Not started | - |
| 7. Gamified Quiz UI Foundation & Feedback | v1.1 | 0/0 | Not started | - |
| 8. Interactive Exercise Types | v1.1 | 0/0 | Not started | - |
