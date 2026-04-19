# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Constitution

All architectural decisions, coding conventions, and technical standards are documented in the `specs/` directory. Read these before making non-trivial changes:

| Document | Read when… |
|----------|-----------|
| [specs/MISSION.md](specs/MISSION.md) | You need to understand what the app is for and who it serves |
| [specs/architecture.md](specs/architecture.md) | You are adding a feature, new screen, or touching the data layer |
| [specs/tech-stack.md](specs/tech-stack.md) | You are adding a dependency or working with an unfamiliar library |
| [specs/testing.md](specs/testing.md) | You are writing or reviewing tests |
| [specs/code-style.md](specs/code-style.md) | You are unsure about naming, file layout, or Compose conventions |

## Build & Run Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires connected device/emulator)
./gradlew clean                  # Clean build outputs
```

## Pre-Commit Checks

Always run these before committing:

```bash
./gradlew :app:detekt            # Lint (auto-corrects formatting)
./gradlew :app:testDebugUnitTest # Unit tests
```

## Architecture

**CertHunter** is a single-Activity Android app (Jetpack Compose + Material 3) that lets users inspect signing certificates of installed apps. Full details in [specs/architecture.md](specs/architecture.md).

### Module / Package Structure

```
app/src/main/java/com/mohsenoid/certhunter/
├── App.kt                          — Application class; initialises Koin + Coil ImageLoader
├── coroutine/                      — DispatcherProvider interface + DefaultDispatcherProvider
├── data/repository/
│   ├── AppRepositoryImpl.kt        — PackageManager queries, certificate parsing
│   └── SignerSelector.kt           — Pure function: active vs historical signer bytes
├── di/AppModule.kt                 — Koin module (singleton repo, ViewModels)
├── domain/
│   ├── model/                      — AppItem, AppDetails, AppCertificateDetails, errors, enums
│   └── repository/AppRepository.kt — Repository interface
└── ui/
    ├── AppNavHost.kt / AppDestinations.kt / MainActivity.kt
    ├── about/
    ├── detail/                     — AppDetailScreen, AppDetailViewModel, AppDetailUiModel
    ├── list/                       — AppListScreen, AppListViewModel, AppListUiModel
    │   └── widget/                 — AppListRow, AppIconFetcher, error widgets
    └── theme/
```

Every class lives in its own file.

### Key Data Models

- `AppItem` — installed app (name, packageName, isSystemApp, firstInstallTime)
- `AppCertificateDetails` — parsed X.509 info (SHA-256, SHA-1, owner, issuer, serialNumber, validFrom, validUntil, validity)
- `AppDetails` — `item: AppItem`, `certificates: List<AppCertificateDetails>`, `historicalCertificates: List<AppCertificateDetails>`
- `CertificateValidity` — sealed: Valid / ExpiringSoon(daysLeft) / Expired
- `AppDetailsError` — sealed: ItemLoadFailed / CertificateNotFound / CertificateParseFailed

### Data Flow

1. `AppListViewModel.init` → `AppRepository.getInstalledApps()` on IO dispatcher
2. Apps displayed in a `LazyColumn`; search + sort filters computed in `AppListUiModel.filteredApps`
3. Tapping an app opens `AppDetailScreen` (AlertDialog); `AppDetailViewModel` loads details
4. Tapping a field copies it to clipboard (toast feedback on Android ≤ 12)

### Certificate API Compatibility

`AppRepositoryImpl.getSignerBytes()` handles two `PackageManager` APIs:

- **API 28+**: `GET_SIGNING_CERTIFICATES` + `SigningInfo`
  - `apkContentsSigners` → active signers (all returned as `certificates`)
  - `signingCertificateHistory` minus the last entry → `historicalCertificates`
  - `SignerSelector.select()` performs this split (pure, unit-tested)
- **API 24–27**: deprecated `GET_SIGNATURES`; all signatures → `certificates`, no history

### Error Handling

- `AppListViewModel` wraps `getInstalledApps()` in try/catch for both initial load and pull-to-refresh; exposes `hasLoadError` + `onRetry()`.
- `AppRepository.getAppDetails()` returns `Result<AppDetails, AppDetailsError>` (kotlin-result).

### Icon Loading

App icons are loaded off the main thread via Coil 2.x:
- `AppIconFetcher` — custom `Fetcher.Factory<AppIconData>` registered globally in `App.onCreate()`
- `AppListRow` passes `AppIconData(packageName)` to `AsyncImage`

### Tech Stack

- Kotlin 2.3.20 + Coroutines 1.10.2
- Jetpack Compose BOM 2026.03.01, Material 3
- Android Gradle Plugin 9.1.1
- Koin 4.2.1, Navigation3 1.1.0, Coil 2.7.0, kotlin-result 2.3.1
- Min SDK 24 / Compile & Target SDK 37 (Android 15)
- Dependencies managed via Gradle version catalog (`gradle/libs.versions.toml`)

Full dependency list with versions in [specs/tech-stack.md](specs/tech-stack.md).

### Testing Stack

- JUnit 5 (`junit-jupiter`) + `kotlin-test-junit5`
- `kotlinx-coroutines-test` with `StandardTestDispatcher` + `advanceUntilIdle()`
- Turbine 1.2.1 (use `StandardTestDispatcher` to avoid virtual-time timeout issues)
- Mockk 1.14.9 (available for Android class mocking)
- `FakeAppRepository` + `TestDispatcherProvider` in `src/test/.../fake/`

Full testing conventions in [specs/testing.md](specs/testing.md).

### Required Permission

`QUERY_ALL_PACKAGES` — needed on Android 11+ to enumerate all installed packages (declared in `AndroidManifest.xml`).
