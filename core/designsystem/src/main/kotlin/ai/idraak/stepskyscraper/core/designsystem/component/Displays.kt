package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyChrome
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

/**
 * Number roll: tabular digits roll vertically per digit (250ms) so the width never jumps.
 * Decreases are shown without rolling backwards (they only happen on a new day).
 */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val text = NumberFormat.getIntegerInstance().format(value)
    val reduce = SsTheme.reduceMotion
    // AnimatedContent doesn't keep reporting its child's baseline once a digit has rolled, so the row reports
    // the baseline itself (callers align the unit with alignByBaseline()).
    val baseline = rememberCssBaseline(style)
    Row(
        modifier
            .clearAndSetSemantics { contentDescription = text }
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, p.height, mapOf(FirstBaseline to baseline, LastBaseline to baseline)) { p.place(0, 0) }
            },
    ) {
        text.forEachIndexed { index, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(tween(SsMotion.CROSSFADE_MS)) togetherWith fadeOut(tween(SsMotion.CROSSFADE_MS))
                    } else {
                        (slideInVertically(tween(SsMotion.NUMBER_ROLL_MS)) { it } + fadeIn(tween(SsMotion.NUMBER_ROLL_MS))) togetherWith
                            (slideOutVertically(tween(SsMotion.NUMBER_ROLL_MS)) { -it } + fadeOut(tween(SsMotion.NUMBER_ROLL_MS)))
                    }
                },
                label = "digit-$index",
            ) { digit -> Text(digit.toString(), style = style, color = color, modifier = Modifier.cssLineBox(style)) }
        }
    }
}

/**
 * The value shown while counting up from 0 to [target] over [durationMs] (final value at once under reduce
 * motion). [onTick] is called for every new value.
 */
@Composable
fun rememberCountUp(target: Int, durationMs: Int = SsMotion.STEP_COUNT_ROLL_MS, onTick: (Int) -> Unit = {}): Int {
    val reduce = SsTheme.reduceMotion
    val anim = remember { Animatable(if (reduce) target.toFloat() else 0f) }
    var shown by remember { mutableIntStateOf(if (reduce) target else 0) }
    LaunchedEffect(target, reduce) {
        if (reduce) {
            anim.snapTo(target.toFloat())
        } else {
            anim.animateTo(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) {
                val v = value.toInt()
                if (v != shown) {
                    shown = v
                    onTick(v)
                }
            }
        }
        shown = target
    }
    return shown
}

/** Counts up from 0 to [target] (onboarding "4,200"). */
@Composable
fun CountUpNumber(
    target: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMs: Int = SsMotion.STEP_COUNT_ROLL_MS,
    prefix: String = "",
    onTick: (Int) -> Unit = {},
) {
    val shown = rememberCountUp(target, durationMs, onTick)
    val format = remember { NumberFormat.getIntegerInstance() }
    val finalText = prefix + format.format(target)
    Text(
        prefix + format.format(shown),
        style = style,
        color = color,
        modifier = modifier.clearAndSetSemantics { contentDescription = finalText }.cssLineBox(style),
    )
}

/** Persistent next-floor pill: number + "to next floor" + 10-segment mini bar. */
@Composable
fun NextFloorPill(
    stepsToNext: Int,
    segmentsFilled: Int,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    container: Color = SsTheme.colors.surface,
    text: Color = SsTheme.colors.onSurface,
    subtext: Color = SsTheme.colors.bodySecondary,
    empty: Color = SsTheme.colors.track,
    fill: Color = SsTheme.colors.primary,
) {
    Row(
        modifier
            .height(SsSpacing.statusChipHeight)
            .background(container, SsRadius.shapeFull)
            .padding(horizontal = SsSpacing.m)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SsSpacing.s),
    ) {
        Text(NumberFormat.getIntegerInstance().format(stepsToNext), style = SsTheme.type.labelSBold, color = text)
        Text(label, style = SsTheme.type.captionRegular, color = subtext)
        MiniSegmentBar(segmentsFilled, fill = fill, empty = empty)
    }
}

/**
 * Sky header used on Build and Skyline: overline, tower/district name (one line, autosize 34 → 26sp, then
 * ellipsis), subtitle, optional extra content and trailing header buttons.
 */
@Composable
fun SkyHeader(
    overline: String,
    title: String,
    subtitle: String,
    chrome: SkyChrome,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = SsSpacing.screenMargin, end = SsSpacing.m, top = SsSpacing.s),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                overline.uppercase(),
                style = SsTheme.type.overline,
                color = chrome.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AutoSizeName(title, SsTheme.type.towerNameHeader, chrome.title)
            Text(subtitle, style = SsTheme.type.bodyXS, color = chrome.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            extra()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.xs)) { actions() }
    }
}

