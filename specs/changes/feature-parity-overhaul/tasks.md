# Tasks: feature-parity-overhaul

## Phase 0: Offline Data Generation Pipeline (Python Scripts)
*Note: This phase runs locally on the developer's machine, outside of the Android project.*
- [ ] A. Write Python script to query `CLIProxyAPI` (`qwen3-235b-a22b-instruct`) to generate `topic` and `scrambledSentenceData` for words.
- [ ] B. Write Python script to query `CLIProxyAPI` (`mimo-v2.5-tts`) to generate `.ogg` audio files.
- [ ] C. Upload `.ogg` files to a GitHub repository and construct the jsDelivr CDN URLs.
- [ ] D. Output the final `vocabulary.json` containing the new fields and CDN URLs, and place it in `app/src/main/assets/`.

## Phase 1: Data Model & Repository Evolution (Android)
- [ ] 1. Add `topic`, `audioUrl`, and `scrambledSentenceData` fields to `VocabularyItem` in `domain/.../Models.kt`.
- [ ] 2. Add `topic`, `audioUrl`, and `scrambledSentenceData` fields to `VocabularyCardEntity`.
- [ ] 3. Update the private `VocabularyAssetItem` DTO inside `VocabularyRepositoryImpl.kt` (line 30) to include the new fields. (Verify if `data/model/VocabularyAssetItem.kt` is unused and delete/deprecate it).
- [ ] 4. Update `fromDomain` and `toDomain` mappers in `VocabularyCardEntity`.
- [ ] 5. Update `checkAndPrepopulate()` in `VocabularyRepositoryImpl.kt` to map the new fields from the JSON DTO to the Entity.
- [ ] 6. In `DataModule.kt`, replace `.fallbackToDestructiveMigration(dropAllTables = true)` with `.addMigrations(MIGRATION_1_2)`. Keep fallback as a secondary safety measure ONLY IF safe to do so, but `.addMigrations` must take precedence.
- [ ] 7. Write `MIGRATION_1_2` in `VocabDatabase.kt` to safely upgrade the database without losing user FSRS progress.
- [ ] 8. Add `getCardsByTopic()` query to `VocabDao` and implement it in `VocabularyRepositoryImpl`.
- [ ] 9. Update `assets/vocabulary.json` schema to include the new fields with sample data for testing.

## Phase 2: Audio CDN Integration
- [ ] 10. Add `androidx.media3:media3-exoplayer` dependencies to `libs.versions.toml` and `app/build.gradle.kts`.
- [ ] 11. Create `CDNAudioPlayer` utilizing ExoPlayer with a local cache directory (SimpleCache).
- [ ] 12. Refactor all UI components to use `CDNAudioPlayer` instead of `TTSManager`.
- [ ] 13. Implement silent fallback logic when CDN audio fails to load due to no internet on the first try.

## Phase 3: Gamification & UI Components
- [ ] 14. Implement `ScrambledQuizCard` Jetpack Compose component (drag-and-drop or tap-to-select word blocks).
- [ ] 15. Implement `FSRSTreeProgressBar` component using `lottie-compose` to visually represent memory stability.
- [ ] 16. Integrate `FSRSTreeProgressBar` into `FlashcardScreen` and `ResultScreen`.
- [ ] 17. Update `QuizViewModel` to dynamically select and serve the correct `QuizType` based on entity data.
- [ ] 18. Add Topic Selection UI (e.g., `TopicPickerScreen` or an option in `SettingsScreen`) to update user preferences.

## Phase 4: Verification
- [ ] 19. Run existing Domain unit tests to ensure FSRS calculations remain untouched and correct.
- [ ] 20. Test offline behavior: Verify that previously played audio works without an internet connection, and un-cached audio fails silently.
- [ ] 21. Check UI against Anti-Dark Border specification.