# Intent
Thu hẹp khoảng cách (gap) giữa Vocab Master và các ứng dụng học tiếng Anh phổ biến (Duolingo, Anki, Drops) bằng cách đại tu 4 khía cạnh: đa dạng hóa bài tập, trực quan hóa FSRS (Gamification), hỗ trợ Audio CDN offline chất lượng cao, và cá nhân hóa lộ trình học theo chủ đề.

# Scope
- **Data Layer:** Thêm `topic`, `audioUrl` vào `VocabularyCardEntity`, JSON assets, và Room DAO.
- **Multimedia:** Tích hợp `Coil` (nếu cần load ảnh CDN sau này), và `ExoPlayer` để tải/phát/cache file `.ogg` từ GitHub CDN, thay thế hoàn toàn `TTSManager`.
- **UI/UX:** Xây dựng `ScrambledQuizCard` (Sắp xếp câu), `MatchImageQuizCard` (Nối hình - nếu có data), và `FSRSTreeProgressBar` (hiển thị độ bền FSRS).
- **Personalization:** Cập nhật UI để người dùng chọn/hiển thị bài học theo Topic.

# Non-Goals
- KHÔNG sử dụng System TTS (Android Text-to-Speech) làm nguồn âm thanh chính.
- KHÔNG nhúng file âm thanh trực tiếp vào trong APK (tránh làm tăng kích thước app quá lớn).
- KHÔNG gọi API trực tiếp trong lúc học (buộc phải có cơ chế cache offline).
- KHÔNG thay đổi lõi thuật toán FSRS v6 (chỉ thay đổi cách hiển thị UI).

# Clarifications
- **Xử lý Audio Offline (Clarified):** Khi tải app lần đầu không có Internet để CDN cache audio, hệ thống sẽ fallback im lặng (bỏ qua phát âm thanh, không hiện Toast hay thông báo lỗi).
- **Mức độ Offline (Clarified):** Ứng dụng theo hướng "Cache-first" thay vì "100% Offline-first". Cần mạng ở lần nghe audio đầu tiên, các lần sau sẽ nghe offline nhờ cache.
- **Nguồn Dữ Liệu (Clarified):** Data Text (JSON) và Audio (Ogg) sẽ được sinh tự động 100% bằng script gọi qua CLIProxyAPI nội bộ (cổng 8317) sử dụng các model như `qwen3-235b` (Text) và `mimo-v2.5-tts` / `kokoro-82m` (Audio). Không cần sự can thiệp thủ công.