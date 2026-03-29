package com.mohsenoid.certhunter.data.repository

import android.content.pm.PackageManager
import android.os.Build
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails
import com.mohsenoid.certhunter.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

class AppRepositoryImpl(private val packageManager: PackageManager) : AppRepository {

    override suspend fun getInstalledApps(): List<AppItem> = withContext(Dispatchers.IO) {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        packages.map {
            AppItem(
                name = it.applicationInfo?.loadLabel(packageManager).toString(),
                packageName = it.packageName,
                icon = it.applicationInfo?.loadIcon(packageManager)
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun getCertificateDetails(packageName: String): CertificateDetails? =
        withContext(Dispatchers.IO) {
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

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                CertificateDetails(
                    sha256 = hashBytes(rawBytes, "SHA-256"),
                    sha1 = hashBytes(rawBytes, "SHA-1"),
                    owner = x509Cert.subjectDN.name,
                    issuer = x509Cert.issuerDN.name,
                    serialNumber = x509Cert.serialNumber.toString(16).uppercase(),
                    validFrom = dateFormat.format(x509Cert.notBefore),
                    validUntil = dateFormat.format(x509Cert.notAfter)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun hashBytes(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
