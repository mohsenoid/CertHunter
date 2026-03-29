package com.mohsenoid.certhunter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.domain.repository.AppRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val packageName: String,
    private val repository: AppRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiModel(packageName = packageName))
    val uiState: StateFlow<AppDetailUiModel> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val appDeferred = async { repository.getAppItem(packageName) }
            val certDeferred = async { repository.getCertificateDetails(packageName) }
            val app = appDeferred.await()
            val cert = certDeferred.await()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    appName = app?.name ?: packageName,
                    isSystemApp = app?.isSystemApp ?: false,
                    details = cert,
                    packageName = packageName,
                )
            }
        }
    }
}
