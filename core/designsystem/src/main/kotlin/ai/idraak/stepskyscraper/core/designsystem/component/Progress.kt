package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 10 × 100-step floor bar. Filled segments are solid, empty ones flat grey; the active one gets a 1dp
 * outline and fills left→right in 160ms. Always paired with the step number (never colour-only).
 */
@Composable
fun SegmentBar(
    filled: Int,
    activeFraction: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    segments: Int = 10,
    height: Dp = 12.dp,
    gap: Dp = 4.dp,
    radius: Dp = 4.dp,
    fill: Color = SsTheme.colors.primary,
    empty: Color = SsTheme.colors.track,
    /** Segments after the active one (onboarding uses a slightly darker #E3DDD4). */
    future: Color = empty,
    outlineActive: Boolean = true,
    dimmed: Boolean = false,
) {
    val reduce = SsTheme.reduceMotion
    val animatedFraction by animateFloatAsState(
        activeFraction.coerceIn(0f, 1f),
        tween(if (reduce) 0 else SsMotion.SEGMENT_FILL_MS),
        label = "segment",
    )
    val animatedFilled by animateFloatAsState(
        filled.toFloat(),
        tween(if (reduce) 0 else SsMotion.SEGMENT_FILL_MS * (filled.coerceAtLeast(1))),
        label = "segments",
    )
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo((filled + activeFraction) / segments, 0f..1f)
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        val alpha = if (dimmed) 0.6f else 1f
        for (i in 0 until segments) {
            val segFill = (animatedFilled - i).coerceIn(0f, 1f)
            val isActive = i == filled && filled < segments
            val partial = when {
                segFill >= 1f -> 1f
                isActive -> animatedFraction
                else -> segFill
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(radius))
                    .drawBehind {
                        val base = if (i > filled) future else empty
                        drawRect(base.copy(alpha = base.alpha * alpha))
                        if (partial > 0f) drawRect(fill.copy(alpha = alpha), size = Size(size.width * partial, size.height))
                        if (isActive && outlineActive) {
                            val w = 1.dp.toPx()
                            drawRoundRect(
                                fill.copy(alpha = alpha),
                                topLeft = Offset(w / 2, w / 2),
                                size = Size(size.width - w, size.height - w),
                                cornerRadius = CornerRadius(radius.toPx() - w / 2),
                                style = Stroke(w),
                            )
                        }
                    },
            )
        }
    }
}

/** 10 × 5dp mini bar used in the next-floor pill. */
@Composable
fun MiniSegmentBar(filled: Int, modifier: Modifier = Modifier, fill: Color = SsTheme.colors.primary, empty: Color = SsTheme.colors.track) {
    Row(modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(10) { i ->
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (i < filled) fill else empty, RoundedCornerShape(1.dp)),
            )
        }
    }
}

/** Weekly ring: floors this week / goal; the number in the centre is always present. */
@Composable
fun WeeklyRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 7.dp,
    track: Color = SsTheme.colors.track,
    fill: Color = SsTheme.colors.primary,
    /** Ring radius as a fraction of half the size (SVG r=26 in a 64 box = 0.8125). */
    radiusFraction: Float = 0.8125f,
    center: @Composable () -> Unit,
) {
    val reduce = SsTheme.reduceMotion
    val sweep by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(if (reduce) 0 else 600), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val r = this.size.minDimension / 2 * radiusFraction
            val inset = this.size.width / 2 - r
            val arcSize = Size(r * 2, r * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            if (sweep > 0f) {
                drawArc(fill, -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

/** Thin rounded linear bar (architect level, achievements, widget week bar, 4-week rows). */
@Composable
fun SsLinearBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    radius: Dp = 4.dp,
    track: Color = SsTheme.colors.track,
    fill: Color = SsTheme.colors.primary,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track, RoundedCornerShape(radius)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(fill, RoundedCornerShape(radius)),
            )
        }
    }
}
