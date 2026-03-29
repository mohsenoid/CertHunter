package com.mohsenoid.certhunter.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.data.repository.getAppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.ui.model.AppUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUiModel())
    val uiState: StateFlow<AppUiModel> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val apps = packages.map {
                AppItem(
                    name = it.applicationInfo?.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.applicationInfo?.loadIcon(pm)
                )
            }.sortedBy { it.name.lowercase() }

            _uiState.update { it.copy(allApps = apps, isLoadingApps = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAppSelected(app: AppItem) {
        _uiState.update { it.copy(selectedApp = app, selectedAppCert = null, isLoadingCert = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val cert = getAppCertificateDetails(pm, app.packageName)
            _uiState.update { it.copy(selectedAppCert = cert, isLoadingCert = false) }
        }
    }

    fun onDialogDismissed() {
        _uiState.update { it.copy(selectedApp = null, selectedAppCert = null) }
    }
}
