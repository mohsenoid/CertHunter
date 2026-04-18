package com.mohsenoid.certhunter.fake

import com.github.michaelbull.result.Result
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.repository.AppRepository

class FakeAppRepository : AppRepository {
    var appsResult: List<AppItem> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun getInstalledApps(): List<AppItem> {
        if (shouldThrow) error("Failed to load apps")
        return appsResult
    }

    override suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError> {
        throw UnsupportedOperationException("Not needed for list tests")
    }
}
