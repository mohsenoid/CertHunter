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

    // --- Initial load ---

    @Test
    fun `initial load failure sets hasLoadError and clears loading`() = runTest(testDispatcher) {
        fakeRepository.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingApps)
        assertTrue(viewModel.uiState.value.hasLoadError)
    }

    @Test
    fun `initial load failure does not set hasRefreshError`() = runTest(testDispatcher) {
        fakeRepository.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasRefreshError)
    }

    @Test
    fun `successful load clears loading and populates apps`() = runTest(testDispatcher) {
        fakeRepository.appsResult = listOf(
            AppItem("CertHunter", "com.mohsenoid.certhunter", false),
            AppItem("Settings", "com.android.settings", true),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingApps)
        assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
        assertFalse(viewModel.uiState.value.hasLoadError)
    }

    // --- Refresh failure ---

    @Test
    fun `refresh failure sets hasRefreshError and clears refreshing`() = runTest(testDispatcher) {
        fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeRepository.shouldThrow = true
        viewModel.onRefresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.uiState.value.hasRefreshError)
    }

    @Test
    fun `refresh failure does not set hasLoadError`() = runTest(testDispatcher) {
        fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeRepository.shouldThrow = true
        viewModel.onRefresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLoadError)
    }

    @Test
    fun `refresh failure keeps existing apps visible`() = runTest(testDispatcher) {
        val existingApps = listOf(AppItem("App", "com.app", false))
        fakeRepository.appsResult = existingApps
        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeRepository.shouldThrow = true
        viewModel.onRefresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
        assertTrue(viewModel.uiState.value.allApps == existingApps)
    }

    @Test
    fun `successful refresh clears hasRefreshError`() = runTest(testDispatcher) {
        fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
        val viewModel = createViewModel()
        advanceUntilIdle()

        fakeRepository.shouldThrow = true
        viewModel.onRefresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasRefreshError)

        fakeRepository.shouldThrow = false
        viewModel.onRefresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasRefreshError)
    }

    // --- Retry ---

    @Test
    fun `retry after initial load failure loads apps successfully`() = runTest(testDispatcher) {
        fakeRepository.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLoadError)

        fakeRepository.shouldThrow = false
        fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
        viewModel.onRetry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLoadError)
        assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
    }
}
