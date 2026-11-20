package ai.idraak.stepskyscraper.gallery

import ai.idraak.stepskyscraper.core.designsystem.component.AchievementBadge
import ai.idraak.stepskyscraper.core.designsystem.component.NavItem
import ai.idraak.stepskyscraper.core.designsystem.component.NextFloorPill
import ai.idraak.stepskyscraper.core.designsystem.component.PageDots
import ai.idraak.stepskyscraper.core.designsystem.component.SegmentBar
import ai.idraak.stepskyscraper.core.designsystem.component.SheetHandle
import ai.idraak.stepskyscraper.core.designsystem.component.SsBottomBar
import ai.idraak.stepskyscraper.core.designsystem.component.SsCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsFilterChip
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconCircleButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsOutlinedButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsRadioCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsSegmentedToggle
import ai.idraak.stepskyscraper.core.designsystem.component.SsSettingsRow
import ai.idraak.stepskyscraper.core.designsystem.component.SsSnackbar
import ai.idraak.stepskyscraper.core.designsystem.component.SsStatusChip
import ai.idraak.stepskyscraper.core.designsystem.component.SsSwitch
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import ai.idraak.stepskyscraper.core.designsystem.component.StatusChipKind
import ai.idraak.stepskyscraper.core.designsystem.component.WeeklyRing
import ai.idraak.stepskyscraper.core.designsystem.component.ssShadow
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTone
import ai.idraak.stepskyscraper.core.designsystem.theme.SsElevation
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.Facade
import ai.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
data object GalleryRoute

/**
 * Component gallery mirroring the design-system board, for side-by-side comparison with the design.
 * Reached from Settings in debug builds; also rendered by the gallery screenshot test (light and dark).
 */
