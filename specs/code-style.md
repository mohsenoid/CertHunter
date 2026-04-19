# Code Style

## Guiding Principle

Code is read far more often than it is written. Prefer clarity over cleverness. The right amount of complexity is the minimum needed for the task.

---

## File Organisation

- **One class per file.** No exceptions. The file name matches the class name exactly.
- **Package hierarchy mirrors logical grouping:** `domain`, `data`, `ui`, `di`, `coroutine`.
- **Widget composables** go in a `widget/` subdirectory inside the screen package that owns them.
- **Test doubles** (`Fake*`, `Test*`) live in `src/test/.../fake/`.

---

## Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes, interfaces, enums, objects | `PascalCase` | `AppRepository`, `CertificateValidity` |
| Composable functions | `PascalCase` | `AppListScreen`, `CertificateValidityBadge` |
| Functions, properties, variables | `camelCase` | `filteredApps`, `loadApps()` |
| Constants (companion object / top-level) | `UPPER_SNAKE_CASE` | `EXPIRY_WARNING_DAYS` |
| Test functions | backtick sentence (GWT) | `` `given X when Y then Z` `` |

### Suffixes

| Suffix | When to use |
|--------|-------------|
| `ViewModel` | `AppListViewModel`, `AppDetailViewModel` |
| `UiModel` | `AppListUiModel`, `AppDetailUiModel` |
| `Screen` | Top-level composable for a destination |
| `Error` | Sealed error class |
| `Impl` | Repository implementation |
| `Provider` | Interface that supplies something (e.g., `DispatcherProvider`) |
| `Fetcher` | Coil fetcher |

Do not suffix sealed classes with `Error` unless they represent errors. `CertificateValidity` is a sealed class — no suffix needed.

---

## Kotlin Style

### Sealed classes over boolean flags

Prefer:
```kotlin
sealed class CertificateValidity {
    data object Valid : CertificateValidity()
    data class ExpiringSoon(val daysLeft: Long) : CertificateValidity()
    data object Expired : CertificateValidity()
}
```
Over:
```kotlin
val isExpired: Boolean
val isExpiringSoon: Boolean
val daysLeft: Long?
```

### Data classes for models

All domain models are `data class`. No mutable state in models.

### Result for typed errors

Use `Result<V, E>` (kotlin-result) at repository boundaries where callers need to distinguish error types. Throw (and catch at ViewModel level) for unrecoverable infrastructure errors.

### Scope functions

- Use `.let {}`, `.also {}`, `.run {}`, `.apply {}` only when they meaningfully reduce noise.
- Do not chain more than two scope functions — break into named variables instead.

### Coroutines

- `suspend` functions at the repository level, never at the ViewModel action level.
- ViewModels launch with `viewModelScope.launch(dispatcherProvider.io)`.
- Never reference `Dispatchers.*` directly in production code — use `dispatcherProvider`.

### Suppression

Suppression annotations must include a comment explaining why:

```kotlin
@Suppress("TooGenericExceptionCaught") // PackageManager can throw undocumented exceptions
```

---

## Compose

### Screen decomposition

Every screen has a **root composable** (`AppListScreen`) that takes `ViewModel`-resolved state and callbacks, and an **inner content composable** that is pure and previewable:

```kotlin
@Composable
fun AppListScreen(viewModel: AppListViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    AppListContent(state = state, onSearch = viewModel::onSearchQueryChanged, …)
}

@Composable
private fun AppListContent(state: AppListUiModel, onSearch: (String) -> Unit, …) { … }
```

This keeps previews fast and free of DI.

### State hoisting

Hoist state to the lowest ViewModel that owns it. Do not pass `ViewModel` instances into child composables — pass lambdas and state values.

### Preview annotations

Use the composite preview annotations in `ui/util/PreviewAnnotation.kt`:

| Annotation | Covers |
|-----------|--------|
| `@DarkLightPreviews` | Light and dark theme |
| `@FontScalesPreviews` | Default and large font scale |
| `@ComponentPreviews` | Dark/light × font scales |
| `@ScreenPreviews` | Full-screen previews in dark/light |

Do not create one-off `@Preview` annotations on individual composables when a standard composite covers it.

### Material 3

Use Material 3 components exclusively. Do not mix Material 2 (`androidx.compose.material`) components.

---

## Error Handling

### Repository errors

Return `Result<T, E>` with a domain-specific sealed error type. Never surface `Exception` types to the UI.

### ViewModel errors

Wrap repository calls in `try/catch`. Map exceptions to UiModel error flags. Log with KLogX at `.e()` level.

### No silent failures

Every error path must either:
1. Update UI state so the user knows something went wrong, or
2. Log at `.w()` or `.e()` level with enough context to diagnose.

Never catch an exception and do nothing.

---

## Logging

Use KLogX. Create a logger per class as an anonymous object in the companion:

```kotlin
companion object {
    private val log = object : DefaultKLogWriter {
        override val tag: String = "AppRepositoryImpl"
    }
}
```

Levels:
- `.d()` — debug/trace information useful during development
- `.w()` — unexpected but recoverable conditions
- `.e(throwable = e)` — errors that affect functionality

Wrap all logging in `if (BuildConfig.DEBUG)` where performance matters.

---

## Dependency Injection

- All bindings are defined in `di/AppModule.kt`. No other files create Koin modules.
- `single<Interface> { Impl(…) }` — always bind to the interface type.
- `viewModel { (param: Type) -> ViewModel(param, get(), get()) }` for parametric ViewModels.
- Never call `get()` outside of a Koin module or `koinViewModel()` composable.

---

## Section Dividers

Use ASCII section dividers in files with many logical sections:

```kotlin
// ─── Certificate parsing ──────────────────────────────────────────────────────
```

---

## What to Avoid

- **God objects** — if a class does two things, split it.
- **Unnecessary abstractions** — don't create an interface for something that will never have a second implementation (outside of testability requirements).
- **Magic numbers** — extract to named constants.
- **Commented-out code** — delete it; git history preserves it.
- **Abbreviations in names** — `packageName` not `pkgNm`, `certificateDetails` not `certDet`.
- **Hard-coded `Dispatchers.*`** in production code — use `DispatcherProvider`.
