package com.mohsenoid.certhunter.data.repository

import android.content.pm.PackageManager
import android.os.Build
import com.mohsenoid.certhunter.domain.model.CertificateDetails
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

fun getAppCertificateDetails(pm: PackageManager, packageName: String): CertificateDetails? {
    try {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val pkgInfo = pm.getPackageInfo(packageName, flags)

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.signingInfo?.apkContentsSigners ?: pkgInfo.signingInfo?.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures
        }

        if (signatures.isNullOrEmpty()) return null

        val rawBytes = signatures[0].toByteArray()
        val certFactory = CertificateFactory.getInstance("X509")
        val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(rawBytes)) as X509Certificate

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return CertificateDetails(
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
        return null
    }
}

private fun hashBytes(bytes: ByteArray, algorithm: String): String {
    val md = MessageDigest.getInstance(algorithm)
    val digest = md.digest(bytes)
    return digest.joinToString(":") { "%02X".format(it) }
}
