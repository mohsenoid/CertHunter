package com.mohsenoid.certhunter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.rememberNavBackStack
import com.mohsenoid.certhunter.ui.detail.AppDetailScreen
import com.mohsenoid.certhunter.ui.detail.AppDetailViewModel
import com.mohsenoid.certhunter.ui.list.AppListScreen
import com.mohsenoid.certhunter.ui.list.AppListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppList)

    val listViewModel: AppListViewModel = koinViewModel()
    val listUiState by listViewModel.uiState.collectAsState()

    AppListScreen(
        uiState = listUiState,
        onSearchQueryChanged = listViewModel::onSearchQueryChanged,
        onAppClick = { app -> backStack.add(AppDetail(app.packageName)) },
        onToggleSystemApps = listViewModel::onToggleSystemApps,
    )

    (backStack.lastOrNull() as? AppDetail)?.let { detail ->
        val detailViewModel: AppDetailViewModel = koinViewModel(key = detail.packageName) { parametersOf(detail.packageName) }
        val detailUiState by detailViewModel.uiState.collectAsState()
        AppDetailScreen(
            uiState = detailUiState,
            onDismiss = { backStack.removeLastOrNull() },
        )
    }
}
