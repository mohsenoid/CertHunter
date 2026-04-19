package com.mohsenoid.certhunter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.rememberNavBackStack
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.ui.about.AppAboutScreen
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
        onSortOrderChanged = listViewModel::onSortOrderChanged,
        onRefresh = listViewModel::onRefresh,
        onRetry = listViewModel::onRetry,
        onDismissRefreshError = listViewModel::onDismissRefreshError,
        onAboutClick = { backStack.add(AppAbout) },
    )

    (backStack.lastOrNull() as? AppDetail)?.let { detail ->
        val detailViewModel: AppDetailViewModel = koinViewModel(key = detail.packageName) {
            parametersOf(detail.packageName)
        }
        val detailUiState by detailViewModel.uiState.collectAsState()
        AppDetailScreen(
            uiState = detailUiState,
            onDismiss = { backStack.removeLastOrNull() },
        )
    }

    if (backStack.lastOrNull() is AppAbout) {
        val context = LocalContext.current
        val unknownVersion = stringResource(R.string.about_version_unknown)
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownVersion
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            unknownVersion
        }
        AppAboutScreen(
            versionName = versionName,
            onDismiss = { backStack.removeLastOrNull() },
        )
    }
}
