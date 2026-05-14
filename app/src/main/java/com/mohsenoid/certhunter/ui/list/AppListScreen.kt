package com.mohsenoid.certhunter.ui.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder
import com.mohsenoid.certhunter.ui.list.widget.AppListLoadError
import com.mohsenoid.certhunter.ui.list.widget.AppListRow
import com.mohsenoid.certhunter.ui.list.widget.RefreshErrorBanner
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    uiState: AppListUiModel,
    displayedApps: List<AppItem>,
    onSearchQueryChanged: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onToggleSystemApps: () -> Unit,
    onSortOrderChanged: (AppSortOrder) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDismissRefreshError: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onAboutClick) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.app_list_about))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        if (uiState.isLoadingApps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.hasLoadError) {
            AppListLoadError(
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AppListContent(
                    uiState = uiState,
                    displayedApps = displayedApps,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onAppClick = onAppClick,
                    onToggleSystemApps = onToggleSystemApps,
                    onSortOrderChanged = onSortOrderChanged,
                    onDismissRefreshError = onDismissRefreshError,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListContent(
    uiState: AppListUiModel,
    displayedApps: List<AppItem>,
    onSearchQueryChanged: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onToggleSystemApps: () -> Unit,
    onSortOrderChanged: (AppSortOrder) -> Unit,
    onDismissRefreshError: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        stickyHeader {
            AppListSearchBar(
                uiState = uiState,
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
                onSearchQueryChanged = onSearchQueryChanged,
                onToggleSystemApps = onToggleSystemApps,
                onSortOrderChanged = onSortOrderChanged,
            )
        }

        if (uiState.hasRefreshError) {
            item {
                RefreshErrorBanner(onDismiss = onDismissRefreshError)
            }
        }

        items(displayedApps) { app ->
            AppListRow(app = app, onClick = { onAppClick(app) })
        }

        if (displayedApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val message = if (uiState.searchQuery.isBlank()) {
                        stringResource(R.string.app_list_no_apps_found)
                    } else {
                        stringResource(R.string.app_list_no_apps_found_matching, uiState.searchQuery)
                    }
                    Text(message, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListSearchBar(
    uiState: AppListUiModel,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSystemApps: () -> Unit,
    onSortOrderChanged: (AppSortOrder) -> Unit,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.app_list_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.app_list_search_clear_content_description),
                            )
                        }
                    } else {
                        AppListSortMenu(
                            uiState = uiState,
                            showMenu = showMenu,
                            onShowMenuChange = onShowMenuChange,
                            onToggleSystemApps = onToggleSystemApps,
                            onSortOrderChanged = onSortOrderChanged,
                        )
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        shape = MaterialTheme.shapes.extraLarge,
        windowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {}
}

@Composable
private fun AppListSortMenu(
    uiState: AppListUiModel,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onToggleSystemApps: () -> Unit,
    onSortOrderChanged: (AppSortOrder) -> Unit,
) {
    Box {
        IconButton(onClick = { onShowMenuChange(!showMenu) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.app_list_more_options_content_description),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onShowMenuChange(false) },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (uiState.showSystemApps) {
                                R.string.app_list_hide_system_apps
                            } else {
                                R.string.app_list_show_system_apps
                            },
                        ),
                    )
                },
                onClick = {
                    onToggleSystemApps()
                    onShowMenuChange(false)
                },
            )
            HorizontalDivider()
            AppSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(stringResource(order.labelRes())) },
                    trailingIcon = {
                        if (uiState.sortOrder == order) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onSortOrderChanged(order)
                        onShowMenuChange(false)
                    },
                )
            }
        }
    }
}

@StringRes
private fun AppSortOrder.labelRes(): Int = when (this) {
    AppSortOrder.NameAscending -> R.string.app_list_sort_name_asc
    AppSortOrder.NameDescending -> R.string.app_list_sort_name_desc
    AppSortOrder.InstallDateNewest -> R.string.app_list_sort_install_newest
    AppSortOrder.InstallDateOldest -> R.string.app_list_sort_install_oldest
}

private val previewApps = listOf(
    AppItem(name = "CertHunter", packageName = "com.mohsenoid.certhunter", isSystemApp = false),
    AppItem(name = "Settings", packageName = "com.android.settings", isSystemApp = true),
    AppItem(name = "Chrome", packageName = "com.android.chrome", isSystemApp = false),
)

@ComponentPreviews
@Composable
private fun AppListScreenPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(allApps = previewApps, isLoadingApps = false),
            displayedApps = previewApps,
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
            onSortOrderChanged = {},
            onRefresh = {},
            onRetry = {},
            onDismissRefreshError = {},
            onAboutClick = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppListScreenLoadingPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(isLoadingApps = true),
            displayedApps = emptyList(),
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
            onSortOrderChanged = {},
            onRefresh = {},
            onRetry = {},
            onDismissRefreshError = {},
            onAboutClick = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppListScreenErrorPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(isLoadingApps = false, hasLoadError = true),
            displayedApps = emptyList(),
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
            onSortOrderChanged = {},
            onRefresh = {},
            onRetry = {},
            onDismissRefreshError = {},
            onAboutClick = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppListScreenRefreshErrorPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(allApps = previewApps, isLoadingApps = false, hasRefreshError = true),
            displayedApps = previewApps,
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
            onSortOrderChanged = {},
            onRefresh = {},
            onRetry = {},
            onDismissRefreshError = {},
            onAboutClick = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppListScreenEmptyPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(
                allApps = emptyList(),
                isLoadingApps = false,
                searchQuery = "nonexistent",
            ),
            displayedApps = emptyList(),
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
            onSortOrderChanged = {},
            onRefresh = {},
            onRetry = {},
            onDismissRefreshError = {},
            onAboutClick = {},
        )
    }
}
