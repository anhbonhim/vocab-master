# External Integrations

**Analysis Date:** 2026-07-22

## APIs & External Services

**Authentication:**
- **Firebase Authentication** — User identity management
  - Android SDK: `com.google.firebase:firebase-auth` (via Firebase BOM 33.1.2)
  - Backend: `firebase-admin==6.5.0` verifies ID tokens (`backend/app/utils/firebase_auth.py`)
  - Auth flow: Google Sign-In → Firebase ID token → Bearer header on API calls
  - Config files:
    - `app/google-services.json` — Android Firebase project config
    - `backend/firebase-service-account.json` — Backend admin credentials (path from `FIREBASE_CREDENTIALS_PATH` env var)

- **Google Sign-In** — Primary auth method
  - Android: `androidx.credentials:credentials` (1.2.2) + `credentials-play-services-auth` + `googleid` (1.1.1)
  - Web client ID: `170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com` (hardcoded in `data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt:48`)
  - Uses Credential Manager API with `GetGoogleIdOption` for token retrieval
  - Flow: Google ID token → `GoogleAuthProvider.getCredential()` → `FirebaseAuth.signInWithCredential()` → Firebase session

**Backend API:**
- **Self-hosted FastAPI** — Android client talks to Python backend
  - Base URL: `http://127.0.0.1:8000/` (configured in `data/build.gradle.kts` as `BuildConfig.API_BASE_URL`)
  - Transport: HTTP/1.1 via Retrofit/OkHttp (plaintext, `usesCleartextTraffic="true"` in AndroidManifest)
  - Auth: Bearer token (Firebase ID token) via `AuthInterceptor` (`data/src/main/java/com/nhimz/vocabmaster/data/remote/AuthInterceptor.kt`)
  - Endpoints:
    - `GET /api/v1/health` — Health check
    - `GET /api/v1/me` — Authenticated user profile
    - `GET /api/v1/vocabulary/topics` — List vocabulary topics
    - `GET /api/v1/vocabulary/catalog` — Paginated vocabulary catalog (by topic, level)
    - `POST /api/v1/placement/start` — Start placement test
    - `POST /api/v1/placement/{session_id}/answer` — Submit placement answer
    - `POST /api/v1/sync/push` — Push local data to server
    - `GET /api/v1/sync/pull` — Pull remote data from server (since timestamp)
  - Swagger UI: `http://localhost:8000/docs`

**CDN / Audio Assets:**
- **jsDelivr** (GitHub-backed CDN) — Audio file hosting
  - Base URL: `https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/`
  - Audio files: OGG format, cached locally via ExoPlayer `SimpleCache` (90MB LRU cache)
  - Upload: `upload_audio_to_cdn.sh` pushes `output/audio/v2/` to GitHub repo `anhbonhim/vocab-assets`
  - Repository URL: `git@github.com:anhbonhim/vocab-assets.git` (SSH-based push)

## Data Storage

**Databases:**
| Database | Engine | Client | Connection | Location |
|----------|--------|--------|------------|----------|
| Local (Android) | SQLite via Room | Room 2.7.1 | Embedded, `vocab_master_db` | Device-local via Room |
| Server (Backend) | SQLite via SQLAlchemy | SQLAlchemy 2.0.51 | `sqlite:///./vocab.db` (from `DATABASE_URL` env var) | `backend/vocab.db` |

**Room Entities (Android Local DB):**
10 entities: `FsrsCardEntity`, `ReviewLogEntity`, `FlaggedItemEntity`, `SectionEntity`, `UnitEntity`, `UnitGuidebookEntity`, `NodeEntity`, `SessionEntity`, `QuestionEntity`, `NodeProgressEntity`, `SessionProgressEntity`, `QuestionAndFsrsCard` (relation). DB version 8 with destructive migration on version bump.

**Backend Models (Server DB):**
| Model | Table | Purpose |
|-------|-------|---------|
| `User` | `users` | Firebase UID mapping |
| `UserSettings` | `user_settings` | Streaks, daily goal, theme, language, placement level |
| `Vocabulary` | `vocabulary` | Word bank with CEFR level, IRT params, audio URLs |
| `UserCard` | `user_cards` | FSRS card state (per-user) |
| `ReviewLog` | `review_logs` | Review history (append-only) |
| `PlacementSession` | `placement_sessions` | Active placement test sessions |

