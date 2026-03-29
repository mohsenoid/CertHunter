package com.mohsenoid.certhunter.domain.model

sealed class AppDetailsError {
    data class ItemLoadFailed(val cause: Throwable) : AppDetailsError()
    data object CertificateNotFound : AppDetailsError()
    data class CertificateParseFailed(val cause: Throwable) : AppDetailsError()
}
