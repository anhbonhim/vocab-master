# Delta Spec: feature-parity-overhaul

## ADDED Requirements

### Requirement: Multimedia CDN and Local Caching
- **Scenario: First Time Audio Playback (Online)**
  Given the user is reviewing a flashcard for the first time and has internet connection,
  When the audio button is pressed,
  Then the system MUST fetch the `.ogg` file from the predefined CDN URL, play it using ExoPlayer, and cache it locally in the device storage.

- **Scenario: Subsequent Audio Playback (Offline)**
  Given the user is reviewing a flashcard whose audio was previously cached,
  When the audio button is pressed (even without internet),
  Then the system MUST play the audio instantly from the local cache without making any network requests.

- **Scenario: Offline Playback Attempt Without Cache**
  Given the user is reviewing a flashcard for the first time without an internet connection,
  When the audio button is pressed,
  Then the system MUST silently ignore the request and MUST NOT crash or show an error Toast.

### Requirement: Varied Interactive Quizzes
- **Scenario: Scrambled Sentence Presentation**
  Given a vocabulary item has valid `scrambled_sentence` data in the database,
  When the quiz engine selects this item for review,
  Then it MAY present the `ScrambledQuizCard` component, requiring the user to tap word blocks in the correct order to form the sentence.

### Requirement: Gamified FSRS Visualization
- **Scenario: FSRS Stability Growth**
  Given the user is on the `ResultScreen` after a successful review,
  When the stability of the card increases,
  Then the `FSRSTreeProgressBar` MUST update its visual state (e.g., advancing a Lottie animation from a seed to a plant) to reflect the new memory durability.

### Requirement: Topic-Based Personalization
- **Scenario: Selecting Study Topic**
  Given the user has navigated to the topic selection settings,
  When the user selects a specific topic (e.g., "Work"),
  Then the `QuizEngine` MUST prioritize fetching and queuing `New` cards from that selected topic.

## MODIFIED Requirements

### Requirement: Audio Playback Engine
- **Scenario: Deprecating System TTS**
  Given the app needs to play pronunciation,
  When the user interacts with the audio feature,
  Then the system MUST use the new CDN/ExoPlayer pipeline and MUST NOT rely on `android.speech.tts.TextToSpeech`.
