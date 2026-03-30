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
) : AppRepository {

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
                    getCertificateDetails(packageName).mapError { error ->
                        when (error) {
                            is CertificateError.NotFound -> AppDetailsError.CertificateNotFound
                            is CertificateError.ParseError -> AppDetailsError.CertificateParseFailed(error.cause)
                        }
                    }.map { certificate ->
                        AppDetails(item = item, certificate = certificate)
                    }
                }
        }

    private fun getCertificateDetails(packageName: String): Result<AppCertificateDetails, CertificateError> =
        runCatching {
            logger.d("Getting certificate details for $packageName")
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val pkgInfo = packageManager.getPackageInfo(packageName, flags)

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
                    ?: pkgInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures
            }

            if (signatures.isNullOrEmpty()) throw NoSuchElementException("No signatures for $packageName")

            val rawBytes = signatures[0].toByteArray()
            val certFactory = CertificateFactory.getInstance("X509")
            val x509Cert =
                certFactory.generateCertificate(ByteArrayInputStream(rawBytes)) as X509Certificate

            val expiryDate = x509Cert.notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
            val validity = when {
                daysLeft < 0 -> CertificateValidity.Expired
                daysLeft <= 30 -> CertificateValidity.ExpiringSoon(daysLeft)
                else -> CertificateValidity.Valid
            }

            AppCertificateDetails(
                sha256 = hashBytes(rawBytes, "SHA-256"),
                sha1 = hashBytes(rawBytes, "SHA-1"),
                owner = x509Cert.subjectX500Principal.name,
                issuer = x509Cert.issuerX500Principal.name,
                serialNumber = x509Cert.serialNumber.toString(16).uppercase(),
                validFrom = x509Cert.notBefore.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter),
                validUntil = expiryDate.format(dateFormatter),
                validity = validity,
            )
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

    private fun ApplicationInfo.toAppItem(packageName: String, firstInstallTime: Long): AppItem = AppItem(
        name = loadLabel(packageManager).toString(),
        packageName = packageName,
        isSystemApp = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
        firstInstallTime = firstInstallTime,
    )

    private fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
