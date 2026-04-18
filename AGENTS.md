# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires connected device/emulator)
./gradlew clean                  # Clean build outputs
```

## Architecture

**CertHunter** is a single-Activity Android app (Jetpack Compose + Material 3) that lets users inspect signing certificates of installed apps.

### Structure

- **`App.kt`** — application entry point; initializes Koin and logging
- **`di/AppModule.kt`** — dependency injection wiring for repository, dispatchers, and ViewModels
- **`data/repository/AppRepositoryImpl.kt`** — `PackageManager` access, certificate extraction, hashing, and validity calculation
- **`domain/model/`** — app, certificate, and UI-supporting domain models
- **`domain/repository/AppRepository.kt`** — repository contract used by ViewModels
- **`ui/AppNavHost.kt`** — Navigation 3 back stack plus screen/dialog orchestration
- **`ui/list/`** — app list screen, UI model, ViewModel, and row widget
- **`ui/detail/`** — certificate detail dialog, UI model, ViewModel, and detail widgets
- **`ui/about/`** — about bottom sheet
- **`ui/theme/`** — Material 3 theme with dynamic color support and app color/typography definitions

### Key Data Models

- `AppItem` — installed app summary (name, packageName, system-app flag, firstInstallTime)
- `AppCertificateDetails` — parsed X.509 info (SHA-256, SHA-1, owner, issuer, serialNumber, validFrom, validUntil, validity)
- `AppDetails` — combines `AppItem` with the selected certificate details for the detail dialog
- `AppSortOrder` / `CertificateValidity` / `AppDetailsError` / `CertificateError` — UI and error-state support models

### Data Flow

1. `App.kt` starts Koin; `MainActivity` hosts `AppNavHost()`
2. `AppListViewModel` loads installed packages from `AppRepository` on `Dispatchers.IO`
3. `AppListUiModel` derives filtered/sorted apps in-memory from search query, system-app toggle, and sort order
4. Selecting an app pushes `AppDetail(packageName)` onto the Navigation 3 back stack
5. `AppDetailViewModel` resolves `AppDetails` from the repository and drives the certificate dialog
6. Tapping a detail row copies the field to clipboard and triggers haptic feedback (plus a toast on Android 12L and below)

### Certificate API Compatibility

The app handles two different `PackageManager` APIs:

- **API 28+**: `PackageManager.GET_SIGNING_CERTIFICATES` + `SigningInfo`
- **API 24–27**: `PackageManager.GET_SIGNATURES` (deprecated but required for older devices)

Be careful on API 28+: `SigningInfo` can represent multiple current signers or certificate history after key rotation. Do not assume `signatures[0]` is the full
answer when changing certificate display logic.

### Implementation Notes

- The list screen currently computes filtering and sorting inside `AppListUiModel.filteredApps`; keep an eye on recomposition cost if the app list grows.
- `AppListRow` currently loads and rasterizes package icons during composition. If touching list performance, prefer moving icon loading/caching out of the
  composable.
- `AppListViewModel` and refresh flows should always reset loading indicators even when `PackageManager` calls fail; preserve recoverable error handling when
  refactoring.
- Certificate validity is derived from `X509Certificate.notAfter` relative to `LocalDate.now()` in the device time zone.

### Tech Stack

- Kotlin 2.3.20 + Coroutines
- Jetpack Compose BOM 2026.03.01, Material 3
- Android Gradle Plugin 9.1.0
- Koin 4.2.0 for DI
- AndroidX Navigation 3
- `kotlin-result` for result-style error handling
- `klogx` for logging
- Min SDK 24 / Compile & Target SDK 36 (Android 15)
- Dependencies managed via Gradle version catalog (`gradle/libs.versions.toml`)

### Required Permission

`QUERY_ALL_PACKAGES` — needed on Android 11+ to enumerate all installed packages (declared in `AndroidManifest.xml`).

### Testing Status

- `./gradlew test` currently passes, but test coverage is minimal and mostly template-level.
- High-value tests to add next: signer-selection/certificate parsing, list filter/sort behavior, and ViewModel error-state handling.
