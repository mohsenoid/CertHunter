package com.mohsenoid.certhunter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val packageName: String,
    private val repository: AppRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiModel(packageName = packageName))
    val uiState: StateFlow<AppDetailUiModel> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = repository.getAppDetails(packageName)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    appName = result.fold({ it.item.name }, { packageName }),
                    isSystemApp = result.fold({ it.item.isSystemApp }, { false }),
                    details = result.fold({ it.certificate }, { null }),
                    certificateError = result.fold({ false }, { it is AppDetailsError.CertificateParseFailed }),
                    packageName = packageName,
                )
            }
        }
    }
}
