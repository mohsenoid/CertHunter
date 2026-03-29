package com.mohsenoid.certhunter.ui.model

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails

data class AppListUiModel(
    val allApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val searchQuery: String = "",
    val showSystemApps: Boolean = true,
    val selectedApp: AppItem? = null,
    val selectedAppCert: CertificateDetails? = null,
    val isLoadingCert: Boolean = false,
) {
    val filteredApps: List<AppItem>
        get() = allApps
            .filter { showSystemApps || !it.isSystemApp }
            .filter {
                searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
}
