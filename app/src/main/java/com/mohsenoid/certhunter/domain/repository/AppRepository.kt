package com.mohsenoid.certhunter.domain.repository

import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppItem

interface AppRepository {
    suspend fun getInstalledApps(): List<AppItem>
    suspend fun getAppItem(packageName: String): AppItem?
    suspend fun getCertificateDetails(packageName: String): AppCertificateDetails?
}
