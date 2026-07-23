# Phase 05: backend-infrastructure-content-api - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 05-backend-infrastructure-content-api
**Areas discussed:** Thiết kế Data Model Backend, Cơ chế Validation & Phản hồi, Tích hợp LLM sinh bài tập

---

## Thiết kế Data Model Backend

| Option | Description | Selected |
|--------|-------------|----------|
| Lưu trong SQLite (Relational) | Lưu Topic/Lesson/Exercise vào SQLite qua SQLAlchemy. App sẽ pull về lưu vào Room. Giữ kiến trúc đơn giản hiện tại. | |
| Lưu Exercises dạng JSON | Lưu trữ Exercises ở dạng JSON (JSON field trong SQLite) cho linh hoạt nếu có nhiều loại exercise. | ✓ |

**User's choice:** Lưu Exercises dạng JSON
**Notes:** N/A

---

## Cơ chế Validation & Phản hồi

| Option | Description | Selected |
|--------|-------------|----------|
| Lưu vào DB (Admin duyệt sau) | API `/report` lưu vào bảng SQLite `user_reports`. Cần admin UI hoặc API để đọc sau. | ✓ |
| Gửi qua Webhook (Discord/Telegram) | Báo cáo gửi thẳng qua Webhook (vd: Discord/Slack/Telegram) để xử lý nhanh, không cần lưu DB Backend. | |

**User's choice:** Lưu vào DB (Admin duyệt sau)
**Notes:** N/A

---

## Tích hợp LLM sinh bài tập

| Option | Description | Selected |
|--------|-------------|----------|
| Backend tự gọi trực tiếp | Backend (FastAPI) trực tiếp gọi OpenAI/Gemini khi admin/script trigger. Python xử lý prompt & Pydantic parse JSON. | ✓ |
| Script độc lập (Tách khỏi API) | Tách riêng một Python script chạy độc lập (cron/manual), gen xong insert thẳng vào SQLite. Backend chỉ việc serve. | |

**User's choice:** Backend tự gọi trực tiếp
**Notes:** N/A

---

## Tích hợp LLM sinh bài tập (Provider)

| Option | Description | Selected |
|--------|-------------|----------|
| OpenAI API | Sử dụng OpenAI API (GPT-4o / GPT-4o-mini) vì JSON mode ổn định. | |
| Google Gemini API | Sử dụng Google Gemini API (vì có sẵn trong hệ sinh thái Google / dễ cấp key). | |
| Claude API | Sử dụng Anthropic Claude API. | |
| Opencode go api | Custom provider provided by user | ✓ |

**User's choice:** Opencode go api
**Notes:** Sử dụng API riêng do user cung cấp.

---

## the agent's Discretion

None.

## Deferred Ideas

None.
