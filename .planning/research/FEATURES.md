# Feature Landscape

**Domain:** Spaced Repetition Vocabulary App
**Researched:** 2026-07-20

## Table Stakes

Features users expect. Missing = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| FSRS Algorithm Integrity | Spaced repetition apps live or die by scheduling correctness. | High | FSRS parameters and scheduling logic must strictly conform to the reference implementation without out-of-bounds errors or negative stabilities. |
| Crash-Free Experience | Users will abandon learning apps that crash mid-session. | Med | Requires removing unsafe casts (`as`, `!!`) and properly handling exceptions instead of swallowing them. |
| Smooth UI/UX | Monolithic screens cause UI jank and state mismanagement. | Med | Refactoring massive Compose files into granular, stateless components ensures smooth recomposition and navigation transitions. |
| Data Privacy | Learning histories and potential PII should not leak into cloud backups. | Low | Must implement explicit `data_extraction_rules.xml` to exclude sensitive data. |

## Differentiators

Features that set product apart. Not expected, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Clean Architecture UI | By isolating business logic in ViewModels and creating pure UI content composables, the app becomes highly testable and extensible. | High | Involves significant refactoring of existing monolithic Compose screens. |

## Anti-Features

Features to explicitly NOT build.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Multi-module refactor of existing domain/data | Clean Architecture structure (app, domain, data modules) is already validated. | Focus refactoring efforts on the `app` module (Compose screens and ViewModels). |
| Complete backend replacement | Out of scope for this milestone. | Fix FastAPI endpoints only if sync contracts break. |
| Adding new study modes | Diverts focus from core stabilization. | Defer to v2. Focus strictly on audit and refactor. |

## Feature Dependencies

```
Crash-Free Experience → Smooth UI/UX (Stable base required before UX polish)
Clean Architecture UI → Crash-Free Experience (Refactoring UI will inherently fix many unsafe casts and state bugs)
```

## MVP Recommendation

Prioritize:
1. Audit and repair FSRS algorithm calculation bugs.
2. Refactor monolithic screens to fix unsafe casts and exception swallowing.
3. Configure `data_extraction_rules.xml` to exclude sensitive databases/preferences.

Defer: New study modes and backend framework replacements.

## Sources

- [Project Specifications and Architecture Rules]