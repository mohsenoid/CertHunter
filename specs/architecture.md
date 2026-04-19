# Architecture

## Overview

CertHunter is a single-Activity Android app following **Clean Architecture** with an **MVI**-flavoured presentation layer. Responsibilities are split into three layers with a strict one-way dependency rule.

```
┌─────────────────────────────┐
│           UI Layer          │  Compose screens, ViewModels, UiModels
├─────────────────────────────┤
│         Data Layer          │  Repository implementation, certificate parsing
├─────────────────────────────┤
│        Domain Layer         │  Models, repository interface (pure Kotlin)
└─────────────────────────────┘
       ↑ depends on ↑
       (never reversed)
```

The domain layer has zero Android dependencies. The data and UI layers depend on domain. Nothing depends on UI.

---

## Package Structure

```
com.mohsenoid.certhunter/
├── App.kt                      Application class — Koin + Coil init
├── coroutine/                  DispatcherProvider abstraction
├── di/                         Koin module
├── domain/
│   ├── model/                  Pure Kotlin data classes and sealed classes
│   └── repository/             Repository interface
├── data/
│   └── repository/             Repository implementation + certificate logic
└── ui/
    ├── MainActivity.kt
    ├── AppNavHost.kt
    ├── AppDestinations.kt
    ├── list/                   App list screen
    │   └── widget/
    ├── detail/                 Certificate detail screen
    │   └── widget/
    ├── about/
    └── theme/
```

One class per file. No exceptions.

---

## Domain Layer

Pure Kotlin. No Android imports.

### Models

| Class | Type | Description |
|-------|------|-------------|
| `AppItem` | data class | Installed app metadata (name, packageName, isSystemApp, firstInstallTime) |
| `AppDetails` | data class | Item + active certificates + historical certificates |
| `AppCertificateDetails` | data class | Parsed X.509 fields: sha256, sha1, owner, issuer, serialNumber, validFrom, validUntil, validity |
| `CertificateValidity` | sealed class | `Valid`, `ExpiringSoon(daysLeft: Long)`, `Expired` |
| `AppSortOrder` | enum class | `NameAscending`, `NameDescending`, `InstallDateNewest`, `InstallDateOldest` |
| `AppDetailsError` | sealed class | `ItemLoadFailed(cause)`, `CertificateNotFound`, `CertificateParseFailed(cause)` |
| `CertificateError` | sealed class | `NotFound`, `ParseError` |

### Repository Interface

```kotlin
interface AppRepository {
    suspend fun getInstalledApps(): List<AppItem>
    suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError>
}
```

`getInstalledApps()` throws on failure (caught at ViewModel level). `getAppDetails()` returns a typed `Result` (`com.michael-bull.kotlin-result`, not `kotlin.Result`).

---

## Data Layer

### AppRepositoryImpl

Implements `AppRepository`. All work runs on `dispatcherProvider.io`.

**Certificate API compatibility:**

| Android version | API used | Capabilities |
|----------------|----------|-------------|
| API 28+ (Android 9+) | `GET_SIGNING_CERTIFICATES` + `SigningInfo` | Active signers + full rotation history |
| API 24–27 | Deprecated `GET_SIGNATURES` | All signatures as active; no history |

### SignerSelector

A pure function (`object` with `select()`) that splits `SigningInfo` into active and historical byte arrays:

- **Multi-signer APK**: All `apkContentsSigners` are active; history ignored.
- **Single-signer APK**: `apkContentsSigners[0]` is current; `signingCertificateHistory` minus the last entry becomes historical.

This is the only place key-rotation logic lives and is unit-tested in isolation.

### Certificate Parsing

- `CertificateFactory.getInstance("X.509")` + `X509Certificate`
- Fingerprints: `MessageDigest` → colon-separated uppercase hex
- Validity: `ChronoUnit.DAYS.between(today, notAfter)` → `CertificateValidity`
- Expiry warning threshold: 30 days

---

## Coroutine Abstraction

`DispatcherProvider` is an interface with three properties: `main`, `io`, `default`.

```kotlin
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
```

`DefaultDispatcherProvider` provides the real `Dispatchers.*` values. Tests inject `TestDispatcherProvider` backed by a `StandardTestDispatcher`.

**Rule:** Never reference `Dispatchers.IO` / `Dispatchers.Main` directly in production code. Always go through `dispatcherProvider`.

---

## Dependency Injection

Single Koin module in `di/AppModule.kt`:

```kotlin
val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<AppRepository> { AppRepositoryImpl(androidContext().packageManager, get()) }
    viewModel { AppListViewModel(get(), get()) }
    viewModel { (packageName: String) -> AppDetailViewModel(packageName, get(), get()) }
}
```

Started in `App.onCreate()`:

```kotlin
startKoin {
    androidContext(this@App)
    modules(appModule)
}
```

ViewModels are resolved in Compose with `koinViewModel()`. Parametric ViewModels use `koinViewModel { parametersOf(packageName) }`.

---

## Presentation Layer (MVI)

### State → UI → Event flow

```
ViewModel
  │  StateFlow<UiModel>
  ▼
Screen (Composable)
  │  User interactions (callbacks)
  ▼
ViewModel (updates state)
```

State flows down. Events flow up. Screens never mutate state directly.

### ViewModel conventions

- Extends `ViewModel()`
- Single `MutableStateFlow<UiModel>` exposed as `StateFlow` via `.asStateFlow()`
- State updates via `_state.update { it.copy(...) }`
- Background work via `viewModelScope.launch(dispatcherProvider.io) { … }`
- `@Suppress("TooGenericExceptionCaught")` used where `PackageManager` APIs can throw undocumented exceptions

### UiModel conventions

- `data class` with all fields defaulted
- Computed properties (e.g., `filteredApps`) derived from raw state fields — never stored separately
- No Android types; all UI-renderable primitives or domain models

---

## Navigation

Navigation3 with type-safe routes:

```kotlin
@Serializable sealed class NavKey
@Serializable data object AppList : NavKey()
@Serializable data class AppDetail(val packageName: String) : NavKey()
@Serializable data object AppAbout : NavKey()
```

Back stack managed in `AppNavHost` via `rememberNavBackStack(AppList)`. Navigation is performed with `backStack.add(route)` and `backStack.removeLastOrNull()`.

---

## Icon Loading

Custom Coil `Fetcher<AppIconData>` registered in `App.onCreate()`:

```kotlin
val imageLoader = ImageLoader.Builder(this)
    .components { add(AppIconFetcher.Factory(packageManager)) }
    .build()
Coil.setImageLoader(imageLoader)
```

`AppListRow` passes `AppIconData(packageName)` to `AsyncImage`. Icon loading is fully off-main-thread.

---

## Data Flow — End to End

```
App launch
  └── MainActivity.onCreate()
        └── AppNavHost → AppListScreen
              └── AppListViewModel.init
                    └── repository.getInstalledApps() [IO thread]
                          └── _state.update { it.copy(allApps = ...) }
                                └── Screen recomposes

User taps an app row
  └── backStack.add(AppDetail(packageName))
        └── AppDetailScreen rendered
              └── AppDetailViewModel(packageName).init
                    └── repository.getAppDetails(packageName) [IO thread]
                          └── Result.fold(...)
                                └── _state.update { it.copy(...) }
                                      └── Screen recomposes
```
