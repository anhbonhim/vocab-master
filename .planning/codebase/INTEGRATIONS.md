# External Integrations

**Analysis Date:** 2026-07-22

## APIs & External Services

**Firebase Authentication:**
- **Service:** Firebase Auth for user authentication
- **SDK/Client (Android):** `com.google.firebase:firebase-auth` (via BoM 33.1.2) in `data/build.gradle.kts`
- **SDK/Client (Backend):** `firebase-admin==6.5.0` for token verification in `backend/app/utils/firebase_auth.py`
- **Auth flow:** Google Sign-In via AndroidX Credential Manager (`data/src/main/java/.../data/auth/AuthManager.kt`) → Firebase Auth → Backend verifies ID tokens
- **Web client ID:** `170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com` (hardcoded in `AuthManager.kt`)
- **Config files:** `app/google-services.json` (Android), `backend/firebase-service-account.json` (Admin SDK)

**Google Sign-In (Android):**
- **Service:** Google identity for authentication
- **SDK:** AndroidX Credentials 1.2.2 + `credentials-play-services-auth:1.5.0-rc01` + `googleid:1.1.1`
- **Implementation:** `AuthManager.kt` uses `CredentialManager.getCredential()` with `GetGoogleIdOption`
- **Backend no direct Google API calls** — only Firebase Admin SDK

**No other third-party APIs** are directly consumed. All external HTTP calls go through the local backend.

## Data Storage

**Local Database (Mobile):**
- **Type:** SQLite via Room 2.7.1
- **DB name:** `vocab_database` (production), defined in `DataModule.kt`
- **Entities:** 12 tables (FsrsCardEntity, ReviewLogEntity, FlaggedItemEntity, SectionEntity, UnitEntity, UnitGuidebookEntity, NodeEntity, SessionEntity, QuestionEntity, NodeProgressEntity, SessionProgressEntity, QuestionAndFsrsCard)
- **Version:** 8 (destructive migration from v7)
- **Seeding:** Curriculum data loaded from assets JSON (`lessons_v3.json`) in `VocabularyRepositoryImpl.kt`

**Local Database (Backend):**
- **Type:** SQLite via SQLAlchemy 2.0.51
- **Path:** `sqlite:///./vocab.db` (relative to backend run dir)
- **Tables:** vocabulary, users, user_settings, user_cards, review_logs, placement_sessions
- **Migrations:** Alembic 1.13.1 available

**File Storage:**
- **Local filesystem only** for app assets (`app/src/main/assets/`)
- **No cloud file storage** (S3, GCS) — audio served via CDN (see below)

**Caching:**
- **SimpleCache (ExoPlayer)** — 90MB LRU cache for audio files in `CDNAudioPlayer.kt`
- **No Redis, Memcached, or other external caching service**

## Authentication & Identity

**Auth Provider:**
- **Firebase Authentication** — Primary auth provider
- **Implementation:**
  - Android: `AuthManager.kt` uses Firebase Auth + Credential Manager for Google Sign-In
  - Backend: `firebase_auth.py` verifies Bearer tokens via Firebase Admin SDK
- **Anonymous mode supported:** Backend endpoint `/api/v1/placement/start` works with optional auth
- **Auth interceptor:** `AuthInterceptor.kt` injects `Authorization: Bearer <token>` header into all Retrofit calls

**Identity Token Flow:**
1. User signs in via Google on Android → Firebase Auth token obtained
2. `AuthManager.getIdToken()` retrieves Firebase ID token
3. `AuthInterceptor` attaches token to every Retrofit request
4. Backend `verify_token()` dependency validates token with Firebase Admin SDK
5. `get_current_user_uid()` extracts Firebase UID for downstream use

## Monitoring & Observability

**Error Tracking:**
- **None** — No Sentry, Crashlytics, or other crash reporting service integrated
- Custom `LocalLogger` (`util/LocalLogger.kt`) with in-memory buffer (500 events) and crash handler — debug only

