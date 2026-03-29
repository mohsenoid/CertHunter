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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohsenoid.certhunter.domain.model.AppCertificateDetails
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
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column {
                Text(text = uiState.appName, fontWeight = FontWeight.Bold)
                if (!uiState.isLoading) {
                    Text(
                        text = "Tap any field to copy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        text = {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.details == null) {
                Text("No signature found or unable to parse.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppDetailRowWidget("Package Name", uiState.packageName)
                    AppDetailRowWidget("System App", if (uiState.isSystemApp) "Yes" else "No")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AppDetailRowWidget("SHA-256", uiState.details.sha256)
                    AppDetailRowWidget("SHA-1", uiState.details.sha1)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AppDetailRowWidget("Owner", uiState.details.owner)
                    AppDetailRowWidget("Issuer", uiState.details.issuer)
                    AppDetailRowWidget("Serial", uiState.details.serialNumber)
                    AppDetailRowWidget("Valid From", uiState.details.validFrom)
                    AppDetailRowWidget("Valid Until", uiState.details.validUntil)
                }
            }
        }
    )
}

private val previewCert = AppCertificateDetails(
    sha256 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
    sha1 = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
    owner = "CN=Example, O=Example Corp, C=US",
    issuer = "CN=Example CA, O=Example Corp, C=US",
    serialNumber = "123456789",
    validFrom = "2023-01-01",
    validUntil = "2033-01-01",
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
                details = previewCert,
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
            uiState = AppDetailUiModel(isLoading = false, appName = "CertHunter", details = null),
            onDismiss = {},
        )
    }
}
