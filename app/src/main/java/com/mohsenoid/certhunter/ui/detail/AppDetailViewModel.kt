package com.mohsenoid.certhunter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.toShareText
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _events = MutableSharedFlow<AppDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AppDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun onAction(action: AppDetailAction) {
        when (action) {
            is AppDetailAction.ShareCertificate -> {
                val state = _uiState.value
                if (state.isLoading || state.error != null || state.certificates.isEmpty()) return
                val details = AppDetails(
                    item = AppItem(
                        name = state.appName,
                        packageName = state.packageName,
                        isSystemApp = state.isSystemApp,
                    ),
                    certificates = state.certificates,
                    historicalCertificates = state.historicalCertificates,
                )
                _events.tryEmit(AppDetailEvent.Share(details.toShareText(action.labels)))
            }
        }
    }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            val result = repository.getAppDetails(packageName)
                .onSuccess {
                    logger.d("Loaded certificate details for $packageName")
                }
                .onFailure { error ->
                    // CertificateNotFound and CertificateParseFailed are already logged
                    // by the repository layer; only ItemLoadFailed needs a log here.
                    if (error is AppDetailsError.ItemLoadFailed) {
                        logger.w("Failed to load package info for $packageName", throwable = error.cause)
                    }
                }
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
