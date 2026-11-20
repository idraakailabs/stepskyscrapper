package ai.idraak.stepskyscraper.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Step Skyscraper colour tokens (design system sheet "Colour · Light / Dark"). Surfaces stay quiet
 * warm-grey so the tower and sky carry the colour; amber appears only on progress and primary actions.
 * There is intentionally no red/error token: failure states use neutral grey and reassuring copy.
 */
@Immutable
data class SsColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val secondaryContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    /** Secondary body text (#4A443D in light). */
    val bodySecondary: Color,
    val outline: Color,
    /** 1dp card / option outline (#E2DBD1). */
    val outlineCard: Color,
    /** Chip and outlined-button border, inactive dots, drag handle (#CFC7BC). */
    val outlineStrong: Color,
    val divider: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryText: Color,
    /** Empty progress segments, ring tracks. */
    val track: Color,
    /** Segments after the active one on the onboarding payoff (#E3DDD4). */
    val trackFuture: Color,
    /** Past-day bars on Me (neutral). */
    val barNeutral: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    /** Inverse "ink" fill used for selected chips, badges and the Architect Pass card. */
    val ink: Color,
    val onInk: Color,
    /** Amber accent used on ink surfaces (#F0B455 in both themes). */
    val inkAccent: Color,
    val snackbar: Color,
    val onSnackbar: Color,
    val snackbarAction: Color,
    val navBar: Color,
    val navLine: Color,
    val navPill: Color,
    val navHandle: Color,
    val scrim: Color,
    val isDark: Boolean,
)

/** Raw palette from the design. Feature code must use [SsColors] via `SsTheme.colors`, never these. */
internal object Palette {
    val Warm50 = Color(0xFFFBFAF7)
    val Warm100 = Color(0xFFF4F1EC)
    val Warm150 = Color(0xFFECE7E0)
    val Warm200 = Color(0xFFE6DFD5)
    val Warm250 = Color(0xFFE2DBD1)
    val Warm275 = Color(0xFFE7E1D8)
    val Warm300 = Color(0xFFD9D2C8)
    val Warm350 = Color(0xFFCFC7BC)
    val Warm400 = Color(0xFFE6E1DA)
    val Warm500 = Color(0xFF8E877D)
    val Warm600 = Color(0xFF6A635A)
    val Warm700 = Color(0xFF4A443D)
    val Warm800 = Color(0xFF2E2A26)
    val Ink = Color(0xFF24211D)
    val Dark0 = Color(0xFF171513)
    val Dark1 = Color(0xFF201D1A)
    val Dark2 = Color(0xFF2B2724)
    val Dark3 = Color(0xFF3A3530)
    val Dark4 = Color(0xFF3D3833)
    val DarkLine = Color(0xFF2F2B27)
    val DarkInk = Color(0xFFF1ECE5)
    val DarkSub = Color(0xFFADA59B)
    val Amber = Color(0xFFE8A23A)
    val AmberDark = Color(0xFFF0B455)
    val OnAmber = Color(0xFF2A1D08)
    val AmberContainer = Color(0xFFF8E6C8)
    val AmberContainerDark = Color(0xFF3A2C17)
    val AmberText = Color(0xFF8F5608)
    val AmberDeep = Color(0xFF6E420A)
    val Info = Color(0xFF4E6A86)
    val InfoDark = Color(0xFF9DB6D0)
    val InfoContainer = Color(0xFFE5ECF3)
    val OnInfoContainer = Color(0xFF3B546D)
}

val LightSsColors = SsColors(
    background = Palette.Warm100,
    surface = Palette.Warm50,
    surfaceVariant = Palette.Warm150,
    secondaryContainer = Palette.Warm200,
    onSurface = Palette.Ink,
    onSurfaceVariant = Palette.Warm600,
    bodySecondary = Palette.Warm700,
    outline = Palette.Warm300,
    outlineCard = Palette.Warm250,
    outlineStrong = Palette.Warm350,
    divider = Palette.Warm150,
    info = Palette.Info,
    infoContainer = Palette.InfoContainer,
    onInfoContainer = Palette.OnInfoContainer,
    primary = Palette.Amber,
    onPrimary = Palette.OnAmber,
    primaryContainer = Palette.AmberContainer,
    onPrimaryContainer = Palette.AmberDeep,
    primaryText = Palette.AmberText,
    track = Palette.Warm150,
    trackFuture = Color(0xFFE3DDD4),
    barNeutral = Palette.Warm350,
    disabledContainer = Palette.Warm400,
    disabledContent = Palette.Warm500,
    ink = Palette.Ink,
    onInk = Palette.Warm50,
    inkAccent = Palette.AmberDark,
    snackbar = Palette.Warm800,
    onSnackbar = Palette.DarkInk,
    snackbarAction = Palette.AmberDark,
    navBar = Palette.Warm50,
    navLine = Palette.Warm275,
    navPill = Palette.Warm200,
    navHandle = Palette.Ink,
    scrim = Palette.Ink.copy(alpha = 0.32f),
    isDark = false,
)

val DarkSsColors = SsColors(
    background = Palette.Dark0,
    surface = Palette.Dark1,
    surfaceVariant = Palette.Dark2,
    secondaryContainer = Palette.Dark3,
    onSurface = Palette.DarkInk,
    onSurfaceVariant = Palette.DarkSub,
    bodySecondary = Palette.DarkSub,
    outline = Palette.Dark4,
    outlineCard = Palette.Dark4,
    outlineStrong = Palette.Dark4,
    divider = Palette.Dark2,
    info = Palette.InfoDark,
    infoContainer = Color(0xFF26313D),
    onInfoContainer = Palette.InfoDark,
    primary = Palette.AmberDark,
    onPrimary = Palette.OnAmber,
    primaryContainer = Palette.AmberContainerDark,
    onPrimaryContainer = Palette.AmberDark,
    primaryText = Palette.AmberDark,
    track = Palette.Dark2,
    trackFuture = Palette.Dark3,
    barNeutral = Palette.Dark4,
    disabledContainer = Palette.Dark2,
    disabledContent = Color(0xFF7C756C),
    ink = Palette.DarkInk,
    onInk = Palette.Dark0,
    inkAccent = Palette.AmberDark,
    snackbar = Palette.Dark3,
    onSnackbar = Palette.DarkInk,
    snackbarAction = Palette.AmberDark,
    navBar = Palette.Dark1,
    navLine = Palette.DarkLine,
    navPill = Palette.Dark3,
    navHandle = Palette.DarkInk,
    scrim = Color.Black.copy(alpha = 0.48f),
    isDark = true,
)

/**
 * Ink-on-dark chips (Architect Pass badge, selected filter chip) keep the light-theme ink in both themes
 * where the design shows a dark chip; this token pair exists so components do not hard-code it.
 */
@Immutable
data class SsBrandColors(
    val passSurface: Color,
    val onPassSurface: Color,
    val passSubtle: Color,
    val passAccent: Color,
)

val BrandColors = SsBrandColors(
    passSurface = Palette.Ink,
    onPassSurface = Palette.Warm50,
    passSubtle = Palette.Warm350,
    passAccent = Palette.AmberDark,
)

val LocalSsColors = staticCompositionLocalOf { LightSsColors }
