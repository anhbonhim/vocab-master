# Technology Stack

**Analysis Date:** 2026-07-22

## Languages

**Primary:**
- **Kotlin** 2.3.20 — Android frontend (app, domain, data modules)
- **Python** 3.9+ — Backend API server (`backend/`)

**Secondary:**
- **Java** 17 — Android JVM target (compileOptions source/target)
- **Bash** — Scripts: `start_backend.sh`, `backend/run.sh`, `backend/setup.sh`, `upload_audio_to_cdn.sh`

## Runtime

**Mobile:**
- **Android** minSdk 24 / targetSdk 36 / compileSdk 36
- Build on Android Gradle Plugin 9.0.1

**Backend:**
- **Python 3.9+** runtime
- **Uvicorn** 0.30.1 (ASGI server)
- **Gunicorn** — Not used; run via `uvicorn` directly

**Package Managers:**
| Platform | Manager | Lockfile | Location |
|----------|---------|----------|----------|
| Android | Gradle (Kotlin DSL) | Yes (`gradle/`) | Root project, `settings.gradle.kts`, version catalog at `gradle/libs.versions.toml` |
| Python | pip | `requirements.txt` (flat, no lock) | `backend/requirements.txt` |
| Android-Global | npm | `package-lock.json` | Root `package.json` (only dev tooling: `opencode-betterglob`, `opencode-bettergrep`) |

## Frameworks

**Core:**
| Framework | Version | Purpose | Module |
|-----------|---------|---------|--------|
| Jetpack Compose | BOM 2026.03.01 | Declarative UI (Material3) | `app` |
| AndroidX Activity Compose | 1.13.0 | Activity + Compose integration | `app` |
| AndroidX Lifecycle | 2.10.0 | ViewModel, runtime-compose | `app`, `data` |
| Navigation3 | 1.0.1 | Type-safe navigation (navigation3-runtime, navigation3-ui) | `app` |
| Hilt (Dagger) | 2.60.1 | Dependency injection | `app`, `data` |
| Room | 2.7.1 | Local SQLite ORM (10 entities, version 8) | `data`, `app` |
| DataStore Preferences | 1.1.1 | Key-value local preferences | `data` |
| FastAPI | 0.111.0 | ASGI web framework (REST API) | `backend` |
| SQLAlchemy | 2.0.51 | Python ORM | `backend` |
| Pydantic | 2.14.0a1 | Request/response validation & schemas | `backend` |
| Firebase Admin SDK | 6.5.0 | Server-side Firebase auth verification | `backend` |

**Testing:**
| Framework | Version | Purpose | Module |
|-----------|---------|---------|--------|
| JUnit | 4.13.2 | Unit tests | `app`, `domain`, `data` |
| Kotlinx Coroutines Test | 1.10.2 | Async test helpers | `app`, `domain`, `data` |
| Robolectric | 4.15.1 | Android instrumented-like unit tests | `data` |
| AndroidX Test | 1.7.0 | Instrumented test runner & core | `app`, `data` |
| AndroidX Test Espresso | 3.7.0 | UI interaction testing | `app` |
| Compose UI Test | (from BOM) | Compose UI test helpers | `app` |

**Build/Dev:**
| Tool | Version | Purpose |
|------|---------|---------|
| Android Gradle Plugin | 9.0.1 | Android build system |
| KSP (Kotlin Symbol Processing) | 2.3.2 | Annotation processing for Room + Hilt |
| Kotlin Compose Compiler plugin | (via Kotlin 2.3.20) | Compose compiler |
| Kotlin Serialization plugin | 1.7.3 | JSON serialization |
| detekt | 1.23.6 | Static analysis (config in `config/detekt/detekt.yml`) |
| Alembic | 1.13.1 | Backend DB migrations |

## Key Dependencies

**Critical (Frontend):**
| Dependency | Version | Why It Matters |
|------------|---------|----------------|
| `androidx.media3:media3-exoplayer` | 1.5.1 | CDN audio playback with caching (ExoPlayer) |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | HTTP client for backend API |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP transport + logging interceptor |
| `com.google.firebase:firebase-auth` | (via BOM 33.1.2) | User authentication |
| `androidx.credentials:credentials` | 1.2.2 | Credential Manager API for Google Sign-In |
| `io.coil-kt:coil-compose` | 2.6.0 | Image loading (Compose), SVG support |
| `com.airbnb.android:lottie-compose` | 6.4.0 | Lottie animations |
| `androidx.navigation3:navigation3-runtime` | 1.0.1 | Type-safe navigation (Nav3) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 | JSON parsing (shared with Retrofit converter) |

**Critical (Backend):**
| Dependency | Version | Why It Matters |
|------------|---------|----------------|
| `fastapi` | 0.111.0 | REST API framework |
| `sqlalchemy` | 2.0.51 | Database ORM (SQLite) |
| `firebase-admin` | 6.5.0 | Server-side Firebase token verification |
| `pydantic-settings` | 2.14.2 | Environment-based config loading |
| `alembic` | 1.13.1 | Database schema migrations |

**Infrastructure:**
| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Async on Android |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | Async in domain module |
| `com.google.dagger:hilt-android` | 2.60.1 | DI framework |

## Configuration

**Environment:**
| File | Purpose | Secrets? |
|------|---------|----------|
| `.env` (in `backend/`) | Backend env vars (DATABASE_URL, FIREBASE_CREDENTIALS_PATH) | Yes (not tracked in git) |
| `backend/app/config.py` | Pydantic `Settings` reading from `.env` and defaults | No |
| `data/build.gradle.kts` | `BuildConfig.API_BASE_URL` (defaults to `http://127.0.0.1:8000/`) | No |
| `app/google-services.json` | Firebase Android config | Contains project IDs, API keys |
| `backend/firebase-service-account.json` | Firebase Admin service account | Yes (not tracked; path configurable) |
| `gradle.properties` | JVM args, AndroidX, KSP flags | No |

**Build:**
| File | Purpose |
|------|---------|
| `build.gradle.kts` | Root Gradle build (detekt config) |
| `app/build.gradle.kts` | Android app module build config |
| `data/build.gradle.kts` | Android data/library module build config |
| `domain/build.gradle.kts` | Kotlin JVM domain module build config |
| `gradle/libs.versions.toml` | Version catalog for all dependencies |
| `settings.gradle.kts` | Multi-module project settings |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle wrapper config |

## Platform Requirements

**Development:**
- **Android Studio** (or IntelliJ with Android plugin) for Kotlin/Android development
- **Python 3.9+** with `pip` for backend
- **JDK 17** (JVM target)
- **Android SDK** (compileSdk 36, platforms/android-36)
- On **Termux (Android)**: `python`, `rust`, `binutils`, `build-essential` for compiling Python packages with native extensions

**Production:**
- **Backend**: Can run on any host with Python 3.9+ (VPS, localhost on Android via Termux)
- **Android**: APK built via Gradle, installed on Android 6.0+ (API 24+)
- **CDN**: jsDelivr serving from GitHub repo `anhbonhim/vocab-assets`

---

*Stack analysis: 2026-07-22*
