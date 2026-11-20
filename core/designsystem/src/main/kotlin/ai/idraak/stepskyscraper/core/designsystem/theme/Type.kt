package ai.idraak.stepskyscraper.core.designsystem.theme

import ai.idraak.stepskyscraper.core.designsystem.R
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** DM Sans (opsz 14 instances) for UI text; the opsz 40 instance for large numbers. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_400, FontWeight.Normal),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold),
)

val DmSansDisplay = FontFamily(Font(R.font.dm_sans_display_600, FontWeight.SemiBold))

/** Big Shoulders Display for tower names and section titles (always caps). */
val BigShoulders = FontFamily(
    Font(R.font.big_shoulders_display_600, FontWeight.SemiBold),
    Font(R.font.big_shoulders_display_700, FontWeight.Bold),
    Font(R.font.big_shoulders_display_800, FontWeight.ExtraBold),
)

private const val TABULAR = "tnum"

/** CSS-like line box: half-leading on both sides, no trimming. */
private val CssLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(
    size: Int,
    lineHeight: Float,
    weight: FontWeight,
    tracking: Float = 0f,
    family: FontFamily = DmSans,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = if (tracking == 0f) TextUnit.Unspecified else tracking.sp,
    fontFeatureSettings = if (tabular) TABULAR else null,
    lineHeightStyle = CssLineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Type scale from the design ("DM Sans (tnum on all numbers) · Big Shoulders Display for tower names ·
 * sp, scales to 200%"). Names follow the design labels; extra styles are the exact one-off sizes used on
 * individual screens so feature code never builds its own TextStyle.
 */
@Immutable
data class SsTypography(
    val numberXL: TextStyle = style(64, 64f, FontWeight.SemiBold, -1.5f, DmSansDisplay, tabular = true),
    val numberHero: TextStyle = style(60, 63f, FontWeight.SemiBold, -1.5f, DmSansDisplay, tabular = true),
    val numberL: TextStyle = style(40, 44f, FontWeight.SemiBold, -0.8f, DmSansDisplay, tabular = true),
    val numberStat: TextStyle = style(36, 39.6f, FontWeight.SemiBold, -0.8f, DmSansDisplay, tabular = true),
    val numberM: TextStyle = style(32, 38f, FontWeight.SemiBold, -0.6f, tabular = true),
    val numberWidget: TextStyle = style(30, 33f, FontWeight.SemiBold, -0.6f, tabular = true),
    val numberWidgetMock: TextStyle = style(26, 28.6f, FontWeight.SemiBold, tabular = true),
    val numberS: TextStyle = style(24, 36f, FontWeight.SemiBold, tabular = true),
    val numberTile: TextStyle = style(24, 30f, FontWeight.SemiBold, tabular = true),
    val numberXS: TextStyle = style(22, 26f, FontWeight.SemiBold, -0.3f, tabular = true),
    val towerName: TextStyle = style(36, 36f, FontWeight.ExtraBold, 0.6f, BigShoulders),
    val towerNameHeader: TextStyle = style(34, 35.7f, FontWeight.ExtraBold, 0.5f, BigShoulders),
    val towerNameCeremony: TextStyle = style(40, 40f, FontWeight.ExtraBold, 0.5f, BigShoulders),
    val towerNameSheet: TextStyle = style(32, 32f, FontWeight.ExtraBold, 0.5f, BigShoulders),
    val towerNameField: TextStyle = style(24, 28f, FontWeight.ExtraBold, 0.5f, BigShoulders),
    val towerNameKit: TextStyle = style(22, 26f, FontWeight.Bold, 0.5f, BigShoulders),
    val towerNameCard: TextStyle = style(20, 24f, FontWeight.Bold, 0.4f, BigShoulders),
    val towerNameWidget: TextStyle = style(18, 22f, FontWeight.ExtraBold, 0.4f, BigShoulders),
    val towerNameChip: TextStyle = style(17, 20f, FontWeight.Bold, 0.3f, BigShoulders),
    val displayTitle: TextStyle = style(36, 36f, FontWeight.ExtraBold, 0.5f, BigShoulders),
    val headlineL: TextStyle = style(30, 34.5f, FontWeight.Bold, -0.4f),
    val headline: TextStyle = style(28, 34f, FontWeight.Bold, -0.3f),
    val headlineS: TextStyle = style(26, 31.2f, FontWeight.Bold, -0.3f),
    val screenTitle: TextStyle = style(22, 28f, FontWeight.Bold),
    val title: TextStyle = style(20, 26f, FontWeight.SemiBold),
    val titleBold: TextStyle = style(20, 26f, FontWeight.Bold),
    val unit: TextStyle = style(20, 26f, FontWeight.SemiBold),
    val sheetTitle: TextStyle = style(18, 24f, FontWeight.Bold),
    val bodyL: TextStyle = style(18, 27f, FontWeight.Normal),
    val body: TextStyle = style(16, 24f, FontWeight.Normal),
    val bodyStrong: TextStyle = style(16, 24f, FontWeight.Bold),
    val button: TextStyle = style(16, 20f, FontWeight.Bold),
    val buttonM: TextStyle = style(15, 20f, FontWeight.Bold),
    val bodyM: TextStyle = style(15, 21.75f, FontWeight.Normal),
    val bodyMStrong: TextStyle = style(15, 21.75f, FontWeight.Bold),
    val labelM: TextStyle = style(15, 20f, FontWeight.SemiBold),
    val bodyS: TextStyle = style(14, 21f, FontWeight.Normal),
    val bodySStrong: TextStyle = style(14, 21f, FontWeight.Bold),
    val label: TextStyle = style(14, 19.6f, FontWeight.SemiBold),
    val chip: TextStyle = style(14, 19.6f, FontWeight.Medium),
    val bodyXS: TextStyle = style(13, 18.85f, FontWeight.Normal),

    // Option / plan cards: the design leaves these at CSS `line-height: normal` (DM Sans ascent + descent = 1.302em).
    val optionTitle: TextStyle = style(16, 20.8f, FontWeight.Bold),
    val optionSubtitle: TextStyle = style(13, 16.9f, FontWeight.Normal),
    val planTitle: TextStyle = style(15, 19.5f, FontWeight.Bold),
    val planSubtitle: TextStyle = style(12, 15.6f, FontWeight.Normal),
    val labelS: TextStyle = style(13, 18f, FontWeight.SemiBold),
    val labelSBold: TextStyle = style(13, 18f, FontWeight.Bold),
    val caption: TextStyle = style(12, 16f, FontWeight.Medium),
    val captionRegular: TextStyle = style(12, 16f, FontWeight.Normal),
    val captionStrong: TextStyle = style(12, 16f, FontWeight.SemiBold),
    val captionBold: TextStyle = style(12, 16f, FontWeight.Bold),
    val overline: TextStyle = style(12, 16f, FontWeight.SemiBold, 0.4f),
    val overlineWide: TextStyle = style(13, 18f, FontWeight.Bold, 1.2f),
    val sectionLabel: TextStyle = style(13, 18f, FontWeight.Bold, 0.6f),
    val micro: TextStyle = style(11, 14f, FontWeight.Medium),
    val microStrong: TextStyle = style(11, 14f, FontWeight.SemiBold),
    val microBold: TextStyle = style(11, 14f, FontWeight.Bold, 0.4f),
    /** "home screen" label on the 1f wallpaper mock (design: DM Mono 11px; the system monospace stands in). */
    val mockLabel: TextStyle = style(11, 14.3f, FontWeight.Normal, family = FontFamily.Monospace),
    val legal: TextStyle = style(11, 15.95f, FontWeight.Normal),
    val nav: TextStyle = style(12, 16f, FontWeight.Medium, 0.2f),
    val navActive: TextStyle = style(12, 16f, FontWeight.Bold, 0.2f),
    val ringCenter: TextStyle = style(13, 16f, FontWeight.Bold, tabular = true),
    val ringCenterL: TextStyle = style(22, 26f, FontWeight.Bold, tabular = true),
)

val LocalSsTypography = staticCompositionLocalOf { SsTypography() }
