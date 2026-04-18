package com.mohsenoid.certhunter.ui.detail

import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError

data class AppDetailUiModel(
    val isLoading: Boolean = true,
    val packageName: String = "",
    val appName: String = "",
    val isSystemApp: Boolean = false,
    val certificates: List<AppCertificateDetails> = emptyList(),
    val historicalCertificates: List<AppCertificateDetails> = emptyList(),
    val error: AppDetailsError? = null,
)
