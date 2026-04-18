package com.mohsenoid.certhunter.domain.model

data class AppDetails(
    val item: AppItem,
    val certificates: List<AppCertificateDetails>,
    val historicalCertificates: List<AppCertificateDetails> = emptyList(),
)
