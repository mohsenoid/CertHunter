@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mohsenoid.certhunter.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import com.mohsenoid.certhunter.data.repository.TestCertificates.CERT_1_DER
import com.mohsenoid.certhunter.data.repository.TestCertificates.CERT_2_DER
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.CertificateValidity
import com.mohsenoid.certhunter.fake.TestDispatcherProvider
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for AppRepositoryImpl.
 *
 * Strategy:
 * - PackageManager is abstract → strict Mockk mock, stubs its methods with every {}
 * - Android data classes (PackageInfo, ApplicationInfo) have public Java FIELDS (not methods),
 *   so Mockk's every {} doesn't capture them. We use relaxed mocks and set fields directly.
 * - SigningInfo and Signature expose methods → every {} works fine on relaxed mocks.
 * - loadLabel() is a method → stubbed with every {}; no need to set nonLocalizedLabel.
 */
@ExtendWith(MockKExtension::class)
class AppRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val packageName = "com.test.app"

    // Mocks created in @BeforeEach so MockKExtension is already active
    private lateinit var mockPm: PackageManager
    private lateinit var mockPkgInfoBasic: PackageInfo
    private lateinit var mockAppInfo: ApplicationInfo
    private lateinit var sig1: Signature
    private lateinit var sig2: Signature

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockPm = mockk()
        mockPkgInfoBasic = mockk(relaxed = true)
        mockAppInfo = mockk(relaxed = true)
        sig1 = mockk(relaxed = true)
        sig2 = mockk(relaxed = true)

        every { sig1.toByteArray() } returns CERT_1_DER
        every { sig2.toByteArray() } returns CERT_2_DER

        // firstInstallTime is a Java public field — set directly on the relaxed mock
        mockPkgInfoBasic.firstInstallTime = 1_000L

        // loadLabel() is a method — stub it via every {}
        every { mockAppInfo.loadLabel(any()) } returns "Test App"
        // flags is a Java public field — set directly
        mockAppInfo.flags = 0

        every { mockPm.getPackageInfo(packageName, 0) } returns mockPkgInfoBasic
        every { mockPm.getApplicationInfo(packageName, 0) } returns mockAppInfo
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repositoryAt(
        sdkVersion: Int,
        clock: Clock = Clock.systemDefaultZone(),
    ) = AppRepositoryImpl(
        packageManager = mockPm,
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
        clock = clock,
        sdkVersion = sdkVersion,
    )

    // ─── API 28+ (signingInfo) ────────────────────────────────────────────────

    @Nested
    inner class Api28 {

        private lateinit var mockPkgInfoSigning: PackageInfo
        private lateinit var mockSigningInfo: SigningInfo

        @BeforeEach
        fun setUpSigning() {
            mockSigningInfo = mockk(relaxed = true)
            mockPkgInfoSigning = mockk(relaxed = true)
            // signingInfo is a Java public field — set directly
            mockPkgInfoSigning.signingInfo = mockSigningInfo

            every {
                mockPm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } returns mockPkgInfoSigning
        }

        @Test
        fun `given single signer without rotation when getAppDetails then one certificate and empty history`() =
            runTest(testDispatcher) {
                // given
                every { mockSigningInfo.hasMultipleSigners() } returns false
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1)
                every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig1)

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(1, details.certificates.size)
                assertTrue(details.historicalCertificates.isEmpty())
            }

        @Test
        fun `given single signer with rotation when getAppDetails then current cert separated from history`() =
            runTest(testDispatcher) {
                // given — history: [oldCert, currentCert], oldest first, current last
                every { mockSigningInfo.hasMultipleSigners() } returns false
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1)
                every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig2, sig1)

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(1, details.certificates.size)
                assertEquals(1, details.historicalCertificates.size)
            }

        @Test
        fun `given multiple current signers when getAppDetails then all returned as active with no history`() =
            runTest(testDispatcher) {
                // given
                every { mockSigningInfo.hasMultipleSigners() } returns true
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1, sig2)
                every { mockSigningInfo.signingCertificateHistory } returns emptyArray()

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(2, details.certificates.size)
                assertTrue(details.historicalCertificates.isEmpty())
            }

        @Test
        fun `given multi-signed app with history when getAppDetails then history is ignored`() =
            runTest(testDispatcher) {
                // given
                every { mockSigningInfo.hasMultipleSigners() } returns true
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1, sig2)
                every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig2, sig1)

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(2, details.certificates.size)
                assertTrue(details.historicalCertificates.isEmpty())
            }

        @Test
        fun `given null signingInfo when getAppDetails then certificate not found error`() =
            runTest(testDispatcher) {
                // given
                mockPkgInfoSigning.signingInfo = null

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertIs<AppDetailsError.CertificateNotFound>(result.getError())
            }

        @Test
        fun `given empty apkContentsSigners when getAppDetails then certificate not found error`() =
            runTest(testDispatcher) {
                // given
                every { mockSigningInfo.hasMultipleSigners() } returns false
                every { mockSigningInfo.apkContentsSigners } returns emptyArray()
                every { mockSigningInfo.signingCertificateHistory } returns emptyArray()

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertIs<AppDetailsError.CertificateNotFound>(result.getError())
            }

        @Test
        fun `given valid certificate bytes when getAppDetails then fingerprints and fields are populated`() =
            runTest(testDispatcher) {
                // given
                every { mockSigningInfo.hasMultipleSigners() } returns false
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1)
                every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig1)

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val cert = result.getOrElse { fail("Expected Ok but got: $it") }.certificates[0]
                assertTrue(cert.sha256.contains(":"))
                assertTrue(cert.sha1.contains(":"))
                assertTrue(cert.owner.isNotBlank())
                assertTrue(cert.serialNumber.isNotBlank())
                assertTrue(cert.validFrom.isNotBlank())
                assertTrue(cert.validUntil.isNotBlank())
            }
    }

    // ─── Legacy API (< 28) ───────────────────────────────────────────────────

    @Nested
    inner class LegacyApi {

        private lateinit var mockPkgInfoLegacy: PackageInfo

        @BeforeEach
        fun setUpLegacy() {
            mockPkgInfoLegacy = mockk(relaxed = true)

            @Suppress("DEPRECATION")
            every {
                mockPm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            } returns mockPkgInfoLegacy
        }

        @Test
        fun `given single legacy signature when getAppDetails then one active cert and no history`() =
            runTest(testDispatcher) {
                // given
                @Suppress("DEPRECATION")
                mockPkgInfoLegacy.signatures = arrayOf(sig1)

                // when
                val result = repositoryAt(Build.VERSION_CODES.N).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(1, details.certificates.size)
                assertTrue(details.historicalCertificates.isEmpty())
            }

        @Test
        fun `given multiple legacy signatures when getAppDetails then all are active certs`() =
            runTest(testDispatcher) {
                // given
                @Suppress("DEPRECATION")
                mockPkgInfoLegacy.signatures = arrayOf(sig1, sig2)

                // when
                val result = repositoryAt(Build.VERSION_CODES.N).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                val details = result.getOrElse { fail("Expected Ok but got: $it") }
                assertEquals(2, details.certificates.size)
                assertTrue(details.historicalCertificates.isEmpty())
            }

        @Test
        fun `given null legacy signatures when getAppDetails then certificate not found error`() =
            runTest(testDispatcher) {
                // given
                @Suppress("DEPRECATION")
                mockPkgInfoLegacy.signatures = null

                // when
                val result = repositoryAt(Build.VERSION_CODES.N).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertIs<AppDetailsError.CertificateNotFound>(result.getError())
            }
    }

    // ─── Error handling ──────────────────────────────────────────────────────

    @Nested
    inner class ErrorHandling {

        @Test
        fun `given package not found when getAppDetails then item load failed error`() =
            runTest(testDispatcher) {
                // given
                every { mockPm.getPackageInfo(packageName, 0) } throws
                    PackageManager.NameNotFoundException()

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertIs<AppDetailsError.ItemLoadFailed>(result.getError())
            }

        @Test
        fun `given invalid certificate bytes when getAppDetails then certificate parse failed error`() =
            runTest(testDispatcher) {
                // given
                val mockPkgInfoSigning = mockk<PackageInfo>(relaxed = true)
                val mockSigningInfo = mockk<SigningInfo>(relaxed = true)
                val badSig = mockk<Signature>(relaxed = true)

                mockPkgInfoSigning.signingInfo = mockSigningInfo
                every {
                    mockPm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                } returns mockPkgInfoSigning
                every { mockSigningInfo.hasMultipleSigners() } returns false
                every { mockSigningInfo.apkContentsSigners } returns arrayOf(badSig)
                every { mockSigningInfo.signingCertificateHistory } returns arrayOf(badSig)
                every { badSig.toByteArray() } returns byteArrayOf(1, 2, 3) // not valid DER

                // when
                val result = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertIs<AppDetailsError.CertificateParseFailed>(result.getError())
            }
    }

    // ─── App item mapping ────────────────────────────────────────────────────

    @Nested
    inner class AppItemMapping {

        private lateinit var mockPkgInfoSigning: PackageInfo
        private lateinit var mockSigningInfo: SigningInfo

        @BeforeEach
        fun setUpSigning() {
            mockSigningInfo = mockk(relaxed = true)
            mockPkgInfoSigning = mockk(relaxed = true)
            mockPkgInfoSigning.signingInfo = mockSigningInfo

            every {
                mockPm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } returns mockPkgInfoSigning
            every { mockSigningInfo.hasMultipleSigners() } returns false
            every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1)
            every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig1)
        }

        @Test
        fun `given app info with label when getAppDetails then item name matches loadLabel`() =
            runTest(testDispatcher) {
                // given
                every { mockAppInfo.loadLabel(any()) } returns "My Cool App"

                // when
                val details = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertEquals("My Cool App", details.getOrElse { fail("$it") }.item.name)
            }

        @Test
        fun `given app info with FLAG_SYSTEM when getAppDetails then item is marked as system app`() =
            runTest(testDispatcher) {
                // given
                mockAppInfo.flags = ApplicationInfo.FLAG_SYSTEM

                // when
                val details = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertTrue(details.getOrElse { fail("$it") }.item.isSystemApp)
            }

        @Test
        fun `given app info without system flag when getAppDetails then item is not a system app`() =
            runTest(testDispatcher) {
                // given
                mockAppInfo.flags = 0

                // when
                val details = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertFalse(details.getOrElse { fail("$it") }.item.isSystemApp)
            }

        @Test
        fun `given package info with firstInstallTime when getAppDetails then item preserves timestamp`() =
            runTest(testDispatcher) {
                // given
                mockPkgInfoBasic.firstInstallTime = 42_000L

                // when
                val details = repositoryAt(Build.VERSION_CODES.P).getAppDetails(packageName)
                advanceUntilIdle()

                // then
                assertEquals(42_000L, details.getOrElse { fail("$it") }.item.firstInstallTime)
            }
    }

    // ─── Validity classification boundaries (with a fixed clock) ─────────────

    @Nested
    inner class ValidityBoundaries {

        // CERT_1_DER notAfter encoded as UTCTime "360415145839Z" -> 2036-04-15 in UTC.
        private val cert1ExpiryUtc: LocalDate = LocalDate.of(2036, 4, 15)
        private val utc: ZoneId = ZoneId.of("UTC")

        private lateinit var mockPkgInfoSigning: PackageInfo
        private lateinit var mockSigningInfo: SigningInfo

        @BeforeEach
        fun setUpSigning() {
            mockSigningInfo = mockk(relaxed = true)
            mockPkgInfoSigning = mockk(relaxed = true)
            mockPkgInfoSigning.signingInfo = mockSigningInfo

            every {
                mockPm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } returns mockPkgInfoSigning
            every { mockSigningInfo.hasMultipleSigners() } returns false
            every { mockSigningInfo.apkContentsSigners } returns arrayOf(sig1)
            every { mockSigningInfo.signingCertificateHistory } returns arrayOf(sig1)
        }

        private fun clockAt(date: LocalDate): Clock =
            Clock.fixed(date.atStartOfDay(utc).toInstant(), utc)

        @Test
        fun `given today is one day after notAfter when getAppDetails then validity is Expired`() =
            runTest(testDispatcher) {
                val clock = clockAt(cert1ExpiryUtc.plusDays(1))

                val result = repositoryAt(Build.VERSION_CODES.P, clock).getAppDetails(packageName)
                advanceUntilIdle()

                val cert = result.getOrElse { fail("Expected Ok but got: $it") }.certificates[0]
                assertEquals(CertificateValidity.Expired, cert.validity)
            }

        @Test
        fun `given today equals notAfter when getAppDetails then validity is ExpiringSoon with zero days left`() =
            runTest(testDispatcher) {
                val clock = clockAt(cert1ExpiryUtc)

                val result = repositoryAt(Build.VERSION_CODES.P, clock).getAppDetails(packageName)
                advanceUntilIdle()

                val cert = result.getOrElse { fail("Expected Ok but got: $it") }.certificates[0]
                assertEquals(CertificateValidity.ExpiringSoon(0), cert.validity)
            }

        @Test
        fun `given today is 30 days before notAfter when getAppDetails then validity is ExpiringSoon with 30 days left`() =
            runTest(testDispatcher) {
                val clock = clockAt(cert1ExpiryUtc.minusDays(30))

                val result = repositoryAt(Build.VERSION_CODES.P, clock).getAppDetails(packageName)
                advanceUntilIdle()

                val cert = result.getOrElse { fail("Expected Ok but got: $it") }.certificates[0]
                assertEquals(CertificateValidity.ExpiringSoon(30), cert.validity)
            }

        @Test
        fun `given today is 31 days before notAfter when getAppDetails then validity is Valid`() =
            runTest(testDispatcher) {
                val clock = clockAt(cert1ExpiryUtc.minusDays(31))

                val result = repositoryAt(Build.VERSION_CODES.P, clock).getAppDetails(packageName)
                advanceUntilIdle()

                val cert = result.getOrElse { fail("Expected Ok but got: $it") }.certificates[0]
                assertEquals(CertificateValidity.Valid, cert.validity)
            }
    }
}
