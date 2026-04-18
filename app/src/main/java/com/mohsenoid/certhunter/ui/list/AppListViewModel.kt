package com.mohsenoid.certhunter.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppSortOrder
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppListViewModel(
    private val repository: AppRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val logger = object : DefaultKLogWriter {
        override val tag: String = "AppListViewModel"
    }

    private val _uiState = MutableStateFlow(AppListUiModel())
    val uiState: StateFlow<AppListUiModel> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(dispatcherProvider.io) {
            @Suppress("TooGenericExceptionCaught") // PackageManager exposes no specific exception contract
            try {
                val apps = repository.getInstalledApps()
                _uiState.update { it.copy(allApps = apps, isLoadingApps = false, hasLoadError = false) }
            } catch (e: Exception) {
                logger.e("Failed to load installed apps", throwable = e)
                _uiState.update { it.copy(isLoadingApps = false, hasLoadError = true) }
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.update { it.copy(isRefreshing = true) }
            @Suppress("TooGenericExceptionCaught") // PackageManager exposes no specific exception contract
            try {
                val apps = repository.getInstalledApps()
                _uiState.update { it.copy(allApps = apps, isRefreshing = false, hasLoadError = false) }
            } catch (e: Exception) {
                logger.e("Failed to refresh installed apps", throwable = e)
                _uiState.update { it.copy(isRefreshing = false, hasLoadError = true) }
            }
        }
    }

    fun onRetry() {
        _uiState.update { it.copy(isLoadingApps = true, hasLoadError = false) }
        loadApps()
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
}
