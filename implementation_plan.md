# Vocab Master — Agentic Team Implementation Plan (Final V5)

> **Dựa trên:** [IMPLEMENTATION_PLAN_V4.md](file:///C:/Users/nhimz/Downloads/IMPLEMENTATION_PLAN_V4.md) và kết quả phân tích UX/Gamification.  
> **Trạng thái:** Đã chốt toàn bộ requirements. Chờ duyệt để bắt đầu Code (Phase 0).

---

## 1. Tổng quan dự án

Ứng dụng Android học từ vựng tiếng Anh A1–C2, offline-first, sử dụng thuật toán FSRS v6. Rewrite from scratch với Jetpack Compose + MVVM + Clean Architecture + Hilt DI.  
Tập trung mạnh vào **Gamification** (XP, Streak, Badges) và **UX mượt mà** (Micro-interactions, Lottie) tương tự Duolingo/Drops.

### Scope MVP (Những gì SẼ làm)
- Phase 0: Project skeleton + Gradle setup
- Phase 1: FSRS v6 engine (port từ FSRS-Kotlin) + unit tests
- Phase 2: Full UI + Gamification (Onboarding → Home → Quiz/Flashcard → Result → Statistics → Settings)
- Phase 4: JSON export/import backup

### Scope POST-MVP (Những gì CHƯA làm ở phiên bản này)
- ❌ On-device LLM (dùng rule-based cho MVP)
- ❌ Server sync / Event Sourcing
- ❌ Đa dạng Quiz types (True/False, Typing...) — MVP chỉ có 4-choice và Flashcard
- ❌ Word of the Day Widget
- ❌ Level Map Visualization

---

## 2. Tất cả quyết định đã chốt (35 quyết định)

| # | Hạng mục | Giá trị chốt |
|---|----------|---------------|
| 1-3 | **Data & Quiz** | User cung cấp file từ vựng (JSON/CSV). Cấu trúc: word, definition, part_of_speech, level, example, IPA. Quiz 2 chiều random (EN→VI / VI→EN). |
| 4-7 | **FSRS & Rules** | Response time → Rating (Sai=Again, >10s=Hard, 3-10s=Good, <3s=Easy). Không giới hạn thời gian trả lời. Desired retention: 0.9 (chỉnh được). |
| 8-12 | **System & UX** | Package: `com.nhimz.vocabmaster`. Min SDK: 26. Ngôn ngữ UI: Tiếng Việt. Dark mode: Light/Dark. TTS: Android mặc định. |
| 13-16 | **App Flow** | Placement Test 30-50 câu adaptive (A2-C2) để unlock level. Đồ họa: LottieFiles. BottomNav: Home \| Quiz \| Stats \| Settings. |
| 17-21 | **Session & UI** | Trộn thẻ due + new card trong 1 session. Xanh dương/tím chủ đạo, gradient nhẹ, Duolingo-style. Hiện Example + IPA. |
| 22-26 | **Others** | Single-device MVP (JSON backup). Windows build env. On-device LLM deferred. |
| **27** | **XP System** | Tính điểm XP mỗi câu đúng. Bonus XP cho chuỗi streak và perfect session. (Duolingo style) |
| **28** | **Streak Freeze** | Freeze 1-2 lần/tuần miễn phí. Cho phép Repair trong 24h nếu lỡ ngày học. |
| **29** | **Onboarding Flow** | Full flow: Welcome → Chọn Goal → Placement Test → First Win (3 câu nhẹ) → Home. |
| **30** | **Daily Goal** | Chọn thời gian (5p/10p/15p/20p) thay vì "10 thẻ". Hiển thị progress ring trên Home. |
| **31** | **Micro-interactions**| Rung (vibration) khi chọn đúng, shake animation khi sai, card flip animation. (Drops style) |
| **32** | **Rich Notification**| Notification daily hiện luôn 1 từ vựng + nghĩa thay vì chỉ nhắc "Vào học đi". |
| **33** | **Badges** | Unlock thành tựu: '100 từ đầu tiên', '7-day streak', 'Level Master', 'Perfect Session'. |
| **34** | **Session Recap** | Confetti animation khi kết thúc session hoàn hảo, kèm hiệu ứng +XP và tăng Streak. |
| **35** | **Quiz Modes & Stats**| Thêm mode 'Flashcard' cho power user. Thêm tab 'Từ hay sai' (>50% sai) trong Statistics. |

---

## 3. Kiến trúc kỹ thuật cập nhật

### Database Schema
*Thêm các trường phục vụ Gamification:*
- `user_profile` (DataStore/DB): XP total, current_streak, longest_streak, available_freezes, last_study_date.
- `badges_unlocked` (DB): badge_id, unlocked_at.
- `user_settings` (DataStore): daily_goal_minutes, notification_time, theme...

### Cấu trúc Màn hình (Compose UI)
1. **Onboarding**: Welcome → GoalPicker → PlacementTest → FirstWin
2. **Home**: Progress ring (Daily Goal), Total XP, Streak (kèm icon Freeze), Start Button.
3. **Quiz**: 
   - Mode 1: 4-choice (Vuốt/Tap)
   - Mode 2: Flashcard (Tap lật mặt → Chọn Again/Hard/Good/Easy)
4. **Result**: Confetti, XP Earned, Streak update, Time spent.
5. **Statistics**: Biểu đồ tiến độ, Badges Collection, Tab "Mistake Bank" (Từ hay sai).

---

## 4. Agentic Team Structure & Workflow

Sử dụng `agency-agents-orchestrator` để tự động spawn các specialized agents cho từng module:

| Agent | Subagent Type | Nhiệm vụ chính |
|-------|---------------|----------------|
| **Orchestrator** | `self` (tôi) | Điều phối, gọi các subagent, báo cáo tiến độ cho bạn |
| **A1: Architect** | `architect` | Tạo skeleton, Gradle Version Catalog, Hilt KSP setup (Phase 0) |
| **A2: FSRS Engine**| `fsrs-engine` | Code thuật toán FSRS v6, Domain logic, Unit tests (Phase 1) |
| **A3: Compose UI** | `compose-ui` | Code toàn bộ Jetpack Compose UI, Lottie, Animations, TTS (Phase 2) |
| **A4: Data Layer** | `data-layer` | Code Room DB, DataStore, JSON backup/restore (Phase 2+4) |
| **A5: QA Tester** | `qa-tester` | Chạy tests, verify builds, fix bugs xuyên suốt quá trình |

---

## 5. Kế hoạch chạy tự động (Next Steps)

Ngay sau khi bạn approve kế hoạch này, tôi sẽ bắt đầu chạy **Phase 0** thông qua kỹ năng `agency-agents-orchestrator`.

1. Khởi tạo `vocab-master` Android project.
2. Thiết lập Gradle Version Catalog (Compose, Room, Hilt, Serialization).
3. Đóng gói base project thành công và chạy `assembleDebug`.
4. Báo cáo kết quả Phase 0 cho bạn xem xét.

## User Review Required

> [!IMPORTANT]
> Bản kế hoạch này đã bao gồm tất cả các tính năng Gamification và giao diện nâng cao theo chuẩn Duolingo/Drops mà chúng ta vừa thảo luận (XP, Badges, Streak Freeze, Goal Picker, Animations, Flashcard mode).
> 
> Nếu bạn đồng ý, hãy gõ lệnh **/agency-agents-orchestrator** (hoặc nói "Đồng ý, chạy orchestrator") để tôi đánh thức đội ngũ sub-agents và bắt đầu tạo Project Skeleton ngay bây giờ!
