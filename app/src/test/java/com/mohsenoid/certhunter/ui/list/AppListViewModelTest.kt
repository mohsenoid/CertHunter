@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.fake.FakeAppRepository
import com.mohsenoid.certhunter.fake.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository

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
}