**File Storage:**
- **Local filesystem only** — Backend uses SQLite file (`backend/vocab.db`)
- No cloud storage (S3, GCS, etc.) or file upload support

**Caching:**
- **ExoPlayer SimpleCache** (Android) — 90MB LRU cache for CDN audio OGG files at `context.cacheDir/audio_cdn_cache/`
- **No server-side caching** — Backend has no Redis, memcached, or in-memory cache

## Authentication & Identity

**Auth Provider:**
- **Firebase Authentication** (Google Sign-In as identity provider)
  - Android: `FirebaseAuth` singleton via `AuthManager` (`data/src/main/java/com/nhimz/vocabmaster/data/auth/AuthManager.kt`)
  - Backend: Firebase Admin SDK token verification via `firebase_admin.auth.verify_id_token()` (`backend/app/utils/firebase_auth.py`)
  - Two auth dependency modes:
    - `get_current_user_uid` — Required auth (returns 401 if missing)
    - `get_optional_user_uid` — Optional auth (returns `None` for guest access)
  - Guest mode supported: Placement test can run without authentication (stateless, client-driven)

**Backend API Security:**
- Bearer token authentication via `HTTPBearer` (`fastapi.security`)
- `AuthInterceptor` on Android attaches `Authorization: Bearer <token>` header to all Retrofit requests (`data/src/main/java/com/nhimz/vocabmaster/data/remote/AuthInterceptor.kt`)
- Token refreshed via `user.getIdToken(false).await()` before expiry

## Monitoring & Observability

**Error Tracking:**
- **None** — No Sentry, Crashlytics, or similar service integrated
- Backend errors surface as FastAPI HTTP 500 responses
- Android uses `LocalLogger` utility (`app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`) for local logging

**Logs:**
- **Android**: `LocalLogger` (custom utility, logs to `android.util.Log` under tag `VocabMaster`)
- **Backend**: Uvicorn stdout logs, plus manual `print()` in Firebase init (`backend/app/utils/firebase_auth.py:13`)
- **Backend log file**: `backend/backend.log` (gitignored)

## CI/CD & Deployment

**Hosting:**
- **Backend**: Self-hosted — no cloud provider (designed to run on local machine or Android Termux)
- **CDN**: jsDelivr (free CDN for open-source GitHub repos)
- **Not deployed** to any cloud platform

**CI Pipeline:**
- **None** — no GitHub Actions, Jenkins, or similar configured
- Static analysis run locally via `detekt` Gradle plugin
- Tests run manually via Gradle (`./gradlew test`)

## Environment Configuration

**Required env vars (Backend):**
| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `sqlite:///./vocab.db` | SQLite database path |
| `FIREBASE_CREDENTIALS_PATH` | `firebase-service-account.json` | Path to Firebase Admin SDK JSON key file |

**Secrets location:**
| Secret | File | Status |
|--------|------|--------|
| Firebase Admin key | `backend/firebase-service-account.json` | Exists on device, gitignored |
| Firebase Android config | `app/google-services.json` | Exists on device, committed |
| Google Sign-In Web Client ID | `AuthManager.kt:48` | Hardcoded in source |
| Environment file | `backend/.env` | Not present; pydantic-settings looks for it |

## Webhooks & Callbacks

**Incoming:**
- **None** — No webhook endpoints defined

**Outgoing:**
- **None** — No outgoing webhook calls from the backend

## Data Sync Architecture

**Sync between Android local DB and server:**
- Protocol: REST over HTTP (push/pull pattern)
- Direction: **Bidirectional** — `POST /api/v1/sync/push` and `GET /api/v1/sync/pull`
- Conflict resolution: Last-write-wins based on `last_modified` epoch ms timestamp
- Sync payload contains: user settings, vocabulary cards (FSRS state), review logs
- Review logs: Append-only with dedup by `questionId + timestamp` uniqueness
- Guest mode: No sync (local-only)
- Auth mode: Sync requires authenticated Firebase user

---

*Integration audit: 2026-07-22*
