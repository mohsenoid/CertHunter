# code-conventions Specification

## Purpose

This convention spec codifies CertHunter's code-style rules. It is **not** a behavioural capability of the running app — it describes how the codebase itself is structured. It lives here so the project constitution stays in one place. Each requirement is reviewable on a pull request the same way a behaviour requirement is reviewable on a feature.

## Requirements

### Requirement: One class per file

The codebase SHALL place each class, interface, enum, sealed class, or object in its own file. The file name SHALL exactly match the declared top-level type's name. No file shall declare more than one top-level type, with the exception of small private helper extension functions that exclusively support a single neighbouring type.

#### Scenario: New domain model

- **WHEN** a new domain model is introduced (for example a sealed class with three subtypes)
- **THEN** the sealed parent and each subtype either live in the same file as nested types of the sealed parent OR each top-level type sits in its own file
- **AND** there is never a file containing two unrelated top-level types

### Requirement: Layered package structure

The codebase SHALL split production code into the packages `domain`, `data`, `ui`, `di`, and `coroutine`, with a strict one-way dependency rule: domain depends on nothing Android, data and ui depend on domain, ui never depends on data implementations directly.

#### Scenario: New repository

- **WHEN** a new repository is added
- **THEN** the interface lives under `domain/repository/`
- **AND** the implementation lives under `data/repository/`
- **AND** the binding lives in `di/AppModule.kt`

#### Scenario: UI helper

- **WHEN** a UI helper composable supports exactly one screen
- **THEN** it lives in that screen's `widget/` subdirectory (for example `ui/list/widget/AppListRow.kt`)

### Requirement: Naming conventions

The codebase SHALL follow Kotlin standard naming:

- Classes, interfaces, sealed classes, enums, objects, composables: `PascalCase`
- Functions, properties, variables: `camelCase`
- Top-level or companion-object constants: `UPPER_SNAKE_CASE`
- Test function names: backticked given-when-then sentences

Standard suffixes SHALL be applied where applicable: `ViewModel`, `UiModel`, `Screen`, `Impl`, `Provider`, `Fetcher`. The `Error` suffix is reserved for sealed error types.

#### Scenario: New screen

- **WHEN** a new screen is added
- **THEN** its top-level composable is named `<Name>Screen`
- **AND** its ViewModel is named `<Name>ViewModel`
- **AND** its UI state class is named `<Name>UiModel`

### Requirement: Sealed classes over boolean flags

The codebase SHALL prefer a sealed class over a combination of nullable / boolean flags whenever the underlying state is mutually exclusive. State that the UI needs to distinguish exhaustively SHALL be modelled as a sealed type.

#### Scenario: Certificate validity

- **WHEN** representing whether a certificate is valid, expiring soon, or expired
- **THEN** a sealed class `CertificateValidity` with `Valid`, `ExpiringSoon(daysLeft)`, and `Expired` is used
- **AND** the alternative `isExpired: Boolean` + `daysLeft: Long?` pair is not used

### Requirement: Typed errors at repository boundaries

The codebase SHALL return `com.michael-bull.kotlin-result.Result<V, E>` from repository methods where callers need to distinguish error categories. `kotlin.Result` SHALL NOT be used for this purpose. The error type SHALL be a sealed class whose members describe the recoverable failure modes.

#### Scenario: Adding a new repository method

- **WHEN** a new repository method has more than one expected failure mode that the UI needs to react to differently
- **THEN** the method returns `Result<T, SomeError>` from `com.michael-bull.kotlin-result`
- **AND** the error type is a sealed class declared under `domain/model/`

### Requirement: Coroutine dispatcher abstraction

The codebase SHALL NOT reference `Dispatchers.IO`, `Dispatchers.Main`, or `Dispatchers.Default` directly in production code. All dispatcher access SHALL go through `DispatcherProvider`. ViewModels SHALL launch background work via `viewModelScope.launch(dispatcherProvider.io)`. Repository `suspend` functions SHALL run their work on `dispatcherProvider.io`.

#### Scenario: Adding async work

- **WHEN** a new piece of async work is added in production code
- **THEN** the dispatcher is resolved via `dispatcherProvider`
- **AND** the change is testable by injecting a `TestDispatcherProvider` backed by a `StandardTestDispatcher`

### Requirement: Compose screen decomposition

Every Compose screen SHALL be split into a root composable that takes a ViewModel (typically via `koinViewModel()`) and an inner content composable that is pure and previewable. The inner composable SHALL accept state and callbacks only — not the ViewModel itself.

#### Scenario: Adding a screen

- **WHEN** a new screen is created
- **THEN** a `<Name>Screen` composable resolves the ViewModel and observes `uiState`
- **AND** a private `<Name>Content` composable receives the state and lambdas and is independently previewable

### Requirement: User-visible strings live in resources

The codebase SHALL place every user-visible string in `res/values/strings.xml` and reference it via `stringResource(R.string.…)`. Hard-coded English literals in composables or other user-facing APIs SHALL NOT be permitted.

#### Scenario: Adding a button label

- **WHEN** a new button or text element with user-facing text is introduced
- **THEN** the string is added to `res/values/strings.xml` and to every translated `values-<locale>/strings.xml`
- **AND** the composable references the resource via `stringResource(R.string.…)`

### Requirement: Material 3 only

The codebase SHALL use Material 3 components exclusively. Mixing components from `androidx.compose.material` (Material 2) and `androidx.compose.material3` SHALL NOT be permitted.

#### Scenario: Adding a button

- **WHEN** a new button is introduced
- **THEN** it is imported from `androidx.compose.material3`
- **AND** no imports from `androidx.compose.material.*` (other than `material.icons` which is Material-independent) appear

### Requirement: Suppression annotations require justification

The codebase SHALL only use `@Suppress(...)` when accompanied by an inline comment explaining the reason. Suppressions without comments SHALL fail review.

#### Scenario: Suppressing TooGenericExceptionCaught

- **WHEN** `@Suppress("TooGenericExceptionCaught")` is added to a function that calls `PackageManager`
- **THEN** an inline comment notes that `PackageManager` can throw undocumented exceptions

### Requirement: Single DI module

The codebase SHALL declare all Koin bindings in `di/AppModule.kt`. No other source file SHALL create Koin modules. `single<Interface> { Impl(...) }` SHALL bind to the interface type, not the concrete class.

#### Scenario: Adding a new ViewModel

- **WHEN** a new ViewModel is created
- **THEN** its binding is added to `appModule` in `di/AppModule.kt`
- **AND** no other file declares a Koin module
