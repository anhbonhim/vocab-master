<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Lưu Exercises dạng JSON (JSON field trong SQLite) cho linh hoạt nếu có nhiều loại exercise — **Reversibility:** costly — Nếu sau này cần query phức tạp trên từng trường của exercise, sẽ phải migrate sang bảng riêng.
- **D-02:** Backend (FastAPI) trực tiếp gọi API (Opencode go api) khi admin/script trigger. Python xử lý prompt & Pydantic parse JSON.
- **D-03:** Phản hồi (User Report) được lưu vào bảng `user_reports` trong DB để Admin duyệt sau.

### the agent's Discretion
- Sử dụng provider LLM thông qua "Opencode go api" thay vì các nhà cung cấp phổ biến như OpenAI/Gemini/Claude.
- Lưu trữ nội dung Exercise dưới dạng chuỗi JSON linh hoạt trong SQLite thay vì tạo các bảng quan hệ phức tạp, giúp dễ dàng mở rộng nhiều format bài tập (Nghe, Điền từ, Sắp xếp).

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CONT-01 | Thiết kế API endpoints trên FastAPI backend để cấp phát dữ liệu Topic, Lesson và bài tập (hỗ trợ tải động theo nhu cầu). | API schema and routing design in FastAPI. |
| CONT-02 | Tích hợp luồng sử dụng AI (LLM) ở backend để tự động sinh bài tập (nghe, điền từ, sắp xếp) từ bộ từ vựng, đi kèm script kiểm định chất lượng nội dung. | Integration with Opencode go api via `httpx` (needs adding) or `urllib`, prompting techniques. |
| CONT-03 | Xây dựng Schema Validation (ví dụ bằng Pydantic trên backend) để đảm bảo dữ liệu sinh ra hoặc nhập vào luôn đúng cấu trúc và phân cấp độ khó. | Pydantic V2 validation strategies for parsed LLM JSON output. |
</phase_requirements>

# Phase 05: Backend Infrastructure & Content API - Research

**Researched:** 2026-07-22
**Domain:** FastAPI Backend, Content Delivery API, LLM Integration, Pydantic Validation
**Confidence:** HIGH

## Summary

Phase 5 tập trung vào việc mở rộng FastAPI backend hiện tại để phục vụ nội dung học tập theo kiến trúc phân cấp mới (Topic -> Lesson -> Exercise). Nó cũng bao gồm một kịch bản sinh bài tập tự động từ danh sách từ vựng hiện có thông qua việc gọi API của Opencode (thay thế cho OpenAI/Gemini) và sử dụng Pydantic để đảm bảo JSON sinh ra hoàn toàn hợp lệ.

**Primary recommendation:** Sử dụng FastAPI routers cho CRUD Topic/Lesson, lưu trữ Exercises dưới dạng chuỗi JSON trong SQLite, gọi Opencode API bằng `httpx` hoặc thư viện async HTTP, và dùng `model_validate_json` của Pydantic V2 để ép kiểu & validate output của LLM.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Quản lý Topic & Lesson | API / Backend | Database | Backend cung cấp REST endpoints, SQLAlchemy thao tác SQLite. |
| Lưu trữ Exercise đa định dạng | Database | API / Backend | SQLite lưu trữ dạng JSON string; Backend deserialize qua Pydantic trước khi trả về. |
| Sinh bài tập tự động (LLM) | API / Backend | External Service | Backend trigger kịch bản, gọi sang Opencode go api để lấy kết quả dạng JSON. |
| Validation dữ liệu LLM | API / Backend | — | Sử dụng Pydantic ở layer service để validate trước khi insert vào DB. |
| Xử lý User Report | API / Backend | Database | Nhận POST request từ client, xác thực qua Firebase, lưu SQLite `user_reports`. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `fastapi` | 0.111.0 | API Framework | Đã có sẵn trong dự án (`backend/requirements.txt`). Rất tốt cho việc parse JSON. |
| `pydantic` | 2.14.0a1 | Data Validation | Cốt lõi của FastAPI, lý tưởng để validate cấu trúc phức tạp từ LLM. |
| `sqlalchemy` | 2.0.51 | ORM | Đã có sẵn, dùng để tương tác với SQLite. |
| `httpx` (cần cài thêm) | >0.25.0 | Async HTTP Client | [ASSUMED] Chuẩn de-facto cho async HTTP requests trong FastAPI để gọi Opencode API (không block event loop). |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `pydantic-settings` | 2.14.2 | Config | Quản lý API Key của Opencode từ biến môi trường. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `httpx` | `requests` | `requests` là synchronous, sẽ block event loop của FastAPI khi chờ LLM phản hồi (vốn thường mất nhiều giây). `httpx` hỗ trợ `async/await`. |
| Relational Exercise Tables | JSON string in SQLite | Theo quyết định D-01, lưu JSON giúp thêm loại bài tập mới (Nghe, Điền từ, v.v.) mà không cần migration schema liên tục, đánh đổi bằng việc khó query SQL trên thuộc tính con. |

