@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mohsenoid.certhunter.ui.detail

import app.cash.turbine.test
import com.github.michaelbull.result.Ok
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateValidity
import com.mohsenoid.certhunter.domain.model.ShareCertificateLabels
import com.mohsenoid.certhunter.domain.model.toShareText
import com.mohsenoid.certhunter.fake.FakeAppRepository
import com.mohsenoid.certhunter.fake.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAppRepository

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

    private val sampleDetails = AppDetails(
        item = AppItem("CertHunter", "com.mohsenoid.certhunter", false),
        certificates = listOf(
            AppCertificateDetails(
                sha256 = "A1:B2:C3:D4",
                sha1 = "A1:B2",
                owner = "CN=Example",
                issuer = "CN=Example CA",
                serialNumber = "1",
                validFrom = "2023-01-01",
                validUntil = "2033-01-01",
                validity = CertificateValidity.Valid,
            ),
        ),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAppRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(packageName: String) = AppDetailViewModel(
        packageName = packageName,
        repository = fakeRepository,
        dispatcherProvider = TestDispatcherProvider(testDispatcher),
    )

    @Test
    fun `given loaded details when ShareCertificate action then Share event has toShareText payload`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.detailsResult = Ok(sampleDetails)
            val viewModel = createViewModel(sampleDetails.item.packageName)
            advanceUntilIdle()

            viewModel.events.test {
                // when
                viewModel.onAction(AppDetailAction.ShareCertificate(labels))
                advanceUntilIdle()

                // then
                val event = awaitItem()
                assertIs<AppDetailEvent.Share>(event)
                assertEquals(sampleDetails.toShareText(labels), event.text)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
