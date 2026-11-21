package ai.idraak.stepskyscraper.core.designsystem.icon

import ai.idraak.stepskyscraper.core.designsystem.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SymbolsOutlined = FontFamily(Font(R.font.material_symbols_rounded_outlined))
private val SymbolsFilled = FontFamily(Font(R.font.material_symbols_rounded_filled))

/** Icon sizes used in the design (dp, not scaled with font size). */
@Stable
object IconSize {
    val xs = 14.dp
    val s = 16.dp
    val sm = 18.dp
    val m = 20.dp
    val ml = 22.dp
    val l = 24.dp
    val xl = 26.dp
    val xxl = 28.dp
    val badge = 30.dp
    val hero = 36.dp
}

/**
 * Material Symbols Rounded, weight 400, outline by default and filled when selected (design rule).
 * Decorative unless [contentDescription] is given.
 */
@Composable
fun SsIcon(
    symbol: Symbol,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.l,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    contentDescription: String? = null,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    val semantics = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Box(modifier.size(size).then(semantics), contentAlignment = Alignment.Center) {
        Text(
            text = symbol.char,
            color = tint,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = if (filled) SymbolsFilled else SymbolsOutlined,
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

/**
 * The "sync" icon, optionally spinning (Build "Syncing…" chip). Drawn from [SyncVector] in a square box so the
 * rotation pivots exactly on the icon's centre; the rotation lives in `graphicsLayer` (default centre origin) and
 * the rotated layer holds nothing but the square glyph, so outer padding or offsets never move the pivot.
 * Rotation is read in the draw phase only, so the spin doesn't recompose. Static when [spinning] is false.
 */
@Composable
fun SsSyncIcon(
    spinning: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.l,
    tint: Color = LocalContentColor.current,
) {
    val painter = rememberVectorPainter(SyncVector)
    val rotation: State<Float> = if (spinning) {
        rememberInfiniteTransition(label = "sync").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SYNC_SPIN_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "spin",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(modifier.size(size).clearAndSetSemantics { }) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation.value },
        )
    }
}

private const val SYNC_SPIN_MS = 1_000
