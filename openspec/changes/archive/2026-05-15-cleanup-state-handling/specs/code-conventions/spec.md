## ADDED Requirements

### Requirement: Coroutines propagate cancellation

Coroutine `catch` blocks in ViewModels, repositories, and other application-layer code SHALL re-throw `kotlinx.coroutines.CancellationException` (and any subclass thereof) rather than treat it as a normal failure. Broad `catch (e: Exception)` blocks at these boundaries SHALL re-check for `CancellationException` and rethrow before applying any other handling such as logging, state mutation, or error-flag updates.

#### Scenario: Coroutine cancelled mid-load

- **WHEN** a ViewModel launches a coroutine that calls into the repository
- **AND** the coroutine is cancelled (for example, the ViewModel is cleared or a new load supersedes it)
- **THEN** the catch block re-throws `CancellationException` so structured cancellation completes normally
- **AND** the ViewModel does not set a `hasLoadError` / `hasRefreshError` flag from the cancellation

#### Scenario: Broad catch is allowed only with rethrow

- **WHEN** an author uses `@Suppress("TooGenericExceptionCaught")` to catch `Exception`
- **THEN** the catch body either re-throws `CancellationException` explicitly before any other handling, or the code path is documented to be unreachable from a cancellable coroutine

### Requirement: ViewModel state has a single source of truth

A ViewModel SHALL NOT hold mutable in-memory state (for example, a `private var foo: Foo?` written from a launched coroutine) that duplicates information already exposed through its `StateFlow` UI model. When an action handler needs that data it SHALL read the current `StateFlow.value` (or derive from it) instead of consulting a parallel mutable field.

#### Scenario: Action handler needs the loaded data

- **WHEN** a ViewModel needs access to data previously fetched by a load coroutine in order to handle a user action
- **THEN** it reads from `uiState.value` (or a derived flow over the same state) rather than from a mutable backing field
- **AND** there is no `private var` that mirrors a field of the `StateFlow` UI model

#### Scenario: Cached state that is intentionally not for the UI

- **WHEN** the ViewModel genuinely needs to cache something that is not part of the UI model (e.g. a paging cursor, an idempotency token)
- **THEN** that field MAY be a `private var` or `MutableStateFlow`, but it MUST NOT duplicate any field already present in the UI model

### Requirement: Derived UI state belongs in the ViewModel layer

Derived UI lists or projections — including filtering, sorting, grouping, and search results — SHALL be computed in the ViewModel (typically as a `StateFlow` built with `combine(...)` over only the inputs that affect them) rather than inside the UI model's constructor or as a property initialiser on the data class. UI-model `data class` definitions SHALL hold only inputs/state, not the products of running pipelines over them.

#### Scenario: Search/filter/sort pipeline

- **WHEN** a list screen exposes a derived list whose contents depend on inputs such as `searchQuery`, `showSystemApps`, and `sortOrder`
- **THEN** the pipeline is implemented as a derived `StateFlow<List<T>>` in the ViewModel, built with `combine(...)` over those inputs (plus the source list)
- **AND** the data class for the screen's UI model does not initialise a property by running the pipeline over its own fields

#### Scenario: Trivial projections

- **WHEN** a derivation is a single null-check or trivial transformation (e.g. `val canShare: Boolean get() = certificates.isNotEmpty()`)
- **THEN** it MAY live on the UI model as a `val ... get() = ...` computed property
- **AND** "trivial" specifically means O(1) work that does not iterate over a collection
