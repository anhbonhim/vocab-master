# Technology Stack

**Project:** VocabMaster
**Researched:** 2026-07-22

## Recommended Stack

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 1.9+ | Core Language | Standard for modern Android development. |
| Jetpack Compose | BOM 2024.02+ | UI Framework | Declarative UI, essential for complex, reactive gamified states. |
| Hilt | 2.50+ | Dependency Injection | Standard, simplifies scoping and testing in Clean Architecture. |

### Database & State
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Room Database | 2.6+ | Local Persistence | Provides compile-time SQL verification, Flow support for reactive UI, and structured relations. |
| Kotlin Coroutines & Flow | 1.8+ | Asynchronous Data & State | Seamless integration with Room and Compose for reactive, non-blocking data streams. |

### UI & Animations
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| DotLottie / Lottie-Compose | 6.4+ | Complex Animations & Feedback | Vector-based, scalable, programmable animations. Far superior to GIFs for performance and state-driven control (Success/Failure states). |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| UI Animations | DotLottie | Coil + GIFs | GIFs are heavy, don't scale well across screen densities, and lack programmatic control over playback state (e.g., pausing halfway or triggering specific frame ranges on success/failure). |
| Database | Room | Realm / SQLite directly | Room provides the best balance of safety (compile-time checks), Coroutine/Flow integration, and adherence to Android recommended architectures. SQLite is too low-level; Realm adds unnecessary SDK weight. |

## Sources

- Room Clean Architecture: [Room Setup with Koin & Clean Architecture | by Saif M.](https://medium.com/@maliksaif070/room-setup-with-koin-clean-architecture-11e7e87e0f6f) (Confidence: HIGH)
- Lottie Compose: [From GIFs to Lottie: Mastering Lottie Animations in Android with Jetpack Compose](https://medium.com/pickme-engineering-blog/from-gifs-to-lottie-mastering-lottie-animations-in-android-with-jetpack-compose-58ae0451195a) (Confidence: HIGH)
- DotLottie Android: [DotLottie Android Interactivity Documentation](https://developers.lottiefiles.com/docs/dotlottie-player/dotlottie-android/usage/interactivity/) (Confidence: HIGH)