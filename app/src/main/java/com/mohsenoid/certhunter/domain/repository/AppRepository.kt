package com.mohsenoid.certhunter.domain.repository

import com.github.michaelbull.result.Result
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.AppItem

interface AppRepository {
    suspend fun getInstalledApps(): List<AppItem>
    suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError>
}
