package com.mohsenoid.certhunter.ui.detail

import com.mohsenoid.certhunter.domain.model.ShareCertificateLabels

sealed class AppDetailAction {
    data class ShareCertificate(val labels: ShareCertificateLabels) : AppDetailAction()
}
