# Proposal: Duolingo UX Overhaul
**Domain:** ux-core
**Change ID:** duolingo-overhaul

## Intent
Nâng cấp trải nghiệm người dùng (UX) và giao diện (UI) của toàn bộ các màn hình làm bài tập (Placement, FirstWin, Quiz) theo các nguyên lý cốt lõi của Duolingo. Mục đích là loại bỏ lỗi hiển thị (viền sẫm), tăng tính tích cực trong phản hồi (feedback banner), và tạo vòng lặp học tập hiệu quả hơn bằng cách yêu cầu làm lại các câu sai.

## Scope
- Xây dựng bộ Shared Components mới: `DuolingoOptionCard`, `FeedbackBanner`, `DuolingoProgressBar`.
- Lưu trữ kết quả Placement Test vào `SettingsRepository`.
- Thiết kế lại `FirstWinScreen`: Dùng 7 câu hỏi động theo level, tích hợp Lottie animation cho màn hình ăn mừng (Celebration).
- Cập nhật logic `QuizScreen` & `FirstWinScreen`: Đẩy câu sai xuống cuối hàng đợi (queue) thay vì bỏ qua.
- Nâng cấp UI của `PlacementTestScreen` (Không áp dụng lặp câu sai).

## Non-goals
- Không can thiệp vào thuật toán FSRS của `FlashcardScreen`.
- Không thêm tính năng mới như Leaderboard hay Gamification tiền tệ.