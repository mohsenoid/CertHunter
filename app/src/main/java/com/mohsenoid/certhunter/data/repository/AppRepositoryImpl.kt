package com.mohsenoid.certhunter.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.klogx.DefaultKLogWriter
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AppRepositoryImpl(
    private val packageManager: PackageManager,
    private val dispatcherProvider: DispatcherProvider,
) : AppRepository {

    private val logger = object : DefaultKLogWriter {
        override val tag: String = "AppRepositoryImpl"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getInstalledApps(): List<AppItem> = withContext(dispatcherProvider.io) {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        packages.map {
            val flags = it.applicationInfo?.flags ?: 0
            AppItem(
                name = it.applicationInfo?.loadLabel(packageManager).toString(),
                packageName = it.packageName,
                isSystemApp = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun getAppItem(packageName: String): AppItem? =
        withContext(dispatcherProvider.io) {
            runCatching {
                val info = packageManager.getApplicationInfo(packageName, 0)
                val flags = info.flags
                AppItem(
                    name = info.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    isSystemApp = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                )
            }.getOrNull()
        }

    override suspend fun getCertificateDetails(packageName: String): AppCertificateDetails? =
        withContext(dispatcherProvider.io) {
            try {
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

                if (signatures.isNullOrEmpty()) return@withContext null

                val rawBytes = signatures[0].toByteArray()
                val certFactory = CertificateFactory.getInstance("X509")
                val x509Cert =
                    certFactory.generateCertificate(ByteArrayInputStream(rawBytes)) as X509Certificate

                AppCertificateDetails(
                    sha256 = hashBytes(rawBytes, "SHA-256"),
                    sha1 = hashBytes(rawBytes, "SHA-1"),
                    owner = x509Cert.subjectX500Principal.name,
                    issuer = x509Cert.issuerX500Principal.name,
                    serialNumber = x509Cert.serialNumber.toString(16).uppercase(),
                    validFrom = x509Cert.notBefore.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter),
                    validUntil = x509Cert.notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter),
                )
            } catch (e: Exception) {
                logger.e("Failed to get certificate for $packageName", throwable = e)
                null
            }
        }

    private fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
