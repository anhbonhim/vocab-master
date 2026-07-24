# Phase 05: backend-infrastructure-content-api - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Cung cấp Backend infrastructure và API để quản lý và cấp phát nội dung học tập theo kiến trúc Curriculum mới (Topic -> Lesson -> Exercise). Bao gồm việc định nghĩa model, xử lý validation, tích hợp LLM để tự động sinh bài tập và cơ chế tiếp nhận phản hồi từ người dùng (User Report).
</domain>

<decisions>
## Implementation Decisions

### Thiết kế Data Model Backend
- **D-01:** Lưu Exercises dạng JSON (JSON field trong SQLite) cho linh hoạt nếu có nhiều loại exercise — **Reversibility:** costly — Nếu sau này cần query phức tạp trên từng trường của exercise, sẽ phải migrate sang bảng riêng.

### Tích hợp LLM sinh bài tập
- **D-02:** Backend (FastAPI) trực tiếp gọi API (Opencode go api) khi admin/script trigger. Python xử lý prompt & Pydantic parse JSON.

### Cơ chế Validation & Phản hồi
- **D-03:** Phản hồi (User Report) được lưu vào bảng `user_reports` trong DB để Admin duyệt sau.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Architecture & Specs
- `backend/app/main.py` — Current FastAPI endpoints and router setup.
- `.planning/REQUIREMENTS.md` — Specifies requirements CONT-01 to CONT-03 for Phase 5.
- `.planning/codebase/STACK.md` — Confirms Python 3.9+, FastAPI, SQLAlchemy, Pydantic, and SQLite for backend.
- `.planning/codebase/INTEGRATIONS.md` — Current SQLite Database context (`backend/vocab.db`) and Firebase Auth integration.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `backend/app/utils/firebase_auth.py` — Can be used to secure the new User Report endpoints to ensure only authenticated users can submit reports.
- `backend/app/config.py` (Pydantic Settings) — Should be extended to include LLM API keys (e.g., OPENCODE_API_KEY).

### Established Patterns
- **Database:** SQLAlchemy ORM models mapped to SQLite tables. (Applies to `user_reports` and Curriculum models like `Topic`, `Lesson`).
- **API Routing:** FastAPI APIRouter structure (e.g., `routers/vocabulary.py`). New endpoints should follow this modular pattern.
- **Validation:** Pydantic models for request/response schemas. Crucial for validating LLM output.

### Integration Points
- Backend needs to expose new endpoints (`/api/v1/curriculum/...` and `/api/v1/reports`) that the Android client will consume via Retrofit.

</code_context>

<specifics>
## Specific Ideas

- Sử dụng provider LLM thông qua "Opencode go api" thay vì các nhà cung cấp phổ biến như OpenAI/Gemini/Claude.
- Lưu trữ nội dung Exercise dưới dạng chuỗi JSON linh hoạt trong SQLite thay vì tạo các bảng quan hệ phức tạp, giúp dễ dàng mở rộng nhiều format bài tập (Nghe, Điền từ, Sắp xếp).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-backend-infrastructure-content-api*
*Context gathered: 2026-07-22*