# Design: feature-parity-overhaul

## Architecture: CDN Audio System
To achieve a cache-first, high-quality audio system without relying on fragile device TTS or bloating the APK size:
1. **Pre-generation Pipeline (Data Generation):** Audio `.ogg` files and structured `vocabulary.json` data will be pre-generated 100% automatically via Python scripts querying the local `CLIProxyAPI` (port 8317). The script will use LLMs (e.g., `qwen3-235b`) for text and TTS models (e.g., `mimo-v2.5-tts`, `kokoro-82m`) via OpenAI-compatible endpoints (`/v1/chat/completions` and `/v1/audio/speech`). Generated audios are hosted on a public GitHub repository served via **jsDelivr CDN**.
2. **App Implementation (Data Playback):** The Android app will use `androidx.media3:media3-exoplayer` configured with a `SimpleCache`. The App DOES NOT call the AI APIs directly.
3. **Flow:** `AudioPlayer` attempts to read from the CacheDataSource. If a cache miss occurs, it resolves the CDN URL, streams the audio, and saves it to the cache simultaneously.

## Data Model Changes
The Room database schema needs to evolve to support the new metadata (topic, audio paths, distractor data for quizzes).

**Target Entity (`VocabularyCardEntity`):**
- Add `topic: String` (Default: "general").
- Add `audioUrl: String?` (Relative path or filename, resolving to the CDN base URL in the app layer).
- Add `scrambledSentenceData: String?` (JSON string containing array of word chunks).

**Migration Strategy:**
- Create an automated Room migration from current version to `version + 1`.
- `ALTER TABLE` to add the new columns with default/null values to preserve existing user progress (FSRS stats).

## Quiz Engine Adaptation
The `QuizViewModel` state machine must be refactored to support polymorphic quiz types. Currently, it assumes a singular `DuolingoOptionCard` flow.
- Introduce a sealed class `QuizType`.
- Subclasses: `MultipleChoice`, `ScrambledSentence`.
- The ViewModel will decide the `QuizType` based on the available data in the current `VocabularyItemWithCard` entity.