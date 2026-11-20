package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** CSS box-shadow layers (elevation 1 / 2) rendered with Compose drop shadows. */
fun Modifier.ssShadow(layers: List<SsElevation.ShadowLayer>, shape: Shape): Modifier =
    layers.fold(this) { m, l -> m.dropShadow(shape, Shadow(radius = l.blur, color = l.color, offset = DpOffset(0.dp, l.offsetY))) }

/** 20dp card on surface (the white cards on every screen). */
@Composable
fun SsCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = SsSpacing.cardPadding, vertical = 18.dp),
    color: Color = SsTheme.colors.surface,
    shape: Shape = SsRadius.shapeCard,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(color, shape)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Drag handle 32 × 4, #CFC7BC. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(SsSpacing.handleWidth, SsSpacing.handleHeight)
            .background(SsTheme.colors.outlineStrong, RoundedCornerShape(2.dp)),
    )
}

/** Page dots: active 20 × 6 ink, inactive 6 × 6 #CFC7BC. */
@Composable
fun PageDots(count: Int, active: Int, contentDescription: String, modifier: Modifier = Modifier) {
    Row(
        modifier.then(Modifier.semanticsLabel(contentDescription)),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == active) 20.dp else 6.dp, 6.dp)
                    .background(if (i == active) SsTheme.colors.ink else SsTheme.colors.outlineStrong, RoundedCornerShape(3.dp)),
            )
        }
    }
}

/**
 * Radio option card (daily target, paywall plans): 1dp #E2DBD1 ring, selected = 2dp ink ring + filled radio.
 */
@Composable
fun SsRadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: Symbol? = null,
    trailing: @Composable (() -> Unit)? = null,
    radius: Dp = SsRadius.card,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l),
    subtitleColor: Color = SsTheme.colors.onSurfaceVariant,
    titleStyle: androidx.compose.ui.text.TextStyle = SsTheme.type.bodyStrong,
    subtitleStyle: androidx.compose.ui.text.TextStyle = SsTheme.type.bodyXS,
) {
    val c = SsTheme.colors
    val shape = RoundedCornerShape(radius)
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.ink else c.outlineCard, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val icon = leading ?: if (selected) Symbol.RadioButtonChecked else Symbol.RadioButtonUnchecked
        SsIcon(
            icon,
            size = if (radius ==
                SsRadius.card
            ) {
                IconSize.l
            } else {
                IconSize.ml
            },
            tint = if (selected) c.onSurface else c.onSurfaceVariant,
            filled =
            selected && leading == null,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = titleStyle, color = c.onSurface)
            Text(subtitle, style = subtitleStyle, color = subtitleColor)
        }
        trailing?.invoke()
    }
}

/** Two-option segmented control on #ECE7E0 (Week / Month, Tower / Skyline, Story / Square). */
@Composable
fun SsSegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = true,
) {
    val c = SsTheme.colors
    val outer = if (large) SsRadius.lg else 18.dp
    Row(
        modifier
            .background(c.surfaceVariant, RoundedCornerShape(outer))
            .padding(if (large) SsSpacing.xs else 3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Row(
                Modifier
                    .then(if (large) Modifier.weight(1f) else Modifier)
                    .height(if (large) 40.dp else 30.dp)
                    .background(if (isSelected) c.surface else Color.Transparent, SsRadius.shapeFull)
                    .selectable(isSelected, role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isSelected && large) {
                    SsIcon(Symbol.Check, size = IconSize.sm, tint = c.onSurface)
                    Box(Modifier.width(6.dp))
                }
                Text(
                    label,
                    style = when {
                        large && isSelected -> SsTheme.type.bodySStrong
                        large -> SsTheme.type.bodyS
                        isSelected -> SsTheme.type.labelSBold
                        else -> SsTheme.type.bodyXS
                    },
                    color = if (isSelected || large) c.onSurface else c.bodySecondary,
                )
            }
        }
    }
}

/** 52 × 32 switch. Off = outline ring + grey knob; on = ink track + white knob with a check. */
@Composable
fun SsSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = SsTheme.colors
    Box(
        modifier
            .size(52.dp, 32.dp)
            .background(if (checked) c.ink else c.surfaceVariant, SsRadius.shapeFull)
            .then(if (!checked) Modifier.border(2.dp, c.disabledContent, SsRadius.shapeFull) else Modifier),
    ) {
        if (checked) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SsSpacing.xs)
                    .size(24.dp)
                    .background(c.onInk, SsRadius.shapeFull),
                contentAlignment = Alignment.Center,
            ) { SsIcon(Symbol.Check, size = IconSize.s, tint = c.ink) }
        } else {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = SsSpacing.s)
                    .size(16.dp)
                    .background(c.disabledContent, SsRadius.shapeFull),
            )
        }
    }
}

/** A labelled row with a switch (whole row toggles, 48dp min). */
@Composable
fun SsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SsSpacing.touchTarget)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SsTheme.type.bodyM, color = SsTheme.colors.onSurface, modifier = Modifier.weight(1f))
        SsSwitch(checked)
    }
}

/** 60dp settings row with icon, title, subtitle and a chevron or a trailing control. */
@Composable
fun SsSettingsRow(
    icon: Symbol,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = SsTheme.colors
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SsIcon(icon, size = IconSize.ml, tint = c.onSurface)
            Column(Modifier.weight(1f).padding(vertical = SsSpacing.s)) {
                Text(title, style = SsTheme.type.bodyM, color = c.onSurface)
                Text(subtitle, style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
            }
            if (trailing != null) trailing() else SsIcon(Symbol.ChevronRight, size = IconSize.ml, tint = c.onSurfaceVariant)
        }
        if (showDivider) HorizontalDivider(Modifier.padding(horizontal = 18.dp), thickness = 1.dp, color = c.divider)
    }
}

/** Section label ("IN PROGRESS", "EARNED"). */
@Composable
fun SsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = SsTheme.type.sectionLabel, color = SsTheme.colors.onSurfaceVariant, modifier = modifier)
}

internal fun Modifier.semanticsLabel(label: String): Modifier = semantics(mergeDescendants = true) { contentDescription = label }
