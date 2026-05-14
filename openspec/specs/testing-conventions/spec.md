# testing-conventions Specification

## Purpose

This convention spec codifies how CertHunter writes and runs tests. Like `code-conventions`, it is not a behaviour of the running app but a constitution rule for contributors. It lives here so the project's testing rules are reviewable on pull requests.

## Requirements

### Requirement: JUnit 5 with Kotlin coroutines test

The codebase SHALL use JUnit 5 (Jupiter) as the test runner. Coroutine-driven tests SHALL be written with `kotlinx-coroutines-test`'s `runTest` and a `StandardTestDispatcher`. `advanceUntilIdle()` SHALL be called after triggering background work and before asserting on final state.

#### Scenario: ViewModel test

- **WHEN** writing a ViewModel test that performs async work
- **THEN** the test uses `runTest(testDispatcher)` and a `StandardTestDispatcher`
- **AND** the test calls `advanceUntilIdle()` after invoking the action that triggers the work
- **AND** assertions read from `viewModel.uiState.value` (or a Turbine `awaitItem()`)

### Requirement: Use Turbine with StandardTestDispatcher

The codebase SHALL use Turbine (`Flow.test {}`) to assert on StateFlow emissions over time. To avoid Turbine's virtual-time timeout issues, Turbine SHALL be paired with `StandardTestDispatcher`, not `UnconfinedTestDispatcher`.

#### Scenario: Observing emissions

- **WHEN** a test needs to observe multiple emissions from a StateFlow
- **THEN** the test uses `flow.test {}` from Turbine
- **AND** the underlying test dispatcher is a `StandardTestDispatcher`
- **AND** the test calls `cancelAndIgnoreRemainingEvents()` at the end

### Requirement: FakeAppRepository for ViewModel tests

The codebase SHALL use a hand-rolled `FakeAppRepository` (at `src/test/.../fake/FakeAppRepository.kt`) when testing ViewModels and use-cases. `AppRepository` SHALL NOT be mocked with Mockk in ViewModel tests — a fake gives compile-time safety when the interface changes.

#### Scenario: New ViewModel test

- **WHEN** a new ViewModel test is added
- **THEN** the test constructs the ViewModel with a `FakeAppRepository` instance
- **AND** test setup configures the fake's fields (such as `appsResult`, `shouldThrow`) rather than stubbing methods

### Requirement: TestDispatcherProvider shared across layers

The codebase SHALL inject a `TestDispatcherProvider` (at `src/test/.../fake/TestDispatcherProvider.kt`) wherever production code receives a `DispatcherProvider`. All three properties (`main`, `io`, `default`) SHALL return the same `StandardTestDispatcher` instance so coroutine execution remains deterministic.

#### Scenario: Wiring a test

- **WHEN** a test instantiates production code that needs a `DispatcherProvider`
- **THEN** the test uses `TestDispatcherProvider(testDispatcher)` and shares the same dispatcher with `runTest`

### Requirement: Mockk for Android SDK classes only

The codebase MAY use Mockk to mock Android SDK types (`PackageManager`, `PackageInfo`, `ApplicationInfo`, `Signature`, …) in `AppRepositoryImplTest`. Mockk SHALL NOT be used to mock project-owned interfaces such as `AppRepository` — see the `FakeAppRepository` rule. Mockk `relaxed = true` mocks SHALL have their public Java fields set directly; `every { … }` is for method stubbing only.

#### Scenario: Mocking PackageManager

- **WHEN** a data-layer test needs `PackageManager`
- **THEN** the test uses `@MockK private lateinit var packageManager: PackageManager`
- **AND** behaviour is stubbed via `every { packageManager.getInstalledPackages(...) } returns ...`

### Requirement: Given-When-Then test naming

Test functions SHALL be named in backticked given-when-then sentence form, readable as a specification. Prefixes may be omitted if the sentence reads naturally. Tests SHALL group related scenarios under `@Nested inner class`.

#### Scenario: Naming a test

- **WHEN** writing a test for the case where the repository throws on load
- **THEN** the function is named `` `given repository throws when loadApps then hasLoadError is true` ``
- **AND** the function lives inside an `@Nested inner class LoadApps` block

### Requirement: Coverage targets

The codebase SHALL unit-test:

- Every branching path of `SignerSelector` (multi-signer, single-signer, empty history).
- Both API paths of `AppRepositoryImpl` (API 28+ and API 24–27), parse-failure handling, and missing-cert handling.
- Every ViewModel: initial state, loading, success, error, retry, and user actions.
- Every UiModel's computed property — for example `AppListUiModel.filteredApps` exhaustively across sort orders.

The codebase SHALL NOT unit-test composable rendering, Koin module wiring, `DefaultDispatcherProvider`, or generated data-class members.

#### Scenario: Adding a new branching path

- **WHEN** new branching logic is added to `SignerSelector` or `AppRepositoryImpl`
- **THEN** a unit test covering the new branch is added alongside the change
- **AND** the test is part of the `testDebugUnitTest` task and runs in CI

### Requirement: Dispatchers.setMain in ViewModel tests

ViewModel tests SHALL call `Dispatchers.setMain(testDispatcher)` in `@BeforeEach` and `Dispatchers.resetMain()` in `@AfterEach`, so that `viewModelScope` uses the test dispatcher.

#### Scenario: ViewModel test lifecycle

- **WHEN** writing a ViewModel test class
- **THEN** `Dispatchers.setMain(testDispatcher)` runs before each test
- **AND** `Dispatchers.resetMain()` runs after each test
