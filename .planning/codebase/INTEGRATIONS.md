# External Integrations

**Analysis Date:** 2026-07-20

## APIs & External Services

**Backend API:**
- Custom Backend - Handles placement, vocabulary, and sync (`PlacementApiService`, `VocabularyApiService`, `SyncApiService`)
  - SDK/Client: Retrofit 2 + OkHttp 3
  - Connection: Base URL `http://127.0.0.1:8000/` (Local development environment)
  - Auth: Handled by custom `AuthInterceptor` using OkHttp

**Content Delivery Network (CDN):**
- jsDelivr / GitHub Pages - Serving audio assets (`https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/`)
  - SDK/Client: AndroidX Media3 ExoPlayer

## Data Storage

**Databases:**
- Room (SQLite) - Local on-device database (`VocabDatabase`, version 7)
  - Client: `androidx.room:room-ktx`

**Key-Value Storage:**
- DataStore - Local preferences and simple state storage
  - Client: `androidx.datastore:datastore-preferences`

## Authentication & Identity

**Auth Provider:**
- Firebase Auth & Google Identity Services
  - Implementation: `AuthManager` integrating `com.google.android.libraries.identity.googleid.GoogleIdTokenCredential` and `FirebaseAuth`
  - Client ID: `170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com`

## Monitoring & Observability

**Error Tracking:**
- Android Logcat (via custom `LocalLogger`)

**Logs:**
- OkHttp Logging Interceptor - Network request/response body logging

## CI/CD & Deployment

**Hosting:**
- GitHub (implied by `vocab-assets` repo for CDN)

**CI Pipeline:**
- None detected in basic configuration (No GitHub Actions workflows visible in current scan, but Gradle Detekt plugin is present for static analysis)

## Environment Configuration

**Required env vars:**
- Local API Base URL (hardcoded currently)
- Firebase `google-services.json` (required by `com.google.gms.google-services` plugin, not explicitly found in text scan)

**Secrets location:**
- Not explicitly centralized; Google Client ID is hardcoded in `AuthManager.kt`.

## Webhooks & Callbacks

**Incoming:**
- Local device notifications scheduled via Android `AlarmManager` and `NotificationScheduler`.

**Outgoing:**
- None

---

*Integration audit: 2026-07-20*
