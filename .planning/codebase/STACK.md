# Technology Stack

**Analysis Date:** 2026-07-20

## Languages

**Primary:**
- Kotlin 2.3.20 - Android App (app module), Data module, Domain module

**Secondary:**
- Python - Scripts in `tools/` (audio pipeline, content generation)
- Shell script - `run_and_log.sh`, `start_backend.sh`, `upload_audio_to_cdn.sh`

## Runtime

**Environment:**
- Android SDK 36 (Min SDK 24)
- Java 17 Compatibility

**Package Manager:**
- Gradle 9.0.1 (Android Gradle Plugin)
- Lockfile: Missing `gradle.lockfile` but versions centralized in `libs.versions.toml`

## Frameworks

**Core:**
- Jetpack Compose (androidx-compose-bom 2026.03.01) - UI framework
- Hilt (2.60.1) - Dependency Injection
- Navigation Compose 3 - App Navigation

**Testing:**
- JUnit 4.13.2 - Unit Testing
- Espresso 3.7.0 - UI Testing
- Kotlinx Coroutines Test 1.10.2 - Async testing

**Build/Dev:**
- KSP 2.3.2 - Kotlin Symbol Processing
- Detekt 1.23.6 - Static code analysis

## Key Dependencies

**Critical:**
- `kotlinx-coroutines` 1.10.2 - Asynchronous programming
- `kotlinx-serialization-json` 1.7.3 - JSON parsing/serialization
- `androidx.room` 2.7.1 - Local SQLite database ORM
- `androidx.datastore.preferences` 1.1.1 - Key-value storage

**Infrastructure:**
- `retrofit2` 2.11.0 - Type-safe HTTP client
- `okhttp3` 4.12.0 - HTTP client with logging interceptor
- `firebase-auth` - User authentication
- `androidx.media3` 1.5.1 - Audio playback (ExoPlayer)
- `coil-compose` 2.6.0 - Image/SVG loading
- `lottie-compose` 6.4.0 - Animations

## Configuration

**Environment:**
- Versions centralized in `gradle/libs.versions.toml`
- Firebase config via `google-services` plugin

**Build:**
- `app/build.gradle.kts`
- `data/build.gradle.kts`
- `domain/build.gradle.kts`
- `settings.gradle.kts`

## Platform Requirements

**Development:**
- Android Studio / JDK 17
- Python 3.x (for tools)

**Production:**
- Android devices running API 24 or higher

---

*Stack analysis: 2026-07-20*
