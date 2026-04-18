package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder

data class AppListUiModel(
    val allApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadError: Boolean = false,
    val searchQuery: String = "",
    val showSystemApps: Boolean = true,
    val sortOrder: AppSortOrder = AppSortOrder.NameAscending,
) {
    val filteredApps: List<AppItem>
        get() = allApps
            .filter { showSystemApps || !it.isSystemApp }
            .filter {
                searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortOrder) {
                    AppSortOrder.NameAscending -> list.sortedBy { it.name.lowercase() }
                    AppSortOrder.NameDescending -> list.sortedByDescending { it.name.lowercase() }
                    AppSortOrder.InstallDateNewest -> list.sortedByDescending { it.firstInstallTime }
                    AppSortOrder.InstallDateOldest -> list.sortedBy { it.firstInstallTime }
                }
            }
}
