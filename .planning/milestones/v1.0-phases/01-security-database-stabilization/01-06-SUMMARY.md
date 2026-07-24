---
key-files:
  created:
    - data/src/test/java/com/nhimz/vocabmaster/data/database/VocabDaoTest.kt
    - app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt
  deleted:
    - domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/FSRS.kt
    - domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/Models.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/FSRSTest.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/reference/ReferenceFSRS.kt
    - domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/reference/ReferenceModels.kt
---

## Self-Check: PASSED

## Summary

Migrated final legacy enum consumers to `v6` components and permanently removed the legacy `FSRS` implementation from the `domain.fsrs` package. This solidifies `v6` as the singular and exclusive FSRS implementation across the application. Also added the `VocabDaoTest` suite simulating atomic transactions (`recordReview_isAtomic`), data reads (`dueAndNewCardsFlow_emitsReactively`), and correct SQLite storage using Robolectric to cover PERS-02 and PERS-03 expectations.

## Enum Migration 

- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/QuizScreen.kt`
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/DebugPanelScreen.kt`
- `app/src/main/java/com/nhimz/vocabmaster/ui/screens/statistics_components/OverviewTab.kt`
- `domain/src/test/java/com/nhimz/vocabmaster/domain/usecase/UseCasesTest.kt`

## Deleted Files

- `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/FSRS.kt`
- `domain/src/main/java/com/nhimz/vocabmaster/domain/fsrs/Models.kt`
- `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/FSRSTest.kt`
- `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/reference/ReferenceFSRS.kt`
- `domain/src/test/java/com/nhimz/vocabmaster/domain/fsrs/reference/ReferenceModels.kt`

## VocabDaoTest Case Inventory

1. **`insertAndReadCard_v8Shape`**: Confirms basic Room SQLite storage preserves exact state without default defaults.
2. **`updateCardPersistsV6Fields`**: Guarantees update queries persist null values without regressions in `step` persistence. 
3. **`dueAndNewCardsFlow_emitsReactively`**: Tests reactive collection (`.first()`) functionality via Flow.
4. **`reviewLogOrderingAndDuration`**: Inserts duration elements and retrieves list ordered exactly.
5. **`recordReview_isAtomic`**: Validates update and insert execute atomically; rollback on artificial exception triggers.
6. **`stateCountsAndLearnedCount`**: Proves exact data queries grouping states appropriately.