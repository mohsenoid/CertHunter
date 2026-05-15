package com.mohsenoid.certhunter.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class AppListViewModel(
    private val repository: AppRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val logger = object : DefaultKLogWriter {
        override val tag: String = "AppListViewModel"
    }

    private val _uiState = MutableStateFlow(AppListUiModel())
    val uiState: StateFlow<AppListUiModel> = _uiState.asStateFlow()

    val displayedApps: StateFlow<List<AppItem>> = combine(
        _uiState.map { it.allApps }.distinctUntilChanged(),
        _uiState.map { it.searchQuery }.distinctUntilChanged(),
        _uiState.map { it.showSystemApps }.distinctUntilChanged(),
        _uiState.map { it.sortOrder }.distinctUntilChanged(),
    ) { apps, query, showSystem, sortOrder ->
        filterAndSort(apps, query, showSystem, sortOrder)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var currentLoadJob: Job? = null

    init {
        loadApps()
    }

    private fun loadApps() {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch(dispatcherProvider.io) {
            executeLoad(
                logMessage = "Failed to load installed apps",
                onSuccess = { apps ->
                    _uiState.update { it.copy(allApps = apps, isLoadingApps = false, hasLoadError = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingApps = false, hasLoadError = true) }
                },
            )
        }
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true, hasRefreshError = false) }
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch(dispatcherProvider.io) {
            executeLoad(
                logMessage = "Failed to refresh installed apps",
                onSuccess = { apps ->
                    _uiState.update { it.copy(allApps = apps, isRefreshing = false, hasRefreshError = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isRefreshing = false, hasRefreshError = true) }
                },
            )
        }
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoadingApps = true, hasLoadError = false) }
        loadApps()
    }

    fun onDismissRefreshError() {
        _uiState.update { it.copy(hasRefreshError = false) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
    }

    fun onSortOrderChanged(sortOrder: AppSortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    // PackageManager exposes no specific exception contract. CancellationException
    // is rethrown explicitly (code-conventions: "Coroutines propagate cancellation")
    // and ensureActive() before the success update prevents a superseded job from
    // overwriting newer state.
    @Suppress("TooGenericExceptionCaught", "RethrowCaughtException")
    private suspend fun executeLoad(
        logMessage: String,
        onSuccess: (List<AppItem>) -> Unit,
        onFailure: () -> Unit,
    ) {
        try {
            val apps = repository.getInstalledApps()
            coroutineContext.ensureActive()
            onSuccess(apps)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(logMessage, throwable = e)
            onFailure()
        }
    }
}

private fun filterAndSort(
    apps: List<AppItem>,
    query: String,
    showSystem: Boolean,
    sortOrder: AppSortOrder,
): List<AppItem> = apps
    .filter { showSystem || !it.isSystemApp }
    .filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }
    .let { list ->
        when (sortOrder) {
            AppSortOrder.NameAscending -> list.sortedBy { it.name.lowercase() }
            AppSortOrder.NameDescending -> list.sortedByDescending { it.name.lowercase() }
            AppSortOrder.InstallDateNewest -> list.sortedByDescending { it.firstInstallTime }
            AppSortOrder.InstallDateOldest -> list.sortedBy { it.firstInstallTime }
        }
    }
