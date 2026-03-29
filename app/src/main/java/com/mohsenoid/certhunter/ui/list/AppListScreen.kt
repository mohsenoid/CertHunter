package com.mohsenoid.certhunter.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    uiState: AppListUiModel,
    onSearchQueryChanged: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onToggleSystemApps: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        if (uiState.isLoadingApps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                stickyHeader {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged,
                                onSearch = {},
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = { Text(stringResource(R.string.app_list_search_placeholder)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.app_list_search_clear_content_description))
                                        }
                                    } else {
                                        Box {
                                            IconButton(onClick = { showMenu = !showMenu }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.app_list_more_options_content_description)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (uiState.showSystemApps) stringResource(R.string.app_list_hide_system_apps)
                                                            else stringResource(R.string.app_list_show_system_apps)
                                                        )
                                                    },
                                                    onClick = {
                                                        onToggleSystemApps()
                                                        showMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        shape = MaterialTheme.shapes.extraLarge,
                        windowInsets = WindowInsets(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {}
                }

                items(uiState.filteredApps) { app ->
                    AppListRowWidget(app = app, onClick = { onAppClick(app) })
                }

                if (uiState.filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
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
    }
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
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppListScreenLoadingPreview() {
    CertHunterTheme {
        AppListScreen(
            uiState = AppListUiModel(isLoadingApps = true),
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
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
            onSearchQueryChanged = {},
            onAppClick = {},
            onToggleSystemApps = {},
        )
    }
}
