package com.mohsenoid.certhunter.data.repository

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignerSelectorTest {

    @Test
    fun `single signer with no history returns one current cert and empty history`() {
        val cert = byteArrayOf(1, 2, 3)

        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(cert),
            historyBytes = listOf(cert), // history contains only current = no rotation
        )

        assertEquals(1, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `single signer with rotation history separates current from historical`() {
        val oldCert = byteArrayOf(10, 20, 30)
        val newCert = byteArrayOf(40, 50, 60)

        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(newCert),
            historyBytes = listOf(oldCert, newCert), // oldest first, current last
        )

        assertEquals(1, current.size)
        assertEquals(1, historical.size)
        assertEquals(newCert, current[0])
        assertEquals(oldCert, historical[0])
    }

    @Test
    fun `multiple current signers returns all as current with empty history`() {
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)

        val (current, historical) = SignerSelector.select(
            isMultiSigned = true,
            currentSignerBytes = listOf(cert1, cert2),
            historyBytes = emptyList(),
        )

        assertEquals(2, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `multi-signed app ignores history even if present`() {
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)
        val oldCert = byteArrayOf(99)

        val (current, historical) = SignerSelector.select(
            isMultiSigned = true,
            currentSignerBytes = listOf(cert1, cert2),
            historyBytes = listOf(oldCert, cert1),
        )

        assertEquals(2, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `rotation chain with multiple historical entries`() {
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)
        val cert3 = byteArrayOf(3)

        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(cert3),
            historyBytes = listOf(cert1, cert2, cert3),
        )

        assertEquals(1, current.size)
        assertEquals(2, historical.size)
        assertEquals(cert1, historical[0])
        assertEquals(cert2, historical[1])
    }
}
