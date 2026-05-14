package com.mohsenoid.certhunter.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AppDetailsShareTextTest {

    private val labels = ShareCertificateLabels(
        sha256 = "SHA-256",
        sha1 = "SHA-1",
        owner = "Owner",
        issuer = "Issuer",
        serial = "Serial",
        validRange = "Valid",
        status = "Status",
        signerHeader = "--- Signer %1\$d ---",
        markerValid = "VALID",
        markerExpired = "EXPIRED",
        markerExpiringSoon = "EXPIRES IN %1\$d DAYS",
    )

    private val baseItem = AppItem(
        name = "CertHunter",
        packageName = "com.mohsenoid.certhunter",
        isSystemApp = false,
    )

    private val baseCert = AppCertificateDetails(
        sha256 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
        sha1 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
        owner = "CN=Example, O=Example Corp, C=US",
        issuer = "CN=Example CA, O=Example Corp, C=US",
        serialNumber = "123456789",
        validFrom = "2023-01-01",
        validUntil = "2033-01-01",
        validity = CertificateValidity.Valid,
    )

    @Test
    fun `given a valid single-signer app when toShareText then matches the canonical fixture`() {
        // given
        val details = AppDetails(item = baseItem, certificates = listOf(baseCert))

        // when
        val text = details.toShareText(labels)

        // then
        val expected = """
            CertHunter
            com.mohsenoid.certhunter

            SHA-256: A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
            SHA-1:   A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
            Owner:   CN=Example, O=Example Corp, C=US
            Issuer:  CN=Example CA, O=Example Corp, C=US
            Serial:  123456789
            Valid:   2023-01-01 → 2033-01-01
            Status:  VALID
        """.trimIndent()
        assertEquals(expected, text)
    }

    @Test
    fun `given an expired certificate when toShareText then status line reads EXPIRED`() {
        // given
        val expiredCert = baseCert.copy(
            validUntil = "2020-01-01",
            validity = CertificateValidity.Expired,
        )
        val details = AppDetails(item = baseItem, certificates = listOf(expiredCert))

        // when
        val text = details.toShareText(labels)

        // then
        assertEquals("Status:  EXPIRED", text.lines().last())
    }

    @Test
    fun `given an expiring-soon certificate when toShareText then status line reads EXPIRES IN N DAYS`() {
        // given
        val expiringCert = baseCert.copy(validity = CertificateValidity.ExpiringSoon(daysLeft = 12))
        val details = AppDetails(item = baseItem, certificates = listOf(expiringCert))

        // when
        val text = details.toShareText(labels)

        // then
        assertEquals("Status:  EXPIRES IN 12 DAYS", text.lines().last())
    }

    @Test
    fun `given a multi-signer app when toShareText then both signers appear with signer headers and history is excluded`() {
        // given
        val secondCert = baseCert.copy(
            sha256 = "B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5",
            sha1 = "B2:C3:D4:E5:F6:A1:B2:C3:D4:E5",
            owner = "CN=Other, O=Example Corp, C=US",
            serialNumber = "987654321",
            validity = CertificateValidity.Expired,
        )
        val historicalCert = baseCert.copy(
            sha256 = "FF:FF:FF:FF",
            sha1 = "FF:FF:FF",
            owner = "CN=Rotated Out, O=Example Corp, C=US",
        )
        val details = AppDetails(
            item = baseItem,
            certificates = listOf(baseCert, secondCert),
            historicalCertificates = listOf(historicalCert),
        )

        // when
        val text = details.toShareText(labels)

        // then
        val expected = """
            CertHunter
            com.mohsenoid.certhunter

            --- Signer 1 ---
            SHA-256: A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
            SHA-1:   A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
            Owner:   CN=Example, O=Example Corp, C=US
            Issuer:  CN=Example CA, O=Example Corp, C=US
            Serial:  123456789
            Valid:   2023-01-01 → 2033-01-01
            Status:  VALID

            --- Signer 2 ---
            SHA-256: B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5
            SHA-1:   B2:C3:D4:E5:F6:A1:B2:C3:D4:E5
            Owner:   CN=Other, O=Example Corp, C=US
            Issuer:  CN=Example CA, O=Example Corp, C=US
            Serial:  987654321
            Valid:   2023-01-01 → 2033-01-01
            Status:  EXPIRED
        """.trimIndent()
        assertEquals(expected, text)
        assertFalse(text.contains("Rotated Out"), "historical certificate must not appear in shared text")
        assertFalse(text.contains("FF:FF:FF"), "historical fingerprint must not appear in shared text")
    }
}
