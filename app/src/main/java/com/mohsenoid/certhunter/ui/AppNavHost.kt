package com.mohsenoid.certhunter.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.rememberNavBackStack
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.ui.about.AppAboutScreen
import com.mohsenoid.certhunter.ui.detail.AppDetailEvent
import com.mohsenoid.certhunter.ui.detail.AppDetailScreen
import com.mohsenoid.certhunter.ui.detail.AppDetailViewModel
import com.mohsenoid.certhunter.ui.list.AppListScreen
import com.mohsenoid.certhunter.ui.list.AppListViewModel
import com.mohsenoid.certhunter.ui.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(AppList)

    val listViewModel: AppListViewModel = koinViewModel()
    val listScreenState by listViewModel.screenState.collectAsState()

    AppListScreen(
        screenState = listScreenState,
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
        val context = LocalContext.current
        val chooserTitle = stringResource(R.string.share_certificate_chooser_title)
        ObserveAsEvents(detailViewModel.events) { event ->
            when (event) {
                is AppDetailEvent.Share -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
                }
            }
        }
        AppDetailScreen(
            uiState = detailUiState,
            onAction = detailViewModel::onAction,
            onDismiss = { backStack.removeLastOrNull() },
        )
    }

    if (backStack.lastOrNull() is AppAbout) {
        val context = LocalContext.current
        val unknownVersion = stringResource(R.string.app_about_version_unknown)
        val versionName = remember(context.packageName) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownVersion
            } catch (_: PackageManager.NameNotFoundException) {
                unknownVersion
            }
        }
        AppAboutScreen(
            versionName = versionName,
            onDismiss = { backStack.removeLastOrNull() },
        )
    }
}
