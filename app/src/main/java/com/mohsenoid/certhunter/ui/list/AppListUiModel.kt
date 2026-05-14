package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder

data class AppListUiModel(
    val allApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadError: Boolean = false,
    val hasRefreshError: Boolean = false,
    val searchQuery: String = "",
    val showSystemApps: Boolean = true,
    val sortOrder: AppSortOrder = AppSortOrder.NameAscending,
)
