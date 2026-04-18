package com.mohsenoid.certhunter.data.repository

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignerSelectorTest {

    @Test
    fun `given single signer with no rotation when select then one current cert and empty history`() {
        // given
        val cert = byteArrayOf(1, 2, 3)

        // when
        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(cert),
            historyBytes = listOf(cert), // history contains only current = no rotation
        )

        // then
        assertEquals(1, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `given single signer with rotation when select then current and historical are separated`() {
        // given
        val oldCert = byteArrayOf(10, 20, 30)
        val newCert = byteArrayOf(40, 50, 60)

        // when
        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(newCert),
            historyBytes = listOf(oldCert, newCert), // oldest first, current last
        )

        // then
        assertEquals(1, current.size)
        assertEquals(1, historical.size)
        assertEquals(newCert, current[0])
        assertEquals(oldCert, historical[0])
    }

    @Test
    fun `given multiple current signers when select then all returned as current with empty history`() {
        // given
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)

        // when
        val (current, historical) = SignerSelector.select(
            isMultiSigned = true,
            currentSignerBytes = listOf(cert1, cert2),
            historyBytes = emptyList(),
        )

        // then
        assertEquals(2, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `given multi-signed app with history when select then history is ignored`() {
        // given
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)
        val oldCert = byteArrayOf(99)

        // when
        val (current, historical) = SignerSelector.select(
            isMultiSigned = true,
            currentSignerBytes = listOf(cert1, cert2),
            historyBytes = listOf(oldCert, cert1),
        )

        // then
        assertEquals(2, current.size)
        assertTrue(historical.isEmpty())
    }

    @Test
    fun `given rotation chain with multiple historical entries when select then all historical entries preserved`() {
        // given
        val cert1 = byteArrayOf(1)
        val cert2 = byteArrayOf(2)
        val cert3 = byteArrayOf(3)

        // when
        val (current, historical) = SignerSelector.select(
            isMultiSigned = false,
            currentSignerBytes = listOf(cert3),
            historyBytes = listOf(cert1, cert2, cert3),
        )

        // then
        assertEquals(1, current.size)
        assertEquals(2, historical.size)
        assertEquals(cert1, historical[0])
        assertEquals(cert2, historical[1])
    }
}
