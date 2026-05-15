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

    // ─── §1.1 / §1.3 — existing error-flag behaviour preserved ───────────────────

    @Test
    fun `given repository throws on load when initial load then hasLoadError is true and loading is false`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()

            // when
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.isLoadingApps)
            assertTrue(viewModel.uiState.value.hasLoadError)
        }

    @Test
    fun `given repository throws on load when initial load then hasRefreshError remains false`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()

            // when
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.hasRefreshError)
        }

    @Test
    fun `given repository succeeds when initial load then loading is false and apps are populated`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.appsResult = listOf(
                AppItem("CertHunter", "com.mohsenoid.certhunter", false),
                AppItem("Settings", "com.android.settings", true),
            )
            val viewModel = createViewModel()

            // when
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.isLoadingApps)
            assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
            assertFalse(viewModel.uiState.value.hasLoadError)
        }

    @Test
    fun `given loaded apps and repository throws on refresh when onRefresh then hasRefreshError is true and refreshing is false`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()

            // when
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertTrue(viewModel.uiState.value.hasRefreshError)
        }

    @Test
    fun `given loaded apps and refresh fails when onRefresh then hasLoadError remains false`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()

            // when
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.hasLoadError)
        }

    @Test
    fun `given loaded apps and refresh fails when onRefresh then existing apps remain visible`() =
        runTest(testDispatcher) {
            // given
            val existingApps = listOf(AppItem("App", "com.app", false))
            fakeRepository.appsResult = existingApps
            val viewModel = createViewModel()
            advanceUntilIdle()

            // when
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()

            // then
            assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
            assertTrue(viewModel.uiState.value.allApps == existingApps)
        }

    @Test
    fun `given refresh error when successful refresh then hasRefreshError is cleared`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasRefreshError)

            // when
            fakeRepository.shouldThrow = false
            viewModel.onRefresh()
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.hasRefreshError)
        }

    @Test
    fun `given initial load error when onRetry with working repository then apps are loaded and error is cleared`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.shouldThrow = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasLoadError)

            // when
            fakeRepository.shouldThrow = false
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            viewModel.onRetry()
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.hasLoadError)
            assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
        }

    // ─── §1.5 — cancellation completes normally, no error flag ───────────────────

    @Test
    fun `given suspended load when viewModelScope cancels then hasLoadError stays false and test completes`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.suspendForever = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasLoadError)

            // when
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()

            // then
            assertFalse(viewModel.uiState.value.hasLoadError)
        }

    // ─── §1.6 — newest request wins ──────────────────────────────────────────────

    @Test
    fun `given a slower in-flight load when a new request supersedes then only the newer result is committed`() =
        runTest(testDispatcher) {
            // given: initial load suspends forever
            fakeRepository.suspendForever = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.allApps.isEmpty())

            // when: retry with a working repository
            fakeRepository.suspendForever = false
            val newer = listOf(AppItem("NEW", "com.new", false))
            fakeRepository.appsResult = newer
            viewModel.onRetry()
            advanceUntilIdle()

            // then: newer result wins; load did not surface as error
            assertEquals(newer, viewModel.uiState.value.allApps)
            assertFalse(viewModel.uiState.value.hasLoadError)
            assertFalse(viewModel.uiState.value.isLoadingApps)
        }

    // ─── §1.7 — refresh banner clears immediately on retry ───────────────────────

    @Test
    fun `given refresh error when onRefresh begins then hasRefreshError is cleared immediately`() =
        runTest(testDispatcher) {
            // given: produce a refresh error first
            fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
            val viewModel = createViewModel()
            advanceUntilIdle()
            fakeRepository.shouldThrow = true
            viewModel.onRefresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasRefreshError)

            // when: start a new refresh that suspends so we can inspect mid-flight
            fakeRepository.shouldThrow = false
            fakeRepository.suspendForever = true
            viewModel.onRefresh()

            // then: banner cleared before the new refresh completes
            assertFalse(viewModel.uiState.value.hasRefreshError)
            assertTrue(viewModel.uiState.value.isRefreshing)

            // tidy up the suspended job so runTest can finish
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
        }

    // ─── §2 — derived displayedApps flow (migrated from AppListUiModelTest) ──────

    @Test
    fun `given no query and system apps visible when displayedApps then all apps returned sorted by name`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(appA, appB, sysC), viewModel.displayedApps.value)
        }

    @Test
    fun `given system apps hidden when displayedApps then only user apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onToggleSystemApps()
            advanceUntilIdle()

            assertEquals(listOf(appA, appB), viewModel.displayedApps.value)
        }

    @Test
    fun `given query matching name when displayedApps then only matching apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("app a")
            advanceUntilIdle()

            assertEquals(listOf(appA), viewModel.displayedApps.value)
        }

    @Test
    fun `given query matching package when displayedApps then matching app returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("com.b")
            advanceUntilIdle()

            assertEquals(listOf(appB), viewModel.displayedApps.value)
        }

    @Test
    fun `given blank query when displayedApps then all apps returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("   ")
            advanceUntilIdle()

            assertEquals(3, viewModel.displayedApps.value.size)
        }

    @Test
    fun `given non-matching query when displayedApps then empty list returned`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("zzznomatch")
            advanceUntilIdle()

            assertTrue(viewModel.displayedApps.value.isEmpty())
        }

    @Test
    fun `given name descending sort when displayedApps then apps ordered Z to A`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.NameDescending)
            advanceUntilIdle()

            assertEquals(listOf(sysC, appB, appA), viewModel.displayedApps.value)
        }

    @Test
    fun `given install date newest sort when displayedApps then most recently installed first`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.InstallDateNewest)
            advanceUntilIdle()

            assertEquals(listOf(appA, sysC, appB), viewModel.displayedApps.value)
        }

    @Test
    fun `given install date oldest sort when displayedApps then oldest installed first`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB, sysC)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onSortOrderChanged(AppSortOrder.InstallDateOldest)
            advanceUntilIdle()

            assertEquals(listOf(appB, sysC, appA), viewModel.displayedApps.value)
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

            assertEquals(listOf(appA, appB), viewModel.displayedApps.value)
        }

    @Test
    fun `given isRefreshing toggles when displayedApps observed then list does not re-emit`() =
        runTest(testDispatcher) {
            fakeRepository.appsResult = listOf(appA, appB)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.displayedApps.test {
                val initial = awaitItem()
                assertEquals(listOf(appA, appB), initial)

                // Toggling isRefreshing does not change any of the four inputs, so the
                // combine pipeline does not re-emit and the same list reference holds.
                viewModel.onRefresh()
                fakeRepository.suspendForever = true
                advanceUntilIdle()

                expectNoEvents()

                // Cleanup so runTest can complete.
                viewModel.viewModelScope.cancel()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
