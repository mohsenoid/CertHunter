package com.mohsenoid.certhunter.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.mohsenoid.certhunter.domain.model.ShareCertificateLabels
import com.mohsenoid.certhunter.ui.detail.widget.AppDetailRow
import com.mohsenoid.certhunter.ui.detail.widget.CertificateValidityBadge
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    uiState: AppDetailUiModel,
    onAction: (AppDetailAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val shareLabels = rememberShareCertificateLabels()
    val canShare = !uiState.isLoading && uiState.error == null && uiState.certificates.isNotEmpty()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dismiss: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = uiState.appName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (canShare) {
                    IconButton(onClick = { onAction(AppDetailAction.ShareCertificate(shareLabels)) }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_certificate_button),
                        )
                    }
                }
                IconButton(onClick = dismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.app_detail_close),
                    )
                }
            }
            if (!uiState.isLoading) {
                Text(
                    text = stringResource(R.string.app_detail_tap_to_copy),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            AppDetailContent(uiState)
        }
    }
}

@Composable
private fun rememberShareCertificateLabels(): ShareCertificateLabels = ShareCertificateLabels(
    sha256 = stringResource(R.string.app_detail_label_sha256),
    sha1 = stringResource(R.string.app_detail_label_sha1),
    owner = stringResource(R.string.app_detail_label_owner),
    issuer = stringResource(R.string.app_detail_label_issuer),
    serial = stringResource(R.string.app_detail_label_serial),
    validRange = stringResource(R.string.share_certificate_label_valid_range),
    status = stringResource(R.string.share_certificate_label_status),
    signerHeader = stringResource(R.string.share_certificate_signer_header),
    markerValid = stringResource(R.string.share_certificate_marker_valid),
    markerExpired = stringResource(R.string.share_certificate_marker_expired),
    markerExpiringSoon = stringResource(R.string.share_certificate_marker_expiring),
)

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
                },
            ),
        )
    } else if (uiState.certificates.isEmpty()) {
        Text(stringResource(R.string.app_detail_no_signature_found))
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                AppDetailRow(stringResource(R.string.app_detail_label_package_name), uiState.packageName)
                AppDetailRow(
                    stringResource(R.string.app_detail_label_system_app),
                    if (uiState.isSystemApp) {
                        stringResource(R.string.app_detail_system_app_yes)
                    } else {
                        stringResource(R.string.app_detail_system_app_no)
                    },
                )
            }
            uiState.certificates.forEachIndexed { index, cert ->
                item {
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
            }
            if (uiState.historicalCertificates.isNotEmpty()) {
                item {
                    HistoricalCertificatesSection(uiState.packageName, uiState.historicalCertificates)
                }
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
private fun HistoricalCertificatesSection(packageName: String, certs: List<AppCertificateDetails>) {
    // Key by package so expansion state resets on every open (no rememberSaveable).
    var expanded by remember(packageName) { mutableStateOf(false) }
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

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppDetailScreenPreview() {
    CertHunterTheme {
        Surface {
            AppDetailScreen(
                uiState = AppDetailUiModel(
                    isLoading = false,
                    packageName = "com.mohsenoid.certhunter",
                    appName = "CertHunter",
                    isSystemApp = false,
                    certificates = listOf(previewCert),
                ),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppDetailScreenMultiSignerPreview() {
    CertHunterTheme {
        Surface {
            AppDetailScreen(
                uiState = AppDetailUiModel(
                    isLoading = false,
                    packageName = "com.mohsenoid.certhunter",
                    appName = "CertHunter",
                    isSystemApp = false,
                    certificates = listOf(previewCert, previewOldCert),
                ),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppDetailScreenWithHistoryPreview() {
    CertHunterTheme {
        Surface {
            AppDetailScreen(
                uiState = AppDetailUiModel(
                    isLoading = false,
                    packageName = "com.mohsenoid.certhunter",
                    appName = "CertHunter",
                    isSystemApp = false,
                    certificates = listOf(previewCert),
                    historicalCertificates = listOf(previewOldCert),
                ),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppDetailScreenLoadingPreview() {
    CertHunterTheme {
        Surface {
            AppDetailScreen(
                uiState = AppDetailUiModel(isLoading = true),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ComponentPreviews
@Composable
private fun AppDetailScreenNoDetailsPreview() {
    CertHunterTheme {
        Surface {
            AppDetailScreen(
                uiState = AppDetailUiModel(isLoading = false, appName = "CertHunter"),
                onAction = {},
                onDismiss = {},
            )
        }
    }
}
