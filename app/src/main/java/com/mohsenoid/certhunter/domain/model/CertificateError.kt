package com.mohsenoid.certhunter.domain.model

sealed class CertificateError {
    data object NotFound : CertificateError()
    data class ParseError(val cause: Throwable) : CertificateError()
}
