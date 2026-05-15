package com.mohsenoid.certhunter.data.repository

/**
 * Pure function that decides which certificate bytes represent the current active signers
 * vs historical (rotated) certificates.
 *
 * Android API 28+ exposes two arrays from SigningInfo:
 *  - apkContentsSigners  : all certs that sign the current APK content
 *  - signingCertificateHistory : full rotation lineage (oldest → newest, current is last)
 *
 * For a multi-signed APK, history is irrelevant; all entries in apkContentsSigners are current.
 * For a single-signer APK with rotation, the current signer is apkContentsSigners[0] and the
 * history is signingCertificateHistory without its last entry (which duplicates the current cert).
 */
internal object SignerSelector {
    /**
     * @param isMultiSigned     true when the APK is simultaneously signed by multiple certs
     * @param currentSignerBytes bytes of the certificates from apkContentsSigners
     * @param historyBytes       bytes of the full certificate lineage (oldest first, current last)
     * @return Pair of (activeCerts, historicalCerts)
     */
    fun select(
        isMultiSigned: Boolean,
        currentSignerBytes: List<ByteArray>,
        historyBytes: List<ByteArray>,
    ): Pair<List<ByteArray>, List<ByteArray>> = if (isMultiSigned) {
        Pair(currentSignerBytes, emptyList())
    } else {
        // Drop the last element from history because it is the current signer (already in currentSignerBytes).
        // dropLast(1) is safe on empty and size-1 lists — both yield an empty historical list.
        Pair(currentSignerBytes, historyBytes.dropLast(1))
    }
}
