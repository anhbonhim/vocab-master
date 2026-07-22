# Technology Stack

**Analysis Date:** 2026-07-22

## Languages

**Primary:**
- **Kotlin 2.3.20** — Android app (all UI, domain logic, data layer). The domain module is pure Kotlin/JVM with zero Android dependencies.
- **Python 3.9+** — Backend API server (`backend/`), data pipeline scripts (`tools/`)

**Secondary:**
- **JavaScript (Node.js)** — Minor tooling (`package.json` has `opencode-betterglob` and `opencode-bettergrep` for CLI tooling)
- **Shell (Bash)** — Build/deploy scripts in `backend/run.sh`, `upload_audio_to_cdn.sh`

## Runtime

**Mobile:**
- **Android** — Min SDK 24 (Android 7.0), Target SDK 36 (Android 16), Compile SDK 36
- **Android Gradle Plugin 9.0.1** — Build system
- **Gradle 9.1.0** — Wrapper version (`gradle/wrapper/gradle-wrapper.properties`)

**Backend:**
- **Python** with **Uvicorn 0.30.1** as ASGI server (`backend/requirements.txt`)
- Runs on `127.0.0.1:8000` by default (`backend/app/config.py`, `backend/run.sh`)

**Package Managers:**
- Gradle with version catalog (`gradle/libs.versions.toml`) on Android side
- pip with `requirements.txt` on backend side
- npm with `package.json` for CLI tooling

## Frameworks

**Mobile UI:**
- **Jetpack Compose** via `androidx.compose:bom:2026.03.01` — All UI is Compose, no XML layouts except launcher icons
- **Material3** (`androidx.compose.material3`) and Material Icons Extended
- **Navigation3** (`androidx.navigation3:navigation3-runtime:1.0.1`) — App navigation

**Backend API:**
- **FastAPI 0.111.0** — REST API framework (`backend/app/main.py`)
- **SQLAlchemy 2.0.51** with SQLite — ORM (`backend/app/database.py`)
- **Pydantic v2** (`pydantic-settings`) — Configuration management (`backend/app/config.py`)

**Spaced Repetition:**
- **FSRS v6** — Pure Kotlin port of `py-fsrs 6.3.1` in `domain/src/main/java/.../domain/fsrs/v6/Scheduler.kt` (516 lines)
- **IRT Engine** — Custom 2PL Item Response Theory implementation in Python (`backend/app/services/irt_engine.py`)

**Testing:**
- **JUnit 4.13.2** — Unit tests for all modules
- **AndroidX Test** (`1.7.0`) + **Espresso 3.7.0** — Instrumented UI tests
- **Robolectric 4.15.1** — Android unit tests in `:data` module
- **Kotlinx Coroutines Test** (`1.10.2`) — Async test helpers
- Python tests: Not detected (no `pytest` or `unittest` usage found)

## Key Dependencies

### Android (from `gradle/libs.versions.toml`)

**Critical:**
- **Kotlin 2.3.20** + **KSP 2.3.2** — Language + symbol processing
- **Hilt 2.60.1** — Dependency injection across all modules (`:app`, `:data`)
- **Jetpack Compose BOM 2026.03.01** — UI framework
- **Room 2.7.1** — Local SQLite database with KSP
- **Retrofit 2.11.0** + **OkHttp 4.12.0** — HTTP networking
- **Kotlinx Serialization 1.7.3** — JSON serialization (used by both Retrofit converter and FSRS persistence)
- **Kotlinx Coroutines 1.10.2** — Async/concurrency
- **Firebase BoM 33.1.2** — Firebase services (Auth)

**Infrastructure:**
- **AndroidX Core KTX 1.18.0** — Core AndroidX
- **AndroidX Lifecycle 2.10.0** — ViewModel + runtime compose
- **AndroidX Activity Compose 1.13.0** — Activity + Compose integration
- **AndroidX DataStore Preferences 1.1.1** — Key-value preferences
- **AndroidX Credentials 1.2.2** — Credential Manager API for Google Sign-In
- **AndroidX Media3 ExoPlayer 1.5.1** — CDN audio playback with caching
- **Coil 2.6.0** (+ SVG decoder) — Image loading
- **Lottie Compose 6.4.0** — Lottie animation support
- **Navigation3 1.0.1** — Type-safe navigation

### Backend (from `backend/requirements.txt`)

- **FastAPI 0.111.0** — Web framework
- **Uvicorn 0.30.1** — ASGI server
- **SQLAlchemy 2.0.51** — ORM
- **Pydantic 2.14.0a1** / **pydantic-settings 2.14.2** — Validation + config
- **Firebase Admin 6.5.0** — Token verification
- **Alembic 1.13.1** — DB migrations
- **Python-dotenv 1.0.1** — Environment loading

## Configuration

**Environment variables:**
- Backend: `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `PROJECT_NAME`, `VERSION` (from `backend/app/config.py`)
- `.env` file supported by `pydantic-settings` in backend (`backend/app/config.py`)
- `app/google-services.json` — Firebase Android config (present, not read)
- `backend/firebase-service-account.json` — Firebase Admin SDK credentials (present, not read)

**Build:**
- `gradle.properties` — JVM args, caching, parallel builds, KSP2 enabled
- `detekt` config at `config/detekt/detekt.yml` — Static analysis
- ProGuard rules: `app/proguard-rules.pro` for release builds
- `local.properties` — SDK/NDK paths (Android-specific)

## Platform Requirements

**Development:**
- Android Studio (recommended) or terminal build with JDK 17+
- Android SDK (compile SDK 36)
- Python 3.9+ for backend
- Rust + build-essential for `cryptography` on ARM (Termux docs)

**Production:**
- Android APK (release build with minification enabled)
- Backend deployed on any Python-capable server

---

*Stack analysis: 2026-07-22*