@Composable
fun ComponentGallery(onBack: () -> Unit) {
    val c = SsTheme.colors
    Column(
        Modifier.fillMaxSize().background(
            c.background,
        ).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SsIconButton(Symbol.ArrowBack, "Back", onBack)
            Text("DESIGN SYSTEM", style = SsTheme.type.displayTitle, color = c.onSurface)
        }
        Section("Colour") { ColourGrid() }
        Section("Sky follows the clock") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SkyTone.Dawn to TowerTime.Dawn,
                    SkyTone.Day to TowerTime.Day,
                    SkyTone.Dusk to TowerTime.Dusk,
                    SkyTone.Night to TowerTime.Night,
                ).forEach { (tone, time) ->
                    Box(
                        Modifier.weight(
                            1f,
                        ).height(130.dp).background(SkyTokens.gradient(tone).brush(), SsRadius.shapeCard).padding(8.dp, 8.dp, 8.dp, 0.dp),
                    ) {
                        TowerCanvas(TowerSpec(floors = 12, target = 12, time = time), tone.name, Modifier.fillMaxSize())
                    }
                }
            }
        }
        Section("Facade kits") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Facade.entries.forEach {
                    Box(Modifier.weight(1f).height(150.dp).background(c.surfaceVariant, SsRadius.shapeCard).padding(6.dp)) {
                        TowerCanvas(
                            TowerSpec(
                                it,
                                14,
                                14,
                                ground = if (it ==
                                    Facade.Eco
                                ) {
                                    GroundStyle.Park
                                } else {
                                    GroundStyle.Plaza
                                },
                            ),
                            it.name,
                            Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        Section("Type scale") {
            val t = SsTheme.type
            listOf<Pair<String, TextStyle>>(
                "340" to t.numberXL,
                "6,660" to t.numberL,
                "MARLOW SPIRE" to t.towerName,
                "Every 1,000 steps builds a floor" to t.headline,
                "Next floor in 340 steps" to t.title,
                "We read your step count only." to t.body,
                "Customise" to t.label,
                "Updated 12 min ago" to t.caption,
            ).forEach { (text, style) -> Text(text, style = style, color = c.onSurface) }
        }
        Section("Buttons") {
            SsPrimaryButton("Start next tower", {}, icon = Symbol.Add, large = false)
            SsTonalButton("Customise · tonal", {}, icon = Symbol.Palette)
            SsOutlinedButton("Outlined", {})
            SsTextButton("Text button", {})
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SsIconCircleButton(Symbol.IosShare, "Share", {})
                SsPrimaryButton("Disabled", {}, enabled = false, large = false)
            }
        }
        Section("Chips") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SsFilterChip("Facade", true, {})
                SsFilterChip("Palette", false, {})
                SsFilterChip("Crown", false, {})
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SsStatusChip("Build 200 floors", StatusChipKind.Lock)
                SsStatusChip("Architect Pass", StatusChipKind.Pass)
                SsStatusChip("Updated 12 min ago", StatusChipKind.Sync)
                SsStatusChip("Target met", StatusChipKind.TargetMet)
            }
        }
        Section("Snackbar") { SsSnackbar(true, "Synced. 2 floors added to Marlow Spire.", actionLabel = "View") }
        Section("Progress") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                WeeklyRing(0.7f, size = 96.dp, stroke = 10.dp, radiusFraction = 40f / 48f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("35", style = SsTheme.type.ringCenterL, color = c.onSurface)
                        Text("of 50", style = SsTheme.type.micro, color = c.onSurfaceVariant)
                    }
                }
                NextFloorPill(340, 6, "to next floor", "Next floor in 340 steps")
            }
            Text("Floor progress · 660 / 1,000", style = SsTheme.type.labelSBold, color = c.onSurface)
            SegmentBar(6, 0.6f, "Floor 15: 660 of 1,000 steps", height = 14.dp)
            PageDots(4, 1, "Page 2 of 4")
        }
        Section("Cards") {
            SsCard(color = c.background) {
                Text("Build Days", style = SsTheme.type.labelS, color = c.onSurfaceVariant)
                Text("38", style = SsTheme.type.numberL, color = c.onSurface)
                Text("Days with at least one floor", style = SsTheme.type.bodyXS, color = c.onSurfaceVariant)
            }
            SsRadioCard("Steady", "7,000 a day · 49 floors a week", true, {})
            SsSettingsRow(
                Symbol.PauseCircle,
                "Pause goals",
                "For rest, travel or illness. Floors still count.",
                {},
                trailing = { SsSwitch(false) },
            )
            var month by remember { mutableStateOf(0) }
            SsSegmentedToggle(listOf("Week", "Month"), month, { month = it }, large = false)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AchievementBadge(Symbol.Foundation, earned = true)
                AchievementBadge(Symbol.DarkMode, earned = false)
            }
        }
        Section("Bottom sheet") {
            Column(
                Modifier.fillMaxWidth().ssShadow(
                    SsElevation.level2,
                    SsRadius.shapeSheet,
                ).background(c.surface, SsRadius.shapeSheet).padding(20.dp, 10.dp, 20.dp, 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SheetHandle(Modifier.align(Alignment.CenterHorizontally))
                Text("Customise Marlow Spire", style = SsTheme.type.sheetTitle, color = c.onSurface)
            }
        }
        Section("Bottom navigation") {
            SsBottomBar(
                listOf(
                    NavItem("build", "Build", Symbol.Apartment),
                    NavItem("skyline", "Skyline", Symbol.LocationCity),
                    NavItem("me", "Me", Symbol.Person),
                ),
                "build",
                {},
            )
        }
        Section("Iconography") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Symbol.Apartment, Symbol.LocationCity, Symbol.Person, Symbol.DirectionsWalk, Symbol.Palette, Symbol.IosShare, Symbol.Lock, Symbol.Architecture,
                    Symbol.Sync, Symbol.PauseCircle, Symbol.EmojiEvents, Symbol.Widgets, Symbol.BatterySaver, Symbol.Favorite, Symbol.Settings, Symbol.WorkspacePremium,
                ).forEach { s ->
                    Box(
                        Modifier.size(44.dp).background(c.background, SsRadius.shapeSm),
                        contentAlignment = Alignment.Center,
                    ) { SsIcon(s) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(SsTheme.colors.surface, SsRadius.shapeLg).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = SsTheme.type.titleBold, color = SsTheme.colors.onSurface)
        content()
    }
}

@Composable
private fun ColourGrid() {
    val c = SsTheme.colors
    val swatches = listOf(
        "background" to c.background, "surface" to c.surface, "surfaceVariant" to c.surfaceVariant,
        "secondaryContainer" to c.secondaryContainer,
        "onSurface" to c.onSurface, "onSurfaceVariant" to c.onSurfaceVariant, "outline" to c.outline, "info" to c.info,
        "primary" to c.primary, "onPrimary" to c.onPrimary, "primaryContainer" to c.primaryContainer, "primaryText" to c.primaryText,
    )
    swatches.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (name, color) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(
                            44.dp,
                        ).background(color, SsRadius.shapeMd).border(1.dp, c.outlineCard, SsRadius.shapeMd),
                    )
                    Text(name, style = SsTheme.type.micro, color = c.onSurface, maxLines = 1)
                }
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 2600, showBackground = true)
@Composable
private fun GalleryLightPreview() = SsTheme(darkTheme = false) { ComponentGallery {} }

@Preview(widthDp = 400, heightDp = 2600, showBackground = true)
@Composable
private fun GalleryDarkPreview() = SsTheme(darkTheme = true) { ComponentGallery {} }
