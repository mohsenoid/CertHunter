package com.mohsenoid.certhunter.ui.detail

import com.mohsenoid.certhunter.domain.model.AppCertificateDetails

data class AppDetailUiModel(
    val isLoading: Boolean = true,
    val packageName: String = "",
    val appName: String = "",
    val isSystemApp: Boolean = false,
    val details: AppCertificateDetails? = null,
)
