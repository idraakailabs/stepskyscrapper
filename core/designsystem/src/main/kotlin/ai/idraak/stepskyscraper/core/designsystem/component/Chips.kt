package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Filter chip: selected = filled ink + check icon (never colour alone); unselected = 1dp #CFC7BC outline.
 * Visual height 36dp (40dp for ground chips); the touch area is padded to 48dp.
 */
@Composable
fun SsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = SsSpacing.chipHeight,
    leading: Symbol? = null,
    leadingFilled: Boolean = false,
    container: Color? = null,
    content: Color? = null,
) {
    val c = SsTheme.colors
    val bg = container ?: if (selected) c.ink else Color.Transparent
    val fg = content ?: if (selected) c.onInk else c.onSurface
    val shape = SsRadius.shapeFull
    Row(
        modifier
            .heightIn(min = SsSpacing.touchTarget)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .height(height)
                .then(if (!selected && container == null) Modifier.border(1.dp, c.outlineStrong, shape) else Modifier)
                .background(bg, shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val icon = if (selected && leading == null) Symbol.Check else leading
            if (icon != null) SsIcon(icon, size = IconSize.sm, tint = fg, filled = leadingFilled)
            Text(label, style = if (selected) SsTheme.type.label else SsTheme.type.chip, color = fg, maxLines = 1)
        }
    }
}

enum class StatusChipKind { Lock, Pass, Sync, TargetMet, Neutral }

/** 32dp status chips from the "Chips" board. */
@Composable
fun SsStatusChip(
    label: String,
    kind: StatusChipKind,
    modifier: Modifier = Modifier,
    spinning: Boolean = false,
    pop: Boolean = false,
) {
    val c = SsTheme.colors
    val (bg, fg, icon) = when (kind) {
        StatusChipKind.Lock -> Triple(c.surfaceVariant, c.bodySecondary, Symbol.Lock)
        StatusChipKind.Pass -> Triple(SsTheme.brand.passSurface, SsTheme.brand.passAccent, Symbol.WorkspacePremium)
        StatusChipKind.Sync -> Triple(c.infoContainer, c.onInfoContainer, Symbol.Sync)
        StatusChipKind.TargetMet -> Triple(c.primaryContainer, c.onPrimaryContainer, Symbol.CheckCircle)
        StatusChipKind.Neutral -> Triple(c.surfaceVariant, c.onSurface, null)
    }
    val scale = remember { Animatable(if (pop) 0.9f else 1f) }
    val reduce = SsTheme.reduceMotion
    LaunchedEffect(pop) {
        if (pop && !reduce) scale.animateTo(1f, tween(SsMotion.CHIP_POP_MS)) else scale.snapTo(1f)
    }
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .height(SsSpacing.statusChipHeight)
            .background(bg, SsRadius.shapeFull)
            .padding(horizontal = SsSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SsSpacing.xs),
    ) {
        if (icon != null) {
            val rotation = if (spinning && !reduce) {
                val t = rememberInfiniteTransition(label = "sync")
                val r by t.animateFloat(
                    0f,
                    360f,
                    infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
                    label = "spin",
                )
                r
            } else {
                0f
            }
            SsIcon(
                icon,
                size = IconSize.s,
                tint = fg,
                filled = kind == StatusChipKind.Pass || kind == StatusChipKind.TargetMet,
                modifier = Modifier.rotate(rotation),
            )
        }
        Text(
            label,
            style = if (kind == StatusChipKind.TargetMet) SsTheme.type.labelSBold else SsTheme.type.labelS,
            color = fg,
            maxLines = 1,
        )
    }
}

/** Small rounded badge ("Suggested", "Save 58%"). */
@Composable
fun SsBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = SsTheme.type.captionBold,
        color = SsTheme.colors.onSurface,
        modifier = modifier
            .background(SsTheme.colors.surfaceVariant, SsRadius.shapeSm)
            .padding(horizontal = 10.dp, vertical = SsSpacing.xs),
        maxLines = 1,
    )
}
