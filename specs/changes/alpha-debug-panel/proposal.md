# Intent
Tạo một màn hình Debug Panel chuyên dụng cho giai đoạn Alpha Test. Màn hình này giúp theo dõi và kiểm soát chất lượng (QC) nội dung asset (đặc biệt là Audio CDN), trực quan hóa trạng thái ẩn của thuật toán (FSRS), và bắt lỗi runtime/network dễ dàng, phục vụ trực tiếp cho quá trình kiểm thử `feature-parity-overhaul` và các tính năng tương lai.

# Scope
- **UI:** Thêm một màn hình/chế độ view (Debug Screen) chỉ hiển thị khi `BuildConfig.DEBUG == true`.
- **Audio CDN QC:** Module nghe thử âm thanh của bất kỳ từ nào trong DB, hiển thị link CDN và trạng thái Cache (Hit/Miss).
- **FSRS & Database Inspector:** Module xem trực tiếp các tham số ẩn của `VocabularyCardEntity` (stability, difficulty, interval) và thống kê database (card count by state/topic).
- **Quiz Engine Stats:** Module thống kê tần suất phân phối các `QuizType`.
- **Log System:** Hệ thống ghi nhận in-memory log cho Crash/Error và Network request, cho phép export ra bộ nhớ máy dưới dạng `.txt`/`.json`.
- **Extensibility:** Cấu trúc UI dạng Tab hoặc Section để dễ dàng cắm (plug-in) các module test mới sau này.

# Non-Goals
- KHÔNG hiển thị hoặc nhúng bất kỳ dòng code/UI nào liên quan đến Debug Panel vào bản build Release (`BuildConfig.DEBUG == false`).
- KHÔNG sửa đổi flow học tập chính (Quiz/Flashcard) ngoài việc emit log event.
- KHÔNG gửi log tự động lên cloud (Firebase Crashlytics, Sentry) — chỉ dùng local log.