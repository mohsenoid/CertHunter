package com.mohsenoid.certhunter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.certhunter.ui.model.AppListUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppListViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiModel())
    val uiState: StateFlow<AppListUiModel> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _uiState.update { it.copy(allApps = apps, isLoadingApps = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAppSelected(app: AppItem) {
        _uiState.update { it.copy(selectedApp = app, selectedAppCert = null, isLoadingCert = true) }
        viewModelScope.launch {
            val cert = repository.getCertificateDetails(app.packageName)
            _uiState.update { it.copy(selectedAppCert = cert, isLoadingCert = false) }
        }
    }

    fun onDialogDismissed() {
        _uiState.update { it.copy(selectedApp = null, selectedAppCert = null) }
    }
}
