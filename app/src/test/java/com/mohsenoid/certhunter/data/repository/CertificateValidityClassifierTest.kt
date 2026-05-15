package com.mohsenoid.certhunter.data.repository

import com.mohsenoid.certhunter.domain.model.CertificateValidity
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class CertificateValidityClassifierTest {

    private val expiryDate: LocalDate = LocalDate.of(2030, 6, 15)

    @Test
    fun `given expired yesterday when classify then Expired`() {
        val today = expiryDate.plusDays(1)

        val validity = CertificateValidityClassifier.classify(today, expiryDate)

        assertEquals(CertificateValidity.Expired, validity)
    }

    @Test
    fun `given expires today when classify then ExpiringSoon with zero days left`() {
        val today = expiryDate

        val validity = CertificateValidityClassifier.classify(today, expiryDate)

        assertEquals(CertificateValidity.ExpiringSoon(0), validity)
    }

    @Test
    fun `given expires in 30 days when classify then ExpiringSoon with 30 days left`() {
        val today = expiryDate.minusDays(30)

        val validity = CertificateValidityClassifier.classify(today, expiryDate)

        assertEquals(CertificateValidity.ExpiringSoon(30), validity)
    }

    @Test
    fun `given expires in 31 days when classify then Valid`() {
        val today = expiryDate.minusDays(31)

        val validity = CertificateValidityClassifier.classify(today, expiryDate)

        assertEquals(CertificateValidity.Valid, validity)
    }
}
