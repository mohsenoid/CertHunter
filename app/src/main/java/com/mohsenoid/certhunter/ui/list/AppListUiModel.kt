package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem

data class AppListUiModel(
    val allApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val searchQuery: String = "",
    val showSystemApps: Boolean = true,
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