/** Tower name on one line: autosize from the style's size down to [min] sp, then ellipsis (design review). */
@Composable
fun AutoSizeName(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, min: TextUnit = 26.sp) {
    var size by remember(text) { mutableIntStateOf(style.fontSize.value.toInt()) }
    val sized = style.copy(fontSize = size.sp, lineHeight = (size * 1.05f).sp)
    Text(
        text.uppercase(),
        style = sized,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.cssLineBox(sized),
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && size > min.value) size -= 1
        },
    )
}

/** Achievement badge tile: earned = ink + amber filled glyph, in progress = neutral. */
@Composable
fun AchievementBadge(
    symbol: Symbol,
    earned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    radius: Dp = 20.dp,
    flip: Boolean = false,
) {
    val c = SsTheme.colors
    val rotation = remember { Animatable(if (flip) 180f else 0f) }
    val reduce = SsTheme.reduceMotion
    LaunchedEffect(flip) {
        if (flip && !reduce) rotation.animateTo(0f, tween(SsMotion.BADGE_FLIP_MS)) else rotation.snapTo(0f)
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }
            .background(if (earned) SsTheme.brand.passSurface else c.surfaceVariant, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        SsIcon(
            symbol,
            size = if (size >= 64.dp) IconSize.badge else IconSize.xl,
            tint = if (earned) SsTheme.brand.passAccent else c.bodySecondary,
            filled = earned,
        )
    }
}

/** Numbered help step (28dp ink circle). */
@Composable
fun HelpStepCard(number: Int, title: String, body: @Composable () -> Unit, action: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val c = SsTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface, SsRadius.shapeCard)
            .padding(start = SsSpacing.l, end = SsSpacing.l, top = SsSpacing.m, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(SsSpacing.m),
    ) {
        Box(Modifier.size(28.dp).background(c.ink, SsRadius.shapeFull), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = SsTheme.type.labelSBold, color = c.onInk)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SsSpacing.xs)) {
            Text(title, style = SsTheme.type.planTitle, color = c.onSurface)
            body()
            // Design: a 44dp inline action with `margin: -6px 0 -6px -12px`, so it takes 32dp of layout and its
            // text lines up with the body copy. The 48dp touch target is kept; it overlaps the padding instead.
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    val pull = INLINE_ACTION_PULL.roundToPx()
                    val consumed = INLINE_ACTION_HEIGHT.roundToPx()
                    layout((p.width - pull).coerceAtLeast(0), consumed) { p.place(-pull, (consumed - p.height) / 2) }
                },
            ) { action() }
        }
    }
}

/** Reward chip ("redeem · Copper Deco facade"). */
@Composable
fun RewardChip(text: String, modifier: Modifier = Modifier, symbol: Symbol = Symbol.Redeem) {
    Row(
        modifier
            .background(SsTheme.colors.background, SsRadius.shapeSm)
            .padding(horizontal = 10.dp, vertical = SsSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SsSpacing.xs),
    ) {
        SsIcon(symbol, size = IconSize.xs, tint = SsTheme.colors.onSurface)
        Text(text, style = SsTheme.type.captionStrong, color = SsTheme.colors.onSurface)
    }
}

/** Small brand mark (three stacked bars) used on share cards. */
@Composable
fun BrandMark(color: Color, modifier: Modifier = Modifier, scale: Float = 1f) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((1 * scale).dp)) {
        Box(Modifier.size((5 * scale).dp, (4 * scale).dp).background(color))
        Box(Modifier.size((10 * scale).dp, (4 * scale).dp).background(color))
        Box(Modifier.size((15 * scale).dp, (5 * scale).dp).background(color))
    }
}

/** Empty / error helper state with calm copy (no red anywhere). */
@Composable
fun SsMessageState(symbol: Symbol, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(SsSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
    ) {
        Box(Modifier.size(72.dp).background(SsTheme.colors.surfaceVariant, SsRadius.shapeLg), contentAlignment = Alignment.Center) {
            SsIcon(symbol, size = IconSize.hero, tint = SsTheme.colors.onSurface)
        }
        Text(title, style = SsTheme.type.sheetTitle, color = SsTheme.colors.onSurface)
        Text(body, style = SsTheme.type.bodyS, color = SsTheme.colors.bodySecondary)
        action?.invoke()
    }
}

private val INLINE_ACTION_PULL = 12.dp
private val INLINE_ACTION_HEIGHT = 32.dp
