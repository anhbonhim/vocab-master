---
phase: "05"
slug: "backend-infrastructure-content-api"
date: "2026-07-22"
---

# Nyquist Validation Strategy: Phase 05

This file defines the acceptable bounds and validation requirements for Phase 05. It bridges the gap between the phase scope and execution outcomes to prevent regressions and untested edge cases.

## Automated Verification Target

| Tool/Framework | Coverage Target | Command |
|----------------|-----------------|---------|
| Pytest         | 80%             | `pytest backend/tests/ -v --cov=backend/app` |

*GSD uses this section to run verification autonomously via `gsd-validate-phase`.*

## Edge Constraints & Validation Requirements

1. **Exercise Schema Validation (CONT-03)**:
   - Data produced by LLM must be strictly validated by Pydantic V2 before database insertion.
   - Any invalid data must be rejected and logged clearly without crashing the service.
   - Pydantic models must include `difficulty` levels.

2. **Async Integrity (CONT-02)**:
   - Interactions with Opencode API must use `httpx.AsyncClient`.
   - Blocking calls (`requests`) are prohibited within the LLM generation service to preserve FastAPI event loop performance.

3. **Curriculum Endpoints (CONT-01)**:
   - Core API models (Topic, Lesson) must match the JSON requirements of the UI client.
   - Firebase UID authentication must be correctly applied to User Report endpoints.