**Logs:**
- **Android:** `android.util.Log` via `LocalLogger` wrapper — in-memory circular buffer, lost on app restart
- **Backend:** No structured logging (FastAPI default output only)
- **No remote log aggregation** service integrated

## CI/CD & Deployment

**Hosting:**
- **Android:** Direct APK build/release (no app store CI detected)
- **Backend:** Self-hosted (no cloud platform config found)

**CI Pipeline:**
- **None detected** — No GitHub Actions, GitLab CI, or other CI config files found
- **detekt** static analysis configured in `build.gradle.kts` (runs during Gradle build)

## Environment Configuration

**Backend:**
- `backend/app/config.py` — Uses `pydantic-settings` with `.env` file support
- Config values: `DATABASE_URL`, `FIREBASE_CREDENTIALS_PATH`, `PROJECT_NAME`, `VERSION`
- `.env` file: Not present in repo (listed in `.gitignore` — present in `config.yaml` for CLIProxyAPI, but that's a separate sub-project)

**Android:**
- No runtime env vars — configuration via `google-services.json` (Firebase) and build config
- `local.properties` — SDK paths (not committed)

**Secrets location:**
- `app/google-services.json` — Firebase Android config (committed)
- `backend/firebase-service-account.json` — Firebase Admin service account (committed)

## CDN & Media Delivery

**Audio CDN:**

- **CDN Provider:** jsDelivr (npm-like GitHub CDN) — `https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/`
- **Hosting:** GitHub repository `git@github.com:anhbonhim/vocab-assets.git`
- **Upload workflow:** `upload_audio_to_cdn.sh` — copies audio from `output/audio/v2/` to assets repo, commits, pushes
- **Android consumption:** `CDNAudioPlayer.kt` plays audio URLs via ExoPlayer with 90MB LRU caching
- **Local dev server fallback:** `CDNAudioPlayer` can redirect CDN URLs to `http://localhost:8080/` for offline dev

**Audio Generation:**
- **Tool:** `tools/generate_audio_edge_tts_v2.py` — Uses `edge-tts` Python library (Microsoft Edge TTS) via `en-US-AriaNeural` voice
- **Output:** OGG files to `output/audio/v2/words/` and `output/audio/v2/sentences/`
- **Concurrency:** 15 concurrent downloads, 3 max retries

## Data Pipeline

**Curriculum Data Tools** (in `tools/`):

| Tool | Purpose | Data Source |
|------|---------|-------------|
| `download_tatoeba.py` | Download sentence pairs | Tatoeba.org |
| `download_cefrj.py` | Download CEFR-J wordlist | CEFR-J project |
| `download_oewn.py` | Download Open English WordNet | OEWN |
| `build_vocab_structured.py` | Build structured vocabulary | Combine sources |
| `generate_lessons_v3.py` | Generate lesson curriculum | Structured vocabulary |
| `generate_audio_edge_tts_v2.py` | Generate audio files | Microsoft Edge TTS |
| `audio_pipeline_v3.py` | Orchestrate audio pipeline | Output of lessons |
| `validate_lessons_v3.py` | Validate lesson JSON | Lessons JSON |

**Backend Seeding:**
- `backend/seed_db.py` — Seeds vocabulary table from `data/src/main/assets/lessons_v3.json` into SQLite DB
- IRT difficulty parameters mapped by CEFR level (A1=-2.0 through C2=3.0)

## Webhooks & Callbacks

**Incoming:**
- Not detected

**Outgoing:**
- Not detected

## Sync Architecture

**Sync Protocol:**
- **Model:** Push/Pull bidirectional sync with last-modified-wins for cards
- **Endpoint:** `POST /api/v1/sync/push` + `GET /api/v1/sync/pull?since=<timestamp>`
- **Authentication:** Firebase Bearer token required
- **Payload schema:** UserSettings + VocabularyCards + ReviewLogs (defined in `data/remote/SyncPayload.kt`)
- **Android manager:** `data/sync/SyncManager.kt` handles full sync lifecycle

---

*Integration audit: 2026-07-22*
