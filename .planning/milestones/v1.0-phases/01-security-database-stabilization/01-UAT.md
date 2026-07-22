---
status: complete
phase: 01-security-database-stabilization
source: 01-01-SUMMARY.md, 01-02-SUMMARY.md, 01-03-SUMMARY.md, 01-04-SUMMARY.md, 01-05-SUMMARY.md, 01-06-SUMMARY.md, 01-07-SUMMARY.md
started: 2026-07-22T00:00:00Z
updated: 2026-07-22T00:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Quiz screen shows a visible error state with retry when session/question loads fail
expected: Màn hình Quiz hiển thị trạng thái lỗi có thể nhìn thấy kèm theo nút thử lại khi tải session/câu hỏi thất bại.
result: pass

### 2. Core scheduling parity: Good×6/Again×2/Good×5 interval history matches py-fsrs [0,2,11,46,163,498,0,0,2,4,7,12,21]
expected: Core scheduling parity: Good×6/Again×2/Good×5 interval history matches py-fsrs [0,2,11,46,163,498,0,0,2,4,7,12,21]
result: pass
source: automated
coverage_id: D1

### 3. Memo-state parity: Again/Good×5 with elapsed days [0,0,1,3,8,21] yields stability 53.62691 and difficulty 6.3574867
expected: Memo-state parity: Again/Good×5 with elapsed days [0,0,1,3,8,21] yields stability 53.62691 and difficulty 6.3574867
result: pass
source: automated
coverage_id: D2

### 4. Learning/relearning step transitions and timing bounds (Again ~1 min, Hard ~5.5 min, Good ~10 min, Easy >=1 day)
expected: Learning/relearning step transitions and timing bounds (Again ~1 min, Hard ~5.5 min, Good ~10 min, Easy >=1 day)
result: pass
source: automated
coverage_id: D3

### 5. Empty learning/relearning steps fall back to Review-state intervals >= 1 day
expected: Empty learning/relearning steps fall back to Review-state intervals >= 1 day
result: pass
source: automated
coverage_id: D4

### 6. Maximum-interval cap is respected on repeated reviews
expected: Maximum-interval cap is respected on repeated reviews
result: pass
source: automated
coverage_id: D5

### 7. Stability lower bound (>= 0.001) holds across 1000 Again reviews
expected: Stability lower bound (>= 0.001) holds across 1000 Again reviews
result: pass
source: automated
coverage_id: D6

### 8. Scheduler parameter validation throws IllegalArgumentException for out-of-bounds, wrong-length arrays
expected: Scheduler parameter validation throws IllegalArgumentException for out-of-bounds, wrong-length arrays
result: pass
source: automated
coverage_id: D7

### 9. Card/ReviewLog/Scheduler dict and JSON round-trips preserve every field using py key names
expected: Card/ReviewLog/Scheduler dict and JSON round-trips preserve every field using py key names
result: pass
source: automated
coverage_id: D8

### 10. rescheduleCard replays sorted logs and rejects mismatched cardId with the expected message
expected: rescheduleCard replays sorted logs and rejects mismatched cardId with the expected message
result: pass
source: automated
coverage_id: D9

### 11. Fuzz property: seeded fuzzed intervals stay inside the computed [minIvl, maxIvl] band and are deterministic
expected: Fuzz property: seeded fuzzed intervals stay inside the computed [minIvl, maxIvl] band and are deterministic
result: pass
source: automated
coverage_id: D10

### 12. FSRS-6 Optimizer ported with BCE batch loss, Adam training, cosine annealing, bounds clamping, and py-fsrs early-return guards
expected: FSRS-6 Optimizer ported with BCE batch loss, Adam training, cosine annealing, bounds clamping, and py-fsrs early-return guards
result: pass
source: automated
coverage_id: D1

### 13. Trained parameters respect LOWER/UPPER_BOUNDS_PARAMETERS and never contain NaN
expected: Trained parameters respect LOWER/UPPER_BOUNDS_PARAMETERS and never contain NaN
result: pass
source: automated
coverage_id: D2

### 14. computeOptimalRetention validates input size and reviewDuration, evaluates candidate retentions, and returns a value from the candidate set
expected: computeOptimalRetention validates input size and reviewDuration, evaluates candidate retentions, and returns a value from the candidate set
result: pass
source: automated
coverage_id: D3

### 15. Room schema v8 stores full py-fsrs Card fields and drops the interval column.
expected: Room schema v8 stores full py-fsrs Card fields and drops the interval column.
result: pass
source: automated
coverage_id: D1

### 16. ReviewLogEntity v8 stores rating/reviewDatetime/reviewDuration with FK + index on cardId.
expected: ReviewLogEntity v8 stores rating/reviewDatetime/reviewDuration with FK + index on cardId.
result: pass
source: automated
coverage_id: D2

### 17. ReviewRepository.recordReview persists card update and review log atomically.
expected: ReviewRepository.recordReview persists card update and review log atomically.
result: pass
source: automated
coverage_id: D3

### 18. QuizViewModel uses v6.Scheduler.reviewCard and reviewRepository.recordReview for production scheduling.
expected: QuizViewModel uses v6.Scheduler.reviewCard and reviewRepository.recordReview for production scheduling.
result: pass
source: automated
coverage_id: D4

### 19. Due-date comparisons use epoch millis consistently across QuizViewModel, MainViewModel, NotificationReceiver.
expected: Due-date comparisons use epoch millis consistently across QuizViewModel, MainViewModel, NotificationReceiver.
result: pass
source: automated
coverage_id: D5

### 20. Backup schema v3 rejects imports from older backup versions.
expected: Backup schema v3 rejects imports from older backup versions.
result: pass
source: automated
coverage_id: D6

### 21. Repository JSON decode paths return Result<T> and throw VocabDataException on malformed input
expected: Repository JSON decode paths return Result<T> and throw VocabDataException on malformed input
result: pass
source: automated
coverage_id: D1

### 22. Zero generic catch-all handlers remain across the four repository implementations
expected: Zero generic catch-all handlers remain across the four repository implementations
result: pass
source: automated
coverage_id: D3

## Summary

total: 22
passed: 22
issues: 0
pending: 0
skipped: 0

## Gaps

