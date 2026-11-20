package com.idraak.stepskyscraper.feature.skyline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idraak.stepskyscraper.core.designsystem.component.AutoSizeName
import com.idraak.stepskyscraper.core.designsystem.component.NextFloorPill
import com.idraak.stepskyscraper.core.designsystem.component.SegmentBar
import com.idraak.stepskyscraper.core.designsystem.component.SkyHeader
import com.idraak.stepskyscraper.core.designsystem.component.SsCard
import com.idraak.stepskyscraper.core.designsystem.component.SsIconCircleButton
import com.idraak.stepskyscraper.core.designsystem.component.SsMessageState
import com.idraak.stepskyscraper.core.designsystem.component.SsModalSheet
import com.idraak.stepskyscraper.core.designsystem.component.SsOutlinedButton
import com.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import com.idraak.stepskyscraper.core.designsystem.component.SsSnackbar
import com.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import com.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import com.idraak.stepskyscraper.core.designsystem.icon.IconSize
import com.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import com.idraak.stepskyscraper.core.designsystem.icon.Symbol
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import com.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import com.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import com.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineSpec
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.model.Tower
import com.idraak.stepskyscraper.core.ui.R as UiR
import com.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import com.idraak.stepskyscraper.core.ui.component.TowerThumb
import com.idraak.stepskyscraper.core.ui.component.boldText
import com.idraak.stepskyscraper.core.ui.format.Formats
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import com.idraak.stepskyscraper.core.ui.platform.LocalUiHost
import com.idraak.stepskyscraper.core.ui.tower.CosmeticVisuals
import com.idraak.stepskyscraper.core.ui.tower.toSkylineTower
import com.idraak.stepskyscraper.core.ui.tower.toSpec
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class SkylineRoute(val highlight: Int? = null)

@Composable
fun SkylineRoute(onShare: (Int?) -> Unit, onOpenBuild: () -> Unit, viewModel: SkylineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { e ->
            when (e) {
                is SkylineEffect.Share -> onShare(e.towerIndex)
                SkylineEffect.OpenBuild -> onOpenBuild()
            }
        }
    }
    SkylineScreen(state, viewModel::onIntent)
}

@Composable
fun SkylineScreen(state: SkylineUiState, onIntent: (SkylineIntent) -> Unit) {
    val c = SsTheme.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        when (val content = state.content) {
            Lce.Loading -> Unit
            is Lce.Error -> SsMessageState(Symbol.LocationCity, content.message.resolve(), "", Modifier.align(Alignment.Center))
            is Lce.Content -> SkylineContentView(content.value, state, onIntent)
        }
        val message = state.message
        LaunchedEffect(message) {
            if (message != null) {
                delay(SsMotion.SNACKBAR_VISIBLE_MS)
                onIntent(SkylineIntent.MessageShown)
            }
        }
        SsSnackbar(
            message != null,
            message?.resolve() ?: "",
            Modifier.align(Alignment.BottomCenter).padding(SsSpacing.l),
            icon = Symbol.Architecture,
        )
    }
}

