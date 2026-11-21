package ai.idraak.stepskyscraper.core.designsystem.component

import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Immutable
data class NavItem(val key: String, val label: String, val symbol: Symbol)

/**
 * Bottom navigation (SSNav): 3 destinations, active = neutral 64 × 32 pill + filled icon + bold label.
 * Amber stays reserved for progress. The system gesture inset sits below the 80dp bar.
 */
@Composable
fun SsBottomBar(items: List<NavItem>, selectedKey: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = SsTheme.colors
    Column(modifier.fillMaxWidth().background(c.navBar)) {
        HorizontalDivider(thickness = 1.dp, color = c.navLine)
        Row(
            Modifier
                .fillMaxWidth()
                .height(SsSpacing.navHeight)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                // The whole column is the touch target, but the press ripple draws only inside the 64 × 32
                // capsule (as SSNav's pill), never as a rectangle over the full 80dp cell.
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = SsSpacing.touchTarget)
                        .selectable(selected, interaction, indication = null, role = Role.Tab) { onSelect(item.key) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SsSpacing.xs, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier
                            .size(SsSpacing.navPillWidth, SsSpacing.navPillHeight)
                            .clip(SsRadius.shapeFull)
                            .background(if (selected) c.navPill else androidx.compose.ui.graphics.Color.Transparent)
                            .indication(interaction, ripple()),
                        contentAlignment = Alignment.Center,
                    ) {
                        SsIcon(item.symbol, size = IconSize.l, tint = if (selected) c.onSurface else c.onSurfaceVariant, filled = selected)
                    }
                    Text(
                        item.label,
                        style = if (selected) SsTheme.type.navActive else SsTheme.type.nav,
                        color = if (selected) c.onSurface else c.onSurfaceVariant,
                    )
                }
            }
        }
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/** Snackbar: ink #2E2A26, amber icon + action; slides up 220ms, auto-dismissed after 4s by the host. */
@Composable
fun SsSnackbar(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    icon: Symbol = Symbol.Apartment,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val reduce = SsTheme.reduceMotion
    val duration = if (reduce) SsMotion.CROSSFADE_MS else SsMotion.SNACKBAR_SLIDE_MS
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduce) fadeIn(tween(duration)) else slideInVertically(tween(duration)) { it } + fadeIn(tween(duration)),
        exit = if (reduce) fadeOut(tween(duration)) else slideOutVertically(tween(duration)) { it } + fadeOut(tween(duration)),
    ) {
        val c = SsTheme.colors
        Row(
            Modifier
                .fillMaxWidth()
                .ssShadow(SsElevation.level2, SsRadius.shapeMd)
                .background(c.snackbar, SsRadius.shapeMd)
                .padding(start = SsSpacing.l, end = SsSpacing.s, top = SsSpacing.m, bottom = SsSpacing.m)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SsSpacing.m),
        ) {
            SsIcon(icon, size = IconSize.m, tint = c.snackbarAction)
            Text(message, style = SsTheme.type.bodyS, color = c.onSnackbar, modifier = Modifier.weight(1f))
            if (actionLabel != null) {
                Box(
                    Modifier
                        .heightIn(min = SsSpacing.touchTarget)
                        .clip(SsRadius.shapeSm)
                        .selectable(false, role = Role.Button, onClick = onAction)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(actionLabel, style = SsTheme.type.bodySStrong, color = c.snackbarAction)
                }
            }
        }
    }
}
