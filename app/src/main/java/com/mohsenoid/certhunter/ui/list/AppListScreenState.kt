package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem

data class AppListScreenState(
    val uiState: AppListUiModel = AppListUiModel(),
    val displayedApps: List<AppItem> = emptyList(),
)
