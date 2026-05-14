package com.mohsenoid.certhunter.domain.model

fun AppDetails.toShareText(labels: ShareCertificateLabels): String {
    val labelWidth = listOf(
        labels.sha256,
        labels.sha1,
        labels.owner,
        labels.issuer,
        labels.serial,
        labels.validRange,
        labels.status,
    ).maxOf { it.length } + ": ".length

    fun row(label: String, value: String): String =
        "$label:".padEnd(labelWidth) + value

    fun statusText(validity: CertificateValidity): String = when (validity) {
        is CertificateValidity.Valid -> labels.markerValid
        is CertificateValidity.Expired -> labels.markerExpired
        is CertificateValidity.ExpiringSoon ->
            labels.markerExpiringSoon.format(validity.daysLeft)
    }

    fun signerBlock(cert: AppCertificateDetails): String = buildString {
        appendLine(row(labels.sha256, cert.sha256))
        appendLine(row(labels.sha1, cert.sha1))
        appendLine(row(labels.owner, cert.owner))
        appendLine(row(labels.issuer, cert.issuer))
        appendLine(row(labels.serial, cert.serialNumber))
        appendLine(row(labels.validRange, "${cert.validFrom} → ${cert.validUntil}"))
        append(row(labels.status, statusText(cert.validity)))
    }

    return buildString {
        appendLine(item.name)
        appendLine(item.packageName)
        appendLine()
        val multiSigner = certificates.size > 1
        certificates.forEachIndexed { index, cert ->
            if (index > 0) {
                appendLine()
                appendLine()
            }
            if (multiSigner) {
                appendLine(labels.signerHeader.format(index + 1))
            }
            append(signerBlock(cert))
        }
    }
}
