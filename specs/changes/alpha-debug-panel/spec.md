# Delta Spec: alpha-debug-panel

## ADDED Requirements

### Requirement: Debug Panel Accessibility
- **Scenario: Debug Build Access**
  Given the app is compiled as a Debug variant (`BuildConfig.DEBUG == true`),
  When the user navigates to the Settings screen (or taps a hidden trigger),
  Then a "Debug Panel" option MUST be visible and accessible.

- **Scenario: Release Build Security**
  Given the app is compiled as a Release variant (`BuildConfig.DEBUG == false`),
  When the user navigates through the app,
  Then the "Debug Panel" MUST NOT be accessible, and its code SHOULD ideally be stripped by R8/ProGuard.

### Requirement: Extensible Debug UI Structure
- **Scenario: Tabbed Navigation**
  Given the user opens the Debug Panel,
  Then the UI MUST present a modular navigation structure (e.g., Tabs or a Drawer) containing at least the following categories: Audio, FSRS/DB, Quiz Stats, Logs.

### Requirement: Audio CDN Quality Control
- **Scenario: Inspecting Audio Cache**
  Given the user is on the Audio Debug tab,
  When they select or search for a specific word,
  Then the system MUST display the assigned CDN URL, indicate whether the `.ogg` file is currently in the local cache (Hit/Miss), and provide a button to play the audio.

### Requirement: FSRS and Database Inspection
- **Scenario: Raw Card Inspection**
  Given the user is on the FSRS/DB Debug tab,
  When they query the database,
  Then the system MUST display raw values for `stability`, `difficulty`, `interval`, `due`, and `state` for the inspected cards, along with aggregate counts grouped by topic and level.

### Requirement: Logging and Exporting
- **Scenario: Capturing Runtime Logs**
  Given the app is running in Debug mode,
  When an exception occurs or a network request is made by ExoPlayer,
  Then the `LocalLogger` MUST record the event details in memory or local storage.

- **Scenario: Exporting Logs**
  Given the user is on the Logs Debug tab,
  When they tap "Export Logs",
  Then the system MUST generate a `.txt` or `.json` file containing all captured logs and save it to the device's public Downloads directory (or prompt a Save As dialog).