# Testing

## Philosophy

Tests prove that the logic works, not that the code exists. Every test should be readable as a specification: given some setup, when something happens, then the outcome is correct.

Unit tests cover the domain and presentation layers. The data layer is tested with a mix of unit tests (certificate parsing, signer selection) and mock-based integration tests against `PackageManager`.

---

## Stack

| Library | Version | Role |
|---------|---------|------|
| JUnit 5 (Jupiter) | 6.0.3 | Test runner, lifecycle, assertions |
| kotlin-test-junit5 | (Kotlin) | `assertEquals`, `assertTrue`, `assertIs` etc. |
| Kotlinx Coroutines Test | 1.10.2 | `runTest`, `StandardTestDispatcher`, `advanceUntilIdle()` |
| Turbine | 1.2.1 | `Flow.test {}` for StateFlow emissions |
| Mockk | 1.14.9 | Mocking, including Android SDK classes |

---

## Test Doubles

### FakeAppRepository

Located at `src/test/.../fake/FakeAppRepository.kt`.

```kotlin
class FakeAppRepository : AppRepository {
    var appsResult: List<AppItem> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun getInstalledApps(): List<AppItem> {
        if (shouldThrow) throw RuntimeException("test error")
        return appsResult
    }

    override suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError> {
        // configurable per test
    }
}
```

Use `FakeAppRepository` for ViewModel tests. Do not mock `AppRepository` directly — a fake gives you compile-time safety when the interface changes.

### TestDispatcherProvider

Located at `src/test/.../fake/TestDispatcherProvider.kt`.

```kotlin
class TestDispatcherProvider(
    private val testDispatcher: TestCoroutineDispatcher = StandardTestDispatcher()
) : DispatcherProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
}
```

All dispatcher properties return the same `StandardTestDispatcher`. This keeps coroutine execution deterministic — nothing runs until `advanceUntilIdle()` is called.

---

## Test Structure

### File layout

```
src/test/java/com/mohsenoid/certhunter/
├── fake/
│   ├── FakeAppRepository.kt
│   └── TestDispatcherProvider.kt
├── data/repository/
│   ├── AppRepositoryImplTest.kt
│   ├── SignerSelectorTest.kt
│   └── TestCertificates.kt
└── ui/list/
    ├── AppListViewModelTest.kt
    └── AppListUiModelTest.kt
```

### Naming

Test functions follow the **Given-When-Then** format:

```kotlin
@Test
fun `given apps loaded when search query changes then filtered list is updated`() = runTest { … }
```

Keep the backtick name readable as a sentence. Omit "given" / "when" / "then" prefixes if the sentence reads naturally without them.

### Grouping with @Nested

Use `@Nested inner class` to group tests by scenario or method under test:

```kotlin
class AppListViewModelTest {
    @Nested
    inner class LoadApps {
        @Test fun `given repository throws when loading then error state is shown`() { … }
        @Test fun `given apps exist when loaded then list is populated`() { … }
    }

    @Nested
    inner class Refresh { … }
}
```

---

## ViewModel Tests

### Setup

```kotlin
@ExtendWith(MockKExtension::class)
class AppListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAppRepository()
    private val dispatcherProvider = TestDispatcherProvider(testDispatcher)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

`Dispatchers.setMain()` is required so that `viewModelScope` uses the test dispatcher.

### Pattern

```kotlin
@Test
fun `given repository throws when loadApps then hasLoadError is true`() = runTest(testDispatcher) {
    // given
    fakeRepository.shouldThrow = true
    val viewModel = AppListViewModel(fakeRepository, dispatcherProvider)

    // when
    advanceUntilIdle()

    // then
    assertTrue(viewModel.uiState.value.hasLoadError)
}
```

Always call `advanceUntilIdle()` after actions that trigger async work, then assert on the final state.

### StateFlow assertions

For testing state emissions over time, use Turbine:

```kotlin
viewModel.uiState.test {
    val initial = awaitItem()
    assertTrue(initial.isLoadingApps)

    advanceUntilIdle()

    val loaded = awaitItem()
    assertEquals(2, loaded.allApps.size)
    cancelAndIgnoreRemainingEvents()
}
```

For simple "final state" assertions, `viewModel.uiState.value` after `advanceUntilIdle()` is sufficient.

---

## Data Layer Tests

### AppRepositoryImplTest

Uses Mockk to mock `PackageManager` and Android data classes:

```kotlin
@ExtendWith(MockKExtension::class)
class AppRepositoryImplTest {
    @MockK private lateinit var packageManager: PackageManager

    @BeforeEach
    fun setUp() {
        // configure packageManager.getInstalledPackages() mock
    }
}
```

Android data classes (`PackageInfo`, `ApplicationInfo`, `Signature`) are mocked with `mockk(relaxed = true)` and their public Java fields set directly:

```kotlin
val packageInfo = mockk<PackageInfo>(relaxed = true)
packageInfo.packageName = "com.example"
packageInfo.applicationInfo = mockk(relaxed = true)
```

Use `every {}` for method stubs — not for field reads on `relaxed` mocks.

### SignerSelectorTest

Pure function. No mocks needed:

```kotlin
@Test
fun `given single signer with history when select then first is active rest is historical`() {
    val result = SignerSelector.select(activeBytes = listOf(CERT_1), historyBytes = listOf(CERT_1, CERT_2))
    assertEquals(listOf(CERT_1), result.first)
    assertEquals(listOf(CERT_2), result.second)
}
```

`TestCertificates.kt` provides real DER-encoded certificate byte arrays (`CERT_1_DER`, `CERT_2_DER`) for realistic certificate parsing tests.

---

## What to Test

| Layer | What to test |
|-------|-------------|
| Domain models | Computed properties, sealed class exhaustiveness |
| SignerSelector | All branching paths (multi-signer, single-signer, empty history) |
| AppRepositoryImpl | API 28+ vs API 24-27 paths, parsing errors, missing certs |
| ViewModels | Initial state, loading, success, error, retry, user actions |
| UiModel | `filteredApps` computed property — all sort orders and filters |

---

## What Not to Test

- Composable rendering (no UI tests in this project currently)
- Koin module wiring (covered by runtime; over-testing DI config adds noise)
- Trivial data class getters/copy (Kotlin generates these correctly)
- `DefaultDispatcherProvider` (thin wrapper; no logic)

---

## Running Tests

```bash
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedAndroidTest       # instrumented tests (device/emulator required)
```

CI runs `testDebugUnitTest` on every pull request as part of the `test` job.
