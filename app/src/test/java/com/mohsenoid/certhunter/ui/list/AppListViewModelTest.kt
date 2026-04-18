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
    fun `initial load failure sets error and clears loading`() = runTest(testDispatcher) {
        fakeRepository.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingApps)
        assertTrue(viewModel.uiState.value.hasLoadError)
    }

    @Test
    fun `refresh failure sets error and clears refreshing`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle() // initial load completes

        fakeRepository.shouldThrow = true
        viewModel.onRefresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.uiState.value.hasLoadError)
    }

    @Test
    fun `retry after error loads apps successfully`() = runTest(testDispatcher) {
        fakeRepository.shouldThrow = true
        val viewModel = createViewModel()
        advanceUntilIdle() // initial load fails

        assertTrue(viewModel.uiState.value.hasLoadError)

        fakeRepository.shouldThrow = false
        fakeRepository.appsResult = listOf(AppItem("App", "com.app", false))
        viewModel.onRetry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLoadError)
        assertTrue(viewModel.uiState.value.allApps.isNotEmpty())
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
}
