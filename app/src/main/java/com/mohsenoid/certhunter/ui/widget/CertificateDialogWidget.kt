package com.mohsenoid.certhunter.ui.widget

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
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun CertificateDialogWidget(
    app: AppItem,
    details: CertificateDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column {
                Text(text = app.name, fontWeight = FontWeight.Bold)
                if (!isLoading) {
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (details == null) {
                Text("No signature found or unable to parse.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailRowWidget("Package Name", app.packageName)
                    DetailRowWidget("System App", if (app.isSystemApp) "Yes" else "No")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRowWidget("SHA-256", details.sha256)
                    DetailRowWidget("SHA-1", details.sha1)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRowWidget("Owner", details.owner)
                    DetailRowWidget("Issuer", details.issuer)
                    DetailRowWidget("Serial", details.serialNumber)
                    DetailRowWidget("Valid From", details.validFrom)
                    DetailRowWidget("Valid Until", details.validUntil)
                }
            }
        }
    )
}

private val previewApp = AppItem(
    name = "CertHunter",
    packageName = "com.mohsenoid.certhunter",
    isSystemApp = false,
)

private val previewCert = CertificateDetails(
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
private fun CertificateDialogLoadingPreview() {
    CertHunterTheme {
        CertificateDialogWidget(
            app = previewApp,
            details = null,
            isLoading = true,
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun CertificateDialogWithDetailsPreview() {
    CertHunterTheme {
        CertificateDialogWidget(
            app = previewApp,
            details = previewCert,
            isLoading = false,
            onDismiss = {},
        )
    }
}

@ComponentPreviews
@Composable
private fun CertificateDialogNoDetailsPreview() {
    CertHunterTheme {
        CertificateDialogWidget(
            app = previewApp,
            details = null,
            isLoading = false,
            onDismiss = {},
        )
    }
}
