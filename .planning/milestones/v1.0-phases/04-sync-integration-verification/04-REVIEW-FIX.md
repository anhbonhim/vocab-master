---
status: all_fixed
findings_in_scope: 5
fixed: 5
skipped: 0
iteration: 1
---

# Code Review Fix Report

All critical and warning issues found during code review have been successfully fixed.

| ID | Severity | Description | Fix Commit |
|----|----------|-------------|------------|
| CR-01 | Critical | `getReviewLogsFlow` gọi suspend function từ non-suspend context | aee8dcd |
| CR-02 | Critical | `VocabularyCardDto` dùng epoch millis string thay vì ISO format | 6318a76 |
| WR-01 | Warning | `triggerSync()` thiếu concurrency guard | 9f7125a |
| WR-02 | Warning | `ApiClient` base URL cứng + HTTP logging không điều kiện | 238ce9c |
| WR-03 | Warning | `SnackbarMessage` data class với lambda field phá equals/hashCode | b9f40f5 |

The 3 info issues were skipped as they were outside of the critical/warning fix scope.