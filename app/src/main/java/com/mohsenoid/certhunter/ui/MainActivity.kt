package com.mohsenoid.certhunter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.CertificateDetails
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CertHunterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppListScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("CertHunter") })
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps or packages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
                HorizontalDivider()
            }
        }
    ) { padding ->
        if (uiState.isLoadingApps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.filteredApps) { app ->
                    AppRow(app = app, onClick = { viewModel.onAppSelected(app) })
                }

                if (uiState.filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No apps found matching \"${uiState.searchQuery}\"",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.selectedApp?.let { app ->
        CertificateDialog(
            app = app,
            details = uiState.selectedAppCert,
            isLoading = uiState.isLoadingCert,
            onDismiss = viewModel::onDialogDismissed
        )
    }
}

@Composable
fun AppRow(app: AppItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                painter = BitmapPainter(drawableToBitmap(app.icon).asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun CertificateDialog(app: AppItem, details: CertificateDetails?, isLoading: Boolean, onDismiss: () -> Unit) {
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
                        color = MaterialTheme.colorScheme.secondary
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
                    DetailRow("Package Name", app.packageName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("SHA-256", details.sha256)
                    DetailRow("SHA-1", details.sha1)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("Owner", details.owner)
                    DetailRow("Issuer", details.issuer)
                    DetailRow("Serial", details.serialNumber)
                    DetailRow("Valid From", details.validFrom)
                    DetailRow("Valid Until", details.validUntil)
                }
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1))
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
