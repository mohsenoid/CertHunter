package com.mohsenoid.certhunter.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
import com.mohsenoid.certhunter.domain.model.AppDetailsError
import com.mohsenoid.certhunter.domain.model.CertificateValidity
import com.mohsenoid.certhunter.ui.detail.widget.AppDetailRow
import com.mohsenoid.certhunter.ui.detail.widget.CertificateValidityBadge
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun AppDetailScreen(
    uiState: AppDetailUiModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_detail_close)) }
        },
        title = {
            Column {
                Text(text = uiState.appName, fontWeight = FontWeight.Bold)
                if (!uiState.isLoading) {
                    Text(
                        text = stringResource(R.string.app_detail_tap_to_copy),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        text = { AppDetailContent(uiState) },
    )
}

@Composable
private fun AppDetailContent(uiState: AppDetailUiModel) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        Text(
            stringResource(
                when (uiState.error) {
                    is AppDetailsError.ItemLoadFailed -> R.string.app_detail_package_load_error
                    AppDetailsError.CertificateNotFound -> R.string.app_detail_no_signature_found
                    is AppDetailsError.CertificateParseFailed -> R.string.app_detail_certificate_error
                }
            )
        )
    } else if (uiState.certificates.isEmpty()) {
        Text(stringResource(R.string.app_detail_no_signature_found))
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppDetailRow(stringResource(R.string.app_detail_label_package_name), uiState.packageName)
            AppDetailRow(
                stringResource(R.string.app_detail_label_system_app),
                if (uiState.isSystemApp) {
                    stringResource(R.string.app_detail_system_app_yes)
                } else {
                    stringResource(R.string.app_detail_system_app_no)
                },
            )

            uiState.certificates.forEachIndexed { index, cert ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (uiState.certificates.size > 1) {
                    Text(
                        text = stringResource(R.string.app_detail_signer_label, index + 1),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                CertificateBlock(cert)
            }

            if (uiState.historicalCertificates.isNotEmpty()) {
                HistoricalCertificatesSection(uiState.historicalCertificates)
            }
        }
    }
}

@Composable
private fun CertificateBlock(cert: AppCertificateDetails) {
    AppDetailRow(stringResource(R.string.app_detail_label_sha256), cert.sha256)
    AppDetailRow(stringResource(R.string.app_detail_label_sha1), cert.sha1)
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    AppDetailRow(stringResource(R.string.app_detail_label_owner), cert.owner)
    AppDetailRow(stringResource(R.string.app_detail_label_issuer), cert.issuer)
    AppDetailRow(stringResource(R.string.app_detail_label_serial), cert.serialNumber)
    AppDetailRow(stringResource(R.string.app_detail_label_valid_from), cert.validFrom)
    AppDetailRow(stringResource(R.string.app_detail_label_valid_until), cert.validUntil)
    CertificateValidityBadge(cert.validity)
}

@Composable
private fun HistoricalCertificatesSection(certs: List<AppCertificateDetails>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.padding(horizontal = 0.dp),
    ) {
        Text(
            text = stringResource(
                if (expanded) R.string.app_detail_history_hide else R.string.app_detail_history_show,
                certs.size,
            ),
            fontSize = 12.sp,
        )
    }
    if (expanded) {
        certs.forEachIndexed { index, cert ->
            Text(
                text = stringResource(R.string.app_detail_history_entry_label, index + 1),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CertificateBlock(cert)
            if (index < certs.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

private val previewCert = AppCertificateDetails(
    sha256 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
    sha1 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
    owner = "CN=Example, O=Example Corp, C=US",
    issuer = "CN=Example CA, O=Example Corp, C=US",
    serialNumber = "123456789",
    validFrom = "2023-01-01",
    validUntil = "2033-01-01",
    validity = CertificateValidity.Valid,
)

private val previewOldCert = previewCert.copy(
    sha256 = "B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5",
    sha1 = "B2:C3:D4:E5:F6:A1:B2:C3:D4:E5",
    owner = "CN=Old Cert, O=Example Corp, C=US",
    validFrom = "2018-01-01",
    validUntil = "2023-01-01",
    validity = CertificateValidity.Expired,
)

@ComponentPreviews
@Composable
private fun AppDetailScreenPreview() {
    CertHunterTheme {
        AppDetailScreen(
            uiState = AppDetailUiModel(
                isLoading = false,
                packageName = "com.mohsenoid.certhunter",
                appName = "CertHunter",
                isSystemApp = false,
                certificates = listOf(previewCert),
            ),
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppDetailScreenMultiSignerPreview() {
    CertHunterTheme {
        AppDetailScreen(
            uiState = AppDetailUiModel(
                isLoading = false,
                packageName = "com.mohsenoid.certhunter",
                appName = "CertHunter",
                isSystemApp = false,
                certificates = listOf(previewCert, previewOldCert),
            ),
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppDetailScreenWithHistoryPreview() {
    CertHunterTheme {
        AppDetailScreen(
            uiState = AppDetailUiModel(
                isLoading = false,
                packageName = "com.mohsenoid.certhunter",
                appName = "CertHunter",
                isSystemApp = false,
                certificates = listOf(previewCert),
                historicalCertificates = listOf(previewOldCert),
            ),
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppDetailScreenLoadingPreview() {
    CertHunterTheme {
        AppDetailScreen(
            uiState = AppDetailUiModel(isLoading = true),
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun AppDetailScreenNoDetailsPreview() {
    CertHunterTheme {
        AppDetailScreen(
            uiState = AppDetailUiModel(isLoading = false, appName = "CertHunter"),
            onDismiss = {},
        )
    }
}
