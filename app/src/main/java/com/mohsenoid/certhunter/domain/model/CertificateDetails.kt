package com.mohsenoid.certhunter.domain.model

data class CertificateDetails(
    val sha256: String,
    val sha1: String,
    val owner: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validUntil: String
)