@Composable
private fun SkylineContentView(content: SkylineContent, state: SkylineUiState, onIntent: (SkylineIntent) -> Unit) {
    val c = SsTheme.colors
    val tone = CosmeticVisuals.tone(content.sky)
    val chrome = SkyTokens.chrome(tone)
    SystemBarsEffect(chrome.lightStatusBar)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val host = LocalUiHost.current
    val showReward = content.reward != null && !content.reward.premium && !content.reward.claimed
    LaunchedEffect(showReward) { if (showReward) onIntent(SkylineIntent.PrepareAd(host)) }

    Box(Modifier.fillMaxWidth().height(top + SsSpacing.skylineHeroHeight - 28.dp).background(SkyTokens.gradient(tone).brush()))
    Column(Modifier.padding(top = top)) {
        SkyHeader(
            overline = stringResource(R.string.sky_district, content.districtIndex),
            title = content.districtName,
            subtitle = stringResource(
                R.string.sky_district_meta,
                content.inDistrict,
                content.floors,
                content.since?.let(Formats::dayMonth) ?: Formats.dayMonth(content.today),
            ),
            chrome = chrome,
            extra = {
                NextFloorPill(
                    content.stepsToNext, content.segmentsFilled, stringResource(UiR.string.to_next_floor),
                    stringResource(UiR.string.next_floor_in_steps, Formats.steps(content.stepsToNext)),
                    Modifier.padding(top = SsSpacing.s),
                    container = chrome.pillFill, text = chrome.pillText, subtext = chrome.pillSubtext, empty = chrome.pillEmpty,
                )
            },
            actions = {
                SsIconCircleButton(Symbol.IosShare, stringResource(R.string.sky_share), {
                    onIntent(SkylineIntent.ShareSkyline)
                }, container = chrome.buttonFill, tint = chrome.buttonIcon)
            },
        )
    }
    if (content.towers.isEmpty()) {
        Column(Modifier.padding(top = top + 150.dp).fillMaxWidth()) {
            SsMessageState(
                Symbol.Construction,
                stringResource(R.string.sky_empty_title),
                stringResource(
                    R.string.sky_empty_body,
                    content.activeTower.name,
                    content.activeTower.floorsBuilt,
                    content.activeTower.target,
                ),
                action = { SsTonalButton(stringResource(R.string.sky_open_build), { onIntent(SkylineIntent.OpenBuild) }) },
            )
        }
        return
    }
    ZoomableSkyline(content, Modifier.padding(top = top + 122.dp, start = 10.dp, end = 10.dp).fillMaxWidth().height(250.dp))
    Column(
        Modifier
            .padding(top = top + SsSpacing.skylineSheetTop - 28.dp)
            .fillMaxSize()
            .background(c.background, SsRadius.shapeSheet)
            .verticalScroll(rememberScrollState())
            .padding(start = SsSpacing.l, end = SsSpacing.l, top = SsSpacing.l, bottom = SsSpacing.l),
        verticalArrangement = Arrangement.spacedBy(SsSpacing.m),
    ) {
        if (showReward && content.reward != null) {
            RewardCardView(content.reward, state.adBusy) { onIntent(SkylineIntent.WatchAd(host)) }
        } else {
            NextDistrictCard(content)
            NextLotRow(content) { onIntent(SkylineIntent.OpenBuild) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
            content.towers.asReversed().forEach { t ->
                TowerChip(
                    t,
                    t.index == content.newestIndex,
                    t.index == state.selectedTower || (state.selectedTower == null && t.index == content.newestIndex),
                ) {
                    onIntent(SkylineIntent.SelectTower(t.index))
                }
            }
        }
    }
    val selected = content.towers.firstOrNull { it.index == state.selectedTower }
    if (selected != null) {
        StatsSheet(selected, onIntent)
    }
    if (state.renaming && selected != null) RenameDialog(selected.name, onIntent)
}

@Composable
private fun ZoomableSkyline(content: SkylineContent, modifier: Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 3f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    SkylineCanvas(
        SkylineSpec(content.towers.map { it.toSkylineTower() }, content.pinnedIndex, CosmeticVisuals.time(content.sky)),
        stringResource(UiR.string.skyline_description, content.towers.size) + ". " + stringResource(R.string.sky_zoom_hint),
        modifier
            .transformable(transform)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

@Composable
private fun RewardCardView(reward: RewardCard, busy: Boolean, onWatch: () -> Unit) {
    val c = SsTheme.colors
    SsCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.m)) {
            SsIcon(Symbol.Architecture, size = IconSize.l, tint = c.primaryText)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.sky_joined, reward.towerName), style = SsTheme.type.bodyMStrong, color = c.onSurface)
                Text(
                    stringResource(R.string.sky_earned, Formats.steps(reward.blueprints)),
                    style = SsTheme.type.bodyXS,
                    color = c.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.height(SsSpacing.m))
        SsTonalButton(
            stringResource(if (busy || !reward.adAvailable) R.string.sky_ad_loading else R.string.sky_watch_ad),
            onWatch,
            Modifier.fillMaxWidth(),
            icon = Symbol.SmartDisplay,
            enabled = reward.adAvailable && !busy,
        )
        Box(Modifier.height(SsSpacing.m))
        Text(
            stringResource(R.string.sky_ad_note),
            style = SsTheme.type.captionRegular,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NextDistrictCard(content: SkylineContent) {
    val c = SsTheme.colors
    val count = content.inDistrict.coerceAtMost(8)
    SsCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = SsSpacing.l)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.sky_next_district), style = SsTheme.type.bodyMStrong, color = c.onSurface)
            Text(stringResource(R.string.sky_next_district_count, count), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant)
        }
        Box(Modifier.height(10.dp))
        SegmentBar(
            count,
            0f,
            stringResource(R.string.sky_next_district_count, count),
            segments = 8,
            height = 10.dp,
            radius = 3.dp,
            outlineActive = false,
        )
        Box(Modifier.height(10.dp))
        val remaining = 8 - count
        Text(
            if (remaining > 0) {
                pluralStringResource(R.plurals.sky_next_district_body, remaining, remaining, content.districtName)
            } else {
                stringResource(R.string.sky_district_complete, content.districtName)
            },
            style = SsTheme.type.bodyXS,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
private fun NextLotRow(content: SkylineContent, onClick: () -> Unit) {
    val c = SsTheme.colors
    val started = content.activeTower.startedOn ?: content.today
    val whenText = if (started ==
        content.today
    ) {
        stringResource(R.string.sky_today)
    } else {
        stringResource(R.string.sky_on_date, Formats.dayMonth(started))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, SsRadius.shapeCard)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SsSpacing.m),
    ) {
        SsIcon(Symbol.Construction, size = IconSize.ml, tint = c.onSurfaceVariant)
        Text(
            boldText(stringResource(R.string.sky_next_lot, content.activeTower.name, whenText)),
            style = SsTheme.type.bodyS,
            color = c.onSurface,
            modifier = Modifier.weight(1f),
        )
        SsIcon(Symbol.ChevronRight, size = IconSize.ml, tint = c.onSurfaceVariant)
    }
}

