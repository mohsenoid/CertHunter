package com.mohsenoid.certhunter.ui.list.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun AppListLoadError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_list_load_error),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.app_list_retry))
            }
        }
    }
}

@ComponentPreviews
@Composable
private fun AppListLoadErrorPreview() {
    CertHunterTheme {
        AppListLoadError(onRetry = {})
    }
}
