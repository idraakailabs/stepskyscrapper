package com.idraak.stepskyscraper.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** True when the system "Remove animations" setting is on (animator duration scale 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Custom Material 3 theme. Dynamic colour is deliberately off so the amber stays the amber.
 * Access tokens through [SsTheme]; feature code never uses raw colours, dp values or text styles.
 */
@Composable
fun SsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkSsColors else LightSsColors
    val context = LocalContext.current
    val systemReduce = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val typography = remember { SsTypography() }
    CompositionLocalProvider(
        LocalSsColors provides colors,
        LocalSsTypography provides typography,
        LocalReduceMotion provides (reduceMotion ?: systemReduce),
    ) {
        MaterialTheme(colorScheme = colors.toMaterial(), content = content)
    }
}

object SsTheme {
    val colors: SsColors
        @Composable @ReadOnlyComposable
        get() = LocalSsColors.current
    val type: SsTypography
        @Composable @ReadOnlyComposable
        get() = LocalSsTypography.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
    val brand: SsBrandColors get() = BrandColors
}

private fun SsColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = onSurfaceVariant,
        onSecondary = surface,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSurface,
        tertiary = info,
        onTertiary = surface,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
        inverseSurface = snackbar,
        inverseOnSurface = onSnackbar,
        inversePrimary = snackbarAction,
        outline = outlineStrong,
        outlineVariant = outline,
        scrim = scrim,
        // No red in the theme: errors use neutral tones (design review "No guilt anywhere").
        error = onSurfaceVariant,
        onError = surface,
        errorContainer = surfaceVariant,
        onErrorContainer = onSurface,
    )
}