@Composable
private fun TowerChip(tower: Tower, isNew: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = SsTheme.colors
    Row(
        Modifier
            .background(c.surface, SsRadius.shapeMd)
            .then(if (selected) Modifier.border(2.dp, c.ink, SsRadius.shapeMd) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TowerThumb(
            tower.toSpec(com.idraak.stepskyscraper.core.domain.usecase.SkyTime.Day, showGround = false),
            tower.name,
            Modifier.size(36.dp, 48.dp),
            tile = androidx.compose.ui.graphics.Color.Transparent,
            padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            pad = 2f,
        )
        Column {
            Text(tower.name.uppercase(), style = SsTheme.type.towerNameChip, color = c.onSurface, maxLines = 1)
            Text(
                if (isNew) {
                    stringResource(
                        R.string.sky_floors_new,
                        tower.floorsBuilt,
                    )
                } else {
                    stringResource(R.string.sky_floors, tower.floorsBuilt)
                },
                style = SsTheme.type.captionRegular,
                color = c.onSurfaceVariant,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StatsSheet(tower: Tower, onIntent: (SkylineIntent) -> Unit) {
    val c = SsTheme.colors
    val kit = CosmeticCatalog.kitOf(tower.record.look.facadeId)
    SsModalSheet(onDismiss = { onIntent(SkylineIntent.CloseSheet) }, skipPartiallyExpanded = false) {
        var drag by remember { mutableFloatStateOf(0f) }
        Column(
            Modifier
                .pointerInput(tower.index) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (drag < -80) {
                                onIntent(SkylineIntent.StepTower(1))
                            } else if (drag > 80) {
                                onIntent(SkylineIntent.StepTower(-1))
                            }
                            drag = 0f
                        },
                    ) { _, amount -> drag += amount }
                }
                .padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, top = 6.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TowerThumb(
                    tower.toSpec(com.idraak.stepskyscraper.core.domain.usecase.SkyTime.Day, showGround = false),
                    tower.name,
                    Modifier.size(64.dp, 84.dp),
                    radius = SsRadius.tile,
                    pad = 2f,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.stats_overline, tower.index, stringResource(CosmeticVisuals.kitName(kit))).uppercase(),
                        style = SsTheme.type.overline,
                        color = c.onSurfaceVariant,
                    )
                    AutoSizeName(tower.name, SsTheme.type.towerNameSheet, c.onSurface)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                    StatTile(stringResource(R.string.stats_floors), tower.floorsBuilt.toString(), true, Modifier.weight(1f))
                    StatTile(stringResource(R.string.stats_build_days), tower.buildDays.toString(), true, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                    StatTile(
                        stringResource(R.string.stats_ground_broken),
                        tower.startedOn?.let(Formats::fullDate) ?: stringResource(R.string.stats_none),
                        false,
                        Modifier.weight(1f),
                    )
                    StatTile(
                        stringResource(R.string.stats_topped_out),
                        tower.toppedOutOn?.let(Formats::fullDate) ?: stringResource(R.string.stats_none),
                        false,
                        Modifier.weight(1f),
                    )
                }
            }
            Column {
                StatRow(stringResource(R.string.stats_steps), Formats.steps(tower.steps), true)
                StatRow(
                    stringResource(R.string.stats_best_day),
                    tower.bestDay?.let { stringResource(R.string.stats_best_day_value, Formats.weekdayDayMonth(it.date), it.floors) }
                        ?: stringResource(R.string.stats_none),
                    true,
                )
                val crown = CosmeticCatalog.item(tower.record.look.crownId)?.name.orEmpty()
                val palette = CosmeticCatalog.item(tower.record.look.paletteId)?.name.orEmpty()
                StatRow(stringResource(R.string.stats_look), "$crown · $palette", false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SsSpacing.s)) {
                SsTonalButton(stringResource(R.string.stats_share), {
                    onIntent(SkylineIntent.ShareTower(tower.index))
                }, Modifier.weight(1f), icon = Symbol.IosShare)
                SsOutlinedButton(
                    stringResource(R.string.stats_rename),
                    { onIntent(SkylineIntent.StartRename) },
                    Modifier.weight(1f),
                    icon = Symbol.Edit,
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, number: Boolean, modifier: Modifier) {
    val c = SsTheme.colors
    Column(
        modifier.background(c.background, SsRadius.shapeMd).padding(horizontal = 14.dp, vertical = SsSpacing.m),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = SsTheme.type.captionRegular, color = c.onSurfaceVariant)
        Text(value, style = if (number) SsTheme.type.numberTile else SsTheme.type.bodyStrong, color = c.onSurface)
    }
}

@Composable
private fun StatRow(label: String, value: String, divider: Boolean) {
    val c = SsTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SsTheme.type.bodyS, color = c.onSurfaceVariant)
        Text(value, style = SsTheme.type.bodySStrong, color = c.onSurface, maxLines = 1)
    }
    if (divider) HorizontalDivider(thickness = 1.dp, color = c.divider)
}

@Composable
private fun RenameDialog(current: String, onIntent: (SkylineIntent) -> Unit) {
    val c = SsTheme.colors
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = { onIntent(SkylineIntent.CancelRename) },
        containerColor = c.surface,
        shape = SsRadius.shapeLg,
        title = { Text(stringResource(R.string.stats_rename_title), style = SsTheme.type.sheetTitle, color = c.onSurface) },
        text = {
            BasicTextField(
                name.uppercase(),
                { name = it.take(18) },
                singleLine = true,
                textStyle = SsTheme.type.towerNameField.copy(color = c.onSurface),
                cursorBrush = SolidColor(c.onSurface),
                modifier = Modifier.fillMaxWidth().border(2.dp, c.ink, SsRadius.shapeMd).padding(SsSpacing.l),
            )
        },
        confirmButton = {
            SsPrimaryButton(stringResource(R.string.stats_save), {
                onIntent(SkylineIntent.Rename(name))
            }, enabled = name.isNotBlank(), large = false)
        },
        dismissButton = { SsTextButton(stringResource(R.string.stats_cancel), { onIntent(SkylineIntent.CancelRename) }) },
    )
}

