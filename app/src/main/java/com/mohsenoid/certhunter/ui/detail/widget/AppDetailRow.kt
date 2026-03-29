package com.mohsenoid.certhunter.ui.detail.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun AppDetailRow(label: String, value: String) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val copiedMessage = stringResource(R.string.app_detail_copied_toast, label)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@ComponentPreviews
@Composable
private fun AppDetailRowPreview() {
    CertHunterTheme {
        AppDetailRow(
            label = "SHA-256",
            value = "A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4",
        )
    }
}
