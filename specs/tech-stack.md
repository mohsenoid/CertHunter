# Tech Stack

## Language & Build

| Tool | Version | Purpose |
|------|---------|---------|
| Kotlin | 2.3.20 | Primary language |
| Android Gradle Plugin | 9.1.1 | Android build toolchain |
| Gradle | wrapper | Build system |
| JDK | 21 | Build JDK (Temurin distribution) |
| Java source/target | 11 | Bytecode compatibility |
| Core library desugaring | 2.1.5 | java.time APIs on API < 26 |

## Android SDK

| Setting | Value |
|---------|-------|
| minSdk | 24 (Android 7.0) |
| targetSdk | 37 (Android 15) |
| compileSdk | 37 |
| applicationId | `com.mohsenoid.certhunter` |

## UI

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2026.03.01 | Compose version alignment |
| Material 3 | (BOM) | Design system and components |
| Compose UI | (BOM) | Core UI toolkit |
| Material Icons Core | (BOM) | Standard icon set |
| Material Icons Extended | (BOM) | Extended icon set |
| Activity Compose | 1.13.0 | `setContent {}`, `ComponentActivity` |

Dynamic color (Material You) is applied on API 31+.

## Navigation

| Library | Version | Notes |
|---------|---------|-------|
| Navigation3 Runtime | 1.1.0 | Jetpack Navigation 3 |
| Navigation3 UI | 1.1.0 | Scaffold + back stack helpers |

Navigation3 was chosen over Navigation 2.x for its first-class Compose support and type-safe routes via `@Serializable` data classes.

## Dependency Injection

| Library | Version | Notes |
|---------|---------|-------|
| Koin Android | 4.2.1 | DI framework |
| Koin AndroidX Compose | 4.2.1 | `koinViewModel()` composable |

Single `appModule` defined in `di/AppModule.kt`. ViewModels are declared with `viewModel {}` and support runtime parameters via `parametersOf()`.

## Concurrency

| Library | Version | Notes |
|---------|---------|-------|
| Kotlinx Coroutines | 1.10.2 | Async execution |

All background work uses `DispatcherProvider` (see `architecture.md`) rather than hard-coding dispatchers, enabling deterministic testing.

## Image Loading

| Library | Version | Notes |
|---------|---------|-------|
| Coil Compose | 2.7.0 | `AsyncImage`, custom fetchers |

App icons are loaded off the main thread via a custom `AppIconFetcher` that wraps `PackageManager.getApplicationIcon()`. The fetcher is registered globally in `App.onCreate()`.

## Error Handling

| Library | Version | Notes |
|---------|---------|-------|
| kotlin-result (`com.michael-bull.kotlin-result`) | 2.3.1 | Two-parameter `Result<V, E>` typed errors |

Not to be confused with Kotlin's built-in `kotlin.Result`. `com.michael-bull.kotlin-result` provides a two-parameter `Result<V, E>` type where both the success and error types are explicit. `AppRepository.getAppDetails()` returns `Result<AppDetails, AppDetailsError>`. Call sites use `.fold()`, `.map()`, `.mapError()`, and `.andThen()` from its DSL.

## Serialization

| Library | Version | Notes |
|---------|---------|-------|
| Kotlinx Serialization JSON | 1.11.0 | `@Serializable` nav keys |

Used exclusively for type-safe navigation route classes.

## Logging

| Library | Version | Notes |
|---------|---------|-------|
| KLogX Core | 2024.09.29 | Structured logging abstraction |
| KLogX Android Logcat | 2024.09.29 | Logcat appender |

Each class creates its own logger via an anonymous `DefaultKLogWriter` with a `tag` override. Logging is conditional on `BuildConfig.DEBUG`.

## Security APIs (Android SDK)

| API | Use |
|-----|-----|
| `PackageManager.GET_SIGNING_CERTIFICATES` | Active signers + rotation history (API 28+) |
| `PackageManager.GET_SIGNATURES` | Legacy signing info (API 24–27) |
| `java.security.cert.X509Certificate` | Certificate field extraction |
| `java.security.MessageDigest` | SHA-256 and SHA-1 fingerprint hashing |
| `java.time.*` | Date handling (desugared for API < 26) |

## Testing

| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 5 (Jupiter) | 6.0.3 | Test runner and assertions |
| kotlin-test-junit5 | (Kotlin) | Kotlin assertion extensions |
| Kotlinx Coroutines Test | 1.10.2 | `runTest`, `StandardTestDispatcher` |
| Turbine | 1.2.1 | Flow emission testing |
| Mockk | 1.14.9 | Mocking (including Android classes) |

See `testing.md` for patterns and conventions.

## Static Analysis

| Tool | Version | Config |
|------|---------|--------|
| Detekt | 1.23.8 | `config/detekt/detekt.yml` |
| Detekt Formatting | 1.23.8 | Auto-correct formatting on lint run |

Key rules: max line length 160, `maxIssues: 0` (strict), trailing commas enforced, Compose function naming exemptions. See `config/detekt/detekt.yml` for the full ruleset.

## Dependency Management

All dependencies are declared in `gradle/libs.versions.toml` (Gradle version catalog) and referenced as `libs.*` aliases in build files. No version numbers appear in `build.gradle.kts`.