**Installation:**
```bash
# Trong thư mục backend
pip install httpx
# Cập nhật requirements.txt
echo "httpx==0.27.0" >> requirements.txt
```

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `httpx` | PyPI | [ASSUMED] | [ASSUMED] | github.com/encode/httpx | [ASSUMED] | Approved - Standard async client |

*Packages discovered via WebSearch or training data that have not been verified against an authoritative source are tagged `[ASSUMED]` and the planner must gate each install behind a `checkpoint:human-verify` task.*

## Architecture Patterns

### Recommended Project Structure
Bám sát cấu trúc hiện có của ứng dụng:
```
backend/app/
├── models/
│   ├── curriculum.py    # Chứa class Topic, Lesson, Exercise (ORM)
│   └── report.py        # Chứa class UserReport (ORM)
├── schemas/
│   ├── curriculum.py    # Pydantic models (TopicCreate, LessonResponse, Exercise schemas)
│   └── report.py        # Pydantic models (ReportCreate)
├── routers/
│   ├── curriculum.py    # GET /api/v1/curriculum/topics, lessons...
│   └── report.py        # POST /api/v1/reports
├── services/
│   ├── llm_service.py   # Chứa logic gọi Opencode go api & Pydantic validation
│   └── content_gen.py   # Script/cronjob wrap llm_service để sinh bài tập tự động
```

### Pattern 1: Pydantic Validation cho LLM Output
**What:** Sử dụng Pydantic để định nghĩa chính xác Schema mà LLM cần sinh ra, sau đó parse chuỗi trả về.
**When to use:** Khi nhận JSON string từ Opencode API.
**Example:**
```python
import json
from pydantic import BaseModel, ValidationError

class ExerciseItem(BaseModel):
    type: str # "multiple_choice", "fill_blank", "listening"
    question: str
    options: list[str] = None
    correct_answer: str

class LLMResponse(BaseModel):
    exercises: list[ExerciseItem]

# Trong service gọi LLM:
def validate_llm_output(json_str: str) -> list[dict]:
    try:
        # Nếu LLM trả về markdown code block, cần parse cẩn thận
        if json_str.startswith("```json"):
            json_str = json_str.strip("```json").strip("```").strip()
            
        validated_data = LLMResponse.model_validate_json(json_str)
        return [ex.model_dump() for ex in validated_data.exercises]
    except ValidationError as e:
        # Handle error (retry LLM or log failure)
        raise ValueError(f"LLM output invalid: {e}")
```

### Anti-Patterns to Avoid
- **Synchronous HTTP calls in FastAPI:** Không dùng thư viện `requests` để gọi Opencode API trong endpoint hoặc service async. Nó sẽ block worker thread. Sử dụng `httpx.AsyncClient`.
- **Trusting LLM JSON blindly:** Không bao giờ lưu thẳng JSON từ LLM vào DB mà không qua bước `BaseModel.model_validate_json()` của Pydantic. LLM có thể sinh ra JSON thiếu field hoặc sai kiểu dữ liệu (ví dụ: trả về string thay vì list).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Gọi HTTP bất đồng bộ | Dùng luồng (threads) thủ công với `requests` | `httpx` | `httpx` thiết kế riêng cho `asyncio`, tích hợp hoàn hảo với FastAPI. |
| Validate cấu trúc JSON | Tự viết các câu lệnh `if key in dict:` | Pydantic Models | Pydantic V2 cực nhanh, cung cấp lỗi chi tiết và tự động ép kiểu dữ liệu. |

## Runtime State Inventory

> Omitted as this is a greenfield phase (adding new endpoints/services, not migrating/renaming existing core systems).

## Common Pitfalls

### Pitfall 1: LLM trả về JSON bị bọc trong Markdown
**What goes wrong:** `httpx` nhận được chuỗi ````json\n{"exercises": [...]}\n```` thay vì `{...}`. `model_validate_json` sẽ throw Exception.
**Why it happens:** LLM thường được huấn luyện để trình bày code block.
**How to avoid:** Luôn preprocess chuỗi (strip backticks và chữ `json`) hoặc sử dụng Prompt chỉ thị: "Trả về CHỈ JSON nguyên thủy, không bọc trong markdown code block".

### Pitfall 2: SQLite JSON Field limitations
**What goes wrong:** Khi lưu Python `dict` vào cột SQLite bằng SQLAlchemy `JSON` type, đôi khi cần đảm bảo dữ liệu được dump đúng.
**How to avoid:** Với SQLAlchemy, cột `JSON` sẽ tự động xử lý. Tuy nhiên, nếu dùng String thuần túy, phải `json.dumps()` trước khi lưu. Khuyến nghị dùng kiểu `JSON` của SQLAlchemy: `from sqlalchemy import JSON`.

## Code Examples

Verified patterns from official sources:

### SQLAlchemy Model với JSON Type
```python
from sqlalchemy import Column, Integer, String, ForeignKey, JSON
from app.database import Base

