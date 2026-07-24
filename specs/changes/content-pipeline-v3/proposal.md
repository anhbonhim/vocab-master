# Intent
Thay thế pipeline sinh bài tập tự động 100% bằng AI (qwen3-235b) — vốn sinh ra nội dung kém chất lượng, sai lệch ngữ pháp và lặp từ — bằng một pipeline lai ghép (hybrid): sinh bài tập 7 dạng (INTRODUCTION, FILL_IN_BLANK, MULTIPLE_CHOICE, LISTENING, MATCHING, SCRAMBLED, TYPING) theo cơ chế xác định (deterministic) từ 4 nguồn dữ liệu mở có bản quyền hợp pháp (CEFR-J Wordlist, Open English WordNet 2025, Tatoeba text/audio CC-BY 4.0, Wiktionary CC-BY-SA 4.0), đồng thời thu hẹp vai trò của LLM chỉ cho việc tạo mạch truyện/bối cảnh kịch bản (narrative). Sau khi kiểm thử bài học v3 thành công, dọn dẹp triệt để các file log, script cũ và asset dư thừa gây lãng phí dung lượng APK.

# Scope
- **Corpus & Data Mining:** Tải và bóc tách dữ liệu từ CEFR-J Wordlist v1.6 (danh sách từ A1-B2), Open English WordNet 2025 (định nghĩa, ví dụ, từ đồng nghĩa/trái nghĩa, hypernym/coordinate terms), Tatoeba (cặp câu En-Vn đã kiểm duyệt và audio CC-BY 4.0), và Wiktionary (dịch nghĩa En-Vn bổ trợ).
- **Structured Vocab DB:** Xây dựng file `vocab_structured.json` hợp nhất dữ liệu từ vựng kèm theo mảng `sources[]` ghi nhận nguồn bản quyền cho từng từ.
- **Deterministic Question Generator:** Viết pipeline `tools/generate_lessons_v3.py` sinh 7 dạng câu hỏi bằng thuật toán cố định (0% variance, 100% đúng ngữ pháp và distractors không lặp). LLM `qwen3-235b` chỉ gọi cho `storySummary`, `scenarioContext` và `keyPhrases`.
- **Hybrid Audio Pipeline:** Kết hợp Edge-TTS cho phát âm từ vựng đơn và Tatoeba audio CC-BY 4.0 cho phát âm câu.
- **Validation & MVP Scale:** Đánh giá chất lượng `lessons_v3.json` trên phạm vi MVP (Section 1 + 2: A1.1 và A1.2, 14 Units, ~98 từ).
- **Runtime Migration:** Cập nhật `VocabularyRepositoryImpl.kt` và `backend/seed_db.py` để đọc file `lessons_v3.json` thay cho `lessons_v2.json`.
- **Attribution & Licenses:** Thêm file `THIRD_PARTY_LICENSES.md` trong assets và bổ sung màn hình hiển thị nguồn dữ liệu trong `SettingsScreen.kt`.
- **Cleanup Phase:** Xóa 9 script Python cũ ở root (`generate_lessons_v2.py`, `generate_assets.py`, `regenerate_broken.py`, v.v.), xóa 4 file checkpoint/repair orphan trong assets (`lessons_v2_repaired.json`, `lessons_v2_checkpoint.json`, `lessons_checkpoint.json`, `regenerate_queue.json`), xóa file legacy `lessons_v2.json` sau khi v3 chạy ổn định, và dọn dẹp toàn bộ file log ở root.

# Non-Goals
- KHÔNG thay đổi lõi thuật toán FSRS v6 hay cấu trúc Room Database của ứng dụng Android.
- KHÔNG sử dụng các nguồn dữ liệu có vấn đề pháp lý hoặc không có giấy phép thương mại (như NGSL thương mại share-alike, audio Tatoeba CC-BY-NC hoặc No-Offsite).
- KHÔNG thay đổi schema JSON của `lessons_v3.json` so với `lessons_v2.json` để tránh phải sửa code Compose UI và ViewModel.
- KHÔNG mở rộng quy mô dữ liệu vượt quá A1 (A1.1 & A1.2) trong giai đoạn MVP này.
