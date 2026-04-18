package com.mohsenoid.certhunter.ui.list.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mohsenoid.certhunter.R
import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme
import com.mohsenoid.certhunter.ui.util.ComponentPreviews

@Composable
fun AppListRow(app: AppItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.app_list_item_click_label), onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = AppIconData(app.packageName),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (app.isSystemApp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.large,
                            )
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_list_system_label),
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Text(
                text = app.packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@ComponentPreviews
@Composable
private fun AppListRowPreview() {
    CertHunterTheme {
        AppListRow(
            app = AppItem(name = "CertHunter", packageName = "com.mohsenoid.certhunter", isSystemApp = true),
            onClick = {},
        )
    }
}
