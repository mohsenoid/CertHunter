package com.mohsenoid.certhunter.data.repository

import com.mohsenoid.certhunter.domain.model.CertificateValidity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal object CertificateValidityClassifier {

    // 30 days matches the typical certificate renewal lead-time recommended by major CAs
    // and gives developers enough runway to rotate before services (Google Pay, Firebase, etc.) reject the cert.
    private const val EXPIRY_WARNING_DAYS = 30L

    fun classify(today: LocalDate, expiryDate: LocalDate): CertificateValidity {
        val daysLeft = ChronoUnit.DAYS.between(today, expiryDate)
        return when {
            daysLeft < 0 -> CertificateValidity.Expired
            daysLeft <= EXPIRY_WARNING_DAYS -> CertificateValidity.ExpiringSoon(daysLeft)
            else -> CertificateValidity.Valid
        }
    }
}
