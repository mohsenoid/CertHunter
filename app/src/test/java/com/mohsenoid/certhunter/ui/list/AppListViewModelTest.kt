@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mohsenoid.certhunter.ui.list

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder
import com.mohsenoid.certhunter.fake.FakeAppRepository
import com.mohsenoid.certhunter.fake.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository

    private val appA = AppItem("App A", "com.a", isSystemApp = false, firstInstallTime = 3000L)
    private val appB = AppItem("App B", "com.b", isSystemApp = false, firstInstallTime = 1000L)
    private val sysC = AppItem("App C", "com.c", isSystemApp = true, firstInstallTime = 2000L)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AppListViewModel(
        repository = fakeRepository,
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
    )

    // ─── §1.1 / §1.3 — error-flag behaviour preserved ────────────────────────────

    @Test
    fun `given repository throws on load when initial load then hasLoadError is true and loading is false`() =
        runTest(testDispatcher) {
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()

            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.isLoadingApps)
            assertTrue(viewModel.screenState.value.uiState.hasLoadError)
        }

    @Test
    fun `given repository throws on load when initial load then hasRefreshError remains false`() =
        runTest(testDispatcher) {
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()

            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.hasRefreshError)
        }

    @Test
    fun `given repository succeeds when initial load then loading is false and apps are populated`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(
                AppItem("CertHunter", "com.mohsenoid.certhunter", false),
                AppItem("Settings", "com.android.settings", true),
            )
            val viewModel = createViewModel()

            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.isLoadingApps)
            assertTrue(viewModel.screenState.value.uiState.allApps.isNotEmpty())
            assertFalse(viewModel.screenState.value.uiState.hasLoadError)
        }

    @Test
    fun `given loaded apps and repository throws on refresh when onRefresh then hasRefreshError is true and refreshing is false`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.isRefreshing)
            assertTrue(viewModel.screenState.value.uiState.hasRefreshError)
        }

    @Test
    fun `given loaded apps and refresh fails when onRefresh then hasLoadError remains false`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.hasLoadError)
        }

    @Test
    fun `given loaded apps and refresh fails when onRefresh then existing apps remain visible`() =
        runTest(testDispatcher) {
            val existingApps = listOf(AppItem("App", "com.app", false))
            fakeRepository.appsResult = existingApps
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            assertTrue(viewModel.screenState.value.uiState.allApps.isNotEmpty())
            assertTrue(viewModel.screenState.value.uiState.allApps == existingApps)
        }

    @Test
    fun `given refresh error when successful refresh then hasRefreshError is cleared`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()
            assertTrue(viewModel.screenState.value.uiState.hasRefreshError)

            fakeRepository.shouldThrow = false
            viewModel.onRefresh()
            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.hasRefreshError)
        }

    @Test
    fun `given initial load error when onRetry with working repository then apps are loaded and error is cleared`() =
        runTest(testDispatcher) {
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.screenState.value.uiState.hasLoadError)

            fakeRepository.shouldThrow = false
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            viewModel.onRetry()
            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.hasLoadError)
            assertTrue(viewModel.screenState.value.uiState.allApps.isNotEmpty())
        }

    // ─── §1.5 — cancellation completes normally, no error flag ───────────────────

    @Test
    fun `given suspended load when viewModelScope cancels then hasLoadError stays false and test completes`() =
        runTest(testDispatcher) {
            fakeRepository.suspendForever = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertFalse(viewModel.screenState.value.uiState.hasLoadError)

            viewModel.viewModelScope.cancel()
            advanceUntilIdle()

            assertFalse(viewModel.screenState.value.uiState.hasLoadError)
        }

    // ─── §1.6 — newest request wins ──────────────────────────────────────────────

    @Test
    fun `given a slower in-flight load when a new request supersedes then only the newer result is committed`() =
        runTest(testDispatcher) {
            fakeRepository.suspendForever = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.screenState.value.uiState.allApps.isEmpty())

            fakeRepository.suspendForever = false
            val newer = listOf(AppItem("NEW", "com.new", false))
            fakeRepository.appsResult = newer
            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(newer, viewModel.screenState.value.uiState.allApps)
            assertFalse(viewModel.screenState.value.uiState.hasLoadError)
            assertFalse(viewModel.screenState.value.uiState.isLoadingApps)
        }

    // ─── §1.7 — refresh banner clears immediately on retry ───────────────────────

    @Test
    fun `given refresh error when onRefresh begins then hasRefreshError is cleared immediately`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()
            assertTrue(viewModel.screenState.value.uiState.hasRefreshError)

            fakeRepository.shouldThrow = false
            fakeRepository.suspendForever = true
            viewModel.onRefresh()
            // Drain the screenState upstream collector without resuming the
            // suspended load (awaitCancellation is not on a scheduled timer).
            runCurrent()

            assertFalse(viewModel.screenState.value.uiState.hasRefreshError)
            assertTrue(viewModel.screenState.value.uiState.isRefreshing)

            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
        }

    // ─── §2 — derived displayedApps inside screenState (migrated from UiModelTest) ─

    @Test
    fun `given no query and system apps visible when displayedApps then all apps returned sorted by name`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(appA, appB, sysC), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given system apps hidden when displayedApps then only user apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onToggleSystemApps()
            advanceUntilIdle()

            assertEquals(listOf(appA, appB), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given query matching name when displayedApps then only matching apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("app a")
            advanceUntilIdle()

            assertEquals(listOf(appA), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given query matching package when displayedApps then matching app returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("com.b")
            advanceUntilIdle()

            assertEquals(listOf(appB), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given blank query when displayedApps then all apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("   ")
            advanceUntilIdle()

            assertEquals(3, viewModel.screenState.value.displayedApps.size)
        }

    @Test
    fun `given non-matching query when displayedApps then empty list returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("zzznomatch")
            advanceUntilIdle()

            assertTrue(viewModel.screenState.value.displayedApps.isEmpty())
        }

    @Test
    fun `given name descending sort when displayedApps then apps ordered Z to A`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.NameDescending)
            advanceUntilIdle()

            assertEquals(listOf(sysC, appB, appA), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given install date newest sort when displayedApps then most recently installed first`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.InstallDateNewest)
            advanceUntilIdle()

            assertEquals(listOf(appA, sysC, appB), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given install date oldest sort when displayedApps then oldest installed first`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.InstallDateOldest)
            advanceUntilIdle()

            assertEquals(listOf(appB, sysC, appA), viewModel.screenState.value.displayedApps)
        }

    @Test
    fun `given query and system apps hidden and install date sort when displayedApps then matching user apps returned newest first`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("App")
            viewModel.onToggleSystemApps()
            viewModel.onSortOrderChanged(AppSortOrder.InstallDateNewest)
            advanceUntilIdle()

            assertEquals(listOf(appA, appB), viewModel.screenState.value.displayedApps)
        }

    // ─── Atomicity: every emission carries a consistent (uiState, displayedApps) pair ─

    @Test
    fun `given search query change when observing screenState then every emission has uiState and displayedApps in sync`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.screenState.test {
                // Every emission must agree with itself: the displayed list must match
                // what filterAndSort would produce for the embedded uiState.
                val initial = awaitItem()
                assertConsistent(initial)

                viewModel.onSearchQueryChanged("App")
                advanceUntilIdle()
                assertConsistent(awaitItem())

                viewModel.onToggleSystemApps()
                advanceUntilIdle()
                assertConsistent(awaitItem())

                viewModel.onSortOrderChanged(AppSortOrder.NameDescending)
                advanceUntilIdle()
                assertConsistent(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun assertConsistent(state: AppListScreenState) {
        val expected = state.uiState.allApps
            .filter { state.uiState.showSystemApps || !it.isSystemApp }
            .filter {
                state.uiState.searchQuery.isBlank() ||
                    it.name.contains(state.uiState.searchQuery, ignoreCase = true) ||
                    it.packageName.contains(state.uiState.searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (state.uiState.sortOrder) {
                    AppSortOrder.NameAscending -> list.sortedBy { it.name.lowercase() }
                    AppSortOrder.NameDescending -> list.sortedByDescending { it.name.lowercase() }
                    AppSortOrder.InstallDateNewest -> list.sortedByDescending { it.firstInstallTime }
                    AppSortOrder.InstallDateOldest -> list.sortedBy { it.firstInstallTime }
                }
            }
        assertEquals(expected, state.displayedApps)
    }
}
