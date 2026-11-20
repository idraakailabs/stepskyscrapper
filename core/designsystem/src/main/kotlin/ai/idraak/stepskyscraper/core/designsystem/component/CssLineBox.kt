package ai.idraak.stepskyscraper.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

/**
 * Makes a [androidx.compose.material3.Text] occupy the line box CSS would give it when the style's line height is
 * smaller than the font's own height (ascent + descent; DM Sans ≈ 1.30em).
 *
 * Android keeps the font's full ascent on the first line and descent on the last line in that case (fallback line
 * spacing), so a 60/63 number lays out 78dp tall where the design has 63dp. CSS splits the negative half-leading
 * evenly, so this trims `natural − lineHeight` from the box, half above and half below, and shifts the baselines
 * to match. Glyphs may overflow the box, exactly as in the browser. Styles whose line height is at least the
 * natural height are left untouched.
 */
@Composable
fun Modifier.cssLineBox(style: TextStyle): Modifier {
    if (!style.lineHeight.isSp) return this
    val measurer = rememberTextMeasurer(cacheSize = 1)
    val natural = remember(style, measurer) {
        measurer.measure("0", style.copy(lineHeight = TextUnit.Unspecified)).size.height
    }
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val excess = natural - style.lineHeight.roundToPx()
        if (excess <= 0 || placeable.height < natural) {
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val top = excess / 2
        val lines = buildMap<AlignmentLine, Int> {
            placeable[FirstBaseline].takeIf { it != AlignmentLine.Unspecified }?.let { put(FirstBaseline, it - top) }
            placeable[LastBaseline].takeIf { it != AlignmentLine.Unspecified }?.let { put(LastBaseline, it - top) }
        }
        layout(placeable.width, placeable.height - excess, lines) { placeable.place(0, -top) }
    }
}

/**
 * Baseline (from the top of a [cssLineBox]-trimmed single line) of text in [style]. Used by layouts whose children
 * don't report a stable baseline, such as the per-digit roll inside `AnimatedContent`.
 */
@Composable
fun rememberCssBaseline(style: TextStyle): Int {
    val measurer = rememberTextMeasurer(cacheSize = 2)
    val density = LocalDensity.current
    return remember(style, measurer, density) {
        val line = measurer.measure("0", style)
        val natural = measurer.measure("0", style.copy(lineHeight = TextUnit.Unspecified)).size.height
        val lineHeightPx = if (style.lineHeight.isSp) with(density) { style.lineHeight.roundToPx() } else natural
        val excess = natural - lineHeightPx
        // Same trim as cssLineBox.
        val top = if (excess > 0 && line.size.height >= natural) excess / 2 else 0
        line.firstBaseline.roundToInt() - top
    }
}
