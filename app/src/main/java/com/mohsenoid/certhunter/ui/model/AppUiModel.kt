package com.mohsenoid.certhunter.ui.model

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails

data class AppUiModel(
    val allApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val searchQuery: String = "",
    val selectedApp: AppItem? = null,
    val selectedAppCert: CertificateDetails? = null,
    val isLoadingCert: Boolean = false,
) {
    val filteredApps: List<AppItem>
        get() = if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
}
