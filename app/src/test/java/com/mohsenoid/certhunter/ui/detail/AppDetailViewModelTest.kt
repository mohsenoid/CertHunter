@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.mohsenoid.certhunter.ui.detail

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
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

    @Test
    fun `given still loading when ShareCertificate action then no event is emitted`() =
        runTest(testDispatcher) {
            // given: load is scheduled but not yet advanced, so uiState.isLoading is still true.
            // Configure detailsResult so the load can complete cleanly after the assertion.
            fakeRepository.detailsResult = Ok(sampleDetails)
            val viewModel = createViewModel(sampleDetails.item.packageName)

            viewModel.events.test {
                // when: act before the load coroutine has been advanced
                viewModel.onAction(AppDetailAction.ShareCertificate(labels))

                // then
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given load error when ShareCertificate action then no event is emitted`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.detailsResult = Err(AppDetailsError.CertificateNotFound)
            val viewModel = createViewModel("com.mohsenoid.certhunter")
            advanceUntilIdle()

            viewModel.events.test {
                // when
                viewModel.onAction(AppDetailAction.ShareCertificate(labels))
                advanceUntilIdle()

                // then
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given loaded details with no certificates when ShareCertificate action then no event is emitted`() =
        runTest(testDispatcher) {
            // given
            fakeRepository.detailsResult = Ok(
                AppDetails(
                    item = AppItem("App", "com.app", false),
                    certificates = emptyList(),
                ),
            )
            val viewModel = createViewModel("com.app")
            advanceUntilIdle()

            viewModel.events.test {
                // when
                viewModel.onAction(AppDetailAction.ShareCertificate(labels))
                advanceUntilIdle()

                // then
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
