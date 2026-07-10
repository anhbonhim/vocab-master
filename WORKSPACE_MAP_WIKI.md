# Vocab Master - Complete Codebase File Map & Directory Guide

Tài liệu này cung cấp một bản đồ chi tiết về cấu trúc thư mục, sơ đồ liên kết và danh sách toàn bộ các tệp tin mã nguồn trong dự án **Vocab Master** để định hướng và cung cấp ngữ cảnh nhanh cho các Agent phát triển dự án sau này.

---

## 📂 Tổng quan cấu trúc thư mục (Multi-module)

```
vocab-master/
├── gradle/
│   └── libs.versions.toml              # Version Catalog quản lý phiên bản
├── domain/                             # Module nghiệp vụ cốt lõi (Pure Kotlin)
├── data/                               # Module lưu trữ & Persistence (Room, DataStore)
└── app/                                # Module giao diện người dùng (Jetpack Compose UI)
```

---

## 📄 Bản đồ chi tiết các tệp tin

### 1. Root Configuration Files
Các file cấu hình dự án ở thư mục gốc:
*   [build.gradle.kts](file:///c:/Users/nhimz/Documents/vocab-master/build.gradle.kts) — Cấu hình Gradle gốc và khai báo các plugin.
*   [settings.gradle.kts](file:///c:/Users/nhimz/Documents/vocab-master/settings.gradle.kts) — Đăng ký các module `:domain`, `:data`, `:app`.
*   [gradle.properties](file:///c:/Users/nhimz/Documents/vocab-master/gradle.properties) — Cấu hình tối ưu hóa JVM Build, bật cache và KSP2.
*   [local.properties](file:///c:/Users/nhimz/Documents/vocab-master/local.properties) — Chứa đường dẫn SDK cục bộ của thiết bị.

---

### 2. Module Nghiệp vụ Lõi — `:domain`
*Không chứa bất kỳ dependency nào của Android Framework.*

*   **Models & Thuật toán FSRS v6:**
    *   [Models.kt (FSRS)](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/Models.kt) — Định nghĩa các enums `Rating`, `State` và thực thể `Card`, `ReviewLog`.
    *   [FSRS.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/FSRS.kt) — Thuật toán FSRS v6 hoàn chỉnh với 21 tham số tính toán Stability & Difficulty.
*   **Business Models & Interface contracts:**
    *   [Models.kt (Model)](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/Models.kt) — Định nghĩa `VocabularyItem`, `DifficultyLevel` và cấu trúc thống kê `ReviewStats`.
    *   [VocabularyItemWithCard.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/VocabularyItemWithCard.kt) — Thực thể kết hợp chứa thông tin từ vựng và trạng thái học FSRS.
    *   [VocabularyRepository.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/VocabularyRepository.kt) — Interface contract quản lý từ vựng.
    *   [ReviewRepository.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/ReviewRepository.kt) — Interface contract quản lý lịch sử ôn tập.
    *   [SettingsRepository.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/SettingsRepository.kt) — Interface contract quản lý tuỳ chọn & Gamification (XP, Streak).
    *   [BackupRepository.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/model/BackupRepository.kt) — Interface contract xuất/nhập JSON backup.
*   **Use Cases (Nghiệp vụ nghiệp vụ độc lập):**
    *   [MapRatingUseCase.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/MapRatingUseCase.kt) — Ánh xạ thời gian trả lời ngầm sang đánh giá FSRS.
    *   [GenerateDistractorsUseCase.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/GenerateDistractorsUseCase.kt) — Sinh 3 đáp án nhiễu trắc nghiệm cùng từ loại.
    *   [PlacementTestUseCase.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/main/java/com/nhimz/vocabmaster/domain/usecase/PlacementTestUseCase.kt) — Logic bài test phân loại trình độ thích ứng.
*   **Unit Tests:**
    *   [FSRSTest.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/FSRSTest.kt) — Kiểm tra toán học FSRS so với test vectors gốc.
    *   [UseCasesTest.kt](file:///c:/Users/nhimz/Documents/vocab-master/domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/UseCasesTest.kt) — Test luồng Test trình độ, sinh đáp án và map rating.

---

### 3. Module Lưu Trữ & Persistence — `:data`
*Quản lý dữ liệu SQLite Room DB, preferences DataStore, nạp asset từ vựng và xuất backup.*

*   **Cấu hình Database (Room):**
    *   [VocabularyCardEntity.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/database/entity/VocabularyCardEntity.kt) — Bảng lưu từ vựng và scheduler state.
    *   [ReviewLogEntity.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/database/entity/ReviewLogEntity.kt) — Bảng lưu lịch sử các lượt ôn tập (cascade foreign key).
    *   [Converters.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/database/Converters.kt) — Ép kiểu LocalDateTime, State và Rating sang kiểu nguyên thuỷ SQLite.
    *   [VocabDao.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDao.kt) — Cung cấp các SQL queries lấy card do/due, đếm stats học tập.
    *   [VocabDatabase.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/database/VocabDatabase.kt) — Khai báo cấu hình Room Database.
*   **Dữ liệu từ vựng mẫu (Assets):**
    *   [vocabulary.json](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/assets/vocabulary.json) — File chứa toàn bộ kho dữ liệu từ vựng gốc `core_words.json` của user.
*   **Triển khai Repositories (Implementations):**
    *   [VocabularyRepositoryImpl.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/repository/VocabularyRepositoryImpl.kt) — Chứa logic `checkAndPrepopulate()` bóc tách file JSON assets và khởi tạo database.
    *   [ReviewRepositoryImpl.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/repository/ReviewRepositoryImpl.kt) — Thống kê XP và số liệu học tập.
    *   [SettingsRepositoryImpl.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/repository/SettingsRepositoryImpl.kt) — Lưu XP, Streaks, và Freeze vào DataStore Preferences.
    *   [BackupModels.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/model/BackupModels.kt) — Định nghĩa thực thể DTO đóng gói JSON backup.
    *   [BackupRepositoryImpl.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/repository/BackupRepositoryImpl.kt) — Thực hiện xuất/nhập tệp tin tiến độ học tập qua Room Transaction.
*   **Dependency Injection:**
    *   [DataModule.kt](file:///c:/Users/nhimz/Documents/vocab-master/data/src/main/java/com/nhimz/vocabmaster/data/di/DataModule.kt) — Khai báo Hilt Providers và Binds cho database, DAOs, và repositories.

---

### 4. Module Giao diện người dùng — `:app`
*Xây dựng toàn bộ giao diện Jetpack Compose, viewmodels và các tích hợp thiết bị (TTS, Alarm).*

*   **Khởi chạy & Điều phối chính:**
    *   [VocabApplication.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/VocabApplication.kt) — Điểm khởi đầu ứng dụng (`@HiltAndroidApp`).
    *   [MainActivity.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/MainActivity.kt) — Thiết lập Bottom Navigation và định tuyến màn hình (Routing).
    *   [AndroidManifest.xml](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/AndroidManifest.xml) — Khai báo các quyền hệ thống (`VIBRATE`, `POST_NOTIFICATIONS`) và Receiver.
*   **Hệ thống UI Screens (Jetpack Compose):**
    *   [WelcomeScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/WelcomeScreen.kt) — Màn hình mở đầu Onboarding.
    *   [GoalPickerScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/GoalPickerScreen.kt) — Màn hình lựa chọn mục tiêu học tập (5p - 20p).
    *   [PlacementTestScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/PlacementTestScreen.kt) — Làm bài test phân loại trình độ adaptive.
    *   [FirstWinScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/FirstWinScreen.kt) — Màn ăn mừng nhanh sau onboarding (3 câu hỏi dễ).
    *   [HomeScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/HomeScreen.kt) — Vòng tròn tiến độ học tập trong ngày, Streak & Freeze status, XP.
    *   [QuizScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt) — Giao diện quiz trắc nghiệm tráo chiều ngẫu nhiên, đo thời gian phản hồi, phát âm từ.
    *   [FlashcardScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/FlashcardScreen.kt) — Giao diện flashcard lật mặt 3D, đánh giá theo 4 nút FSRS thủ công.
    *   [ResultScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/ResultScreen.kt) — Ăn mừng phiên học thành công với hiệu ứng confetti.
    *   [StatisticsScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/StatisticsScreen.kt) — Vẽ biểu đồ cột học tập 7 ngày, Badges, và tab ôn tập Mistake Bank.
    *   [SettingsScreen.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt) — Tinh chỉnh desired retention, set giờ thông báo, backup/restore SAF.
*   **ViewModels:**
    *   [MainViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/MainViewModel.kt), [PlacementTestViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/PlacementTestViewModel.kt), [HomeViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/HomeViewModel.kt), [QuizViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/QuizViewModel.kt), [FlashcardViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/FlashcardViewModel.kt), [ResultViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/ResultViewModel.kt), [StatisticsViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/StatisticsViewModel.kt), [SettingsViewModel.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/SettingsViewModel.kt).
*   **Tích hợp hệ thống Android:**
    *   [TTSManager.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/tts/TTSManager.kt) — Quản lý giọng đọc Text-to-speech lifecycle-aware.
    *   [FeedbackHelper.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/ui/util/FeedbackHelper.kt) — Phát âm thanh bíp bíp đúng/sai và rung haptic có try-catch an toàn.
    *   [NotificationScheduler.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/notification/NotificationScheduler.kt) — Thiết lập báo thức hàng ngày bằng AlarmManager.
    *   [NotificationReceiver.kt](file:///c:/Users/nhimz/Documents/vocab-master/app/src/main/java/com/nhimz/vocabmaster/notification/NotificationReceiver.kt) — Xử lý sự kiện báo thức, tự động lấy 1 từ vựng A2/Due ngẫu nhiên trong DB và hiển thị thông báo.

---

## ⚠️ Quy tắc & Lưu ý quan trọng cho các Agent phát triển sau
1.  **Strict separation of layers:** Module `:domain` là Kotlin thuần. Tuyệt đối không được import bất kỳ class nào của Android (ví dụ: `android.*`, `Context`, `Flow` từ thư viện Android khác ngoài coroutines thuần).
2.  **Room migrations:** Nếu thay đổi cấu trúc bảng trong Entity, hãy cập nhật phiên bản DB trong `VocabDatabase.kt`. Room hiện đang được set `fallbackToDestructiveMigration()` để reset nhanh trong quá trình phát triển (MVP), nhưng cần tạo migration script nếu chuyển sang Production.
3.  **FSRS 21 parameters:** Tuyệt đối không thay đổi mảng 21 tham số mặc định của FSRS v6 trong `FSRS.kt` trừ khi tích hợp bộ Python Optimizer để huấn luyện weights mới từ lịch sử học tập.
4.  **Vibration permissions:** Nếu gọi bất kỳ dịch vụ phần cứng nào khác của hệ thống, luôn đảm bảo quyền tương ứng đã được khai báo trong `AndroidManifest.xml` và bọc hàm thực thi trong `try-catch` để phòng chống crash (tương tự như `FeedbackHelper`).
5.  **Navigation System:** Ứng dụng sử dụng **Custom state-based navigation**, tuyệt đối **KHÔNG** sử dụng `Jetpack Navigation Component` (ví dụ: `NavHost`, `NavController`). Mọi sự kiện định tuyến đều thông qua thay đổi trạng thái (State) ở `MainActivity`.
