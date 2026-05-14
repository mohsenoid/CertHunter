package com.mohsenoid.certhunter.domain.model

data class ShareCertificateLabels(
    val sha256: String,
    val sha1: String,
    val owner: String,
    val issuer: String,
    val serial: String,
    val validRange: String,
    val status: String,
    val signerHeader: String,
    val markerValid: String,
    val markerExpired: String,
    val markerExpiringSoon: String,
)
