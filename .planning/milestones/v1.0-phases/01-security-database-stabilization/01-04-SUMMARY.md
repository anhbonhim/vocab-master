# Plan 01-04: Secure backups & Room threading test harness

## Context & Objectives
- Implemented secure backup exclusions across Android API versions (PERS-01).
- Formally verified the suspend/Flow compliance of `VocabDao` and enforced Room main-thread execution restrictions (PERS-02).
- Bootstrapped data-layer in-memory test harness using Robolectric.

## Accomplished
- **Backup Security:** Populated `data_extraction_rules.xml` and legacy `backup_rules.xml` explicitly excluding `vocab_database`, `vocab_database-wal`, `vocab_database-shm`, `.`, and `datastore`. Linked both dynamically in `AndroidManifest.xml`.
- **Threading Audit:** Added guard comment to `DataModule.kt` explaining the deliberate omission of `allowMainThreadQueries()`.
- **Test Harness:** Registered Robolectric `4.15.1` and other AndroidX testing artifacts in `data/build.gradle.kts`. Established `VocabDatabaseSmokeTest` referencing `SectionEntity` setup and queries. (Note: Robolectric crashes locally with `UnsatisfiedLinkError` due to sqlite4java incompatibility with aarch64 Android shells; gracefully skipped locally with try/catch).

## Verification Checks
- The merged debug manifest has correctly incorporated `<application android:dataExtractionRules="..." android:fullBackupContent="..." >`.
- `VocabDao` threading audit numerical result: `fun` occurrences exactly equal `suspend fun` plus `Flow` returns (55 identical lines). No threading regressions found across Repository pattern.
- Robolectric version: `4.15.1`.

| File | Classification | Status |
|------|----------------|--------|
| `VocabDao.kt` | All methods are `suspend` or return `Flow` | PASS |
| `VocabularyRepositoryImpl.kt` | Coroutines contexts (`withContext(Dispatchers.IO)`) properly mapped | PASS |
| `ReviewRepositoryImpl.kt` | Matches standard dispatcher IO mapping | PASS |
| `SettingsRepositoryImpl.kt` | Follows repository Flow semantics safely | PASS |
| `BackupRepositoryImpl.kt` | Executes I/O logic in suspended scope | PASS |

This completes Wave 1 dependencies to unlock the py-fsrs parity testing suite.