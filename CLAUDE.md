# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

- **`MainActivity.kt`** — the entire app in one file (~400 lines): data models, business logic, and all Compose UI
- **`ui/theme/`** — Material 3 theme with dynamic color support (Android 12+) and a purple/pink palette

### Key Data Models

- `AppItem` — installed app (name, packageName, icon)
- `CertificateDetails` — parsed X.509 info (SHA-256, SHA-1, owner, issuer, serialNumber, validFrom, validUntil)

### Data Flow

1. `LaunchedEffect` on `Dispatchers.IO` loads all installed packages via `PackageManager`
2. Apps shown in a `LazyColumn`; search filters the list in real-time
3. Tapping an app calls `getAppCertificateDetails()` → shows `CertificateDialog`
4. Tapping a field copies it to clipboard (toast feedback on Android ≤ 12)

### Certificate API Compatibility

The app handles two different `PackageManager` APIs:
- **API 28+**: `PackageManager.GET_SIGNING_CERTIFICATES` + `SigningInfo`
- **API 24–27**: `PackageManager.GET_SIGNATURES` (deprecated but required for older devices)

### Tech Stack

- Kotlin 2.0.21 + Coroutines
- Jetpack Compose BOM 2024.09.00, Material 3
- Android Gradle Plugin 9.0.0
- Min SDK 24 / Compile & Target SDK 36 (Android 15)
- Dependencies managed via Gradle version catalog (`gradle/libs.versions.toml`)

### Required Permission

`QUERY_ALL_PACKAGES` — needed on Android 11+ to enumerate all installed packages (declared in `AndroidManifest.xml`).
