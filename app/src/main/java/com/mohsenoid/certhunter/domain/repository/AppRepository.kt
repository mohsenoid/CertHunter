package com.mohsenoid.certhunter.domain.repository

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails

interface AppRepository {
    suspend fun getInstalledApps(): List<AppItem>
    suspend fun getCertificateDetails(packageName: String): CertificateDetails?
}
