package com.mohsenoid.certhunter.ui.detail.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.domain.model.CertificateValidity
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.theme.OnWarning
import com.mohsenoid.certhunter.ui.theme.OnWarningContainerDark
import com.mohsenoid.certhunter.ui.theme.Warning
import com.mohsenoid.certhunter.ui.theme.WarningContainerDark
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun CertificateValidityBadge(validity: CertificateValidity) {
    if (validity == CertificateValidity.Valid) return

    val isDark = isSystemInDarkTheme()
    val (backgroundColor, contentColor, label) = when (validity) {
        is CertificateValidity.Expired -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.app_detail_certificate_status_expired),
        )

        is CertificateValidity.ExpiringSoon -> Triple(
            if (isDark) WarningContainerDark else Warning,
            if (isDark) OnWarningContainerDark else OnWarning,
            stringResource(R.string.app_detail_certificate_status_expiring_soon, validity.daysLeft),
        )

        CertificateValidity.Valid -> return
    }

    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.3f), MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@ComponentPreviews
@Composable
private fun CertificateValidityBadgeExpiredPreview() {
    CertHunterTheme {
        CertificateValidityBadge(validity = CertificateValidity.Expired)
    }
}

@ComponentPreviews
@Composable
private fun CertificateValidityBadgeExpiringSoonPreview() {
    CertHunterTheme {
        CertificateValidityBadge(validity = CertificateValidity.ExpiringSoon(daysLeft = 15))
    }
}