class Lesson(Base):
    __tablename__ = "lessons"
    id = Column(Integer, primary_key=True, index=True)
    topic_id = Column(Integer, ForeignKey("topics.id"))
    title = Column(String, index=True)
    # Lưu JSON chứa danh sách bài tập. D-01: "Lưu Exercises dạng JSON"
    exercises_data = Column(JSON, default=list) 
```

### Gọi Opencode API bằng HTTPX
```python
import httpx
from app.config import settings

async def generate_exercises_from_llm(vocabulary_list: list[str]) -> str:
    prompt = f"Tạo bài tập cho các từ vựng sau: {', '.join(vocabulary_list)}. Trả về định dạng JSON với cấu trúc: {{'exercises': [...]}}"
    
    # Thay endpoint thực tế của Opencode go api vào đây
    api_url = "http://localhost:8080/v1/chat/completions" # [ASSUMED] Cần config đúng url
    headers = {"Authorization": f"Bearer {settings.OPENCODE_API_KEY}"}
    payload = {
        "model": "gemini-3.1-pro-low(high)", # Hoặc model được cấp
        "messages": [{"role": "user", "content": prompt}]
    }
    
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(api_url, json=payload, headers=headers)
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]
```

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Dùng `httpx` làm HTTP client cho FastAPI | Standard Stack | Nếu không được phép cài thêm package, phải dùng thư viện chuẩn `urllib` bọc trong `run_in_threadpool`. |
| A2 | Cấu trúc payload của Opencode go api giống với OpenAI (messages, choices) | Code Examples | Sẽ lỗi parse response JSON. Cần cập nhật đúng cấu trúc API thực tế lúc implement. |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Python | Backend | ✓ | 3.14.6 | — |
| Opencode API | Sinh bài tập | ✗ | — | Cần config `OPENCODE_API_KEY` và URL trong `.env` |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | pytest |
| Config file | none — see Wave 0 |
| Quick run command | `pytest backend/tests/` |
| Full suite command | `pytest backend/tests/` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CONT-01 | GET /api/v1/curriculum/topics trả về 200 và JSON array | unit | `pytest backend/tests/test_curriculum.py` | ❌ Wave 0 |
| CONT-02 | Service LLM gọi httpx và trả về chuỗi JSON | unit | `pytest backend/tests/test_llm_service.py` | ❌ Wave 0 |
| CONT-03 | Pydantic validator throws error khi JSON thiếu field | unit | `pytest backend/tests/test_schemas.py` | ❌ Wave 0 |

### Wave 0 Gaps
- [ ] `backend/tests/test_curriculum.py` — covers CONT-01
- [ ] `backend/tests/test_llm_service.py` — covers CONT-02
- [ ] `backend/tests/test_schemas.py` — covers CONT-03
- [ ] Framework install: `pip install pytest httpx` 

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Sử dụng `get_current_user_uid` từ Firebase auth cho các endpoint tạo report hoặc học tập. |
| V4 Access Control | yes | Admin/Script trigger tính năng LLM cần có role hoặc secret key bảo vệ, không để public. |
| V5 Input Validation | yes | Pydantic V2 models cho mọi dữ liệu vào (User Report) và dữ liệu từ LLM. |
| V6 Cryptography | yes | HTTPX gọi Opencode API nên dùng HTTPS. |

### Known Threat Patterns for FastAPI

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Prompt Injection từ User | Spoofing/Tampering | Nếu truyền input của user vào LLM, cần sanitize hoặc bọc trong cấu trúc nghiêm ngặt. (Ở phase này, input là bộ từ vựng có sẵn ở DB, ít rủi ro hơn). |
| Chặn Event Loop do LLM chậm | DoS | Dùng `async def` và `httpx.AsyncClient` để không block FastAPI threads. Đặt timeout (VD: 60s) cho HTTP client. |
| LLM trả về mã độc XSS trong bài học | Tampering | Dù Validate qua Pydantic, Client (Android) cũng cần xử lý text an toàn, không render raw HTML (với Compose thì mặc định an toàn). |
