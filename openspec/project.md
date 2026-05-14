# CertHunter

## Purpose

CertHunter is a single-Activity Android app that lets developers, release engineers, QA, and mobile teams inspect the signing certificate of any app installed on the device. The installed app is the source of truth: there is no need to dig through CI, keystores, secret stores, or tribal knowledge to answer the question *"which key actually signed this app?"*.

Everything is processed on-device. No network requests, no telemetry, no accounts.

## Who it is for

- Developers verifying which certificate a specific flavor (dev / nightly / staging / production) is using
- Release engineers comparing installs of the same app across flavors on one device
- Engineers integrating with services that require fingerprint verification (Google Pay, Google Sign-In, Firebase, Maps, …)
- QA teams auditing key rotation history on apps that have rotated signing keys (API 28+)
- Anyone debugging "is this the right cert?" without leaving the device

## Design principles

- **The device is the source of truth.** CertHunter surfaces the certificate directly from `PackageManager` — never from a build system or external store.
- **Developer-grade detail.** Full certificate chain, historical signers, validity state, one-tap clipboard copy.
- **Local only.** No network access, no telemetry, no accounts. The `QUERY_ALL_PACKAGES` permission is required because broad package visibility *is* the core function; it is not used for any secondary purpose.
- **Broad compatibility.** Android 7.0 (API 24) through Android 15 (API 37), handling both the legacy `GET_SIGNATURES` and the modern `GET_SIGNING_CERTIFICATES` + `SigningInfo` APIs transparently.

## Engineering standards

- Clean architecture: the domain layer is pure Kotlin with no Android dependencies.
- Every class in its own file; no god objects.
- Unit-tested repository logic and ViewModel state with JUnit 5, Mockk, and Turbine.
- Automated CI (tests + lint + build) on every PR; automated CD to Google Play internal testing on every release.

## Tech stack at a glance

The authoritative source for library versions is `gradle/libs.versions.toml`. The summary below documents *what role each component plays*, not pinned version numbers.

| Area | Technology |
|---|---|
| Language / build | Kotlin · Android Gradle Plugin · JDK 21 build, Java 11 target · core library desugaring for `java.time` on API < 26 |
| Android SDK | minSdk 24 · targetSdk 37 · compileSdk 37 · `applicationId` `com.mohsenoid.certhunter` |
| UI | Jetpack Compose (BOM) · Material 3 · Material Icons Core + Extended · Activity Compose · Material You dynamic color on API 31+ |
| Navigation | Navigation 3 (runtime + UI) with type-safe `@Serializable` routes |
| DI | Koin Android + Koin AndroidX Compose; single `appModule` in `di/AppModule.kt` |
| Concurrency | Kotlinx Coroutines via the `DispatcherProvider` abstraction (never `Dispatchers.*` directly in production code) |
| Image loading | Coil Compose with a custom `AppIconFetcher` registered in `App.onCreate()` |
| Typed errors | `com.michael-bull.kotlin-result` — two-parameter `Result<V, E>` (not `kotlin.Result`) |
| Serialization | Kotlinx Serialization JSON (used only for nav routes) |
| Logging | KLogX Core + Android Logcat appender, gated on `BuildConfig.DEBUG` |
| Security APIs | `GET_SIGNING_CERTIFICATES` (API 28+) · `GET_SIGNATURES` (API 24–27) · `X509Certificate` · `MessageDigest` (SHA-256, SHA-1) · `java.time.*` |
| Testing | JUnit 5 (Jupiter) · `kotlin-test-junit5` · Kotlinx Coroutines Test · Turbine · Mockk |
| Static analysis | Detekt + Detekt Formatting (config at `config/detekt/detekt.yml`, `maxIssues: 0`) |

## Repository layout

```
app/src/main/java/com/mohsenoid/certhunter/
├── App.kt                       Application — Koin + Coil ImageLoader init
├── coroutine/                   DispatcherProvider interface + DefaultDispatcherProvider
├── data/repository/             AppRepositoryImpl + SignerSelector
├── di/AppModule.kt              Single Koin module
├── domain/
│   ├── model/                   Pure Kotlin data + sealed classes
│   └── repository/              Repository interface
└── ui/
    ├── AppNavHost.kt / AppDestinations.kt / MainActivity.kt
    ├── about/
    ├── detail/                  AppDetailScreen, ViewModel, UiModel
    ├── list/                    AppListScreen, ViewModel, UiModel (+ widget/)
    └── theme/

openspec/
├── project.md                   This file
├── specs/                       Living capability + convention specs
└── changes/                     In-flight proposals (archived under changes/archive/)
```

## A note on convention specs

`openspec/specs/code-conventions/` and `openspec/specs/testing-conventions/` describe how this codebase is structured and tested, not behaviours of the running app. They are deliberately encoded as OpenSpec specs (with SHALL rules and review-style scenarios) so the constitution lives in one place alongside the capability specs. This is a documented compromise — OpenSpec specs are normally behavioural — and is intentional rather than accidental.

## Workflow

CertHunter uses the OpenSpec spec-driven workflow:

1. Start a new change with `openspec new change <slug>`.
2. Edit the generated proposal, optional design, delta specs, and tasks. Run `openspec validate <slug> --strict` as you go.
3. Implement the tasks; tick them off in `tasks.md` as you go.
4. Run `openspec archive <slug>` to fold the deltas into the living specs and move the change under `openspec/changes/archive/`.

Agent-specific shortcuts (slash commands, skill packs, plugins) may exist on a per-developer basis but are not required. The `openspec` CLI is the canonical interface. See [AGENTS.md](../AGENTS.md) for the full workflow and CLI reference; install the CLI per the project README.
