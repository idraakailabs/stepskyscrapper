package com.idraak.stepskyscraper.core.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme

/** Every screen preview renders at the design's 360 × 800 frame, light and dark. */
@Preview(name = "Light", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ScreenPreviews

/** Light / dark at 200% font scale (accessibility check). */
@Preview(name = "Font 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
annotation class FontScalePreview

@Composable
fun PreviewSurface(dark: Boolean = false, content: @Composable () -> Unit) {
    SsTheme(darkTheme = dark, reduceMotion = true) {
        Box(Modifier.fillMaxSize().background(SsTheme.colors.background)) { content() }
    }
}
