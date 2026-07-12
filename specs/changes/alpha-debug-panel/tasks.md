# Tasks: alpha-debug-panel

## Phase 1: Local Logging System
- [ ] 1. Create a `LocalLogger` singleton (or Hilt-injected class) capable of storing `LogEvent` objects (timestamp, tag, message, level) in memory (bounded list) or an SQLite table/DataStore.
- [ ] 2. Implement log capturing for unhandled exceptions (Thread.setDefaultUncaughtExceptionHandler wrapper for Debug mode).
- [ ] 3. Inject `LocalLogger` into `CDNAudioPlayer` (when built) and `QuizViewModel` to record network requests, cache misses, and quiz type distributions.

## Phase 2: Debug UI Framework & Export
- [ ] 4. Create a new Jetpack Compose screen `DebugPanelScreen` using a `TabRow` or `ScrollableTabRow` for modular sections.
- [ ] 5. Implement the export functionality: write the captured logs to a `.txt`/`.json` file using Android's `MediaStore` API (for Android 10+) or `Context.getExternalFilesDir()` targeting the Downloads folder.

## Phase 3: Module Implementation
- [ ] 6. **Audio QC Tab:** Build UI to list/search vocabulary, show CDN URL, check `SimpleCache` status for that URL, and a play button.
- [ ] 7. **FSRS/DB Tab:** Build UI to execute predefined queries (e.g., `vocabDao.getAllCards()`) and display them in a raw, scrollable table/list view showing `stability`, `difficulty`, etc.
- [ ] 8. **Quiz/Log Tab:** Build UI to display the collected `LocalLogger` entries and Quiz distribution statistics.

## Phase 4: Integration & Security
- [ ] 9. Add navigation to `DebugPanelScreen` from `SettingsScreen`, guarded by `if (BuildConfig.DEBUG)` block.
- [ ] 10. Verify that the Release build completely hides the entry point and, if possible, avoids compiling the Heavy Debug UI components.