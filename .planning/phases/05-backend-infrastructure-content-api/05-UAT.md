---
status: complete
phase: 05-backend-infrastructure-content-api
source: 05-01-SUMMARY.md, 05-02-SUMMARY.md, 05-03-SUMMARY.md
started: 2026-07-23T00:28:35Z
updated: 2026-07-23T00:28:35Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. D1
expected: GET /api/v1/curriculum/topics returns [] when no Topic rows exist (tracer happy path, end-to-end DB -> HTTP response)
result: pass
source: automated
coverage_id: D1

### 2. D2
expected: GET /api/v1/curriculum/topics/{id}/lessons returns [] for a known topic with no lessons, and 404 for an unknown topic
result: pass
source: automated
coverage_id: D2

### 3. D3
expected: Lesson.exercises_data round-trips as a JSON list through Pydantic response (D-01 storage contract)
result: pass
source: automated
coverage_id: D3

### 4. D4
expected: POST /api/v1/reports requires Firebase auth, persists with the token-derived uid, rejects empty messages, and ignores body-spoofed uids
result: pass
source: automated
coverage_id: D4

### 5. D5
expected: SQLAlchemy Lesson model declares exercises_data as a JSON column (D-01)
result: pass
source: automated
coverage_id: D5

### 6. D6
expected: UserReport ORM model exists for D-03 storage
result: pass
source: automated
coverage_id: D6

### 7. D1
expected: httpx==0.27.0 is pinned in backend/requirements.txt (CONT-02 foundation; async Opencode client)
result: pass
source: automated
coverage_id: D1

### 8. D2
expected: Settings class parses OPENCODE_API_KEY / URL / MODEL / TIMEOUT_SECONDS from env
result: pass
source: automated
coverage_id: D2

### 9. D3
expected: Valid JSON payload parses into LLMResponse with a list of ExerciseItem
result: pass
source: automated
coverage_id: D3

### 10. D4
expected: Markdown ```json ... ``` and bare ``` ... ``` wrappers are stripped before Pydantic validation (LLM-typical shape)
result: pass
source: automated
coverage_id: D4

### 11. D5
expected: Missing required fields raise ValidationError, propagated to the caller as ValueError
result: pass
source: automated
coverage_id: D5

### 12. D6
expected: Malformed JSON string (broken syntax or non-JSON text) raises ValueError
result: pass
source: automated
coverage_id: D6

### 13. D7
expected: Empty / whitespace-only input is rejected (does not silently pass as an empty model)
result: pass
source: automated
coverage_id: D7

### 14. D8
expected: Well-formed payload with an empty exercises list is accepted (intentional zero-exercise response)
result: pass
source: automated
coverage_id: D8

### 15. D9
expected: Pydantic type-checks options: a non-list options value is rejected with ValueError
result: pass
source: automated
coverage_id: D9

### 16. D1
expected: Async LLM call is made via httpx.AsyncClient (NOT httpx.Client sync, NOT requests) per plan must_haves
result: pass
source: automated
coverage_id: D1

### 17. D2
expected: Configurable timeout is set on httpx.AsyncClient (T-05-03 DoS mitigation, default 60.0s from settings)
result: pass
source: automated
coverage_id: D2

### 18. D3
expected: httpx.TimeoutException is caught and re-raised as LLMServiceError so callers catch one exception type
result: pass
source: automated
coverage_id: D3

### 19. D4
expected: httpx.HTTPStatusError (non-2xx) is caught and re-raised as LLMServiceError
result: pass
source: automated
coverage_id: D4

### 20. D5
expected: Service refuses to issue any HTTP call when OPENCODE_API_KEY is empty (defensive guard)
result: pass
source: automated
coverage_id: D5

### 21. D6
expected: Service returns raw assistant text (NOT parsed JSON) so validate_llm_output remains the single chokepoint (T-05-02)
result: pass
source: automated
coverage_id: D6

### 22. D7
expected: Content script wires the full pipeline: fetch vocab -> LLM call -> validate -> save to Lesson.exercises_data (D-01)
result: pass
source: automated
coverage_id: D7

### 23. D8
expected: Content script aborts BEFORE calling LLM when no vocabulary rows exist (no wasted tokens, no junk persisted)
result: pass
source: automated
coverage_id: D8

### 24. D9
expected: LLMServiceError and Pydantic ValueError both propagate from the script so the CLI can log + exit non-zero
result: pass
source: automated
coverage_id: D9

### 25. D10
expected: save_exercises_to_lesson round-trips through the SQLAlchemy JSON column (D-01)
result: pass
source: automated
coverage_id: D10

## Summary

total: 25
passed: 25
issues: 0
pending: 0
skipped: 0

## Gaps

