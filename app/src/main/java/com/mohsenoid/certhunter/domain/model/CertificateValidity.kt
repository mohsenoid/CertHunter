package com.mohsenoid.certhunter.domain.model

sealed class CertificateValidity {
    data object Valid : CertificateValidity()
    data class ExpiringSoon(val daysLeft: Long) : CertificateValidity()
    data object Expired : CertificateValidity()
}
