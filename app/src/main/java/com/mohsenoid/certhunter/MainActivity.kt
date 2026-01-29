package com.mohsenoid.certhunter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppListScreen()
                }
            }
        }
    }
}

// --- Data Models ---

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

data class CertificateDetails(
    val sha256: String,
    val sha1: String,
    val owner: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validUntil: String
)

// --- UI Composables ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen() {
    val context = LocalContext.current

    // Master list of all apps
    var allApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    // Loading state
    var isLoading by remember { mutableStateOf(true) }
    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Selected app for dialog
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedAppCert by remember { mutableStateOf<CertificateDetails?>(null) }

    // Load apps asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val apps = packages.map {
                AppItem(
                    name = it.applicationInfo?.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.applicationInfo?.loadIcon(pm)
                )
            }.sortedBy { it.name.lowercase() }

            allApps = apps
            isLoading = false
        }
    }

    // Dynamic filtering based on search query
    val filteredList = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("CertHunter") })
                // Search Bar Area
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps or packages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(filteredList) { app ->
                    AppRow(app = app) {
                        selectedApp = app
                        selectedAppCert = getAppCertificateDetails(context.packageManager, app.packageName)
                    }
                }

                // Show a helpful message if search returns nothing
                if (filteredList.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No apps found matching \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Show Dialog
    if (selectedApp != null) {
        CertificateDialog(
            app = selectedApp!!,
            details = selectedAppCert,
            onDismiss = { selectedApp = null }
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
        // Render the app icon
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
fun CertificateDialog(app: AppItem, details: CertificateDetails?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column {
                Text(text = app.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "Tap any field to copy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            if (details == null) {
                Text("No signature found or unable to parse.")
            } else {
                // Make the content scrollable in case strings are very long
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
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
                // 1. Get Clipboard Manager
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                // 2. Create ClipData with the label as the tag and value as the text
                val clip = ClipData.newPlainText(label, value)

                // 3. Set the clip
                clipboard.setPrimaryClip(clip)

                // 4. Show Feedback
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // Only show Toast on Android 12 and below.
                    // Android 13+ shows a system UI confirmation automatically.
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp) // Add padding for touch target
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

// --- Helper Functions ---

fun getAppCertificateDetails(pm: PackageManager, packageName: String): CertificateDetails? {
    try {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val pkgInfo = pm.getPackageInfo(packageName, flags)

        // Extract raw signature bytes
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.signingInfo?.apkContentsSigners ?: pkgInfo.signingInfo?.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures
        }

        if (signatures.isNullOrEmpty()) return null

        // Parse the first signature into an X509Certificate object
        val rawBytes = signatures[0].toByteArray()
        val certFactory = CertificateFactory.getInstance("X509")
        val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(rawBytes)) as X509Certificate

        // Format Dates
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

fun hashBytes(bytes: ByteArray, algorithm: String): String {
    val md = MessageDigest.getInstance(algorithm)
    val digest = md.digest(bytes)
    return digest.joinToString(":") { "%02X".format(it) }
}

// Helper to convert Drawable to Bitmap for Compose
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