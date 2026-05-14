# AGENTS.md

This file is the canonical reference for any AI coding agent or human contributor working on CertHunter. Agent-specific instruction files (`CLAUDE.md`, `GEMINI.md`, `.cursor/rules/`, etc.) should be thin pointers to this document; do not duplicate workflow or architecture content into them.

## Project Constitution

The project constitution lives under `openspec/`. Read the relevant document before making non-trivial changes:

| Document | Read when… |
|---|---|
| [openspec/project.md](openspec/project.md) | You need to understand what the app is for and who it serves |
| [openspec/specs/app-inventory/spec.md](openspec/specs/app-inventory/spec.md) | You touch the app list, filtering, sorting, refresh, or icon loading |
| [openspec/specs/certificate-inspection/spec.md](openspec/specs/certificate-inspection/spec.md) | You touch certificate parsing, the detail dialog, validity, copy, or sharing |
| [openspec/specs/platform-support/spec.md](openspec/specs/platform-support/spec.md) | You change min/target SDK, the API 24–27 vs 28+ paths, or permissions |
| [openspec/specs/code-conventions/spec.md](openspec/specs/code-conventions/spec.md) | You are unsure about naming, file layout, or Compose conventions |
| [openspec/specs/testing-conventions/spec.md](openspec/specs/testing-conventions/spec.md) | You are writing or reviewing tests |
| [openspec/changes/](openspec/changes/) | Before starting work — check for an active proposal |
| [docs/roadmap.md](docs/roadmap.md) | You want to see Now / Next / Later priorities |

## OpenSpec Workflow

CertHunter uses [OpenSpec](https://github.com/Fission-AI/OpenSpec) for spec-driven development. Every non-trivial change starts as a proposal under `openspec/changes/<change-name>/` containing:

- `proposal.md` — what changes and why
- `design.md` *(optional)* — how it will be implemented; required when the change touches multiple capabilities, introduces a new pattern, or has non-obvious trade-offs
- `tasks.md` — checklist of implementation steps
- `specs/<capability>/spec.md` — delta to add/modify/remove requirements on the living spec

Drive the workflow with the `openspec` CLI:

```bash
openspec list                                       # list active changes
openspec list --json                                # machine-readable form
openspec new change "<name>"                        # scaffold a new change
openspec status --change "<name>"                   # artifact completion status
openspec status --change "<name>" --json
openspec instructions <artifact> --change "<name>" --json   # per-artifact guidance
openspec validate "<name>" --strict                 # validate a single change
openspec validate --all --strict                    # validate everything
openspec archive "<name>"                           # fold deltas into living specs
```

Typical flow for a new change:

1. `openspec new change <name>`
2. Author `proposal.md`, then `specs/<capability>/spec.md`, then `tasks.md`. Add `design.md` if the change is non-trivial.
3. `openspec validate <name> --strict`
4. Implement the tasks; tick them off in `tasks.md` as you go.
5. Run pre-commit checks (see below).
6. `openspec archive <name>` to fold the deltas into the living specs once merged.

Agent-specific conveniences (slash commands, skill packs, plugins) may exist on a per-developer basis but are not required. The CLI above is the canonical interface.

## Development Prerequisites

- JDK 21 (Temurin) for the Gradle build
- Android SDK with platform 37 installed
- `openspec` CLI on `PATH` — install with `npm install -g @fission-ai/openspec@latest` (requires Node ≥ 20.19)

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
./gradlew :app:detekt            # Lint
./gradlew :app:testDebugUnitTest # Unit tests
```

## Architecture

**CertHunter** is a single-Activity Android app (Jetpack Compose + Material 3) that lets users inspect signing certificates of installed apps. Full behavioural details in [openspec/specs/app-inventory/spec.md](openspec/specs/app-inventory/spec.md) and [openspec/specs/certificate-inspection/spec.md](openspec/specs/certificate-inspection/spec.md).

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

Do not assume `signatures[0]` is the full answer when changing certificate display logic — `SigningInfo` can represent multiple current signers or a rotation history.

### Error Handling

- `AppListViewModel` wraps `getInstalledApps()` in try/catch for both initial load and pull-to-refresh; exposes `hasLoadError` + `onRetry()`.
- `AppRepository.getAppDetails()` returns `Result<AppDetails, AppDetailsError>` (`com.michael-bull.kotlin-result`).

### Icon Loading

App icons are loaded off the main thread via Coil 2.x:

- `AppIconFetcher` — custom `Fetcher.Factory<AppIconData>` registered globally in `App.onCreate()`
- `AppListRow` passes `AppIconData(packageName)` to `AsyncImage`

## Tech Stack

- Kotlin 2.3.20 + Coroutines 1.10.2
- Jetpack Compose BOM 2026.03.01, Material 3
- Android Gradle Plugin 9.1.1
- Koin 4.2.1, Navigation3 1.1.0, Coil 2.7.0, michael-bull/kotlin-result 2.3.1
- Min SDK 24 / Compile & Target SDK 37 (Android 15)
- Dependencies managed via Gradle version catalog (`gradle/libs.versions.toml`)

The authoritative source for library versions is [gradle/libs.versions.toml](gradle/libs.versions.toml); see [openspec/project.md](openspec/project.md) for a high-level summary.

## Testing Stack

- JUnit 5 (`junit-jupiter`) + `kotlin-test-junit5`
- `kotlinx-coroutines-test` with `StandardTestDispatcher` + `advanceUntilIdle()`
- Turbine 1.2.1 (use `StandardTestDispatcher` to avoid virtual-time timeout issues)
- Mockk 1.14.9 (available for Android class mocking)
- `FakeAppRepository` + `TestDispatcherProvider` in `src/test/.../fake/`

Full testing conventions in [openspec/specs/testing-conventions/spec.md](openspec/specs/testing-conventions/spec.md).

## Required Permission

`QUERY_ALL_PACKAGES` — needed on Android 11+ to enumerate all installed packages (declared in `AndroidManifest.xml`).
