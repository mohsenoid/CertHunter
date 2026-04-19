package com.mohsenoid.certhunter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
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

    private val logger = object : DefaultKLogWriter {
        override val tag: String = "AppDetailViewModel"
    }

    private val _uiState = MutableStateFlow(AppDetailUiModel(packageName = packageName))
    val uiState: StateFlow<AppDetailUiModel> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = repository.getAppDetails(packageName)
                .onSuccess { logger.d("Loaded certificate details for $packageName") }
                .onFailure { logger.w("Failed to load certificate details for $packageName: $it") }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    appName = result.fold({ it.item.name }, { packageName }),
                    isSystemApp = result.fold({ it.item.isSystemApp }, { false }),
                    certificates = result.fold({ it.certificates }, { emptyList() }),
                    historicalCertificates = result.fold({ it.historicalCertificates }, { emptyList() }),
                    error = result.fold({ null }, { it }),
                    packageName = packageName,
                )
            }
        }
    }
}