internal fun previewSkyline(withReward: Boolean, sky: com.idraak.stepskyscraper.core.domain.usecase.SkyTime): SkylineContent {
    val start = java.time.LocalDate.of(2026, 8, 14)
    val kits = listOf(
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Brut to 18,
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Eco to 20,
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Glass to 22,
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Brick to 16,
        com.idraak.stepskyscraper.core.domain.model.FacadeKit.Deco to 24,
    )
    val names = listOf("Quarry Works", "Alder House", "Calder Glass", "Harbour Row", "Marlow Spire")
    val towers = kits.mapIndexed { i, (kit, floors) ->
        Tower(
            com.idraak.stepskyscraper.core.domain.model.TowerRecord(
                i + 1,
                names[i],
                com.idraak.stepskyscraper.core.domain.usecase.CityCalculator.defaultLook(kit),
                floors,
                start,
                true,
                false,
            ),
            floors,
            start.plusDays(i * 20L),
            start.plusDays(i * 20L + 8),
            8,
            floors * 1_040L,
            null,
        )
    }
    val today = java.time.LocalDate.of(2026, 11, 20)
    return SkylineContent(
        1, "Juniper Quarter", towers, start, 4, if (withReward) 5 else null,
        if (withReward) RewardCard(5, "Marlow Spire", 120, adAvailable = true, claimed = false, premium = false) else null,
        towers.last().copy(record = towers.last().record.copy(index = 6, name = "Juniper Works"), floorsBuilt = 0), 340, 6, sky, today,
    )
}

@com.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
@Composable
private fun SkylineNewTowerPreview() = com.idraak.stepskyscraper.core.ui.preview.PreviewSurface {
    SkylineScreen(SkylineUiState(Lce.Content(previewSkyline(true, com.idraak.stepskyscraper.core.domain.usecase.SkyTime.Day))), {})
}

@com.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
@Composable
private fun SkylineNightPreview() = com.idraak.stepskyscraper.core.ui.preview.PreviewSurface(dark = true) {
    SkylineScreen(SkylineUiState(Lce.Content(previewSkyline(false, com.idraak.stepskyscraper.core.domain.usecase.SkyTime.Night))), {})
}
