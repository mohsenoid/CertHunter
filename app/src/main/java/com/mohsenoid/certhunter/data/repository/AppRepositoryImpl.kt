package com.mohsenoid.certhunter.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateError
import com.mohsenoid.certhunter.domain.model.CertificateValidity
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class AppRepositoryImpl(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
    private val sdkVersion: Int = Build.VERSION.SDK_INT,
) : AppRepository {

    companion object {
        private const val EXPIRY_WARNING_DAYS = 30
        private const val HEX_RADIX = 16
    }

    private val logger = object : DefaultKLogWriter {
        override val tag: String = "AppRepositoryImpl"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getInstalledApps(): List<AppItem> = withContext(dispatcherProvider.io) {
        logger.d("Getting installed apps")
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        packages
            .mapNotNull { pkgInfo -> pkgInfo.applicationInfo?.toAppItem(pkgInfo.packageName, pkgInfo.firstInstallTime) }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun getAppDetails(packageName: String): Result<AppDetails, AppDetailsError> =
        withContext(dispatcherProvider.io) {
            logger.d("Getting app details for $packageName")
            runCatching {
                val firstInstallTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                packageManager.getApplicationInfo(packageName, 0).toAppItem(packageName, firstInstallTime)
            }.mapError { AppDetailsError.ItemLoadFailed(it) }
                .andThen { item ->
                    getAllCertificateDetails(packageName).mapError { error ->
                        when (error) {
                            is CertificateError.NotFound -> AppDetailsError.CertificateNotFound
                            is CertificateError.ParseError -> AppDetailsError.CertificateParseFailed(error.cause)
                        }
                    }.map { (certificates, historicalCertificates) ->
                        AppDetails(
                            item = item,
                            certificates = certificates,
                            historicalCertificates = historicalCertificates,
                        )
                    }
                }
        }

    /**
     * Returns Pair(activeCerts, historicalCerts).
     * On API 28+ this correctly distinguishes multi-signers from rotation history.
     * On API < 28 all signatures are treated as active (no rotation API available).
     */
    private fun getAllCertificateDetails(
        packageName: String,
    ): Result<Pair<List<AppCertificateDetails>, List<AppCertificateDetails>>, CertificateError> =
        runCatching {
            logger.d("Getting certificate details for $packageName")

            val (activeBytes, historicalBytes) = getSignerBytes(packageName)

            if (activeBytes.isEmpty()) throw NoSuchElementException("No signatures for $packageName")

            val active = activeBytes.map { parseCertificate(it) }
            val historical = historicalBytes.map { parseCertificate(it) }
            Pair(active, historical)
        }.mapError { e ->
            when (e) {
                is NoSuchElementException -> {
                    logger.w("No certificate found for $packageName", throwable = e)
                    CertificateError.NotFound
                }

                else -> {
                    logger.e("Failed to get certificate for $packageName", throwable = e)
                    CertificateError.ParseError(e)
                }
            }
        }

    private fun getSignerBytes(packageName: String): Pair<List<ByteArray>, List<ByteArray>> {
        return if (sdkVersion >= Build.VERSION_CODES.P) {
            val pkgInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val signingInfo = pkgInfo.signingInfo
            val currentBytes = signingInfo?.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
            val historyBytes = signingInfo?.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
            SignerSelector.select(
                isMultiSigned = signingInfo?.hasMultipleSigners() == true,
                currentSignerBytes = currentBytes,
                historyBytes = historyBytes,
            )
        } else {
            @Suppress("DEPRECATION")
            val pkgInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)

            @Suppress("DEPRECATION")
            val bytes = pkgInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            Pair(bytes, emptyList())
        }
    }

    private fun parseCertificate(rawBytes: ByteArray): AppCertificateDetails {
        val certFactory = CertificateFactory.getInstance("X509")
        val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(rawBytes)) as X509Certificate
        val expiryDate = x509Cert.notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
        val validity = when {
            daysLeft < 0 -> CertificateValidity.Expired
            daysLeft <= EXPIRY_WARNING_DAYS -> CertificateValidity.ExpiringSoon(daysLeft)
            else -> CertificateValidity.Valid
        }
        return AppCertificateDetails(
            sha256 = hashBytes(rawBytes, "SHA-256"),
            sha1 = hashBytes(rawBytes, "SHA-1"),
            owner = x509Cert.subjectX500Principal.name,
            issuer = x509Cert.issuerX500Principal.name,
            serialNumber = x509Cert.serialNumber.toString(HEX_RADIX).uppercase(),
            validFrom = x509Cert.notBefore.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(dateFormatter),
            validUntil = expiryDate.format(dateFormatter),
            validity = validity,
        )
    }

    private fun ApplicationInfo.toAppItem(packageName: String, firstInstallTime: Long): AppItem = AppItem(
        name = loadLabel(packageManager).toString(),
        packageName = packageName,
        isSystemApp = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
        firstInstallTime = firstInstallTime,
    )

    private fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        return md.digest(bytes).joinToString(":") { "%02X".format(it) }
    }
}
