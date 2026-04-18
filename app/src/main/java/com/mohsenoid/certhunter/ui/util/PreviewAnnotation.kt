package com.mohsenoid.certhunter.ui.util

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Small(85%)",
    group = "Font scales",
    showBackground = true,
    fontScale = 0.85f,
    locale = "en",
)
@Preview(
    name = "Normal(100%)",
    group = "Font scales",
    showBackground = true,
    fontScale = 1f,
    locale = "en",
)
@Preview(
    name = "Large(115%)",
    group = "Font scales",
    showBackground = true,
    fontScale = 1.15f,
    locale = "en",
)
@Preview(
    name = "Largest(130%)",
    group = "Font scales",
    showBackground = true,
    fontScale = 1.3f,
    locale = "en",
)
annotation class FontScalesPreviews

@Preview(
    name = "Light",
    locale = "en",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    uiMode = UI_MODE_NIGHT_NO or UI_MODE_TYPE_NORMAL,
)
annotation class LightPreview

@Preview(
    name = "Dark",
    locale = "en",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL,
)
annotation class DarkPreview

@LightPreview
@DarkPreview
annotation class DarkLightPreviews

@Preview(
    name = "Phone",
    group = "Form factors",
    showBackground = true,
    device = Devices.PHONE,
)
@Preview(
    name = "Foldable",
    group = "Form factors",
    showBackground = true,
    device = Devices.FOLDABLE,
)
@Preview(
    name = "Tablet",
    group = "Form factors",
    showBackground = true,
    device = Devices.TABLET,
)
annotation class FormFactorsPreviews

@Preview(
    name = "RTL",
    locale = "he",
    showBackground = true,
)
annotation class RTLPreview

@RTLPreview
@DarkLightPreviews
annotation class ScreenMinimalPreviews

@DarkLightPreviews
@RTLPreview
@FontScalesPreviews
@FormFactorsPreviews
annotation class ScreenPreviews

@DarkLightPreviews
@RTLPreview
@FontScalesPreviews
annotation class ComponentPreviews
