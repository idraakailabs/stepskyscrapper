package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared press behaviour: scale .97 and 8% darker over 90ms (design "Buttons" note).
 * Disabled under reduce-motion except for the colour change.
 */
@Composable
private fun pressFeedback(interaction: MutableInteractionSource, base: Color): Pair<Float, Color> {
    val pressed by interaction.collectIsPressedAsState()
    val reduce = SsTheme.reduceMotion
    val scale by animateFloatAsState(
        if (pressed && !reduce) SsMotion.PRESS_SCALE else 1f,
        tween(SsMotion.PRESS_MS),
        label = "press-scale",
    )
    val color by animateColorAsState(
        if (pressed) lerp(base, Color.Black, SsMotion.PRESS_DARKEN) else base,
        tween(SsMotion.PRESS_MS),
        label = "press-color",
    )
    return scale to color
}

@Composable
private fun BaseButton(
    onClick: () -> Unit,
    container: Color,
    content: Color,
    height: Dp,
    textStyle: TextStyle,
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    icon: Symbol?,
    horizontalPadding: Dp,
    borderColor: Color? = null,
    iconSize: Dp = IconSize.m,
) {
    val interaction = remember { MutableInteractionSource() }
    val c = SsTheme.colors
    val bg = if (enabled) {
        container
    } else if (container == Color.Transparent) {
        Color.Transparent
    } else {
        c.disabledContainer
    }
    val fg = if (enabled) content else c.disabledContent
    val (scale, pressedBg) = pressFeedback(interaction, bg)
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = height)
            .height(height)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, SsRadius.shapeFull) else Modifier)
            .background(if (bg == Color.Transparent) bg else pressedBg, SsRadius.shapeFull)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            SsIcon(icon, size = iconSize, tint = fg)
            Spacer(Modifier.width(SsSpacing.s))
        }
        Text(text, style = textStyle, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** 52dp amber button. One per screen. */
@Composable
fun SsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Symbol? = null,
    enabled: Boolean = true,
    large: Boolean = true,
) {
    val c = SsTheme.colors
    BaseButton(
        onClick, c.primary, c.onPrimary, SsSpacing.primaryButtonHeight,
        if (large) SsTheme.type.button else SsTheme.type.buttonM, text, modifier, enabled, icon, SsSpacing.xl,
    )
}

/** 48dp tonal button (secondaryContainer). [strong] uses the 15/700 label of the ceremony & help CTAs. */
@Composable
fun SsTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Symbol? = null,
    enabled: Boolean = true,
    strong: Boolean = false,
    height: Dp = SsSpacing.buttonHeight,
) {
    val c = SsTheme.colors
    BaseButton(
        onClick, c.secondaryContainer, c.onSurface, height,
        if (strong) SsTheme.type.buttonM else SsTheme.type.label, text, modifier, enabled, icon, 22.dp,
    )
}

@Composable
fun SsOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Symbol? = null, enabled: Boolean = true) {
    val c = SsTheme.colors
    BaseButton(
        onClick, Color.Transparent, c.onSurface, SsSpacing.buttonHeight, SsTheme.type.label, text, modifier, enabled, icon, 22.dp,
        borderColor = c.outlineStrong,
    )
}

/** Text button in primaryText (#8F5608). */
@Composable
fun SsTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, strong: Boolean = false) {
    val c = SsTheme.colors
    BaseButton(
        onClick, Color.Transparent, c.primaryText, SsSpacing.buttonHeight,
        if (strong) SsTheme.type.bodySStrong else SsTheme.type.labelM, text, modifier, enabled, null, SsSpacing.m,
    )
}

/** 48dp circular icon button with a fill (header buttons, share composer download). */
@Composable
fun SsIconCircleButton(
    symbol: Symbol,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = SsTheme.colors.surfaceVariant,
    tint: Color = SsTheme.colors.onSurface,
    size: Dp = SsSpacing.touchTarget,
    iconSize: Dp = IconSize.ml,
) {
    val interaction = remember { MutableInteractionSource() }
    val (scale, bg) = pressFeedback(interaction, container)
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(bg, SsRadius.shapeFull)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        SsIcon(symbol, size = iconSize, tint = tint)
    }
}

/** 48dp transparent icon target (back, close, settings, dice). */
@Composable
fun SsIconButton(
    symbol: Symbol,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SsTheme.colors.onSurface,
    iconSize: Dp = IconSize.l,
) {
    Box(
        modifier
            .size(SsSpacing.touchTarget)
            // Round ripple for round icon buttons (it used to be a 48dp square).
            .clip(SsRadius.shapeFull)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        SsIcon(symbol, size = iconSize, tint = tint)
    }
}

/** Inline amber text action used in help steps ("Open Health Connect"), 44dp min height. */
@Composable
fun SsInlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(SsRadius.shapeSm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(PaddingValues(horizontal = SsSpacing.m)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = SsTheme.type.bodySStrong, color = SsTheme.colors.primaryText)
    }
}
