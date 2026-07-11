# Tasks: duolingo-overhaul
*Derived from: specs/changes/duolingo-overhaul/spec.md*

## Phase 1: Infrastructure & Colors
- [P] `gradle/libs.versions.toml` & `app/build.gradle.kts` - Thêm thư viện `com.airbnb.android:lottie-compose` (version 6.4.0)
- [P] `app/src/main/java/com/nhimz/vocabmaster/ui/theme/Color.kt` - Khai báo các màu Solid chống viền sẫm (SuccessGreen: #58CC02, ErrorRed: #FF4B4B, và các màu nền nhạt tương ứng). Xóa các đoạn comment liên quan đến lỗi alpha nếu có.
- `domain/src/main/java/com/nhimz/vocabmaster/domain/model/SettingsRepository.kt` & `data/src/main/java/com/nhimz/vocabmaster/data/repository/SettingsRepositoryImpl.kt` - Thêm storage cho `placementLevel` bằng DataStore Preference.
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/MainViewModel.kt` - Thêm hàm `savePlacementLevel(level: DifficultyLevel)`.
- `app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt` - Lưu kết quả PlacementTest qua MainViewModel trước khi gọi navigateTo(FirstWin).

## Phase 2: Shared UI Components
- [P] `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/DuolingoProgressBar.kt` - Viết thanh progress mảnh, fill mượt mà, cố định sát mép trên.
- [P] `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/DuolingoOptionCard.kt` - Viết card chọn đáp án, RÀNG BUỘC: elevation=0.dp, containerColor=solid. Thay đổi viền theo state (Default, Selected, Correct, Wrong).
- [P] `app/src/main/java/com/nhimz/vocabmaster/ui/components/quiz/FeedbackBanner.kt` - Viết banner trượt dưới đáy (AnimatedVisibility slideInVertically), chứa icon, text thông báo (Xanh/Đỏ) và nút Tiếp Tục to bản. Play sound khi bật lên.

## Phase 3: Core Logic (Mảng câu hỏi & Lặp câu sai)
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt` - Sửa logic `handleAnswer` và `nextQuestion`: Khi trả lời sai, thêm clone của câu hiện tại vào mảng để bắt buộc làm lại ở cuối. Cập nhật size progress.
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/FirstWinViewModel.kt` - (Tạo mới) Đọc level từ Settings, sinh ngẫu nhiên 7 câu hỏi, implement logic hàng đợi như QuizViewModel.

## Phase 4: Screens Integration
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/PlacementTestScreen.kt` - Tái cấu trúc Layout dùng bộ Shared UI mới. KHÔNG dùng tính năng lặp câu sai ở viewmodel (Test là test).
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt` - Tái cấu trúc Layout dùng bộ Shared UI mới. Tích hợp tính năng lặp câu sai.
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/FirstWinScreen.kt` - (Viết lại) Tích hợp `FirstWinViewModel`. UI Quiz dùng Shared Components. UI Celebration tích hợp Lottie + đếm tăng dần XP (0->50). Đảm bảo flow mượt mà từ Quiz sang Celebration.
