package com.mohsenoid.certhunter.fake

import com.github.michaelbull.result.Result
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.repository.AppRepository
import kotlinx.coroutines.awaitCancellation

class FakeAppRepository : AppRepository {
    var appsResult: List<AppItem> = emptyList()
    var shouldThrow: Boolean = false
    var detailsResult: Result<AppDetails, AppDetailsError>? = null

    // When true, getInstalledApps suspends until the calling coroutine is cancelled.
    // Used by cancellation / newest-request-wins tests in AppListViewModelTest.
    var suspendForever: Boolean = false

    override suspend fun getInstalledApps(): List<AppItem> {
        if (suspendForever) awaitCancellation()
        if (shouldThrow) error("Failed to load apps")
        return appsResult
    }

    override suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError> {
        return detailsResult ?: error("FakeAppRepository.detailsResult not configured")
    }
}
